package com.example.bank.rules;

public class RiskFactorFact {

    private Long clientId;
    private double points;
    private String reason;

    public RiskFactorFact() {}

    public RiskFactorFact(Long clientId, double points, String reason) {
        this.clientId = clientId;
        this.points = points;
        this.reason = reason;
    }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }
    public double getPoints() { return points; }
    public void setPoints(double points) { this.points = points; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
