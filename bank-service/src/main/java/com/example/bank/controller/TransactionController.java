package com.example.bank.controller;

import com.example.bank.dto.PaymentRequest;
import com.example.bank.dto.PaymentResponse;
import com.example.bank.dto.SuspiciousTransactionResponse;
import com.example.bank.model.Transaction;
import com.example.bank.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/process")
    public ResponseEntity<PaymentResponse> processPayment(@RequestBody PaymentRequest request) {
        PaymentResponse response = transactionService.processPayment(request);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/suspicious/{accountNumber}")
    public ResponseEntity<List<SuspiciousTransactionResponse>> getSuspiciousTransactions(
            @PathVariable String accountNumber) {
        List<Transaction> transactions = transactionService.getSuspiciousTransactions(accountNumber);
        List<SuspiciousTransactionResponse> response = transactions.stream()
            .map(tx -> {
                SuspiciousTransactionResponse r = new SuspiciousTransactionResponse();
                r.setId(tx.getId());
                r.setSenderAccountNumber(tx.getSenderAccountNumber());
                r.setReceiverAccountNumber(tx.getReceiverAccountNumber());
                r.setAmount(tx.getAmount());
                r.setCurrency(tx.getCurrency());
                r.setStatus(tx.getStatus());
                r.setFraudReason(tx.getFraudReason());
                r.setCreatedAt(tx.getCreatedAt());
                return r;
            })
            .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Map<String, String>> approveTransaction(@PathVariable Long id) {
        boolean updated = transactionService.approveTransaction(id);
        if (updated) {
            return ResponseEntity.ok(Map.of("message", "Transaction approved"));
        }
        return ResponseEntity.badRequest().body(Map.of("message", "Transaction not found or not in suspicious state"));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Map<String, String>> rejectTransaction(@PathVariable Long id) {
        boolean updated = transactionService.rejectTransaction(id);
        if (updated) {
            return ResponseEntity.ok(Map.of("message", "Transaction rejected"));
        }
        return ResponseEntity.badRequest().body(Map.of("message", "Transaction not found or not in suspicious state"));
    }
}
