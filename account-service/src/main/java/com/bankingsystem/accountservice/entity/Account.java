package com.bankingsystem.accountservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.annotation.CreatedBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @Column(nullable = false)
    private String accountNumber;
    @Column(nullable = false)
    private String accountHolderName;
    @Column(nullable = false)
    private String email;
    @Column(nullable = false)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType accountType;

@Enumerated(EnumType.STRING)
@Column(nullable = false)
    private AccountStatus status;
  @Column(nullable = false,precision = 15,scale = 2)
    private BigDecimal balance;

  @Column(nullable = false,precision = 15,scale = 2)
    private BigDecimal dailyTransactionLimit;
  @CreationTimestamp
    private LocalDateTime createdDate;
  @UpdateTimestamp
  private LocalDateTime updatedAt;





}
