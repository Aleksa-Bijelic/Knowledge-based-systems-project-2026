package com.example.bank.controller;

import com.example.bank.dto.PaymentRequest;
import com.example.bank.dto.PaymentResponse;
import com.example.bank.dto.SuspiciousTransactionResponse;
import com.example.bank.model.BankUser;
import com.example.bank.model.Transaction;
import com.example.bank.repository.BankUserRepository;
import com.example.bank.service.TransactionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class TransactionController {

    private final TransactionService transactionService;
    private final BankUserRepository bankUserRepository;

    public TransactionController(TransactionService transactionService,
                                 BankUserRepository bankUserRepository) {
        this.transactionService = transactionService;
        this.bankUserRepository = bankUserRepository;
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

    @GetMapping("/suspicious")
    public ResponseEntity<List<SuspiciousTransactionResponse>> getSuspiciousTransactions() {
        BankUser user = currentUser();
        if (user == null) {
            return ResponseEntity.badRequest().build();
        }
        List<Transaction> transactions = transactionService.getSuspiciousTransactions(user.getId());
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
        BankUser user = currentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized"));
        }
        boolean updated = transactionService.approveTransaction(id, user.getId());
        if (updated) {
            return ResponseEntity.ok(Map.of("message", "Transaction approved"));
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Transaction not found, not in suspicious state, or does not belong to you"));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Map<String, String>> rejectTransaction(@PathVariable Long id) {
        BankUser user = currentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized"));
        }
        boolean updated = transactionService.rejectTransaction(id, user.getId());
        if (updated) {
            return ResponseEntity.ok(Map.of("message", "Transaction rejected"));
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Transaction not found, not in suspicious state, or does not belong to you"));
    }

    private BankUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        return bankUserRepository.findByUsername(auth.getName()).orElse(null);
    }
}
