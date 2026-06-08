package com.example.bank.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class PackageAccountResponse {
    private Long id;
    private String name;
    private String ownerUsername;
    private LocalDateTime createdAt;
    private BankAccountDto account;
    private PaymentCardDto card;

    public PackageAccountResponse() {
    }

    public PackageAccountResponse(Long id, String name, String ownerUsername, LocalDateTime createdAt,
                                  BankAccountDto account, PaymentCardDto card) {
        this.id = id;
        this.name = name;
        this.ownerUsername = ownerUsername;
        this.createdAt = createdAt;
        this.account = account;
        this.card = card;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public BankAccountDto getAccount() {
        return account;
    }

    public void setAccount(BankAccountDto account) {
        this.account = account;
    }

    public PaymentCardDto getCard() {
        return card;
    }

    public void setCard(PaymentCardDto card) {
        this.card = card;
    }

    public static class BankAccountDto {
        private Long id;
        private String accountNumber;
        private double balance;
        private String currency;
        private double dailyLimit;
        private double monthlyLimit;

        public BankAccountDto() {
        }

        public BankAccountDto(Long id, String accountNumber, double balance, String currency,
                              double dailyLimit, double monthlyLimit) {
            this.id = id;
            this.accountNumber = accountNumber;
            this.balance = balance;
            this.currency = currency;
            this.dailyLimit = dailyLimit;
            this.monthlyLimit = monthlyLimit;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getAccountNumber() {
            return accountNumber;
        }

        public void setAccountNumber(String accountNumber) {
            this.accountNumber = accountNumber;
        }

        public double getBalance() {
            return balance;
        }

        public void setBalance(double balance) {
            this.balance = balance;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public double getDailyLimit() {
            return dailyLimit;
        }

        public void setDailyLimit(double dailyLimit) {
            this.dailyLimit = dailyLimit;
        }

        public double getMonthlyLimit() {
            return monthlyLimit;
        }

        public void setMonthlyLimit(double monthlyLimit) {
            this.monthlyLimit = monthlyLimit;
        }
    }

    public static class PaymentCardDto {
        private Long id;
        private String cardNumber;
        private String maskedCardNumber;
        private String cardholderName;
        private LocalDate expirationDate;
        private String cvv;

        public PaymentCardDto() {
        }

        public PaymentCardDto(Long id, String cardNumber, String cardholderName,
                              LocalDate expirationDate, String cvv) {
            this.id = id;
            this.cardNumber = cardNumber;
            this.maskedCardNumber = maskCardNumber(cardNumber);
            this.cardholderName = cardholderName;
            this.expirationDate = expirationDate;
            this.cvv = cvv;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getCardNumber() {
            return cardNumber;
        }

        public void setCardNumber(String cardNumber) {
            this.cardNumber = cardNumber;
            this.maskedCardNumber = maskCardNumber(cardNumber);
        }

        public String getMaskedCardNumber() {
            return maskedCardNumber;
        }

        public void setMaskedCardNumber(String maskedCardNumber) {
            this.maskedCardNumber = maskedCardNumber;
        }

        public String getCardholderName() {
            return cardholderName;
        }

        public void setCardholderName(String cardholderName) {
            this.cardholderName = cardholderName;
        }

        public LocalDate getExpirationDate() {
            return expirationDate;
        }

        public void setExpirationDate(LocalDate expirationDate) {
            this.expirationDate = expirationDate;
        }

        public String getCvv() {
            return cvv;
        }

        public void setCvv(String cvv) {
            this.cvv = cvv;
        }

        private static String maskCardNumber(String cardNumber) {
            if (cardNumber == null || cardNumber.length() < 4) {
                return cardNumber;
            }
            String last4 = cardNumber.substring(cardNumber.length() - 4);
            return "**** **** **** " + last4;
        }
    }
}
