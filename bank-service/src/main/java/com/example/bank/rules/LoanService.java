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
import java.util.List;

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

            var objects = kieSession.getObjects(obj -> obj instanceof LoanAssessment);
            if (!objects.isEmpty()) {
                return (LoanAssessment) objects.iterator().next();
            }

            LoanAssessment fallback = new LoanAssessment(loanRequest.getClientId());
            fallback.setApproved(false);
            fallback.addReason("Unable to complete loan assessment");
            return fallback;
        } finally {
            kieSession.dispose();
        }
    }

    private ClientFinancialProfile buildFinancialProfile(Long clientId) {
        BankUser client = bankUserRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client not found: " + clientId));

        int age = Period.between(client.getCreatedAt().toLocalDate(), LocalDate.now()).getYears() + 25;

        List<PackageAccount> packageAccounts = packageAccountRepository.findByClientId(clientId);
        double totalBalance = 0.0;
        for (PackageAccount pa : packageAccounts) {
            for (BankAccount account : bankAccountRepository.findAll()) {
                if (account.getPackageAccount() != null && account.getPackageAccount().getId().equals(pa.getId())) {
                    totalBalance += account.getBalance();
                }
            }
        }

        List<Transaction> allTransactions = transactionRepository
                .findBySenderAccountNumberOrReceiverAccountNumber("", "");

        double monthlyIncome = estimateMonthlyIncome(clientId, allTransactions);
        double existingLoanPayments = 0.0;
        int existingLoanCount = 0;
        double totalExistingLoanAmount = 0.0;

        boolean hasGoodRepaymentHistory = checkRepaymentHistory(clientId, allTransactions);

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

    private double estimateMonthlyIncome(Long clientId, List<Transaction> transactions) {
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

    private boolean checkRepaymentHistory(Long clientId, List<Transaction> transactions) {
        return true;
    }
}
