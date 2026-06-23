package com.example.bank.rules;

public class RiskThresholdFact {

    private double threshold;

    public RiskThresholdFact() {
        this.threshold = 50.0;
    }

    public RiskThresholdFact(double threshold) {
        this.threshold = threshold;
    }

    public double getThreshold() { return threshold; }
    public void setThreshold(double threshold) { this.threshold = threshold; }
}
