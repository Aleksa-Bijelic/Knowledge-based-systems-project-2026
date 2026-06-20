package com.example.bank.rules;

import java.time.LocalDate;

public class LoanRequestFact {

    private Long clientId;
    private double loanAmount;
    private int numberOfInstallments;
    private String employmentStatus;
    private LocalDate contractStartDate;
    private LocalDate contractEndDate;

    public LoanRequestFact() {
    }

    public LoanRequestFact(Long clientId, double loanAmount, int numberOfInstallments,
                           String employmentStatus, LocalDate contractStartDate,
                           LocalDate contractEndDate) {
        this.clientId = clientId;
        this.loanAmount = loanAmount;
        this.numberOfInstallments = numberOfInstallments;
        this.employmentStatus = employmentStatus;
        this.contractStartDate = contractStartDate;
        this.contractEndDate = contractEndDate;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public double getLoanAmount() {
        return loanAmount;
    }

    public void setLoanAmount(double loanAmount) {
        this.loanAmount = loanAmount;
    }

    public int getNumberOfInstallments() {
        return numberOfInstallments;
    }

    public void setNumberOfInstallments(int numberOfInstallments) {
        this.numberOfInstallments = numberOfInstallments;
    }

    public String getEmploymentStatus() {
        return employmentStatus;
    }

    public void setEmploymentStatus(String employmentStatus) {
        this.employmentStatus = employmentStatus;
    }

    public LocalDate getContractStartDate() {
        return contractStartDate;
    }

    public void setContractStartDate(LocalDate contractStartDate) {
        this.contractStartDate = contractStartDate;
    }

    public LocalDate getContractEndDate() {
        return contractEndDate;
    }

    public void setContractEndDate(LocalDate contractEndDate) {
        this.contractEndDate = contractEndDate;
    }
}
