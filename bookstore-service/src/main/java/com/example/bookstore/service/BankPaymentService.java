package com.example.bookstore.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class BankPaymentService {

    private final RestTemplate restTemplate;
    private final String bankServiceUrl;
    private final String receiverAccountNumber;

    public BankPaymentService(@Value("${bank.service.url}") String bankServiceUrl,
                              @Value("${bank.service.receiver-account-number}") String receiverAccountNumber) {
        this.restTemplate = new RestTemplate();
        this.bankServiceUrl = bankServiceUrl;
        this.receiverAccountNumber = receiverAccountNumber;
    }

    public Map<String, Object> processCardPayment(String cardNumber, String cardCvv,
                                                   String cardExpirationDate, String cardholderName,
                                                   double amount, String clientIp) {
        // Build mutable map to optionally include clientIp
        java.util.Map<String, Object> paymentRequest = new java.util.HashMap<>();
        paymentRequest.put("receiverAccountNumber", receiverAccountNumber);
        paymentRequest.put("amount", amount);
        paymentRequest.put("cardNumber", cardNumber);
        paymentRequest.put("cardCvv", cardCvv);
        paymentRequest.put("cardExpirationDate", cardExpirationDate);
        paymentRequest.put("cardholderName", cardholderName);
        paymentRequest.put("description", "Bookstore purchase");
        if (clientIp != null && !clientIp.isBlank()) {
            paymentRequest.put("clientIp", clientIp);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(paymentRequest, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    bankServiceUrl + "/api/payments/process",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );
            return response.getBody();
        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "message", "Failed to connect to bank service: " + e.getMessage()
            );
        }
    }
}
