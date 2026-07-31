package com.bankingsystem.accountservice.utils;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class AccountNumberGenerator {
    private static final SecureRandom RANDOM = new SecureRandom();

    // Optional prefix for bank identification (e.g., 1001)
    private static final String BANK_PREFIX = "1001";

    public String generate() {
        // Generates a random 6-digit suffix -> Total 10-digit account number (1001XXXXXX)
        int suffix = 100_000 + RANDOM.nextInt(900_000);
        return BANK_PREFIX + suffix;
    }
}
