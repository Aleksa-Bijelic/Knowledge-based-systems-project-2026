package com.example.bank.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class BankIdentifierService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ACCOUNT_PREFIX = "265";
    private static final String CARD_PREFIX = "4539";

    public String generateAccountNumber() {
        StringBuilder builder = new StringBuilder(ACCOUNT_PREFIX);
        for (int i = 0; i < 13; i++) {
            builder.append(RANDOM.nextInt(10));
        }
        return builder.toString();
    }

    public String generateCardNumber() {
        StringBuilder builder = new StringBuilder(CARD_PREFIX);
        for (int i = 0; i < 12; i++) {
            builder.append(RANDOM.nextInt(10));
        }
        String number = builder.toString();
        return number + luhnCheckDigit(number);
    }

    public String generateCvv() {
        return String.format("%03d", RANDOM.nextInt(1000));
    }

    private char luhnCheckDigit(String number) {
        int sum = 0;
        boolean alternate = true;
        for (int i = number.length() - 1; i >= 0; i--) {
            int n = number.charAt(i) - '0';
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        int mod = sum % 10;
        return (char) ((mod == 0) ? '0' : (10 - mod) + '0');
    }
}
