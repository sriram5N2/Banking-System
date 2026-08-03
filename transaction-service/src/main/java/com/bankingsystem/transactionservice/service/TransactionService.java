package com.bankingsystem.transactionservice.service;

import com.bankingsystem.transactionservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {
    private final TransactionRepository transactionRepository;


    private static final String Transaction_INITIATED_TOPIC="transaction_initiated";
    private static final String Transaction_COMPLETED_TOPIC_2="transaction_completed";
    private static final String Transaction_REFUNDED_TOPIC="transaction_refunded";

    /**
     * SAGA STEP 1
     * Deducts from sender via feign
     * saves transaction as processing
     * publish event to kafka for fraud check
     * Returns
     * @param request
     * @return
     */
}
