package com.example.bank.controller;

import com.example.bank.dto.PaymentRequest;
import com.example.bank.dto.PaymentResponse;
import com.example.bank.dto.SuspiciousTransactionResponse;
import com.example.bank.model.BankUser;
import com.example.bank.model.Transaction;
import com.example.bank.repository.BankUserRepository;
import com.example.bank.security.JwtUtil;
import com.example.bank.service.TransactionService;
import jakarta.servlet.http.HttpServletRequest;
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
    private final JwtUtil jwtUtil;

    public TransactionController(TransactionService transactionService,
                                 BankUserRepository bankUserRepository,
                                 JwtUtil jwtUtil) {
        this.transactionService = transactionService;
        this.bankUserRepository = bankUserRepository;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/process")
    public ResponseEntity<PaymentResponse> processPayment(@RequestBody PaymentRequest request,
                                                           HttpServletRequest httpRequest) {
        PaymentResponse response = transactionService.processPayment(request, httpRequest);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/suspicious")
    public ResponseEntity<List<SuspiciousTransactionResponse>> getSuspiciousTransactions(HttpServletRequest request) {
        BankUser user = currentUser(request);
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
                r.setCity(tx.getCity());
                r.setCountry(tx.getCountry());
                r.setDescription(tx.getDescription());
                return r;
            })
            .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Map<String, String>> approveTransaction(@PathVariable("id") Long id, HttpServletRequest request) {
        BankUser user = currentUser(request);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized"));
        }
        String error = transactionService.approveTransaction(id, user.getId());
        if (error == null) {
            return ResponseEntity.ok(Map.of("message", "Transaction approved"));
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", error));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Map<String, String>> rejectTransaction(@PathVariable("id") Long id, HttpServletRequest request) {
        BankUser user = currentUser(request);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized"));
        }
        String error = transactionService.rejectTransaction(id, user.getId());
        if (error == null) {
            return ResponseEntity.ok(Map.of("message", "Transaction rejected"));
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", error));
    }

    private BankUser currentUser(HttpServletRequest request) {
        // First try SecurityContext (set by JwtAuthenticationFilter)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            BankUser user = bankUserRepository.findByUsername(auth.getName()).orElse(null);
            if (user != null) return user;
        }
        // Fallback: parse JWT directly from Authorization header
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtUtil.validateToken(token)) {
                String username = jwtUtil.getUsernameFromToken(token);
                return bankUserRepository.findByUsername(username).orElse(null);
            }
        }
        return null;
    }
}
