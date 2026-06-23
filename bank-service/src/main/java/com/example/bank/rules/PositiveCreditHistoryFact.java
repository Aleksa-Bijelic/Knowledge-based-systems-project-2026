package com.example.bank.rules;

public class PositiveCreditHistoryFact {

    private Long clientId;

    public PositiveCreditHistoryFact() {}

    public PositiveCreditHistoryFact(Long clientId) {
        this.clientId = clientId;
    }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }
}
