package com.bankingsystem.transactionservice.service;

import com.bankingsystem.transactionservice.client.AccountServiceClient;
import com.bankingsystem.transactionservice.dto.TransactionResponse;
import com.bankingsystem.transactionservice.dto.TransferRequest;
import com.bankingsystem.transactionservice.entity.Transaction;
import com.bankingsystem.transactionservice.entity.TransactionStatus;
import com.bankingsystem.transactionservice.entity.TransactionType;
import com.bankingsystem.transactionservice.event.TransactionInitiatedEvent;
import com.bankingsystem.transactionservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService  {
    private final TransactionRepository transactionRepository;
    private final AccountServiceClient accountServiceClient;
   private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String Transaction_INITIATED_TOPIC="transaction_initiated";
    private static final String Transaction_COMPLETED_TOPIC_2="transaction_completed";
    private static final String Transaction_REFUNDED_TOPIC="transaction_refunded";

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

    public TransactionResponse getTransaction(String transactionId)
    {
        return maptoResponse(transactionRepository.findById(transactionId).orElseThrow(()-> new RuntimeException("Transaction not found:"+transactionId)));
    }

    public List<TransactionResponse> getTransactionHistory(String accountNumber)
    {
        return transactionRepository.findBySenderAccountNumberOrderByCreatedAtDesc(accountNumber)
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
