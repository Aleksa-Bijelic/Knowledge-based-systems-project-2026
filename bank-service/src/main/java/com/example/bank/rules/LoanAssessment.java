package com.example.bank.rules;

import java.util.ArrayList;
import java.util.List;

public class LoanAssessment {

    private Long clientId;
    private boolean approved;
    private List<String> reasons;
    private double riskScore;
    private double monthlyPayment;
    private double debtToIncomeRatio;
    private String riskLevel;

    public LoanAssessment() {
        this.reasons = new ArrayList<>();
        this.approved = true;
        this.riskScore = 0.0;
    }

    public LoanAssessment(Long clientId) {
        this.clientId = clientId;
        this.reasons = new ArrayList<>();
        this.approved = true;
        this.riskScore = 0.0;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public void setReasons(List<String> reasons) {
        this.reasons = reasons;
    }

    public void addReason(String reason) {
        this.reasons.add(reason);
    }

    public double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(double riskScore) {
        this.riskScore = riskScore;
    }

    public double getMonthlyPayment() {
        return monthlyPayment;
    }

    public void setMonthlyPayment(double monthlyPayment) {
        this.monthlyPayment = monthlyPayment;
    }

    public double getDebtToIncomeRatio() {
        return debtToIncomeRatio;
    }

    public void setDebtToIncomeRatio(double debtToIncomeRatio) {
        this.debtToIncomeRatio = debtToIncomeRatio;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }
}
