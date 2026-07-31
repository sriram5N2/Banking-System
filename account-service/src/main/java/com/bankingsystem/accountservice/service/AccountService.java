package com.bankingsystem.accountservice.service;

import com.bankingsystem.accountservice.dto.AccountResponse;
import com.bankingsystem.accountservice.dto.CreateAccountRequest;
import com.bankingsystem.accountservice.entity.Account;
import com.bankingsystem.accountservice.entity.AccountStatus;
import com.bankingsystem.accountservice.entity.AccountType;
import com.bankingsystem.accountservice.repository.AccountRepository;
import com.bankingsystem.accountservice.utils.AccountNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;
    private final AccountNumberGenerator accountNumberGenerator;
    public AccountResponse createAccount(CreateAccountRequest request) {
        log.info("creating Account for:{}",request.getEmail());

        if(accountRepository.existsByEmail(request.getEmail()))
        {
            throw new RuntimeException("Email Already Exists");
        }
        Account account = new Account();
        account.setAccountHolderName(request.getAccountHolderName());
        account.setEmail(request.getEmail());
        account.setAccountType(request.getAccountType());
        account.setStatus(AccountStatus.ACTIVE);
        account.setBalance(request.getInitialDeposit());
        account.setPhone(request.getPhone());
        account.setAccountNumber(generateAccountNumber());
        account.setDailyTransactionLimit(request.getAccountType()== AccountType.SAVINGS ? new BigDecimal(100000) : new BigDecimal(50000));
       Account savedAccount= accountRepository.save(account);
        log.info("Account Created Successfully");

        return mapToResponse(savedAccount);
    }
    private String generateAccountNumber() {
        String accountNumber;
        do {
            accountNumber = accountNumberGenerator.generate();
        } while (accountRepository.existsByAccountNumber(accountNumber)); // DB lookup check

        return accountNumber;
    }
  //  public AccountResponse getAccountByAccountNumber(String accountNumber) {}
   public AccountResponse getAccount(String accountNumber) {

        Account account= accountRepository.findByAccountNumber(accountNumber).orElseThrow(()-> new ResourceNotFoundException("Account Not Found Please Enter Valid Account Number"));
       return mapToResponse(account);
   }
   public void deductBalance(String accountNumber,BigDecimal amount)
   {
        log.info("deducting balance for:{}",accountNumber);
        Account account=accountRepository.findByAccountNumber(accountNumber).orElseThrow(()-> new ResourceNotFoundException("Account Not Found Please Enter Valid Account Number"));
       if(account.getStatus()!=AccountStatus.ACTIVE)
           throw new RuntimeException("Account Status Not Active");
     if(account.getBalance().subtract(amount).doubleValue()<=0)
         throw new RuntimeException("Balance Not Enough");

        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);
        log.info("Account deducted Successfully");

   }
   public void creditBalance(String accountNumber,BigDecimal amount)
   {
       log.info("credit balance for:{}",accountNumber);
       Account account=accountRepository.findByAccountNumber(accountNumber).orElseThrow(()-> new ResourceNotFoundException("Account Not Found Please Enter Valid Account Number"));
       if(account.getStatus()!=AccountStatus.ACTIVE)
           throw new RuntimeException("Account Status Not Active");
       account.setBalance(account.getBalance().add(amount));
       accountRepository.save(account);
       log.info("Account credit Successfully");
   }
    public BigDecimal getBalance(String accountNumber) {

        Account account= accountRepository.findByAccountNumber(accountNumber).orElseThrow(()-> new ResourceNotFoundException("Account Not Found Please Enter Valid Account Number"));
        return account.getBalance();
    }
    public void blockAccount(String accountNumber) {
       log.info("blocking Account for:{}",accountNumber);
        Account account = accountRepository.findByAccountNumber(accountNumber).orElseThrow(()-> new ResourceNotFoundException("Account Not Found Please Enter Valid Account Number"));
        account.setStatus(AccountStatus.BLOCKED);
        accountRepository.save(account);
        log.info("Account Blocked Successfully");
    }
    private AccountResponse mapToResponse(Account account) {
        AccountResponse accountResponse = new AccountResponse();
        accountResponse.setId(account.getId());
        accountResponse.setEmail(account.getEmail());
        accountResponse.setAccountHolderName(account.getAccountHolderName());
        accountResponse.setAccountType(account.getAccountType());
        accountResponse.setBalance(account.getBalance());
        accountResponse.setPhone(account.getPhone());
        accountResponse.setDailyTransactionLimit(account.getDailyTransactionLimit());
        accountResponse.setAccountNumber(account.getAccountNumber());
        accountResponse.setStatus(account.getStatus());
        return accountResponse;
    }


}
