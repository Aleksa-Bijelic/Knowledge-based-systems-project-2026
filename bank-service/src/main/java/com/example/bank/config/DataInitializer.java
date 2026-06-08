package com.example.bank.config;

import com.example.bank.model.BankAccount;
import com.example.bank.model.BankUser;
import com.example.bank.model.PackageAccount;
import com.example.bank.model.PaymentCard;
import com.example.bank.repository.BankAccountRepository;
import com.example.bank.repository.BankUserRepository;
import com.example.bank.repository.PackageAccountRepository;
import com.example.bank.repository.PaymentCardRepository;
import com.example.bank.service.BankIdentifierService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initDefaultUsers(BankUserRepository bankUserRepository,
                                              PasswordEncoder passwordEncoder,
                                              PackageAccountRepository packageAccountRepository,
                                              BankAccountRepository bankAccountRepository,
                                              PaymentCardRepository paymentCardRepository,
                                              BankIdentifierService identifierService) {
        return args -> {
            String officerUsername = "sluzbenik";
            if (!bankUserRepository.existsByUsername(officerUsername)) {
                BankUser officer = new BankUser();
                officer.setUsername(officerUsername);
                officer.setEmail("sluzbenik@bank.example.com");
                officer.setPassword(passwordEncoder.encode("sluzbenik123"));
                officer.setFirstName("Petar");
                officer.setLastName("Petrović");
                officer.setRole("ROLE_OFFICER");
                officer.setCreatedAt(LocalDateTime.now());
                bankUserRepository.save(officer);
                System.out.println("Created default bank officer: sluzbenik / sluzbenik123");
            }

            String clientUsername = "oliva";
            if (!bankUserRepository.existsByUsername(clientUsername)) {
                BankUser client = new BankUser();
                client.setUsername(clientUsername);
                client.setEmail("oliva@bank.example.com");
                client.setPassword(passwordEncoder.encode("oliva123"));
                client.setFirstName("Oliva");
                client.setLastName("Maslina");
                client.setRole("ROLE_CLIENT");
                client.setCreatedAt(LocalDateTime.now());
                bankUserRepository.save(client);
                System.out.println("Created default bank client: oliva / oliva123");
            }

            // Create bookstore account for oliva if it doesn't exist
            String bookstoreAccountName = "Bookstore Revenue";
            if (!packageAccountRepository.existsByNameAndClientUsername(bookstoreAccountName, clientUsername)) {
                BankUser olivaClient = bankUserRepository.findByUsername(clientUsername)
                        .orElseThrow(() -> new RuntimeException("Client oliva not found"));

                PackageAccount bookstorePackage = new PackageAccount();
                bookstorePackage.setName(bookstoreAccountName);
                bookstorePackage.setClient(olivaClient);
                bookstorePackage.setCreatedAt(LocalDateTime.now());
                PackageAccount savedPackage = packageAccountRepository.save(bookstorePackage);

                BankAccount bookstoreBankAccount = new BankAccount();
                bookstoreBankAccount.setAccountNumber(identifierService.generateAccountNumber());
                bookstoreBankAccount.setBalance(0.0);
                bookstoreBankAccount.setCurrency("RSD");
                bookstoreBankAccount.setCreatedAt(LocalDateTime.now());
                bookstoreBankAccount.setPackageAccount(savedPackage);
                BankAccount savedAccount = bankAccountRepository.save(bookstoreBankAccount);

                PaymentCard bookstoreCard = new PaymentCard();
                bookstoreCard.setCardNumber(identifierService.generateCardNumber());
                bookstoreCard.setCardholderName("Oliva Maslina");
                bookstoreCard.setExpirationDate(LocalDate.now().plusYears(3).withDayOfMonth(1));
                bookstoreCard.setCvv(identifierService.generateCvv());
                bookstoreCard.setCreatedAt(LocalDateTime.now());
                bookstoreCard.setPackageAccount(savedPackage);
                PaymentCard savedCard = paymentCardRepository.save(bookstoreCard);

                System.out.println("=== BOOKSTORE ACCOUNT CREATED ===");
                System.out.println("Account number: " + savedAccount.getAccountNumber());
                System.out.println("Card number: " + savedCard.getCardNumber());
                System.out.println("Card CVV: " + savedCard.getCvv());
                System.out.println("Card expiration: " + savedCard.getExpirationDate());
                System.out.println("Cardholder: " + savedCard.getCardholderName());
                System.out.println("=================================");
            }
        };
    }
}
