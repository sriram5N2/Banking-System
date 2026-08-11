package com.bankingsystem.paymentservice.service;

import com.bankingsystem.paymentservice.dto.CreatePaymentRequest;
import com.bankingsystem.paymentservice.dto.PaymentResponse;
import com.bankingsystem.paymentservice.entity.Payment;
import com.bankingsystem.paymentservice.entity.PaymentStatus;
import com.bankingsystem.paymentservice.repository.PaymentRepository;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    @Value("${razorpay.webhook-secret}")
    private String webhookSecret;

    private static final String PAYMENT_COMPLETED_TOPIC = "payment.completed";
    private static final String PAYMENT_FAILED_TOPIC = "payment.failed";

    public PaymentResponse createPaymentOrder(CreatePaymentRequest createPaymentRequest) throws RazorpayException {

        log.info("Creating payment Order for account : {} amount : {}", createPaymentRequest.getAccountNumber(), createPaymentRequest.getAmount());

        RazorpayClient razorpayClient = new RazorpayClient(keyId, keySecret);

        int convertedAmount = createPaymentRequest.getAmount().multiply(BigDecimal.valueOf(100)).intValue();

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", convertedAmount);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "rcpt_" + System.currentTimeMillis() + UUID.randomUUID().toString()
                .replace("-", "").substring(0, 10));

        Order razorpayOrder = razorpayClient.orders.create(orderRequest);
        String razorpayOrderId = razorpayOrder.get("id").toString();

        log.info("RazorPay order created {}", razorpayOrderId);

        Payment payment = new Payment();
        payment.setAccountNumber(createPaymentRequest.getAccountNumber());
        payment.setAmount(createPaymentRequest.getAmount());
        payment.setCurrency("INR");
        payment.setStatus(PaymentStatus.CREATED);
        payment.setDescription(createPaymentRequest.getDescription());

        // FIX 1: Set the razorpayOrderId on the payment entity
        payment.setRazorpayOrderId(razorpayOrderId);

        Payment savedPayment = paymentRepository.save(payment);

        return new PaymentResponse(
                savedPayment.getId(),
                razorpayOrderId,
                createPaymentRequest.getAmount(),
                "INR",
                "CREATED",
                keyId
        );
    }

    // Pass signature and raw body from controller for verification
    public void handleWebhook(Map<String, Object> payload, String rawResponseBody, String razorpaySignature) {

        // FIX 4: Verify Webhook Signature for security
        try {
            if (razorpaySignature != null && webhookSecret != null) {
                Utils.verifyWebhookSignature(rawResponseBody, razorpaySignature, webhookSecret);
            }
        } catch (RazorpayException e) {
            log.error("Invalid Razorpay Webhook Signature", e);
            throw new RuntimeException("Invalid webhook signature");
        }

        log.info("Received Razorpay webhook: {}", payload.get("event"));
        String event = (String) payload.get("event");

        if ("payment.captured".equals(event)) {
            handlePaymentSuccess(payload);
        } else if ("payment.failed".equals(event)) {
            handlePaymentFailure(payload);
        }
    }

    private void handlePaymentSuccess(Map<String, Object> payload) {
        try {
            Map<String, String> paymentData = extractPaymentData(payload);
            String orderId = paymentData.get("orderId");
            String paymentId = paymentData.get("paymentId");

            Payment payment = paymentRepository.findByRazorpayOrderId(orderId)
                    .orElseThrow(() -> new RuntimeException("Payment not found for order: " + orderId));

            payment.setRazorpayPaymentId(paymentId);
            payment.setStatus(PaymentStatus.COMPLETED);
            paymentRepository.save(payment);

            Map<String, Object> event = new HashMap<>();
            event.put("paymentId", payment.getId());
            event.put("accountNumber", payment.getAccountNumber());
            event.put("amount", payment.getAmount());
            event.put("razorpayPaymentId", paymentId);

            kafkaTemplate.send(PAYMENT_COMPLETED_TOPIC, String.valueOf(payment.getId()), event);
            log.info("Payment Completed {}", payment.getId());

        } catch (Exception e) {
            // FIX 3: Pass exception to logger
            log.error("Error Handling payment success", e);
        }
    }

    private void handlePaymentFailure(Map<String, Object> payload) {
        try {
            Map<String, String> paymentData = extractPaymentData(payload);
            String orderId = paymentData.get("orderId");

            Payment payment = paymentRepository.findByRazorpayOrderId(orderId)
                    .orElseThrow(() -> new RuntimeException("Payment not found for order: " + orderId));

            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Payment Failed via RazorPay");
            paymentRepository.save(payment);

            Map<String, Object> event = new HashMap<>();
            event.put("paymentId", payment.getId());
            event.put("accountNumber", payment.getAccountNumber());
            event.put("amount", payment.getAmount());
            event.put("reason", "Payment Failed via Razorpay");

            kafkaTemplate.send(PAYMENT_FAILED_TOPIC, String.valueOf(payment.getId()), event);
            log.info("Payment Failed {}", payment.getId());

        } catch (Exception e) {
            // FIX 3: Pass exception to logger
            log.error("Error Handling payment failure", e);
        }
    }

    // FIX 2: Correct keys for Razorpay's snake_case JSON payload
    private Map<String, String> extractPaymentData(Map<String, Object> payload) {
        Map<String, Object> payloadMap = (Map<String, Object>) payload.get("payload");
        Map<String, Object> paymentWrapper = (Map<String, Object>) payloadMap.get("payment");
        Map<String, Object> entity = (Map<String, Object>) paymentWrapper.get("entity");

        Map<String, String> extractedData = new HashMap<>();
        extractedData.put("orderId", (String) entity.get("order_id")); // snake_case in Razorpay JSON
        extractedData.put("paymentId", (String) entity.get("id"));     // "id" in entity represents payment_id

        return extractedData;
    }
}