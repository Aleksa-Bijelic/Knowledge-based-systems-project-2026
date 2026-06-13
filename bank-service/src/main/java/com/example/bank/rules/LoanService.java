package com.example.bank.rules;

import com.example.bank.model.BankAccount;
import com.example.bank.model.BankUser;
import com.example.bank.model.PackageAccount;
import com.example.bank.model.Transaction;
import com.example.bank.repository.BankAccountRepository;
import com.example.bank.repository.BankUserRepository;
import com.example.bank.repository.PackageAccountRepository;
import com.example.bank.repository.TransactionRepository;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Results;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.internal.io.ResourceFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class LoanService {

    private final KieContainer kieContainer;
    private final BankUserRepository bankUserRepository;
    private final PackageAccountRepository packageAccountRepository;
    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;

    public LoanService(BankUserRepository bankUserRepository,
                       PackageAccountRepository packageAccountRepository,
                       BankAccountRepository bankAccountRepository,
                       TransactionRepository transactionRepository) {
        this.bankUserRepository = bankUserRepository;
        this.packageAccountRepository = packageAccountRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.transactionRepository = transactionRepository;

        KieServices kieServices = KieServices.Factory.get();
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
        kieFileSystem.write(ResourceFactory.newClassPathResource("rules/loan-issuing-rules.drl"));

        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
        kieBuilder.buildAll();

        Results results = kieBuilder.getResults();
        if (results.hasMessages(org.kie.api.builder.Message.Level.ERROR)) {
            throw new IllegalStateException("Drools build errors: " + results.getMessages());
        }

        this.kieContainer = kieServices.newKieContainer(kieServices.getRepository().getDefaultReleaseId());
    }

    public LoanAssessment evaluateLoanRequest(LoanRequest loanRequest) {
        ClientFinancialProfile profile = buildFinancialProfile(loanRequest.getClientId());

        KieSession kieSession = kieContainer.newKieSession();
        try {
            kieSession.insert(loanRequest);
            kieSession.insert(profile);

            kieSession.fireAllRules();

            LoanAssessment assessment = (LoanAssessment) kieSession.getObjects(
                    o -> o instanceof LoanAssessment
            ).iterator().next();

            return assessment;
        } finally {
            kieSession.dispose();
        }
    }

    private ClientFinancialProfile buildFinancialProfile(Long clientId) {
        BankUser client = bankUserRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client not found: " + clientId));

        int age;
        if (client.getDateOfBirth() != null) {
            age = Period.between(client.getDateOfBirth(), LocalDate.now()).getYears();
        } else {
            age = Period.between(client.getCreatedAt().toLocalDate(), LocalDate.now()).getYears();
        }

        List<PackageAccount> packageAccounts = packageAccountRepository.findByClientId(clientId);
        double totalBalance = 0.0;
        List<String> clientAccountNumbers = new ArrayList<>();

        for (PackageAccount pa : packageAccounts) {
            List<BankAccount> accounts = bankAccountRepository.findByPackageAccountId(pa.getId());
            for (BankAccount account : accounts) {
                totalBalance += account.getBalance();
                clientAccountNumbers.add(account.getAccountNumber());
            }
        }

        List<Transaction> clientTransactions = new ArrayList<>();
        for (String accountNumber : clientAccountNumbers) {
            clientTransactions.addAll(transactionRepository.findBySenderAccountNumber(accountNumber));
            clientTransactions.addAll(transactionRepository.findByReceiverAccountNumber(accountNumber));
        }

        Set<Long> seenTransactionIds = new HashSet<>();
        List<Transaction> uniqueTransactions = new ArrayList<>();
        for (Transaction t : clientTransactions) {
            if (seenTransactionIds.add(t.getId())) {
                uniqueTransactions.add(t);
            }
        }

        double monthlyIncome = estimateMonthlyIncome(uniqueTransactions);
        double existingLoanPayments = 0.0;
        int existingLoanCount = 0;
        double totalExistingLoanAmount = 0.0;

        for (Transaction t : uniqueTransactions) {
            if (t.getDescription() != null && t.getDescription().toLowerCase().contains("loan")) {
                existingLoanCount++;
                if (t.getSenderAccountNumber() != null
                    && clientAccountNumbers.contains(t.getSenderAccountNumber())
                    && t.getStatus().equals("COMPLETED")) {
                    existingLoanPayments += t.getAmount();
                }
            }
        }

        boolean hasGoodRepaymentHistory = false;
        long completedIncoming = uniqueTransactions.stream()
                .filter(t -> t.getReceiverAccountNumber() != null
                        && t.getStatus().equals("COMPLETED"))
                .count();
        hasGoodRepaymentHistory = completedIncoming >= 3;

        return new ClientFinancialProfile(
                clientId,
                age,
                monthlyIncome,
                existingLoanPayments,
                hasGoodRepaymentHistory,
                existingLoanCount,
                totalExistingLoanAmount,
                totalBalance
        );
    }

    private double estimateMonthlyIncome(List<Transaction> transactions) {
        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
        double totalReceived = 0.0;

        for (Transaction t : transactions) {
            if (t.getCreatedAt().isAfter(sixMonthsAgo)
                    && t.getReceiverAccountNumber() != null
                    && t.getStatus().equals("COMPLETED")) {
                totalReceived += t.getAmount();
            }
        }

        return totalReceived / 6.0;
    }
}
