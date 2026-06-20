package com.example.bank.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "loan_requests")
public class LoanRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "loan_amount", nullable = false)
    private double loanAmount;

    @Column(name = "number_of_installments", nullable = false)
    private int numberOfInstallments;

    @Column(name = "employment_status", nullable = false)
    private String employmentStatus;

    @Column(name = "contract_start_date")
    private LocalDate contractStartDate;

    @Column(name = "contract_end_date")
    private LocalDate contractEndDate;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "risk_score")
    private double riskScore;

    @Column(name = "risk_level")
    private String riskLevel;

    @Column(name = "monthly_payment")
    private double monthlyPayment;

    @Column(name = "dti_ratio")
    private double debtToIncomeRatio;

    @Column(name = "system_recommendation")
    private String systemRecommendation;

    @Column(name = "officer_decision")
    private String officerDecision;

    @Column(name = "officer_username")
    private String officerUsername;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public LoanRequest() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }
    public double getLoanAmount() { return loanAmount; }
    public void setLoanAmount(double loanAmount) { this.loanAmount = loanAmount; }
    public int getNumberOfInstallments() { return numberOfInstallments; }
    public void setNumberOfInstallments(int numberOfInstallments) { this.numberOfInstallments = numberOfInstallments; }
    public String getEmploymentStatus() { return employmentStatus; }
    public void setEmploymentStatus(String employmentStatus) { this.employmentStatus = employmentStatus; }
    public LocalDate getContractStartDate() { return contractStartDate; }
    public void setContractStartDate(LocalDate contractStartDate) { this.contractStartDate = contractStartDate; }
    public LocalDate getContractEndDate() { return contractEndDate; }
    public void setContractEndDate(LocalDate contractEndDate) { this.contractEndDate = contractEndDate; }
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
