package com.bankingsystem.paymentservice.dto;

import java.math.BigDecimal;

public class PaymentResponse {

    private String paymentId;
    private String razorpayOrderId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String razorpayKeyId;
    
}
