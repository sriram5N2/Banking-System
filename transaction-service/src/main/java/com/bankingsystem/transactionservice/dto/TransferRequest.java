package com.bankingsystem.transactionservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransferRequest {

    @NotNull(message = "Sender Account number is required")
    private String senderAccountNumber;
    @NotNull(message = "Receiver Account number is required")
    private String recipientAccountNumber;

    @NotNull(message = "Please enter the amount")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    private String description;

}
