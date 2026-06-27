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
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

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
    public PaymentResponse processPayment(PaymentRequest request, HttpServletRequest httpRequest) {
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

        // -- resolve IP and location --
        // Priority: 1) forwarded clientIp from request body, 2) HttpServletRequest remote address
        String clientIp = request.getClientIp();
        if (clientIp == null || clientIp.isBlank()) {
            clientIp = resolveClientIp(httpRequest);
        }
        String city = request.getCity();
        String country = request.getCountry();
        Double latitude = request.getLatitude();
        Double longitude = request.getLongitude();

        // If city not provided explicitly, try to derive from IP
        if ((city == null || city.isBlank()) && clientIp != null && !clientIp.isBlank()) {
            IpLocation loc = resolveIpToLocation(clientIp);
            if (loc != null) {
                city = loc.city();
                country = loc.country();
                latitude = loc.latitude();
                longitude = loc.longitude();
                log.info("Location resolved from IP {}: {}, {} ({},{})", clientIp, city, country, latitude, longitude);
            } else {
                log.debug("Could not resolve location from IP {}", clientIp);
            }
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
        transaction.setClientIp(clientIp);
        transaction.setLatitude(latitude != null ? latitude : 0.0);
        transaction.setLongitude(longitude != null ? longitude : 0.0);
        transaction.setCity(city != null ? city : "");
        transaction.setCountry(country != null ? country : "");
        Transaction saved = transactionRepository.save(transaction);

        // -- build event and detect fraud using CEP session (history is already in session) --
        long nowEpoch = saved.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        CardTransactionEvent event = new CardTransactionEvent(
            saved.getId(), clientId, card.getId(),
            senderAccount.getAccountNumber(), receiverAccount.getAccountNumber(),
            request.getAmount(), nowEpoch,
            saved.getLatitude(), saved.getLongitude(),
            saved.getCity() != null ? saved.getCity() : "",
            saved.getCountry() != null ? saved.getCountry() : ""
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
    public String approveTransaction(Long transactionId, Long clientId) {
        Transaction tx = transactionRepository.findById(transactionId).orElse(null);
        if (tx == null) {
            log.warn("approveTransaction: transaction {} not found", transactionId);
            return "Transaction not found";
        }
        if (!"SUSPICIOUS".equals(tx.getStatus())) {
            log.warn("approveTransaction: transaction {} status is {}, not SUSPICIOUS", transactionId, tx.getStatus());
            return "Transaction is not in suspicious state";
        }

        BankAccount senderAccount = bankAccountRepository.findByAccountNumberWithClient(tx.getSenderAccountNumber())
            .orElse(null);
        if (senderAccount == null) {
            log.warn("approveTransaction: sender account {} not found", tx.getSenderAccountNumber());
            return "Sender account not found";
        }
        if (senderAccount.getPackageAccount() == null
            || senderAccount.getPackageAccount().getClient() == null) {
            log.warn("approveTransaction: sender account {} has no package account or client", tx.getSenderAccountNumber());
            return "Sender account has no package account";
        }
        if (!senderAccount.getPackageAccount().getClient().getId().equals(clientId)) {
            log.warn("approveTransaction: client mismatch. Tx client: {}, caller client: {}",
                senderAccount.getPackageAccount().getClient().getId(), clientId);
            return "Transaction does not belong to you";
        }

        BankAccount receiverAccount = bankAccountRepository.findByAccountNumber(tx.getReceiverAccountNumber())
            .orElse(null);
        if (receiverAccount == null) {
            log.warn("approveTransaction: receiver account {} not found", tx.getReceiverAccountNumber());
            return "Receiver account not found";
        }
        if (senderAccount.getBalance() < tx.getAmount()) {
            log.warn("approveTransaction: insufficient funds. Balance: {}, needed: {}",
                senderAccount.getBalance(), tx.getAmount());
            return "Insufficient funds";
        }

        senderAccount.setBalance(senderAccount.getBalance() - tx.getAmount());
        receiverAccount.setBalance(receiverAccount.getBalance() + tx.getAmount());
        bankAccountRepository.save(senderAccount);
        bankAccountRepository.save(receiverAccount);

        tx.setStatus("APPROVED");
        transactionRepository.save(tx);
        return null;
    }

    @Transactional
    public String rejectTransaction(Long transactionId, Long clientId) {
        Transaction tx = transactionRepository.findById(transactionId).orElse(null);
        if (tx == null) {
            log.warn("rejectTransaction: transaction {} not found", transactionId);
            return "Transaction not found";
        }
        if (!"SUSPICIOUS".equals(tx.getStatus())) {
            log.warn("rejectTransaction: transaction {} status is {}, not SUSPICIOUS", transactionId, tx.getStatus());
            return "Transaction is not in suspicious state";
        }

        BankAccount senderAccount = bankAccountRepository.findByAccountNumberWithClient(tx.getSenderAccountNumber())
            .orElse(null);
        if (senderAccount == null) {
            log.warn("rejectTransaction: sender account {} not found", tx.getSenderAccountNumber());
            return "Sender account not found";
        }
        if (senderAccount.getPackageAccount() == null
            || senderAccount.getPackageAccount().getClient() == null) {
            log.warn("rejectTransaction: sender account {} has no package account or client", tx.getSenderAccountNumber());
            return "Sender account has no package account";
        }
        if (!senderAccount.getPackageAccount().getClient().getId().equals(clientId)) {
            log.warn("rejectTransaction: client mismatch. Tx client: {}, caller client: {}",
                senderAccount.getPackageAccount().getClient().getId(), clientId);
            return "Transaction does not belong to you";
        }

        tx.setStatus("REJECTED");
        transactionRepository.save(tx);
        return null;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        log.debug("Resolved client IP: {}", ip);
        return ip;
    }

    private record IpLocation(String city, String country, double latitude, double longitude) {}

    private IpLocation resolveIpToLocation(String ip) {
        try {
            InetAddress inet = InetAddress.getByName(ip);
            if (inet.isSiteLocalAddress() || inet.isLoopbackAddress()) {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
        // Free IP geolocation API fallback
        try {
            java.net.URL url = new java.net.URL("http://ip-api.com/json/" + ip + "?fields=city,country,lat,lon,status");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setRequestMethod("GET");
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                String json = new String(conn.getInputStream().readAllBytes());
                conn.disconnect();
                com.fasterxml.jackson.databind.JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(json);
                if ("success".equals(node.get("status").asText())) {
                    String city = node.get("city").asText("");
                    String country = node.get("country").asText("");
                    double lat = node.get("lat").asDouble();
                    double lon = node.get("lon").asDouble();
                    log.info("IP location resolved: {} -> {}/{}({},{})", ip, city, country, lat, lon);
                    return new IpLocation(city, country, lat, lon);
                }
            }
            conn.disconnect();
        } catch (Exception e) {
            log.debug("Failed to resolve IP location for {}: {}", ip, e.getMessage());
        }
        return null;
    }

    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return cardNumber;
        }
        String last4 = cardNumber.substring(cardNumber.length() - 4);
        return "**** **** **** " + last4;
    }
}
