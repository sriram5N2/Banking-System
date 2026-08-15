package com.bankingsystem.transactionservice.repository;

import com.bankingsystem.transactionservice.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, String> {

   // List<Transaction> findByAccountNumber(String accountNumber);
    List<Transaction> findBySenderAccountNumberOrderByTransactionDateDesc(String senderAccountNumber);
}
