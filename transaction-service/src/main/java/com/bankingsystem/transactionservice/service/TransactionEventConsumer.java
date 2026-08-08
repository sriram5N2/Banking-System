package com.bankingsystem.transactionservice.service;

import com.bankingsystem.transactionservice.entity.Transaction;
import com.bankingsystem.transactionservice.entity.TransactionStatus;
import com.bankingsystem.transactionservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionEventConsumer {

    private final TransactionRepository transactionRepository;
    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String,Object> kafkaTemplate;
    private static final long OTP_EXPIRY_MINUTES=5;
    private static final String TRANSACTION_OTP_GENERATED_TOPIC="transaction.otp.generated";



    /*
    * Consume Verification.required
    * Generate OTP and ask user to verify
    * @Param Payload
    *
     */
    @KafkaListener(topics = "verification.required")

    public void consumeVerificationRequired(@Payload Map<String,Object> payload)
    {
        try{

            String transactionId =  (String) payload.get("transactionId");
            String accountNumber =  (String) payload.get("accountNumber");
            String reason =  (String) payload.get("reason");

            log.info("Verification Required - transaction: {}, reason: {}",
                    transactionId, reason);

            Transaction transaction=transactionRepository.findById(transactionId).orElseThrow(() -> new RuntimeException("Transaction not found"+ transactionId));

            if(transaction.getStatus()!= TransactionStatus.PROCESSING)
            {
                log.warn("Transaction {} not PROCESSING - skipping", transactionId);
                return;

            }

            // Generate 6 digit OTP
            String otp = String.format("%06d",(int) (Math.random()*900000)+100000);

            String otpKey = "Verification:otp" + transactionId;
            redisTemplate.opsForValue().set(otpKey, otp, OTP_EXPIRY_MINUTES, TimeUnit.MINUTES);

            transaction.setStatus(TransactionStatus.PROCESSING);


            transactionRepository.save(transaction);

            log.info("OTP genreated for transaction: {} expires in {} min", transactionId, OTP_EXPIRY_MINUTES);

            // Notify User
            Map<String,Object> otpEvent = new HashMap<>();
            otpEvent.put("otp",otp);
            otpEvent.put("transactionId",transactionId);
            otpEvent.put("accountNumber",accountNumber);
            otpEvent.put("reason",reason);
            otpEvent.put("amount",payload.get("amount"));

            kafkaTemplate.send(TRANSACTION_OTP_GENERATED_TOPIC,transactionId,otpEvent);



        } catch (Exception e) {
            log.error("Error while sending OTP event",e);


        }
    }

}
