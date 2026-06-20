package com.example.bank.dto;

import java.util.List;

public class LoanResponseDTO {

    private Long requestId;
    private Long clientId;
    private boolean approved;
    private List<String> reasons;
    private double riskScore;
    private String riskLevel;
    private double monthlyPayment;
    private double debtToIncomeRatio;

    public LoanResponseDTO() {
    }

    public LoanResponseDTO(Long requestId, Long clientId, boolean approved, List<String> reasons,
                           double riskScore, String riskLevel, double monthlyPayment,
                           double debtToIncomeRatio) {
        this.requestId = requestId;
        this.clientId = clientId;
        this.approved = approved;
        this.reasons = reasons;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.monthlyPayment = monthlyPayment;
        this.debtToIncomeRatio = debtToIncomeRatio;
    }

    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }
    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }
    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }
    public List<String> getReasons() { return reasons; }
    public void setReasons(List<String> reasons) { this.reasons = reasons; }
    public double getRiskScore() { return riskScore; }
    public void setRiskScore(double riskScore) { this.riskScore = riskScore; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public double getMonthlyPayment() { return monthlyPayment; }
    public void setMonthlyPayment(double monthlyPayment) { this.monthlyPayment = monthlyPayment; }
    public double getDebtToIncomeRatio() { return debtToIncomeRatio; }
    public void setDebtToIncomeRatio(double debtToIncomeRatio) { this.debtToIncomeRatio = debtToIncomeRatio; }
}
