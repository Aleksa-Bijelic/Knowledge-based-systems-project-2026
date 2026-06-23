package com.example.bank.rules;

public class DecisionReasonFact {

    private Long clientId;
    private String reason;

    public DecisionReasonFact() {}

    public DecisionReasonFact(Long clientId, String reason) {
        this.clientId = clientId;
        this.reason = reason;
    }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
