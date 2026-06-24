package com.example.bank.rules;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class CardTransactionEvent {

    private Long transactionId;
    private Long clientId;
    private Long cardId;
    private String payerAccountNumber;
    private String receiverAccountNumber;
    private double amount;
    private long timestamp;
    private double latitude;
    private double longitude;
    private String city;
    private String country;

    public CardTransactionEvent() {
    }

    public CardTransactionEvent(Long transactionId, Long clientId, Long cardId,
                                 String payerAccountNumber, String receiverAccountNumber,
                                 double amount, long timestamp,
                                 double latitude, double longitude,
                                 String city, String country) {
        this.transactionId = transactionId;
        this.clientId = clientId;
        this.cardId = cardId;
        this.payerAccountNumber = payerAccountNumber;
        this.receiverAccountNumber = receiverAccountNumber;
        this.amount = amount;
        this.timestamp = timestamp;
        this.latitude = latitude;
        this.longitude = longitude;
        this.city = city;
        this.country = country;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public Long getClientId() {
        return clientId;
    }

    public Long getCardId() {
        return cardId;
    }

    public String getPayerAccountNumber() {
        return payerAccountNumber;
    }

    public String getReceiverAccountNumber() {
        return receiverAccountNumber;
    }

    public double getAmount() {
        return amount;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getHour() {
        return LocalDateTime.ofInstant(
            Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()).getHour();
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getCity() {
        return city;
    }

    public String getCountry() {
        return country;
    }
}
