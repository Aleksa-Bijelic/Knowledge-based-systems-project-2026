package com.example.bank.rules;

public class SuspiciousTransactionFact {

    private Long transactionId;
    private String reason;

    public SuspiciousTransactionFact() {
    }

    public SuspiciousTransactionFact(Long transactionId, String reason) {
        this.transactionId = transactionId;
        this.reason = reason;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public String getReason() {
        return reason;
    }
}
