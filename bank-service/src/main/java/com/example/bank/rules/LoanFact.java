package com.example.bank.rules;

public class LoanFact {

    private Long loanId;
    private Long clientId;
    private double monthlyPayment;
    private double remainingBalance;
    private String status;
    private int numberOfInstallments;

    public LoanFact() {}

    public LoanFact(Long loanId, Long clientId, double monthlyPayment,
                    double remainingBalance, String status, int numberOfInstallments) {
        this.loanId = loanId;
        this.clientId = clientId;
        this.monthlyPayment = monthlyPayment;
        this.remainingBalance = remainingBalance;
        this.status = status;
        this.numberOfInstallments = numberOfInstallments;
    }

    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }
    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }
    public double getMonthlyPayment() { return monthlyPayment; }
    public void setMonthlyPayment(double monthlyPayment) { this.monthlyPayment = monthlyPayment; }
    public double getRemainingBalance() { return remainingBalance; }
    public void setRemainingBalance(double remainingBalance) { this.remainingBalance = remainingBalance; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getNumberOfInstallments() { return numberOfInstallments; }
    public void setNumberOfInstallments(int numberOfInstallments) { this.numberOfInstallments = numberOfInstallments; }
}
