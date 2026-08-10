package com.bankingsystem.paymentservice.service;

import com.bankingsystem.paymentservice.dto.CreatePaymentRequest;
import com.bankingsystem.paymentservice.dto.PaymentResponse;
import com.bankingsystem.paymentservice.entity.Payment;
import com.bankingsystem.paymentservice.entity.PaymentStatus;
import com.bankingsystem.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Value("${razorpay.key-id}")
    private String keyId;


    @Value("${razorpay.key-secret}")
    private String keySecret;


    private static  final String PAYMENT_COMPLETED_TOPIC="payment.completed";
    private static final String PAYMENT_FAILED_TOPIC="payment.failed";

    /*
    Flow
    1. Create order in razorPay
    2. Save Payment record in DB
    3. Return order details to frontend
    4. Frontend show Razorpay Checkout
    5. User Pays
    6. RazorPay calls webhook

     */
    public PaymentResponse createPaymentOrder(CreatePaymentRequest createPaymentRequest) throws RazorpayException
    {

        log.info("Creating payment Order for account : {} amount : {}", createPaymentRequest.getAccountNumber(),createPaymentRequest.getAmount());

        RazorpayClient razorpayClient = new RazorpayClient(keyId,keySecret);


        int convertedAmount = createPaymentRequest.getAmount().multiply(BigDecimal.valueOf(100)).intValue();

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount",convertedAmount);
        orderRequest.put("currency","USD/INR");
        orderRequest.put("receipt","rcpt_"+ System.currentTimeMillis()+UUID.randomUUID().toString()
                .replace("-","").substring(0,10));


        Order razorpayOrder = razorpayClient.orders.create(orderRequest);

        log.info("RazorPay order created  {}", razorpayOrder.get("id").toString());


        Payment payment = new Payment();
        payment.setAccountNumber(createPaymentRequest.getAccountNumber());
        payment.setAmount(createPaymentRequest.getAmount());
        payment.setCurrency("USD/INR");
        payment.setStatus(PaymentStatus.CREATED);
        payment.setDescription(createPaymentRequest.getDescription());

        Payment savedPayment = paymentRepository.save(payment);

        return new PaymentResponse(savedPayment.getId(),
                razorpayOrder.get("id").toString(),
                request.getAmount(),
                "USD/INR",
                "CREATED",
                keyId
                );


    }


}
