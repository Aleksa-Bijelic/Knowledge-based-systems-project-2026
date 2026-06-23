package com.example.bank.rules;

public class DebtToIncomeFact {

    private Long clientId;
    private double value;

    public DebtToIncomeFact() {}

    public DebtToIncomeFact(Long clientId, double value) {
        this.clientId = clientId;
        this.value = value;
    }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }
    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }
}
