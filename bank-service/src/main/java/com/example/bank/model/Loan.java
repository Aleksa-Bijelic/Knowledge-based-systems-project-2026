package com.example.bank.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "loans")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "request_id")
    private Long requestId;

    @Column(name = "loan_amount", nullable = false)
    private double loanAmount;

    @Column(name = "number_of_installments", nullable = false)
    private int numberOfInstallments;

    @Column(name = "monthly_payment")
    private double monthlyPayment;

    @Column(name = "interest_rate")
    private double interestRate;

    @Column(name = "remaining_balance")
    private double remainingBalance;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "officer_decision", nullable = false)
    private String officerDecision;

    @Column(name = "officer_username")
    private String officerUsername;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Loan() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }
    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }
    public double getLoanAmount() { return loanAmount; }
    public void setLoanAmount(double loanAmount) { this.loanAmount = loanAmount; }
    public int getNumberOfInstallments() { return numberOfInstallments; }
    public void setNumberOfInstallments(int numberOfInstallments) { this.numberOfInstallments = numberOfInstallments; }
    public double getMonthlyPayment() { return monthlyPayment; }
    public void setMonthlyPayment(double monthlyPayment) { this.monthlyPayment = monthlyPayment; }
    public double getInterestRate() { return interestRate; }
    public void setInterestRate(double interestRate) { this.interestRate = interestRate; }
    public double getRemainingBalance() { return remainingBalance; }
    public void setRemainingBalance(double remainingBalance) { this.remainingBalance = remainingBalance; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getOfficerDecision() { return officerDecision; }
    public void setOfficerDecision(String officerDecision) { this.officerDecision = officerDecision; }
    public String getOfficerUsername() { return officerUsername; }
    public void setOfficerUsername(String officerUsername) { this.officerUsername = officerUsername; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
