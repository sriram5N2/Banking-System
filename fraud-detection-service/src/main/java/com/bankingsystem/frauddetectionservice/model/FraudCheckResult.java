package com.bankingsystem.frauddetectionservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FraudCheckResult {
    
    private boolean fraud;
    private String reason;
}
