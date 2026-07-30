package com.bankingsystem.accountservice.dto;

import com.bankingsystem.accountservice.entity.AccountType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateAccountRequest {

    @NotBlank(message = "Account holder name is required")
    private String accountHolderName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid phone number format")
    private String phone;

    @NotNull(message = "Account type is required")
    private AccountType accountType;

    @NotNull(message = "Initial balance is required")
    @Positive(message = "Initial deposit must be positive")
    private BigDecimal initialDeposit;


}
