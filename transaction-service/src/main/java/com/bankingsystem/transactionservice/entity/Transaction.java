package com.bankingsystem.transactionservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name="transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

@Id
@GeneratedValue(strategy= GenerationType.UUID)
private String transactionId;

@Column(nullable = false)
private String senderAccountNumber;
@Column(nullable = false)
private String receiverAccountNumber;
@Column(nullable = false,precision=15,scale=2)
private BigDecimal amount;
@Enumerated(EnumType.STRING)
@Column(nullable = false)
private  TransactionStatus status;
@Enumerated(EnumType.STRING)
@Column(nullable=false)
private TransactionType type;

private String description;
private String failureResaon;
private String refernceNumber;
@CreationTimestamp
private LocalDateTime transactionDate;
private LocalDateTime completionDate;

}
