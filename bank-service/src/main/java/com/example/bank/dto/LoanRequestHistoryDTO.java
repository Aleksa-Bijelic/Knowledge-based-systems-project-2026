package com.example.bank.dto;

import java.time.LocalDateTime;

public class LoanRequestHistoryDTO {

    private Long id;
    private Long clientId;
    private String clientName;
    private double loanAmount;
    private int numberOfInstallments;
    private String employmentStatus;
    private String status;
    private double riskScore;
    private String riskLevel;
    private double monthlyPayment;
    private double debtToIncomeRatio;
    private String systemRecommendation;
    private String officerDecision;
    private String officerUsername;
    private LocalDateTime createdAt;

    public LoanRequestHistoryDTO() {}

    public LoanRequestHistoryDTO(Long id, Long clientId, String clientName,
                                  double loanAmount, int numberOfInstallments,
                                  String employmentStatus, String status,
                                  double riskScore, String riskLevel,
                                  double monthlyPayment, double debtToIncomeRatio,
                                  String systemRecommendation, String officerDecision,
                                  String officerUsername, LocalDateTime createdAt) {
        this.id = id;
        this.clientId = clientId;
        this.clientName = clientName;
        this.loanAmount = loanAmount;
        this.numberOfInstallments = numberOfInstallments;
        this.employmentStatus = employmentStatus;
        this.status = status;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.monthlyPayment = monthlyPayment;
        this.debtToIncomeRatio = debtToIncomeRatio;
        this.systemRecommendation = systemRecommendation;
        this.officerDecision = officerDecision;
        this.officerUsername = officerUsername;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public double getLoanAmount() { return loanAmount; }
    public void setLoanAmount(double loanAmount) { this.loanAmount = loanAmount; }
    public int getNumberOfInstallments() { return numberOfInstallments; }
    public void setNumberOfInstallments(int numberOfInstallments) { this.numberOfInstallments = numberOfInstallments; }
    public String getEmploymentStatus() { return employmentStatus; }
    public void setEmploymentStatus(String employmentStatus) { this.employmentStatus = employmentStatus; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public double getRiskScore() { return riskScore; }
    public void setRiskScore(double riskScore) { this.riskScore = riskScore; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public double getMonthlyPayment() { return monthlyPayment; }
    public void setMonthlyPayment(double monthlyPayment) { this.monthlyPayment = monthlyPayment; }
    public double getDebtToIncomeRatio() { return debtToIncomeRatio; }
    public void setDebtToIncomeRatio(double debtToIncomeRatio) { this.debtToIncomeRatio = debtToIncomeRatio; }
    public String getSystemRecommendation() { return systemRecommendation; }
    public void setSystemRecommendation(String systemRecommendation) { this.systemRecommendation = systemRecommendation; }
    public String getOfficerDecision() { return officerDecision; }
    public void setOfficerDecision(String officerDecision) { this.officerDecision = officerDecision; }
    public String getOfficerUsername() { return officerUsername; }
    public void setOfficerUsername(String officerUsername) { this.officerUsername = officerUsername; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
