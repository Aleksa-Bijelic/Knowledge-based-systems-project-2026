package com.example.bank.dto;

import java.time.LocalDateTime;

public class PaymentResponse {
    private boolean success;
    private String message;
    private Long transactionId;
    private double amount;
    private String senderAccountNumber;
    private String receiverAccountNumber;
    private LocalDateTime timestamp;

    public PaymentResponse() {
    }

    public PaymentResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public static PaymentResponse ok(Long transactionId, double amount, String sender, String receiver) {
        PaymentResponse resp = new PaymentResponse(true, "Payment successful");
        resp.setTransactionId(transactionId);
        resp.setAmount(amount);
        resp.setSenderAccountNumber(sender);
        resp.setReceiverAccountNumber(receiver);
        return resp;
    }

    public static PaymentResponse fail(String message) {
        return new PaymentResponse(false, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getSenderAccountNumber() {
        return senderAccountNumber;
    }

    public void setSenderAccountNumber(String senderAccountNumber) {
        this.senderAccountNumber = senderAccountNumber;
    }

    public String getReceiverAccountNumber() {
        return receiverAccountNumber;
    }

    public void setReceiverAccountNumber(String receiverAccountNumber) {
        this.receiverAccountNumber = receiverAccountNumber;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
