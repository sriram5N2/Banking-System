package com.bankingsystem.transactionservice.dto;

import com.bankingsystem.transactionservice.entity.TransactionStatus;
import com.bankingsystem.transactionservice.entity.TransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    private String transactionId;
    private String senderAccountNumber;
    private String receiverAccountNumber;
    private BigDecimal amount;
    private TransactionStatus status;
    private TransactionType type;
    private String description;
    private String failureResaon;
    private String refernceNumber;
    private LocalDateTime transactionDate;
    private LocalDateTime completionDate;


}
