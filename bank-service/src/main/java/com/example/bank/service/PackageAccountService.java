package com.example.bank.service;

import com.example.bank.dto.PackageAccountRequest;
import com.example.bank.dto.PackageAccountResponse;
import com.example.bank.model.BankAccount;
import com.example.bank.model.BankUser;
import com.example.bank.model.PackageAccount;
import com.example.bank.model.PaymentCard;
import com.example.bank.repository.BankAccountRepository;
import com.example.bank.repository.BankUserRepository;
import com.example.bank.repository.PackageAccountRepository;
import com.example.bank.repository.PaymentCardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PackageAccountService {

    private static final Set<String> SUPPORTED_CURRENCIES = Set.of("RSD", "EUR", "USD");
    private static final double DEFAULT_DAILY_LIMIT = 50000.0;
    private static final double DEFAULT_MONTHLY_LIMIT = 300000.0;
    private static final int DEFAULT_CARD_VALIDITY_YEARS = 3;

    private final BankUserRepository bankUserRepository;
    private final PackageAccountRepository packageAccountRepository;
    private final BankAccountRepository bankAccountRepository;
    private final PaymentCardRepository paymentCardRepository;
    private final BankIdentifierService identifierService;

    public PackageAccountService(BankUserRepository bankUserRepository,
                                 PackageAccountRepository packageAccountRepository,
                                 BankAccountRepository bankAccountRepository,
                                 PaymentCardRepository paymentCardRepository,
                                 BankIdentifierService identifierService) {
        this.bankUserRepository = bankUserRepository;
        this.packageAccountRepository = packageAccountRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.paymentCardRepository = paymentCardRepository;
        this.identifierService = identifierService;
    }

    @Transactional
    public PackageAccountResponse createPackageAccount(String username, PackageAccountRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Package account name is required");
        }
        if (request.getCurrency() == null || request.getCurrency().isBlank()) {
            throw new IllegalArgumentException("Currency is required");
        }
        String currency = request.getCurrency().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_CURRENCIES.contains(currency)) {
            throw new IllegalArgumentException("Unsupported currency. Allowed: " + SUPPORTED_CURRENCIES);
        }
        if (request.getInitialBalance() != null && request.getInitialBalance() < 0) {
            throw new IllegalArgumentException("Initial balance cannot be negative");
        }
        if (request.getCardholderName() == null || request.getCardholderName().isBlank()) {
            throw new IllegalArgumentException("Cardholder name is required");
        }

        BankUser client = bankUserRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Client not found: " + username));

        String name = request.getName().trim();
        if (packageAccountRepository.existsByNameAndClientUsername(name, username)) {
            throw new IllegalArgumentException("You already have a package account with this name");
        }

        PackageAccount packageAccount = new PackageAccount();
        packageAccount.setName(name);
        packageAccount.setClient(client);
        packageAccount.setCreatedAt(LocalDateTime.now());
        PackageAccount savedPackage = packageAccountRepository.save(packageAccount);

        BankAccount bankAccount = new BankAccount();
        bankAccount.setAccountNumber(generateUniqueAccountNumber());
        bankAccount.setBalance(request.getInitialBalance() != null ? request.getInitialBalance() : 0.0);
        bankAccount.setCurrency(currency);
        bankAccount.setDailyLimit(DEFAULT_DAILY_LIMIT);
        bankAccount.setMonthlyLimit(DEFAULT_MONTHLY_LIMIT);
        bankAccount.setCreatedAt(LocalDateTime.now());
        bankAccount.setPackageAccount(savedPackage);
        BankAccount savedAccount = bankAccountRepository.save(bankAccount);

        int years = request.getCardExpirationYears() != null && request.getCardExpirationYears() > 0
                ? request.getCardExpirationYears()
                : DEFAULT_CARD_VALIDITY_YEARS;

        PaymentCard card = new PaymentCard();
        card.setCardNumber(generateUniqueCardNumber());
        card.setCardholderName(request.getCardholderName().trim());
        card.setExpirationDate(LocalDate.now().plusYears(years).withDayOfMonth(1));
        card.setCvv(identifierService.generateCvv());
        card.setCreatedAt(LocalDateTime.now());
        card.setPackageAccount(savedPackage);
        PaymentCard savedCard = paymentCardRepository.save(card);

        return mapToResponse(savedPackage, savedAccount, savedCard, client.getUsername());
    }

    public List<PackageAccountResponse> getPackageAccountsForClient(String username) {
        return packageAccountRepository.findByClientUsername(username).stream()
                .map(pa -> mapToResponse(pa, findAccountForPackage(pa), findCardForPackage(pa), username))
                .collect(Collectors.toList());
    }

    public PackageAccountResponse getPackageAccountForClient(String username, Long packageAccountId) {
        PackageAccount packageAccount = packageAccountRepository.findById(packageAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Package account not found"));
        if (!packageAccount.getClient().getUsername().equals(username)) {
            throw new IllegalArgumentException("Package account does not belong to the authenticated user");
        }
        BankAccount account = findAccountForPackage(packageAccount);
        PaymentCard card = findCardForPackage(packageAccount);
        return mapToResponse(packageAccount, account, card, username);
    }

    private BankAccount findAccountForPackage(PackageAccount packageAccount) {
        return bankAccountRepository.findAll().stream()
                .filter(a -> a.getPackageAccount() != null && a.getPackageAccount().getId().equals(packageAccount.getId()))
                .findFirst()
                .orElse(null);
    }

    private PaymentCard findCardForPackage(PackageAccount packageAccount) {
        return paymentCardRepository.findAll().stream()
                .filter(c -> c.getPackageAccount() != null && c.getPackageAccount().getId().equals(packageAccount.getId()))
                .findFirst()
                .orElse(null);
    }

    private String generateUniqueAccountNumber() {
        for (int i = 0; i < 20; i++) {
            String candidate = identifierService.generateAccountNumber();
            if (!bankAccountRepository.existsByAccountNumber(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not generate a unique account number");
    }

    private String generateUniqueCardNumber() {
        for (int i = 0; i < 20; i++) {
            String candidate = identifierService.generateCardNumber();
            if (!paymentCardRepository.existsByCardNumber(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not generate a unique card number");
    }

    private PackageAccountResponse mapToResponse(PackageAccount packageAccount, BankAccount account,
                                                 PaymentCard card, String ownerUsername) {
        PackageAccountResponse.BankAccountDto accountDto = account != null
                ? new PackageAccountResponse.BankAccountDto(
                        account.getId(),
                        account.getAccountNumber(),
                        account.getBalance(),
                        account.getCurrency(),
                        account.getDailyLimit(),
                        account.getMonthlyLimit())
                : null;

        PackageAccountResponse.PaymentCardDto cardDto = card != null
                ? new PackageAccountResponse.PaymentCardDto(
                        card.getId(),
                        card.getCardNumber(),
                        card.getCardholderName(),
                        card.getExpirationDate(),
                        card.getCvv())
                : null;

        return new PackageAccountResponse(
                packageAccount.getId(),
                packageAccount.getName(),
                ownerUsername,
                packageAccount.getCreatedAt(),
                accountDto,
                cardDto);
    }
}
