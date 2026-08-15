package com.bankingsystem.frauddetectionservice.service;

import com.bankingsystem.frauddetectionservice.client.AccountServiceClient;
import com.bankingsystem.frauddetectionservice.model.FraudCheckResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudDetectionService {

    private final AccountServiceClient accountServiceClient;
    private final KafkaTemplate<String,Object> kafkaTemplate;
    private final StringRedisTemplate redisTemplate;
    @Value("${fraud.max-transactions-per-minute}")
    private int maxTransactionsPerMinute;
    @Value("${fraud.suspicious-amount-multiplier}")
    private double suspiciousAmountMultiplier;

    @Value("${fraud.max-balance-percentage}")
    private double maxBalancePercentage;
    private static final String VERIFICATION_REQUIRED_TOPIC="verification.required";
    private static final String FRAUD_CHECK_CLEAN_RESULT_TOPIC="Fraud.check.clean";

    public void checkTransaction(Map<String,Object> payload)
    {
        String transactionId= payload.get("transactionId").toString();
        String accountNumber= payload.get("senderAccountNumber").toString();

        BigDecimal amount= new BigDecimal(payload.get("amount").toString());

        BigDecimal senderBalance= accountServiceClient.getBalance(accountNumber);

        log.info("Checking transaction: {} account {} amount: {} balance: {}",transactionId,accountNumber,amount,senderBalance);

        FraudCheckResult result = performFraudChecks(accountNumber,amount,senderBalance);

        if(result.isFraud())
        {
            log.info("Suspicious activity detected - account:{}"+"reason: {}- requesting OTP verification");
            Map<String,Object> verificationEvent=new HashMap<>();
            verificationEvent.put("transactionId",transactionId);
            verificationEvent.put("accountNumber",accountNumber);
            verificationEvent.put("amount",amount);
            verificationEvent.put("senderBalance",senderBalance);
            kafkaTemplate.send(VERIFICATION_REQUIRED_TOPIC,verificationEvent);

        }
        else
        {
            log.info("Transaction Clean");

            Map<String,Object> transactionCleanEvent= new HashMap<>();
            transactionCleanEvent.put("transactionId",transactionId);
            transactionCleanEvent.put("isFraud",false);
            transactionCleanEvent.put("reason",null);

            kafkaTemplate.send(FRAUD_CHECK_CLEAN_RESULT_TOPIC,transactionId,transactionCleanEvent);
            
        }
    }

    private FraudCheckResult performFraudChecks(String accountNumber,
                                               BigDecimal amount,
                                               BigDecimal senderBalance)
    {
        if(isVelocityExceeded(accountNumber))
        {
            return new FraudCheckResult(true,"Too many transactions in 60 seconds"+"Velocity level exceeded");
        }

        if(isAmountSuspicious(accountNumber,amount))
        {
            return new FraudCheckResult(true,"unusual transaction amount"+"exceeds your average");
        }

        if(senderBalance.compareTo(BigDecimal.ZERO) > 0 && isBalanceCheckFailed(senderBalance, amount))
            return new FraudCheckResult(false,"Transaction exceed 90% of account Balance");

        return new FraudCheckResult(false,null);

    }

    private boolean isBalanceCheckFailed(BigDecimal senderBalance,BigDecimal amount)
    {
        BigDecimal maxAllowed= senderBalance.multiply(BigDecimal.valueOf(maxBalancePercentage));
        log.info("Balance Check - amount :{} maxAllowed : {} suspicious: {}",
                amount,maxAllowed,amount.compareTo(senderBalance) > 0);

      return amount.compareTo(senderBalance) > 0;


    }

    private boolean isVelocityExceeded(String accountNumber)
    {
        String key="Fraud:velocity" + accountNumber;
        Long count=redisTemplate.opsForValue().increment(key);

        if(count != null && count==1)
            redisTemplate.expire(key,60, TimeUnit.SECONDS);

        log.info("Velocity check account: {} count: {}/{}",accountNumber,count,maxTransactionsPerMinute);


        return count != null && count > maxTransactionsPerMinute;

    }

    private boolean isAmountSuspicious(String accountNumber,BigDecimal amount)
    {
        String avgKey="Fraud:avg_amount" + accountNumber;
        String avgStr= redisTemplate.opsForValue().get(avgKey);

        if(avgStr == null)
        {
            redisTemplate.opsForValue().set(avgKey,amount.toString());
            return false;
        }

        BigDecimal avgAmount= new BigDecimal(avgStr);
        BigDecimal threshold= avgAmount.multiply(BigDecimal.valueOf(suspiciousAmountMultiplier));

        BigDecimal newAvg= avgAmount.add(amount).
                divide(BigDecimal.valueOf(2),2, RoundingMode.HALF_UP);
        redisTemplate.opsForValue().set(avgKey,newAvg.toString());

        log.info("Amount Check - amount: {} threshold: {} suspicious: {}",amount,threshold,amount.compareTo(threshold)>0);

        return amount.compareTo(threshold) > 0 ;

    }
}
