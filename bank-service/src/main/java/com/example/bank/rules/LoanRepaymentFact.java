package com.example.bank.rules;

import java.time.LocalDate;

public class LoanRepaymentFact {

    private Long repaymentId;
    private Long loanId;
    private double amount;
    private LocalDate dueDate;
    private LocalDate paidDate;
    private String status;

    public LoanRepaymentFact() {}

    public LoanRepaymentFact(Long repaymentId, Long loanId, double amount,
                             LocalDate dueDate, LocalDate paidDate, String status) {
        this.repaymentId = repaymentId;
        this.loanId = loanId;
        this.amount = amount;
        this.dueDate = dueDate;
        this.paidDate = paidDate;
        this.status = status;
    }

    public Long getRepaymentId() { return repaymentId; }
    public void setRepaymentId(Long repaymentId) { this.repaymentId = repaymentId; }
    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public LocalDate getPaidDate() { return paidDate; }
    public void setPaidDate(LocalDate paidDate) { this.paidDate = paidDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
