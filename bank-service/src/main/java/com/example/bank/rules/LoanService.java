package com.example.bank.rules;

import com.example.bank.model.BankAccount;
import com.example.bank.model.BankUser;
import com.example.bank.model.Loan;
import com.example.bank.model.LoanRepayment;
import com.example.bank.model.LoanRequest;
import com.example.bank.model.PackageAccount;
import com.example.bank.model.Transaction;
import com.example.bank.repository.BankAccountRepository;
import com.example.bank.repository.BankUserRepository;
import com.example.bank.repository.LoanRepaymentRepository;
import com.example.bank.repository.LoanRepository;
import com.example.bank.repository.LoanRequestRepository;
import com.example.bank.repository.PackageAccountRepository;
import com.example.bank.repository.TransactionRepository;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Results;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.QueryResults;
import org.kie.internal.io.ResourceFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collection;
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
    private final LoanRepository loanRepository;
    private final LoanRepaymentRepository loanRepaymentRepository;
    private final LoanRequestRepository loanRequestRepository;

    public LoanService(BankUserRepository bankUserRepository,
                       PackageAccountRepository packageAccountRepository,
                       BankAccountRepository bankAccountRepository,
                       TransactionRepository transactionRepository,
                       LoanRepository loanRepository,
                       LoanRepaymentRepository loanRepaymentRepository,
                       LoanRequestRepository loanRequestRepository) {
        this.bankUserRepository = bankUserRepository;
        this.packageAccountRepository = packageAccountRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.transactionRepository = transactionRepository;
        this.loanRepository = loanRepository;
        this.loanRepaymentRepository = loanRepaymentRepository;
        this.loanRequestRepository = loanRequestRepository;

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

    public LoanAssessment evaluateLoanRequest(LoanRequestFact loanRequest) {
        ClientFinancialProfile profile = buildFinancialProfile(loanRequest.getClientId());

        KieSession kieSession = kieContainer.newKieSession();
        try {
            kieSession.insert(loanRequest);
            kieSession.insert(profile);

            // Insert existing loan data as facts for DRL reasoning
            List<Loan> activeLoans = loanRepository.findByClientId(loanRequest.getClientId());
            for (Loan loan : activeLoans) {
                LoanFact fact = new LoanFact(
                    loan.getId(),
                    loan.getClientId(),
                    loan.getMonthlyPayment(),
                    loan.getRemainingBalance(),
                    loan.getStatus(),
                    loan.getNumberOfInstallments()
                );
                kieSession.insert(fact);

                List<LoanRepayment> repayments = loanRepaymentRepository.findByLoanId(loan.getId());
                for (LoanRepayment rp : repayments) {
                    LoanRepaymentFact rpFact = new LoanRepaymentFact(
                        rp.getId(),
                        rp.getLoanId(),
                        rp.getAmount(),
                        rp.getDueDate(),
                        rp.getPaidDate(),
                        rp.getStatus()
                    );
                    kieSession.insert(rpFact);
                }
            }

            kieSession.fireAllRules();

            Collection<?> assessments = kieSession.getObjects(
                    o -> o instanceof LoanAssessment
            );
            if (assessments.isEmpty()) {
                throw new IllegalStateException(
                        "LoanAssessment was not generated by the rule engine"
                );
            }
            LoanAssessment assessment = (LoanAssessment) assessments.iterator().next();

            // Collect DecisionReasonFacts inserted by rules
            Collection<DecisionReasonFact> decisionReasons = (Collection<DecisionReasonFact>) (Collection<?>)
                    kieSession.getObjects(o -> o instanceof DecisionReasonFact);
            for (DecisionReasonFact reason : decisionReasons) {
                if (reason.getClientId().equals(assessment.getClientId())) {
                    assessment.addReason(reason.getReason());
                }
            }

            boolean approved = queryExists(kieSession, "isLoanApproved",
                    new Object[]{assessment.getClientId()});

            if (approved) {
                assessment.setApproved(true);
                assessment.addReason("Recommendation: Approve loan");
            } else {
                assessment.setApproved(false);
            }

            return assessment;
        } finally {
            kieSession.dispose();
        }
    }

    public LoanRequest saveLoanRequest(Long clientId, double loanAmount, int numberOfInstallments,
                                        String employmentStatus, LocalDate contractStartDate,
                                        LocalDate contractEndDate, LoanAssessment assessment) {
        LoanRequest entity = new LoanRequest();
        entity.setClientId(clientId);
        entity.setLoanAmount(loanAmount);
        entity.setNumberOfInstallments(numberOfInstallments);
        entity.setEmploymentStatus(employmentStatus);
        entity.setContractStartDate(contractStartDate);
        entity.setContractEndDate(contractEndDate);
        entity.setStatus("ASSESSED");
        entity.setRiskScore(assessment.getRiskScore());
        entity.setRiskLevel(assessment.getRiskLevel());
        entity.setMonthlyPayment(assessment.getMonthlyPayment());
        entity.setDebtToIncomeRatio(assessment.getDebtToIncomeRatio());
        entity.setSystemRecommendation(assessment.isApproved() ? "APPROVE" : "REJECT");
        entity.setCreatedAt(LocalDateTime.now());
        return loanRequestRepository.save(entity);
    }

    public LoanRequest saveOfficerDecision(Long requestId, String decision, String officerUsername) {
        LoanRequest entity = loanRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Loan request not found: " + requestId));
        entity.setOfficerDecision(decision);
        entity.setOfficerUsername(officerUsername);
        entity.setStatus(decision);

        loanRequestRepository.save(entity);

        if ("APPROVED".equals(decision)) {
            Loan loan = new Loan();
            loan.setClientId(entity.getClientId());
            loan.setRequestId(entity.getId());
            loan.setLoanAmount(entity.getLoanAmount());
            loan.setNumberOfInstallments(entity.getNumberOfInstallments());
            loan.setMonthlyPayment(entity.getMonthlyPayment());
            loan.setInterestRate(0.05);
            loan.setRemainingBalance(entity.getLoanAmount());
            loan.setStatus("ACTIVE");
            loan.setOfficerDecision("APPROVED");
            loan.setOfficerUsername(officerUsername);
            loan.setApprovedAt(LocalDateTime.now());
            loan.setCreatedAt(LocalDateTime.now());
            loanRepository.save(loan);
        }

        return entity;
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

        List<Transaction> allTransactions = new ArrayList<>();
        for (String accountNumber : clientAccountNumbers) {
            allTransactions.addAll(transactionRepository.findBySenderAccountNumber(accountNumber));
            allTransactions.addAll(transactionRepository.findByReceiverAccountNumber(accountNumber));
        }

        Set<Long> seenIds = new HashSet<>();
        List<Transaction> uniqueTransactions = new ArrayList<>();
        for (Transaction t : allTransactions) {
            if (seenIds.add(t.getId())) {
                uniqueTransactions.add(t);
            }
        }

        double monthlyIncome = estimateMonthlyIncome(uniqueTransactions);

        List<Loan> activeLoans = loanRepository.findByClientIdAndStatus(clientId, "ACTIVE");
        int existingLoanCount = activeLoans.size();
        double totalExistingLoanPayments = activeLoans.stream()
                .mapToDouble(Loan::getMonthlyPayment)
                .sum();

        return new ClientFinancialProfile(
                clientId,
                age,
                monthlyIncome,
                totalExistingLoanPayments,
                existingLoanCount,
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

    private boolean queryExists(KieSession kieSession, String queryName, Object[] args) {
        QueryResults results = kieSession.getQueryResults(queryName, args);
        return results != null && results.size() > 0;
    }
}
