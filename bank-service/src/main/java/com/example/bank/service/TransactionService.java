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
        // --- validation ---
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

        PaymentCard card = paymentCardRepository.findByCardNumber(request.getCardNumber())
                .orElse(null);
        if (card == null) return PaymentResponse.fail("Invalid card number");
        if (!card.getCvv().equals(request.getCardCvv())) return PaymentResponse.fail("Invalid CVV");

        if (request.getCardExpirationDate() != null && !request.getCardExpirationDate().isBlank()) {
            try {
                LocalDate providedExpiry = LocalDate.parse(request.getCardExpirationDate());
                if (!providedExpiry.equals(card.getExpirationDate())) {
                    return PaymentResponse.fail("Invalid card expiration date");
                }
            } catch (Exception e) {
                return PaymentResponse.fail("Invalid card expiration date format");
            }
        }

        if (!card.getCardholderName().trim().equalsIgnoreCase(request.getCardholderName().trim())) {
            return PaymentResponse.fail("Invalid cardholder name");
        }

        BankAccount senderAccount = bankAccountRepository.findByPackageAccountId(card.getPackageAccount().getId())
                .stream().findFirst().orElse(null);
        if (senderAccount == null) return PaymentResponse.fail("No bank account found for this card");

        if (request.getSenderAccountNumber() != null
                && !request.getSenderAccountNumber().equals(senderAccount.getAccountNumber())) {
            return PaymentResponse.fail("Sender account number does not match the card");
        }

        if (senderAccount.getBalance() < request.getAmount()) {
            return PaymentResponse.fail("Insufficient funds");
        }

        BankAccount receiverAccount = bankAccountRepository.findByAccountNumber(request.getReceiverAccountNumber())
                .orElse(null);
        if (receiverAccount == null) return PaymentResponse.fail("Receiver account not found");
        if (senderAccount.getAccountNumber().equals(receiverAccount.getAccountNumber())) {
            return PaymentResponse.fail("Cannot transfer to the same account");
        }

        // -- create transaction record (status PENDING until fraud check) --
        Long clientId = senderAccount.getPackageAccount().getClient().getId();
        Transaction transaction = new Transaction();
        transaction.setSenderAccountNumber(senderAccount.getAccountNumber());
        transaction.setReceiverAccountNumber(receiverAccount.getAccountNumber());
        transaction.setAmount(request.getAmount());
        transaction.setCurrency(senderAccount.getCurrency());
        transaction.setStatus("PENDING");
        transaction.setCardNumber(maskCardNumber(request.getCardNumber()));
        transaction.setCardholderName(card.getCardholderName());
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setDescription(request.getDescription());
        transaction.setLatitude(request.getLatitude());
        transaction.setLongitude(request.getLongitude());
        transaction.setCity(request.getCity());
        transaction.setCountry(request.getCountry());
        Transaction saved = transactionRepository.save(transaction);

        // -- build event and detect fraud using CEP session (history is already in session) --
        long nowEpoch = saved.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        CardTransactionEvent event = new CardTransactionEvent(
            saved.getId(), clientId, card.getId(),
            senderAccount.getAccountNumber(), receiverAccount.getAccountNumber(),
            request.getAmount(), nowEpoch,
            request.getLatitude(), request.getLongitude(),
            request.getCity() != null ? request.getCity() : "",
            request.getCountry() != null ? request.getCountry() : ""
        );

        List<SuspiciousTransactionFact> fraudResults =
            fraudDetectionService.evaluateTransaction(event);

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

        // -- if not suspicious, finalise transfer --
        senderAccount.setBalance(senderAccount.getBalance() - request.getAmount());
        receiverAccount.setBalance(receiverAccount.getBalance() + request.getAmount());
        bankAccountRepository.save(senderAccount);
        bankAccountRepository.save(receiverAccount);

        saved.setStatus("COMPLETED");
        transactionRepository.save(saved);

        return PaymentResponse.ok(saved.getId(), request.getAmount(),
                senderAccount.getAccountNumber(), receiverAccount.getAccountNumber());
    }

    public List<Transaction> getSuspiciousTransactions(Long clientId) {
        return transactionRepository.findSuspiciousByClientId(clientId);
    }

    @Transactional
    public boolean approveTransaction(Long transactionId, Long clientId) {
        List<Transaction> matching = transactionRepository.findByIdAndClientId(transactionId, clientId);
        if (matching.isEmpty()) return false;
        Transaction tx = matching.get(0);
        if (!"SUSPICIOUS".equals(tx.getStatus())) return false;

        BankAccount senderAccount = bankAccountRepository.findByAccountNumber(tx.getSenderAccountNumber())
            .orElse(null);
        BankAccount receiverAccount = bankAccountRepository.findByAccountNumber(tx.getReceiverAccountNumber())
            .orElse(null);
        if (senderAccount == null || receiverAccount == null) return false;
        if (senderAccount.getBalance() < tx.getAmount()) return false;

        senderAccount.setBalance(senderAccount.getBalance() - tx.getAmount());
        receiverAccount.setBalance(receiverAccount.getBalance() + tx.getAmount());
        bankAccountRepository.save(senderAccount);
        bankAccountRepository.save(receiverAccount);

        tx.setStatus("APPROVED");
        transactionRepository.save(tx);
        return true;
    }

    @Transactional
    public boolean rejectTransaction(Long transactionId, Long clientId) {
        List<Transaction> matching = transactionRepository.findByIdAndClientId(transactionId, clientId);
        if (matching.isEmpty()) return false;
        Transaction tx = matching.get(0);
        if (!"SUSPICIOUS".equals(tx.getStatus())) return false;
        tx.setStatus("REJECTED");
        transactionRepository.save(tx);
        return true;
    }

    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return cardNumber;
        }
        String last4 = cardNumber.substring(cardNumber.length() - 4);
        return "**** **** **** " + last4;
    }
}
