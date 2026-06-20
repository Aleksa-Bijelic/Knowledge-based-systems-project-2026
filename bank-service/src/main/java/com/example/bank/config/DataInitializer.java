package com.example.bank.config;

import com.example.bank.model.BankAccount;
import com.example.bank.model.BankUser;
import com.example.bank.model.Loan;
import com.example.bank.model.LoanRepayment;
import com.example.bank.model.PackageAccount;
import com.example.bank.model.PaymentCard;
import com.example.bank.model.Transaction;
import com.example.bank.repository.BankAccountRepository;
import com.example.bank.repository.BankUserRepository;
import com.example.bank.repository.LoanRepaymentRepository;
import com.example.bank.repository.LoanRepository;
import com.example.bank.repository.PackageAccountRepository;
import com.example.bank.repository.PaymentCardRepository;
import com.example.bank.repository.TransactionRepository;
import com.example.bank.service.BankIdentifierService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initDefaultUsers(BankUserRepository bankUserRepository,
                                               PasswordEncoder passwordEncoder,
                                               PackageAccountRepository packageAccountRepository,
                                               BankAccountRepository bankAccountRepository,
                                               PaymentCardRepository paymentCardRepository,
                                               TransactionRepository transactionRepository,
                                               LoanRepository loanRepository,
                                               LoanRepaymentRepository loanRepaymentRepository,
                                               BankIdentifierService identifierService) {
        return args -> {
            String officerUsername = "sluzbenik";
            if (!bankUserRepository.existsByUsername(officerUsername)) {
                BankUser officer = new BankUser();
                officer.setUsername(officerUsername);
                officer.setEmail("sluzbenik@bank.example.com");
                officer.setPassword(passwordEncoder.encode("sluzbenik123"));
                officer.setFirstName("Petar");
                officer.setLastName("Petrovic");
                officer.setRole("ROLE_OFFICER");
                officer.setDateOfBirth(LocalDate.of(1985, 3, 15));
                officer.setCreatedAt(LocalDateTime.now());
                bankUserRepository.save(officer);
                System.out.println("Created default bank officer: sluzbenik / sluzbenik123");
            }

            // === Client 1: oliva — high income, no existing loans => APPROVED ===
            String olivaAcc = createTestClient(bankUserRepository, passwordEncoder, packageAccountRepository,
                    bankAccountRepository, paymentCardRepository, transactionRepository,
                    identifierService,
                    "oliva", "oliva123", "oliva@bank.example.com", "Oliva", "Maslina",
                    LocalDate.of(1990, 7, 22), 150000.0);
            System.out.println("oliva account: " + olivaAcc);

            // === Client 2: marko — unemployed, no income => REJECTED ===
            createTestClient(bankUserRepository, passwordEncoder, packageAccountRepository,
                    bankAccountRepository, paymentCardRepository, transactionRepository,
                    identifierService,
                    "marko", "marko123", "marko@bank.example.com", "Marko", "Markovic",
                    LocalDate.of(1988, 3, 10), 0.0);

            // === Client 3: jovana — 65 years old, 2 existing active loans => HIGH RISK ===
            String jovanaAcc = createTestClient(bankUserRepository, passwordEncoder, packageAccountRepository,
                    bankAccountRepository, paymentCardRepository, transactionRepository,
                    identifierService,
                    "jovana", "jovana123", "jovana@bank.example.com", "Jovana", "Jovanovic",
                    LocalDate.of(1959, 11, 5), 200000.0);
            createExistingLoan(loanRepository, loanRepaymentRepository, bankUserRepository,
                    "jovana", 2000000.0, 120, 25000.0, true);
            createExistingLoan(loanRepository, loanRepaymentRepository, bankUserRepository,
                    "jovana", 1000000.0, 60, 20000.0, false);

            // === Client 4: ivan — low income, 1 existing loan => REJECT (insufficient) ===
            String ivanAcc = createTestClient(bankUserRepository, passwordEncoder, packageAccountRepository,
                    bankAccountRepository, paymentCardRepository, transactionRepository,
                    identifierService,
                    "ivan", "ivan123", "ivan@bank.example.com", "Ivan", "Ivanovic",
                    LocalDate.of(1995, 6, 20), 25000.0);
            createExistingLoan(loanRepository, loanRepaymentRepository, bankUserRepository,
                    "ivan", 500000.0, 36, 16000.0, true);

            // === Client 5: milica — very low income => REJECT (insufficient) ===
            createTestClient(bankUserRepository, passwordEncoder, packageAccountRepository,
                    bankAccountRepository, paymentCardRepository, transactionRepository,
                    identifierService,
                    "milica", "milica123", "milica@bank.example.com", "Milica", "Milic",
                    LocalDate.of(1992, 4, 15), 8000.0);

            // === Create oliva's bookstore account ===
            String bookstoreAccountName = "Bookstore Revenue";
            if (!packageAccountRepository.existsByNameAndClientUsername(bookstoreAccountName, "oliva")) {
                BankUser olivaClient = bankUserRepository.findByUsername("oliva")
                        .orElseThrow(() -> new RuntimeException("Client oliva not found"));

                PackageAccount pkg = new PackageAccount();
                pkg.setName(bookstoreAccountName);
                pkg.setClient(olivaClient);
                pkg.setCreatedAt(LocalDateTime.now());
                PackageAccount savedPackage = packageAccountRepository.save(pkg);

                BankAccount account = new BankAccount();
                account.setAccountNumber(identifierService.generateAccountNumber());
                account.setBalance(0.0);
                account.setCurrency("RSD");
                account.setCreatedAt(LocalDateTime.now());
                account.setPackageAccount(savedPackage);
                BankAccount savedAccount = bankAccountRepository.save(account);

                PaymentCard card = new PaymentCard();
                card.setCardNumber(identifierService.generateCardNumber());
                card.setCardholderName("Oliva Maslina");
                card.setExpirationDate(LocalDate.now().plusYears(3).withDayOfMonth(1));
                card.setCvv(identifierService.generateCvv());
                card.setCreatedAt(LocalDateTime.now());
                card.setPackageAccount(savedPackage);
                paymentCardRepository.save(card);

                System.out.println("=== BOOKSTORE ACCOUNT CREATED ===");
                System.out.println("Account number: " + savedAccount.getAccountNumber());
                System.out.println("=================================");
            }
        };
    }

    private String createTestClient(BankUserRepository bankUserRepository,
                                    PasswordEncoder passwordEncoder,
                                    PackageAccountRepository packageAccountRepository,
                                    BankAccountRepository bankAccountRepository,
                                    PaymentCardRepository paymentCardRepository,
                                    TransactionRepository transactionRepository,
                                    BankIdentifierService identifierService,
                                    String username, String rawPassword, String email,
                                    String firstName, String lastName, LocalDate dateOfBirth,
                                    double monthlyIncome) {
        if (bankUserRepository.existsByUsername(username)) {
            // Find their account number
            List<PackageAccount> pkgs = packageAccountRepository.findByClientId(
                    bankUserRepository.findByUsername(username).get().getId());
            if (!pkgs.isEmpty()) {
                List<BankAccount> accounts = bankAccountRepository.findByPackageAccountId(pkgs.get(0).getId());
                if (!accounts.isEmpty()) return accounts.get(0).getAccountNumber();
            }
            return "EXISTS";
        }

        BankUser client = new BankUser();
        client.setUsername(username);
        client.setEmail(email);
        client.setPassword(passwordEncoder.encode(rawPassword));
        client.setFirstName(firstName);
        client.setLastName(lastName);
        client.setRole("ROLE_CLIENT");
        client.setDateOfBirth(dateOfBirth);
        client.setCreatedAt(LocalDateTime.now());
        bankUserRepository.save(client);
        System.out.println("Created bank client: " + username + " / " + rawPassword);

        String pkgName = "Personal - " + username;
        PackageAccount pkg = new PackageAccount();
        pkg.setName(pkgName);
        pkg.setClient(client);
        pkg.setCreatedAt(LocalDateTime.now());
        PackageAccount savedPackage = packageAccountRepository.save(pkg);

        BankAccount account = new BankAccount();
        account.setAccountNumber(identifierService.generateAccountNumber());
        account.setBalance(monthlyIncome * 2);
        account.setCurrency("RSD");
        account.setCreatedAt(LocalDateTime.now());
        account.setPackageAccount(savedPackage);
        BankAccount savedAccount = bankAccountRepository.save(account);

        PaymentCard card = new PaymentCard();
        card.setCardNumber(identifierService.generateCardNumber());
        card.setCardholderName(firstName + " " + lastName);
        card.setExpirationDate(LocalDate.now().plusYears(3).withDayOfMonth(1));
        card.setCvv(identifierService.generateCvv());
        card.setCreatedAt(LocalDateTime.now());
        card.setPackageAccount(savedPackage);
        paymentCardRepository.save(card);

        if (monthlyIncome > 0) {
            for (int i = 1; i <= 6; i++) {
                Transaction salary = new Transaction();
                salary.setSenderAccountNumber("EMPLOYER_RSD");
                salary.setReceiverAccountNumber(savedAccount.getAccountNumber());
                salary.setAmount(monthlyIncome);
                salary.setCurrency("RSD");
                salary.setStatus("COMPLETED");
                salary.setCreatedAt(LocalDateTime.now().minusMonths(i).withDayOfMonth(1));
                salary.setDescription("Monthly salary");
                transactionRepository.save(salary);
            }
        }

        System.out.println("  -> Account: " + savedAccount.getAccountNumber()
                + ", Balance: " + savedAccount.getBalance() + " RSD");
        return savedAccount.getAccountNumber();
    }

    private void createExistingLoan(LoanRepository loanRepository,
                                     LoanRepaymentRepository loanRepaymentRepository,
                                     BankUserRepository bankUserRepository,
                                     String username,
                                     double amount, int installments, double monthlyPayment,
                                     boolean allPaidOnTime) {
        BankUser client = bankUserRepository.findByUsername(username).orElse(null);
        if (client == null) return;

        // Check if this client already has a loan with these params
        List<com.example.bank.model.Loan> existing = loanRepository.findByClientId(client.getId());
        for (com.example.bank.model.Loan l : existing) {
            if (Math.abs(l.getLoanAmount() - amount) < 1) return; // already exists
        }

        Loan loan = new Loan();
        loan.setClientId(client.getId());
        loan.setLoanAmount(amount);
        loan.setNumberOfInstallments(installments);
        loan.setMonthlyPayment(monthlyPayment);
        loan.setInterestRate(0.05);
        loan.setRemainingBalance(amount - (monthlyPayment * 12));
        loan.setStatus("ACTIVE");
        loan.setOfficerDecision("APPROVED");
        loan.setOfficerUsername("sluzbenik");
        loan.setApprovedAt(LocalDateTime.now().minusMonths(12));
        loan.setCreatedAt(LocalDateTime.now().minusMonths(12));
        Loan savedLoan = loanRepository.save(loan);

        // Create repayment schedule (12 months of payments)
        for (int i = 1; i <= 12; i++) {
            LoanRepayment rp = new LoanRepayment();
            rp.setLoanId(savedLoan.getId());
            rp.setAmount(monthlyPayment);
            rp.setDueDate(LocalDate.now().minusMonths(12 - i).withDayOfMonth(15));
            rp.setStatus("PAID");
            rp.setPaidDate(LocalDate.now().minusMonths(12 - i).withDayOfMonth(13));
            rp.setCreatedAt(LocalDateTime.now().minusMonths(12 - i));
            loanRepaymentRepository.save(rp);
        }

        // Create remaining future repayments
        for (int i = 0; i < installments - 12; i++) {
            LoanRepayment rp = new LoanRepayment();
            rp.setLoanId(savedLoan.getId());
            rp.setAmount(monthlyPayment);
            rp.setDueDate(LocalDate.now().plusMonths(i).withDayOfMonth(15));
            rp.setStatus("PENDING");
            rp.setCreatedAt(LocalDateTime.now());
            loanRepaymentRepository.save(rp);
        }

        System.out.println("  -> Created existing loan for " + username + ": " + amount + " RSD, "
                + monthlyPayment + " RSD/month");
    }
}
