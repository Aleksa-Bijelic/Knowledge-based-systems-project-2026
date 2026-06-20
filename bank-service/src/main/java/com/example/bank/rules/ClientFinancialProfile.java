package com.example.bank.rules;

public class ClientFinancialProfile {

    private Long clientId;
    private int age;
    private double monthlyIncome;
    private double totalExistingLoanPayments;
    private int existingLoanCount;
    private double accountBalance;

    public ClientFinancialProfile() {}

    public ClientFinancialProfile(Long clientId, int age, double monthlyIncome,
                                  double totalExistingLoanPayments,
                                  int existingLoanCount,
                                  double accountBalance) {
        this.clientId = clientId;
        this.age = age;
        this.monthlyIncome = monthlyIncome;
        this.totalExistingLoanPayments = totalExistingLoanPayments;
        this.existingLoanCount = existingLoanCount;
        this.accountBalance = accountBalance;
    }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    public double getMonthlyIncome() { return monthlyIncome; }
    public void setMonthlyIncome(double monthlyIncome) { this.monthlyIncome = monthlyIncome; }
    public double getTotalExistingLoanPayments() { return totalExistingLoanPayments; }
    public void setTotalExistingLoanPayments(double totalExistingLoanPayments) { this.totalExistingLoanPayments = totalExistingLoanPayments; }
    public int getExistingLoanCount() { return existingLoanCount; }
    public void setExistingLoanCount(int existingLoanCount) { this.existingLoanCount = existingLoanCount; }
    public double getAccountBalance() { return accountBalance; }
    public void setAccountBalance(double accountBalance) { this.accountBalance = accountBalance; }
}
