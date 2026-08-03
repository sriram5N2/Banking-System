package com.bankingsystem.transactionservice.controller;

import com.bankingsystem.transactionservice.dto.TransactionResponse;
import com.bankingsystem.transactionservice.dto.TransferRequest;
import com.bankingsystem.transactionservice.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@Slf4j
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
@PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> Transfer(@Valid
                                                        @RequestBody TransferRequest transferRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                transactionService.transfer(transferRequest)
        );
    }
   @GetMapping("/account/{accountNumber}")
    public ResponseEntity<TransactionResponse> getTransactions(@PathVariable String transactionId) {
    return ResponseEntity.ok(transactionService.getTransaction(transactionId));
    }

    @GetMapping("/{transactionHistory}")
    public ResponseEntity<List<TransactionResponse>> getTransactionHistory(@PathVariable String accountNumber) {
        return ResponseEntity.ok(transactionService.getTransactionHistory(accountNumber));
    }

    public ResponseEntity<TransactionResponse> verifyOTP(@PathVariable String transactionId,
                                                         @RequestParam String otp) {
    log.info("OTP verification request - transaction {}", transactionId);
    return ResponseEntity.ok(transactionService.verifyOTP(transactionId,otp));
    }


}
