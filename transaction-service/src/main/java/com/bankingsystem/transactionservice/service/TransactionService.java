package com.bankingsystem.transactionservice.service;

import com.bankingsystem.transactionservice.client.AccountServiceClient;
import com.bankingsystem.transactionservice.dto.TransactionResponse;
import com.bankingsystem.transactionservice.dto.TransferRequest;
import com.bankingsystem.transactionservice.entity.Transaction;
import com.bankingsystem.transactionservice.entity.TransactionStatus;
import com.bankingsystem.transactionservice.entity.TransactionType;
import com.bankingsystem.transactionservice.event.TransactionCompletedEvent;
import com.bankingsystem.transactionservice.event.TransactionInitiatedEvent;
import com.bankingsystem.transactionservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService  {
    private final TransactionRepository transactionRepository;
    private final AccountServiceClient accountServiceClient;
   private final KafkaTemplate<String, Object> kafkaTemplate;
   private final RedisTemplate<String, String> redisTemplate;
    private static final String Transaction_INITIATED_TOPIC="transaction-initiated";
    private static final String Transaction_COMPLETED_TOPIC="transaction-completed";
    private static final String Transaction_REFUNDED_TOPIC="transaction-refunded";
   private static final String FRAUD_DETECTED="fraud.detected";
    /**
     * SAGA STEP 1
     * Deducts from sender via feign
     * saves transaction as processing
     * publish event to kafka for fraud check
     * Returns
     * @paramrequest
     * @return
     */

    public TransactionResponse transfer (TransferRequest request)
    {
        accountServiceClient.deductBalance(
                request.getSenderAccountNumber(),request.getAmount());

        Transaction transaction = new Transaction();
        transaction.setSenderAccountNumber(request.getSenderAccountNumber());
        transaction.setAmount(request.getAmount());
        transaction.setReceiverAccountNumber(request.getRecipientAccountNumber());
        transaction.setStatus(TransactionStatus.PROCESSING);
        transaction.setType(TransactionType.TRANSFER);
        transaction.setDescription(request.getDescription());
        transaction.setRefernceNumber(UUID.randomUUID().toString());

        Transaction savedTransaction = transactionRepository.save(transaction);

        log.info("Transaction saved as Processing:{}",savedTransaction.getTransactionId());
        TransactionInitiatedEvent event=new TransactionInitiatedEvent();
        event.setTransactionId(savedTransaction.getTransactionId());
        event.setSenderAccountNumber(savedTransaction.getSenderAccountNumber());
        event.setReceiverAccountNumber(savedTransaction.getReceiverAccountNumber());
        event.setAmount(savedTransaction.getAmount());
        event.setDescription(savedTransaction.getDescription());

        kafkaTemplate.send(Transaction_INITIATED_TOPIC,savedTransaction.getTransactionId(),event);
        log.info("Transaction saved as Initiated:{}",savedTransaction.getTransactionId());

        return maptoResponse(savedTransaction);
    }

    public TransactionResponse verifyOTP(String transactionId,String otp)
    {
        log.info("OTP verification for the transaction : {}",transactionId);

        Transaction transaction = transactionRepository.findById(transactionId).orElseThrow(()-> new RuntimeException("Transaction not found"+ transactionId));

        String otpKey = "verification:otp" + transactionId;
        String storedOtp = redisTemplate.opsForValue().get(otpKey);

        if(storedOtp==null)
        {
            log.warn("OTP expired for the transaction : {}",transactionId);
            compensateTransaction(transaction,"OTP expirred - transaction cancelled and amount refunded");

            return maptoResponse(transaction);

        }
        if(!storedOtp.equals(otp))
        {
            log.warn("Wrong OTP - blocking the account and refunding the amount:{}",transactionId);
            redisTemplate.delete(otpKey);
            blockAccountAndCompensate(transaction,"Wrong OTP entered - transaction cancelled, "+
                    "account blocked for security");
            return maptoResponse(transaction);
        }

        log.info("OTP verified successfully for the transaction : {}",transactionId);
        redisTemplate.delete(otpKey);
        completeTransaction(transaction);
        return maptoResponse(transaction);


    }
    private void blockAccountAndCompensate(Transaction transaction,String reason)
    {
        Map<String,Object> fraudEvent = new HashMap<>();

        fraudEvent.put("transactionId",transaction.getTransactionId());
        fraudEvent.put("senderAccountNumber",transaction.getSenderAccountNumber());
        fraudEvent.put("reason",reason);

        kafkaTemplate.send(FRAUD_DETECTED,transaction.getSenderAccountNumber(),fraudEvent);
        log.warn("Fraud.detected published-account:{} will be blocked",transaction.getSenderAccountNumber());

        // SAGA COMPENSATION - refind sender
        compensateTransaction(transaction,reason);


    }

    private void  completeTransaction(Transaction transaction)
    {
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setCompletionDate(LocalDateTime.now());
        transactionRepository.save(transaction);
        TransactionCompletedEvent  event = new TransactionCompletedEvent();
        event.setTransactionId(transaction.getTransactionId());
        event.setSenderAccountNumber(transaction.getSenderAccountNumber());
        event.setReceiverAccountNumber(transaction.getReceiverAccountNumber());
        event.setAmount(transaction.getAmount());
        event.setDescription(transaction.getDescription());
        kafkaTemplate.send(Transaction_COMPLETED_TOPIC,transaction.getTransactionId(),event);
        log.info("Transaction saved as Completed:{}",transaction.getTransactionId());
        // SAGA COMPLETED

    }
    private void compensateTransaction(Transaction transaction,String message)
    {
        log.warn("SAGA COMPENSATION - REFUNDING: {} AMOUNT: {}",
                transaction.getAmount(),transaction.getSenderAccountNumber());


        accountServiceClient.creditBalance(transaction.getSenderAccountNumber(),transaction.getAmount());
        transaction.setStatus(TransactionStatus.FLAGGED);
        transaction.setFailureResaon(message+"- SAGA Compensation executed");
        transactionRepository.save(transaction);

        Map<String,Object> refundEvent = new HashMap<>();

        refundEvent.put("transactionId",transaction.getTransactionId());
        refundEvent.put("senderAccountNumber",transaction.getSenderAccountNumber());
        refundEvent.put("amount",transaction.getAmount());
         refundEvent.put("reason",message);

         kafkaTemplate.send(Transaction_REFUNDED_TOPIC,refundEvent);

         log.info("SAGA COMPENSATION COMPLETED - REFUNDED AMOUNT TO ",transaction.getAmount(),transaction.getSenderAccountNumber());

    }

    public void processCleanResult(String transactionId)
    {
        Transaction transaction = transactionRepository.findById(transactionId).orElseThrow(()-> new RuntimeException("Transaction not found"+ transactionId));
        if(transaction.getStatus()!= TransactionStatus.PROCESSING)
        {
            log.warn("Transaction {} not PROCESSING - skipping", transactionId);
            return;

        }

        completeTransaction(transaction);
        


    }

    public TransactionResponse getTransaction(String transactionId)
    {
        return maptoResponse(transactionRepository.findById(transactionId).orElseThrow(()-> new RuntimeException("Transaction not found:"+transactionId)));
    }

    public List<TransactionResponse> getTransactionHistory(String accountNumber)
    {
        return transactionRepository.findBySenderAccountNumberOrderByTransactionDateDesc(accountNumber)
                .stream()
                .map(this::maptoResponse)
                .collect(Collectors.toList());
    }






    private TransactionResponse maptoResponse(Transaction savedTransaction)
    {
        TransactionResponse response = new TransactionResponse();
        response.setTransactionId(savedTransaction.getTransactionId());
        response.setSenderAccountNumber(savedTransaction.getSenderAccountNumber());
        response.setAmount(savedTransaction.getAmount());
        response.setDescription(savedTransaction.getDescription());
        response.setRefernceNumber(savedTransaction.getRefernceNumber());
        response.setStatus(savedTransaction.getStatus());
        response.setType(savedTransaction.getType());
        response.setFailureResaon(savedTransaction.getFailureResaon());
        response.setReceiverAccountNumber(savedTransaction.getReceiverAccountNumber());
        response.setCompletionDate(savedTransaction.getCompletionDate());
        response.setTransactionDate(savedTransaction.getTransactionDate());
        return response;
    }






}
