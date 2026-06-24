package com.example.bank.service;

import com.example.bank.dto.PaymentRequest;
import com.example.bank.dto.PaymentResponse;
import com.example.bank.model.BankAccount;
import com.example.bank.model.PaymentCard;
import com.example.bank.model.Transaction;
import com.example.bank.repository.BankAccountRepository;
import com.example.bank.repository.PaymentCardRepository;
import com.example.bank.repository.TransactionRepository;
import com.example.bank.rules.CardTransactionEvent;
import com.example.bank.rules.FraudDetectionService;
import com.example.bank.rules.SuspiciousTransactionFact;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class TransactionService {

    private final BankAccountRepository bankAccountRepository;
    private final PaymentCardRepository paymentCardRepository;
    private final TransactionRepository transactionRepository;
    private final FraudDetectionService fraudDetectionService;

    public TransactionService(BankAccountRepository bankAccountRepository,
                              PaymentCardRepository paymentCardRepository,
                              TransactionRepository transactionRepository,
                              FraudDetectionService fraudDetectionService) {
        this.bankAccountRepository = bankAccountRepository;
        this.paymentCardRepository = paymentCardRepository;
        this.transactionRepository = transactionRepository;
        this.fraudDetectionService = fraudDetectionService;
    }

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        if (request.getCardNumber() == null || request.getCardNumber().isBlank()) {
            return PaymentResponse.fail("Card number is required");
        }
        if (request.getCardCvv() == null || request.getCardCvv().isBlank()) {
            return PaymentResponse.fail("Card CVV is required");
        }
        if (request.getCardholderName() == null || request.getCardholderName().isBlank()) {
            return PaymentResponse.fail("Cardholder name is required");
        }
        if (request.getReceiverAccountNumber() == null || request.getReceiverAccountNumber().isBlank()) {
            return PaymentResponse.fail("Receiver account number is required");
        }
        if (request.getAmount() <= 0) {
            return PaymentResponse.fail("Amount must be greater than zero");
        }

        // Find card by card number
        PaymentCard card = paymentCardRepository.findByCardNumber(request.getCardNumber())
                .orElse(null);
        if (card == null) {
            return PaymentResponse.fail("Invalid card number");
        }

        // Validate CVV
        if (!card.getCvv().equals(request.getCardCvv())) {
            return PaymentResponse.fail("Invalid CVV");
        }

        // Validate expiration date
        if (request.getCardExpirationDate() != null && !request.getCardExpirationDate().isBlank()) {
            try {
                LocalDate providedExpiry = LocalDate.parse(request.getCardExpirationDate());
                if (providedExpiry.isBefore(card.getExpirationDate()) || providedExpiry.isAfter(card.getExpirationDate())) {
                    return PaymentResponse.fail("Invalid card expiration date");
                }
            } catch (Exception e) {
                return PaymentResponse.fail("Invalid card expiration date format");
            }
        }

        // Validate cardholder name (case-insensitive, ignoring extra spaces)
        String providedName = request.getCardholderName().trim().toLowerCase();
        String actualName = card.getCardholderName().trim().toLowerCase();
        if (!providedName.equals(actualName)) {
            return PaymentResponse.fail("Invalid cardholder name");
        }

        // Find sender account (the account tied to this card's package account)
        BankAccount senderAccount = bankAccountRepository.findAll().stream()
                .filter(a -> a.getPackageAccount() != null
                        && a.getPackageAccount().getId().equals(card.getPackageAccount().getId()))
                .findFirst()
                .orElse(null);
        if (senderAccount == null) {
            return PaymentResponse.fail("No bank account found for this card");
        }

        // Validate sender account number matches
        if (request.getSenderAccountNumber() != null
                && !request.getSenderAccountNumber().equals(senderAccount.getAccountNumber())) {
            return PaymentResponse.fail("Sender account number does not match the card");
        }

        // Check sufficient balance
        if (senderAccount.getBalance() < request.getAmount()) {
            return PaymentResponse.fail("Insufficient funds");
        }

        // Find receiver account
        BankAccount receiverAccount = bankAccountRepository.findByAccountNumber(request.getReceiverAccountNumber())
                .orElse(null);
        if (receiverAccount == null) {
            return PaymentResponse.fail("Receiver account not found");
        }

        // Cannot transfer to same account
        if (senderAccount.getAccountNumber().equals(receiverAccount.getAccountNumber())) {
            return PaymentResponse.fail("Cannot transfer to the same account");
        }

        // Perform transfer
        senderAccount.setBalance(senderAccount.getBalance() - request.getAmount());
        receiverAccount.setBalance(receiverAccount.getBalance() + request.getAmount());
        bankAccountRepository.save(senderAccount);
        bankAccountRepository.save(receiverAccount);

        // Create transaction record
        Transaction transaction = new Transaction();
        transaction.setSenderAccountNumber(senderAccount.getAccountNumber());
        transaction.setReceiverAccountNumber(receiverAccount.getAccountNumber());
        transaction.setAmount(request.getAmount());
        transaction.setCurrency(senderAccount.getCurrency());
        transaction.setStatus("COMPLETED");
        transaction.setCardNumber(maskCardNumber(request.getCardNumber()));
        transaction.setCardholderName(card.getCardholderName());
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setDescription(request.getDescription());
        Transaction saved = transactionRepository.save(transaction);

        // Run fraud detection
        long nowEpoch = LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        CardTransactionEvent event = new CardTransactionEvent(
            saved.getId(),
            senderAccount.getPackageAccount().getClient().getId(),
            card.getId(),
            senderAccount.getAccountNumber(),
            receiverAccount.getAccountNumber(),
            request.getAmount(),
            nowEpoch,
            request.getLatitude(),
            request.getLongitude(),
            request.getCity() != null ? request.getCity() : "",
            request.getCountry() != null ? request.getCountry() : ""
        );

        // Load historical transactions for the sender account
        List<Transaction> recentTransactions = transactionRepository
            .findRecentCompletedBySenderAccount(senderAccount.getAccountNumber());
        List<CardTransactionEvent> historicalEvents = new ArrayList<>();
        for (Transaction tx : recentTransactions) {
            if (tx.getId().equals(saved.getId())) continue;
            long txEpoch = tx.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            historicalEvents.add(new CardTransactionEvent(
                tx.getId(),
                senderAccount.getPackageAccount().getClient().getId(),
                card.getId(),
                tx.getSenderAccountNumber(),
                tx.getReceiverAccountNumber(),
                tx.getAmount(),
                txEpoch,
                0, 0, "", ""
            ));
        }

        List<SuspiciousTransactionFact> fraudResults =
            fraudDetectionService.evaluateTransactionWithHistory(event, historicalEvents);

        if (!fraudResults.isEmpty()) {
            String combinedReason = fraudResults.stream()
                .map(SuspiciousTransactionFact::getReason)
                .reduce((a, b) -> a + "; " + b)
                .orElse("");
            saved.setStatus("SUSPICIOUS");
            saved.setFraudReason(combinedReason);
            transactionRepository.save(saved);
            return PaymentResponse.suspicious(saved.getId(), request.getAmount(),
                senderAccount.getAccountNumber(), receiverAccount.getAccountNumber(), combinedReason);
        }

        return PaymentResponse.ok(saved.getId(), request.getAmount(),
                senderAccount.getAccountNumber(), receiverAccount.getAccountNumber());
    }

    public List<Transaction> getSuspiciousTransactions(String accountNumber) {
        return transactionRepository.findByStatus("SUSPICIOUS").stream()
            .filter(t -> t.getSenderAccountNumber().equals(accountNumber))
            .toList();
    }

    public boolean approveTransaction(Long transactionId) {
        return transactionRepository.findById(transactionId)
            .map(tx -> {
                if (!"SUSPICIOUS".equals(tx.getStatus())) return false;
                tx.setStatus("APPROVED");
                transactionRepository.save(tx);
                return true;
            })
            .orElse(false);
    }

    public boolean rejectTransaction(Long transactionId) {
        return transactionRepository.findById(transactionId)
            .map(tx -> {
                if (!"SUSPICIOUS".equals(tx.getStatus())) return false;
                tx.setStatus("REJECTED");
                transactionRepository.save(tx);
                return true;
            })
            .orElse(false);
    }

    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return cardNumber;
        }
        String last4 = cardNumber.substring(cardNumber.length() - 4);
        return "**** **** **** " + last4;
    }
}
