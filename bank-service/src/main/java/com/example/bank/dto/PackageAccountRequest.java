package com.example.bank.dto;

import java.time.LocalDate;

public class PackageAccountRequest {
    private String name;
    private String currency;
    private Double initialBalance;
    private String cardholderName;
    private Integer cardExpirationYears;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Double getInitialBalance() {
        return initialBalance;
    }

    public void setInitialBalance(Double initialBalance) {
        this.initialBalance = initialBalance;
    }

    public String getCardholderName() {
        return cardholderName;
    }

    public void setCardholderName(String cardholderName) {
        this.cardholderName = cardholderName;
    }

    public Integer getCardExpirationYears() {
        return cardExpirationYears;
    }

    public void setCardExpirationYears(Integer cardExpirationYears) {
        this.cardExpirationYears = cardExpirationYears;
    }
}
