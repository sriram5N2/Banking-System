package com.bankingsystem.accountservice.controller;

import com.bankingsystem.accountservice.dto.AccountResponse;
import com.bankingsystem.accountservice.dto.CreateAccountRequest;
import com.bankingsystem.accountservice.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/accounts")
@Slf4j
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping("/createAccount")
    public ResponseEntity<AccountResponse> CreateAccount(
            @Valid @RequestBody CreateAccountRequest request
            )
    {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.createAccount(request));
    }
    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable String accountNumber){
        return ResponseEntity.ok(accountService.getAccount(accountNumber));

    }

    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<BigDecimal> getBalance(@PathVariable String accountNumber){
        return ResponseEntity.ok(accountService.getBalance(accountNumber));

    }
    @PutMapping("/{accountNumber}/block")
    public ResponseEntity<String> blockAccount(@PathVariable String accountNumber)
    {
        accountService.blockAccount(accountNumber);
        return ResponseEntity.ok("Account Blocked Successfully");
    }

    @PutMapping("/{accountNumber}/deduct")
    public ResponseEntity<String> deductBalance(@PathVariable String accountNumber,@RequestParam BigDecimal amount)
    {
        accountService.deductBalance(accountNumber,amount);
        return ResponseEntity.ok("Account Deducted Successfully");
    }

    public ResponseEntity<String> creditBalance(@PathVariable String accountNumber,@RequestParam BigDecimal amount)
    {
        accountService.creditBalance(accountNumber,amount);
        return ResponseEntity.ok("Account Credit Successfully");
    }








}
