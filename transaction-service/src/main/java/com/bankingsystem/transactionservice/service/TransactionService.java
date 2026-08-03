package com.bankingsystem.transactionservice.service;

import com.bankingsystem.transactionservice.client.AccountServiceClient;
import com.bankingsystem.transactionservice.dto.TransactionResponse;
import com.bankingsystem.transactionservice.dto.TransferRequest;
import com.bankingsystem.transactionservice.entity.Transaction;
import com.bankingsystem.transactionservice.entity.TransactionStatus;
import com.bankingsystem.transactionservice.entity.TransactionType;
import com.bankingsystem.transactionservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService  {
    private final TransactionRepository transactionRepository;
    private final AccountServiceClient accountServiceClient;

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

    }




}
