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

            // ========================================================================
            // EXISTING TEST CLIENTS (keep for backward compatibility)
            // ========================================================================

            // === Client 1: oliva — high income, young, indefinite, no loans => IDEAL ===
            String olivaAcc = createTestClient(bankUserRepository, passwordEncoder, packageAccountRepository,
                    bankAccountRepository, paymentCardRepository, transactionRepository,
                    identifierService,
                    "oliva", "oliva123", "oliva@bank.example.com", "Oliva", "Maslina",
                    LocalDate.of(1990, 7, 22), 150000.0);
            System.out.println("oliva account: " + olivaAcc);

            // === Client 2: marko — unemployed, no income => REJECT ===
            createTestClient(bankUserRepository, passwordEncoder, packageAccountRepository,
                    bankAccountRepository, paymentCardRepository, transactionRepository,
                    identifierService,
                    "marko", "marko123", "marko@bank.example.com", "Marko", "Markovic",
                    LocalDate.of(1988, 3, 10), 0.0);

            // === Client 3: jovana — old age (67), high income, 2 active loans => HIGH RISK ===
            String jovanaAcc = createTestClient(bankUserRepository, passwordEncoder, packageAccountRepository,
                    bankAccountRepository, paymentCardRepository, transactionRepository,
                    identifierService,
                    "jovana", "jovana123", "jovana@bank.example.com", "Jovana", "Jovanovic",
                    LocalDate.of(1959, 11, 5), 200000.0);
            createLoanWithRepayments(loanRepository, loanRepaymentRepository, bankUserRepository,
                    "jovana", 2000000.0, 120, 25000.0, "ACTIVE", true);
            createLoanWithRepayments(loanRepository, loanRepaymentRepository, bankUserRepository,
                    "jovana", 1000000.0, 60, 20000.0, "ACTIVE", true);

            // === Client 4: ivan — low income (25000), 1 active loan (16000/m) => DTI FAIL ===
            String ivanAcc = createTestClient(bankUserRepository, passwordEncoder, packageAccountRepository,
                    bankAccountRepository, paymentCardRepository, transactionRepository,
                    identifierService,
                    "ivan", "ivan123", "ivan@bank.example.com", "Ivan", "Ivanovic",
                    LocalDate.of(1995, 6, 20), 25000.0);
            createLoanWithRepayments(loanRepository, loanRepaymentRepository, bankUserRepository,
                    "ivan", 500000.0, 36, 16000.0, "ACTIVE", true);

            // === Client 5: milica — very low income (8000) => DTI FAIL ===
            createTestClient(bankUserRepository, passwordEncoder, packageAccountRepository,
                    bankAccountRepository, paymentCardRepository, transactionRepository,
                    identifierService,
                    "milica", "milica123", "milica@bank.example.com", "Milica", "Milic",
                    LocalDate.of(1992, 4, 15), 8000.0);

            // ========================================================================
            // NEW COMPREHENSIVE TEST CLIENTS
            // ========================================================================

            // === Client 6: stefan — ideal: young (28), high income, indefinite, no loans ===
            createTestClient(bankUserRepository, passwordEncoder, packageAccountRepository,
                    bankAccountRepository, paymentCardRepository, transactionRepository,
                    identifierService,
                    "stefan", "stefan123", "stefan@bank.example.com", "Stefan", "Stefanovic",
                    LocalDate.of(1998, 4, 10), 120000.0);

            // === Client 7: ana — fixed-term, good credit (1 active + 1 paid loan) ===
            createTestClient(bankUserRepository, passwordEncoder, packageAccountRepository,
                    bankAccountRepository, paymentCardRepository, transactionRepository,
                    identifierService,
                    "ana", "ana123", "ana@bank.example.com", "Ana", "Anic",
                    LocalDate.of(1981, 3, 22), 90000.0);
            createLoanWithRepayments(loanRepository, loanRepaymentRepository, bankUserRepository,
                    "ana", 600000.0, 60, 10000.0, "ACTIVE", true);
            // Paid-off loan for good credit history
            createLoanWithRepayments(loanRepository, loanRepaymentRepository, bankUserRepository,
                    "ana", 200000.0, 24, 9000.0, "PAID", true);

            // === Client 8: nikola — older (58), 2 active loans => age risk factors ===
            createTestClient(bankUserRepository, passwordEncoder, packageAccountRepository,
                    bankAccountRepository, paymentCardRepository, transactionRepository,
                    identifierService,
                    "nikola", "nikola123", "nikola@bank.example.com", "Nikola", "Nikolic",
                    LocalDate.of(1968, 1, 15), 100000.0);
            createLoanWithRepayments(loanRepository, loanRepaymentRepository, bankUserRepository,
                    "nikola", 800000.0, 84, 12000.0, "ACTIVE", true);
            createLoanWithRepayments(loanRepository, loanRepaymentRepository, bankUserRepository,
                    "nikola", 500000.0, 60, 15000.0, "ACTIVE", true);

            // === Client 9: maja — old (62), unemployed, no income => REJECT ===
            createTestClient(bankUserRepository, passwordEncoder, packageAccountRepository,
                    bankAccountRepository, paymentCardRepository, transactionRepository,
                    identifierService,
                    "maja", "maja123", "maja@bank.example.com", "Maja", "Majic",
                    LocalDate.of(1964, 11, 30), 0.0);

            // === Client 10: petar — short tenure (2 months, INDEFINITE) => TENURE FAIL ===
            BankUser petar = createUserOnly(bankUserRepository, passwordEncoder,
                    "petar", "petar123", "petar@bank.example.com", "Petar", "Petrovic",
                    LocalDate.of(2004, 5, 18));
            String petarAcc = createAccountWithSalary(bankUserRepository, packageAccountRepository,
                    bankAccountRepository, paymentCardRepository, transactionRepository,
                    identifierService, petar, 60000.0, 2);

            // === Client 11: sofija — fixed-term, contract ending soon (4 months left) ===
            createTestClient(bankUserRepository, passwordEncoder, packageAccountRepository,
                    bankAccountRepository, paymentCardRepository, transactionRepository,
                    identifierService,
                    "sofija", "sofija123", "sofija@bank.example.com", "Sofija", "Sofijic",
                    LocalDate.of(1986, 7, 8), 130000.0);

            // === Client 12: luka — delinquent repayments (late/missed) ===
            createTestClient(bankUserRepository, passwordEncoder, packageAccountRepository,
                    bankAccountRepository, paymentCardRepository, transactionRepository,
                    identifierService,
                    "luka", "luka123", "luka@bank.example.com", "Luka", "Lukic",
                    LocalDate.of(1976, 2, 14), 80000.0);
            createLoanWithRepayments(loanRepository, loanRepaymentRepository, bankUserRepository,
                    "luka", 400000.0, 48, 12000.0, "ACTIVE", false);

            // === Client 13: dunja — 3 active loans => EXCESSIVE LOANS ===
            createTestClient(bankUserRepository, passwordEncoder, packageAccountRepository,
                    bankAccountRepository, paymentCardRepository, transactionRepository,
                    identifierService,
                    "dunja", "dunja123", "dunja@bank.example.com", "Dunja", "Dunjic",
                    LocalDate.of(1991, 8, 25), 70000.0);
            createLoanWithRepayments(loanRepository, loanRepaymentRepository, bankUserRepository,
                    "dunja", 200000.0, 36, 5000.0, "ACTIVE", true);
            createLoanWithRepayments(loanRepository, loanRepaymentRepository, bankUserRepository,
                    "dunja", 350000.0, 48, 8000.0, "ACTIVE", true);
            createLoanWithRepayments(loanRepository, loanRepaymentRepository, bankUserRepository,
                    "dunja", 250000.0, 36, 6000.0, "ACTIVE", true);

            // === Client 14: lazar — old (70), good income, no loans => AGE+TERM LIMIT ===
            createTestClient(bankUserRepository, passwordEncoder, packageAccountRepository,
                    bankAccountRepository, paymentCardRepository, transactionRepository,
                    identifierService,
                    "lazar", "lazar123", "lazar@bank.example.com", "Lazar", "Lazarevic",
                    LocalDate.of(1956, 1, 5), 50000.0);

            // === Client 15: tamara — has a DEFAULTED loan ===
            createTestClient(bankUserRepository, passwordEncoder, packageAccountRepository,
                    bankAccountRepository, paymentCardRepository, transactionRepository,
                    identifierService,
                    "tamara", "tamara123", "tamara@bank.example.com", "Tamara", "Tamaric",
                    LocalDate.of(1984, 4, 30), 85000.0);
            createLoanWithRepayments(loanRepository, loanRepaymentRepository, bankUserRepository,
                    "tamara", 700000.0, 60, 20000.0, "DEFAULTED", false);

            // ========================================================================
            // FRAUD DETECTION TEST HISTORY (oliva only)
            // ========================================================================
            BankUser olivaClient = bankUserRepository.findByUsername("oliva")
                    .orElseThrow(() -> new RuntimeException("Client oliva not found"));
            List<PackageAccount> olivaPackages = packageAccountRepository.findByClientId(olivaClient.getId());
            if (!olivaPackages.isEmpty()) {
                PackageAccount mainPkg = olivaPackages.stream()
                        .filter(p -> p.getName().equals("Personal - oliva"))
                        .findFirst().orElse(olivaPackages.get(0));
                List<BankAccount> mainAccounts = bankAccountRepository.findByPackageAccountId(mainPkg.getId());
                if (!mainAccounts.isEmpty()) {
                    BankAccount mainAccount = mainAccounts.get(0);
                    String senderAcc = mainAccount.getAccountNumber();

                    List<BankAccount> allAccounts = bankAccountRepository.findAll();
                    String receiverAcc = allAccounts.stream()
                            .map(BankAccount::getAccountNumber)
                            .filter(acc -> !acc.equals(senderAcc))
                            .findFirst().orElse(senderAcc);

                    List<Transaction> existingHistory = transactionRepository.findBySenderAccountNumberAndStatusIn(
                            senderAcc, List.of("COMPLETED", "APPROVED"));
                    if (existingHistory.size() <= 6) {
                        Object[][] historyData = {
                            {1500.0, 45.8150, 15.9819, "Zagreb", "Croatia", "Coffee shop"},
                            {1200.0, 45.8120, 15.9780, "Zagreb", "Croatia", "Lunch"},
                            {1800.0, 45.8080, 15.9750, "Zagreb", "Croatia", "Supermarket"},
                            {900.0,  45.8200, 15.9850, "Zagreb", "Croatia", "Taxi"},
                            {2200.0, 45.8100, 15.9800, "Zagreb", "Croatia", "Dinner"},
                            {1100.0, 45.4900, 16.3700, "Sisak", "Croatia", "Gas station"},
                            {3000.0, 45.8150, 15.9819, "Zagreb", "Croatia", "Electronics"},
                            {800.0,  45.8130, 15.9830, "Zagreb", "Croatia", "Bakery"},
                            {1600.0, 45.4850, 16.3750, "Sisak", "Croatia", "Restaurant"},
                            {2500.0, 45.8180, 15.9790, "Zagreb", "Croatia", "Clothing store"},
                        };
                        LocalDateTime now = LocalDateTime.now();
                        for (int i = 0; i < historyData.length; i++) {
                            Transaction tx = new Transaction();
                            tx.setSenderAccountNumber(senderAcc);
                            tx.setReceiverAccountNumber(receiverAcc);
                            tx.setAmount((Double) historyData[i][0]);
                            tx.setCurrency("RSD");
                            tx.setStatus("COMPLETED");
                            tx.setCreatedAt(now.minusDays(60 - i * 6).minusHours(i * 3));
                            tx.setDescription((String) historyData[i][5]);
                            tx.setLatitude((Double) historyData[i][1]);
                            tx.setLongitude((Double) historyData[i][2]);
                            tx.setCity((String) historyData[i][3]);
                            tx.setCountry((String) historyData[i][4]);
                            tx.setCardholderName("Oliva Maslina");
                            transactionRepository.save(tx);
                        }
                        System.out.println("=== FRAUD TEST HISTORY: 10 transactions added for oliva ===");
                    }
                }
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

        String pkgName = "Personal - " + username;
        PackageAccount pkg = new PackageAccount();
        pkg.setName(pkgName);
        pkg.setClient(client);
        pkg.setCreatedAt(LocalDateTime.now());
        PackageAccount savedPackage = packageAccountRepository.save(pkg);

        BankAccount account = new BankAccount();
        account.setAccountNumber(identifierService.generateAccountNumber());
        account.setBalance(monthlyIncome * 3);
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
            for (int i = 0; i < 6; i++) {
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

    private BankUser createUserOnly(BankUserRepository bankUserRepository,
                                     PasswordEncoder passwordEncoder,
                                     String username, String rawPassword, String email,
                                     String firstName, String lastName, LocalDate dateOfBirth) {
        if (bankUserRepository.existsByUsername(username)) {
            return bankUserRepository.findByUsername(username).get();
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
        return client;
    }

    private String createAccountWithSalary(BankUserRepository bankUserRepository,
                                            PackageAccountRepository packageAccountRepository,
                                            BankAccountRepository bankAccountRepository,
                                            PaymentCardRepository paymentCardRepository,
                                            TransactionRepository transactionRepository,
                                            BankIdentifierService identifierService,
                                            BankUser client, double monthlyIncome, int salaryMonths) {
        String pkgName = "Personal - " + client.getUsername();
        PackageAccount pkg = new PackageAccount();
        pkg.setName(pkgName);
        pkg.setClient(client);
        pkg.setCreatedAt(LocalDateTime.now());
        PackageAccount savedPackage = packageAccountRepository.save(pkg);

        BankAccount account = new BankAccount();
        account.setAccountNumber(identifierService.generateAccountNumber());
        account.setBalance(monthlyIncome * 3);
        account.setCurrency("RSD");
        account.setCreatedAt(LocalDateTime.now());
        account.setPackageAccount(savedPackage);
        BankAccount savedAccount = bankAccountRepository.save(account);

        PaymentCard card = new PaymentCard();
        card.setCardNumber(identifierService.generateCardNumber());
        card.setCardholderName(client.getFirstName() + " " + client.getLastName());
        card.setExpirationDate(LocalDate.now().plusYears(3).withDayOfMonth(1));
        card.setCvv(identifierService.generateCvv());
        card.setCreatedAt(LocalDateTime.now());
        card.setPackageAccount(savedPackage);
        paymentCardRepository.save(card);

        if (monthlyIncome > 0) {
            for (int i = 0; i < salaryMonths; i++) {
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
                + ", Balance: " + savedAccount.getBalance() + " RSD (" + salaryMonths + " salary tx)");
        return savedAccount.getAccountNumber();
    }

    private void createLoanWithRepayments(LoanRepository loanRepository,
                                           LoanRepaymentRepository loanRepaymentRepository,
                                           BankUserRepository bankUserRepository,
                                           String username,
                                           double amount, int totalInstallments,
                                           double monthlyPayment,
                                           String loanStatus,
                                           boolean allRepaymentsOnTime) {
        BankUser client = bankUserRepository.findByUsername(username).orElse(null);
        if (client == null) return;

        List<Loan> existing = loanRepository.findByClientId(client.getId());
        for (Loan l : existing) {
            if (Math.abs(l.getLoanAmount() - amount) < 1) return;
        }

        LocalDateTime now = LocalDateTime.now();
        int pastMonths;
        if ("PAID".equals(loanStatus)) {
            pastMonths = totalInstallments;
        } else {
            pastMonths = Math.min(12, totalInstallments);
        }

        Loan loan = new Loan();
        loan.setClientId(client.getId());
        loan.setLoanAmount(amount);
        loan.setNumberOfInstallments(totalInstallments);
        loan.setMonthlyPayment(monthlyPayment);
        loan.setInterestRate(0.05);
        if ("PAID".equals(loanStatus) || "DEFAULTED".equals(loanStatus)) {
            loan.setRemainingBalance(0.0);
        } else {
            loan.setRemainingBalance(Math.max(0, amount - (monthlyPayment * pastMonths)));
        }
        loan.setStatus(loanStatus);
        loan.setOfficerDecision("APPROVED");
        loan.setOfficerUsername("sluzbenik");
        loan.setApprovedAt(now.minusMonths(pastMonths));
        loan.setCreatedAt(now.minusMonths(pastMonths));
        Loan savedLoan = loanRepository.save(loan);

        for (int i = pastMonths - 1; i >= 0; i--) {
            LoanRepayment rp = new LoanRepayment();
            rp.setLoanId(savedLoan.getId());
            rp.setAmount(monthlyPayment);
            rp.setDueDate(LocalDate.now().minusMonths(i).withDayOfMonth(15));
            rp.setCreatedAt(LocalDate.now().minusMonths(i).atStartOfDay());

            if (!allRepaymentsOnTime && i < 3) {
                if ("DEFAULTED".equals(loanStatus)) {
                    rp.setStatus("MISSED");
                    rp.setPaidDate(null);
                } else {
                    rp.setStatus("LATE");
                    rp.setPaidDate(LocalDate.now().minusMonths(i).withDayOfMonth(25));
                }
            } else {
                rp.setStatus("PAID");
                rp.setPaidDate(LocalDate.now().minusMonths(i).withDayOfMonth(13));
            }
            loanRepaymentRepository.save(rp);
        }

        if ("ACTIVE".equals(loanStatus) && totalInstallments > pastMonths) {
            for (int i = 0; i < totalInstallments - pastMonths; i++) {
                LoanRepayment rp = new LoanRepayment();
                rp.setLoanId(savedLoan.getId());
                rp.setAmount(monthlyPayment);
                rp.setDueDate(LocalDate.now().plusMonths(i).withDayOfMonth(15));
                rp.setStatus("PENDING");
                rp.setCreatedAt(LocalDateTime.now());
                loanRepaymentRepository.save(rp);
            }
        }

        System.out.println("  -> Created " + loanStatus + " loan for " + username + ": "
                + amount + " RSD, " + monthlyPayment + " RSD/month");
    }
}
