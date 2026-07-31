package com.bankingsystem.accountservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountEventConsumer {
private final AccountService accountService;

   @KafkaListener(topics = "transaction.completed")
    public void consumeTransactionCompleted(@Payload Map<String,Object> payload)
    {

        try{
            String receiverAccount=payload.get("receiverAccount").toString();
            String senderAccount=payload.get("senderAccount").toString();
            BigDecimal amount=(BigDecimal)payload.get("amount");

            log.info("Receiver Account : "+receiverAccount);
            accountService.creditBalance(receiverAccount,amount);
            log.info("Sender Account : "+senderAccount);


        }
        catch (Exception e)
        {
            log.error("Exception occured while consuming transaction completed:{}", e.getMessage());



        }
    }
    @KafkaListener(topics = "fraud.detected")
    public void consumeFraudDetected(@Payload Map<String,Object> payload)
    {
        try{
            String accountNumber=payload.get("accountNumber").toString();
            log.info("Fraud Detected-BlockingAccount : "+accountNumber);
            accountService.blockAccount(accountNumber);

        }
        catch (Exception e)
        {
            log.error("Blocking Account:{}",e.getMessage());
        }
    }

}
