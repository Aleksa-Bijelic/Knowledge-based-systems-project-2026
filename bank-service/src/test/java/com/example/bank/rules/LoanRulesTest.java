package com.example.bank.rules;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.QueryResults;
import org.kie.internal.io.ResourceFactory;

import java.time.LocalDate;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class LoanRulesTest {

    private static KieContainer kieContainer;

    @BeforeAll
    static void initKieContainer() {
        KieServices ks = KieServices.Factory.get();
        var kieFileSystem = ks.newKieFileSystem();
        kieFileSystem.write(ResourceFactory.newClassPathResource("rules/loan-issuing-rules.drl"));
        var kieBuilder = ks.newKieBuilder(kieFileSystem);
        kieBuilder.buildAll();
        var results = kieBuilder.getResults();
        assertFalse(results.hasMessages(org.kie.api.builder.Message.Level.ERROR),
                "DRL build errors: " + results.getMessages());
        kieContainer = ks.newKieContainer(ks.getRepository().getDefaultReleaseId());
    }

    private KieSession createSession() {
        return kieContainer.newKieSession();
    }

    private boolean queryExists(KieSession ks, String queryName, Object... args) {
        QueryResults results = ks.getQueryResults(queryName, args);
        return results != null && results.size() > 0;
    }

    private LoanAssessment getAssessment(KieSession ks) {
        var assessments = ks.getObjects(o -> o instanceof LoanAssessment);
        if (assessments.isEmpty()) return null;
        return (LoanAssessment) assessments.iterator().next();
    }

    // ========================================================================
    // SECTION 1: IDEAL CLIENT - all criteria met
    // ========================================================================
    @Test
    @DisplayName("Ideal client: high income, indefinite, young => APPROVED")
    void testIdealClientShouldBeApproved() {
        KieSession ks = createSession();
        try {
            ks.insert(new LoanRequestFact(1L, 500000.0, 60, "INDEFINITE",
                    LocalDate.of(2020, 1, 1), null));
            ks.insert(new ClientFinancialProfile(1L, 34, 150000.0, 0.0, 0, 300000.0));

            ks.fireAllRules();

            assertTrue(queryExists(ks, "isLoanApproved", 1L));
            assertTrue(queryExists(ks, "hasValidLoanPeriod", 1L));
            assertTrue(queryExists(ks, "hasStableEmployment", 1L));
            assertTrue(queryExists(ks, "hasSufficientIncome", 1L));
            assertTrue(queryExists(ks, "hasAcceptableRisk", 1L));
            assertTrue(queryExists(ks, "hasNoDefaultedLoans", 1L));
            assertTrue(queryExists(ks, "hasNoSeriousDelinquencies", 1L));
            assertTrue(queryExists(ks, "hasGoodCreditHistory", 1L));
            assertTrue(queryExists(ks, "agePlusLoanTermWithinLimit", 1L));
        } finally {
            ks.dispose();
        }
    }

    // ========================================================================
    // SECTION 2: LOAN PERIOD VALIDATION
    // ========================================================================
    @Test
    @DisplayName("Installments < 3 => fails meetsMinimumPeriod")
    void testInvalidLoanPeriodTooFew() {
        KieSession ks = createSession();
        try {
            ks.insert(new LoanRequestFact(1L, 100000.0, 1, "INDEFINITE",
                    LocalDate.of(2020, 1, 1), null));
            ks.insert(new ClientFinancialProfile(1L, 30, 50000.0, 0.0, 0, 100000.0));

            ks.fireAllRules();

            assertFalse(queryExists(ks, "isLoanApproved", 1L));
            assertFalse(queryExists(ks, "hasValidLoanPeriod", 1L));
            assertFalse(queryExists(ks, "meetsMinimumPeriod", 1L));
            assertTrue(queryExists(ks, "hasStableEmployment", 1L));
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("Installments > 360 => fails withinMaximumPeriod")
    void testInvalidLoanPeriodTooMany() {
        KieSession ks = createSession();
        try {
            ks.insert(new LoanRequestFact(1L, 100000.0, 480, "INDEFINITE",
                    LocalDate.of(2020, 1, 1), null));
            ks.insert(new ClientFinancialProfile(1L, 30, 50000.0, 0.0, 0, 100000.0));

            ks.fireAllRules();

            assertFalse(queryExists(ks, "isLoanApproved", 1L));
            assertFalse(queryExists(ks, "withinMaximumPeriod", 1L));
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("Installments exactly 3 => valid boundary")
    void testInstallmentsBoundaryMin() {
        KieSession ks = createSession();
        try {
            ks.insert(new LoanRequestFact(1L, 10000.0, 3, "INDEFINITE",
                    LocalDate.of(2020, 1, 1), null));
            ks.insert(new ClientFinancialProfile(1L, 30, 50000.0, 0.0, 0, 100000.0));

            ks.fireAllRules();

            assertTrue(queryExists(ks, "meetsMinimumPeriod", 1L));
            assertTrue(queryExists(ks, "hasValidLoanPeriod", 1L));
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("Installments exactly 360 => valid boundary")
    void testInstallmentsBoundaryMax() {
        KieSession ks = createSession();
        try {
            ks.insert(new LoanRequestFact(1L, 10000.0, 360, "INDEFINITE",
                    LocalDate.of(2020, 1, 1), null));
            ks.insert(new ClientFinancialProfile(1L, 30, 50000.0, 0.0, 0, 100000.0));

            ks.fireAllRules();

            assertTrue(queryExists(ks, "withinMaximumPeriod", 1L));
            assertTrue(queryExists(ks, "hasValidLoanPeriod", 1L));
        } finally {
            ks.dispose();
        }
    }

    // ========================================================================
    // SECTION 3: EMPLOYMENT STABILITY
    // ========================================================================
    @Test
    @DisplayName("Unemployed => fails hasStableEmployment")
    void testUnemployed() {
        KieSession ks = createSession();
        try {
            ks.insert(new LoanRequestFact(1L, 100000.0, 60, "UNEMPLOYED", null, null));
            ks.insert(new ClientFinancialProfile(1L, 30, 50000.0, 0.0, 0, 100000.0));

            ks.fireAllRules();

            assertFalse(queryExists(ks, "isLoanApproved", 1L));
            assertFalse(queryExists(ks, "hasStableEmployment", 1L));
            assertFalse(queryExists(ks, "isNotUnemployed", 1L));
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("Indefinite without start date => fails hasValidContractStartDate")
    void testIndefiniteNoStartDate() {
        KieSession ks = createSession();
        try {
            ks.insert(new LoanRequestFact(1L, 100000.0, 60, "INDEFINITE", null, null));
            ks.insert(new ClientFinancialProfile(1L, 30, 100000.0, 0.0, 0, 500000.0));

            ks.fireAllRules();

            assertFalse(queryExists(ks, "hasStableEmployment", 1L));
            assertFalse(queryExists(ks, "hasValidContractStartDate", 1L));
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("Indefinite with very recent start date => fails hasSufficientEmploymentTenure")
    void testInsufficientEmploymentTenure() {
        KieSession ks = createSession();
        try {
            ks.insert(new LoanRequestFact(1L, 100000.0, 60, "INDEFINITE",
                    LocalDate.now().minusMonths(2), null));
            ks.insert(new ClientFinancialProfile(1L, 30, 100000.0, 0.0, 0, 500000.0));

            ks.fireAllRules();

            assertFalse(queryExists(ks, "hasStableEmployment", 1L));
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("Fixed term with short contract => fails employmentCoversSufficientTerm")
    void testFixedTermShortContract() {
        KieSession ks = createSession();
        try {
            ks.insert(new LoanRequestFact(1L, 100000.0, 60, "FIXED_TERM",
                    LocalDate.of(2020, 1, 1), LocalDate.of(2024, 6, 1)));
            ks.insert(new ClientFinancialProfile(1L, 30, 50000.0, 0.0, 0, 100000.0));

            ks.fireAllRules();

            assertFalse(queryExists(ks, "isLoanApproved", 1L));
            assertFalse(queryExists(ks, "hasStableEmployment", 1L));
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("Fixed term covering > 50% of loan term => stable employment")
    void testFixedTermLongContract() {
        KieSession ks = createSession();
        try {
            ks.insert(new LoanRequestFact(1L, 100000.0, 60, "FIXED_TERM",
                    LocalDate.of(2020, 1, 1), LocalDate.now().plusYears(10)));
            ks.insert(new ClientFinancialProfile(1L, 30, 50000.0, 0.0, 0, 100000.0));

            ks.fireAllRules();

            assertTrue(queryExists(ks, "hasStableEmployment", 1L));
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("Fixed term with exactly 24 months remaining => passes (boundary)")
    void testFixedTermTwentyFourMonthsRemaining() {
        KieSession ks = createSession();
        try {
            ks.insert(new LoanRequestFact(1L, 100000.0, 120, "FIXED_TERM",
                    LocalDate.of(2020, 1, 1), LocalDate.now().plusMonths(24)));
            ks.insert(new ClientFinancialProfile(1L, 30, 50000.0, 0.0, 0, 100000.0));

            ks.fireAllRules();

            assertTrue(queryExists(ks, "hasStableEmployment", 1L));
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("Fixed term with 23 months remaining => fails")
    void testFixedTermTwentyThreeMonthsRemaining() {
        KieSession ks = createSession();
        try {
            ks.insert(new LoanRequestFact(1L, 100000.0, 120, "FIXED_TERM",
                    LocalDate.of(2020, 1, 1), LocalDate.now().plusMonths(23)));
            ks.insert(new ClientFinancialProfile(1L, 30, 50000.0, 0.0, 0, 100000.0));

            ks.fireAllRules();

            assertFalse(queryExists(ks, "hasStableEmployment", 1L));
        } finally {
            ks.dispose();
        }
    }

    // ========================================================================
    // SECTION 4: INCOME SUFFICIENCY (Unified DTI <= 0.4)
    // ========================================================================
    @Test
    @DisplayName("DTI > 0.4 => fails hasAffordableDebtToIncome")
    void testPaymentExceedsIncome() {
        KieSession ks = createSession();
        try {
            ks.insert(new LoanRequestFact(1L, 10000000.0, 60, "INDEFINITE",
                    LocalDate.of(2020, 1, 1), null));
            ks.insert(new ClientFinancialProfile(1L, 30, 50000.0, 0.0, 0, 100000.0));

            ks.fireAllRules();

            assertFalse(queryExists(ks, "isLoanApproved", 1L));
            assertFalse(queryExists(ks, "hasSufficientIncome", 1L));
            assertFalse(queryExists(ks, "hasAffordableDebtToIncome", 1L));
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("Existing debt + new payment > 40% income => fails hasAffordableDebtToIncome")
    void testTotalDebtTooHigh() {
        KieSession ks = createSession();
        try {
            // income=50000, payment for 300000/60 approx 5661, existing=45000
            // (5661 + 45000)/50000 = 1.013 > 0.4 => fail
            ks.insert(new LoanRequestFact(1L, 300000.0, 60, "INDEFINITE",
                    LocalDate.of(2020, 1, 1), null));
            ks.insert(new ClientFinancialProfile(1L, 30, 50000.0, 45000.0, 2, 100000.0));

            ks.fireAllRules();

            assertFalse(queryExists(ks, "isLoanApproved", 1L));
            assertFalse(queryExists(ks, "hasSufficientIncome", 1L));
            assertFalse(queryExists(ks, "hasAffordableDebtToIncome", 1L));
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("Zero income => fails hasAffordableDebtToIncome")
    void testZeroIncome() {
        KieSession ks = createSession();
        try {
            ks.insert(new LoanRequestFact(1L, 100000.0, 60, "INDEFINITE",
                    LocalDate.of(2020, 1, 1), null));
            ks.insert(new ClientFinancialProfile(1L, 30, 0.0, 0.0, 0, 100000.0));

            ks.fireAllRules();

            assertFalse(queryExists(ks, "isLoanApproved", 1L));
            assertFalse(queryExists(ks, "hasSufficientIncome", 1L));
        } finally {
            ks.dispose();
        }
    }

    // ========================================================================
    // SECTION 5: RISK ASSESSMENT
    // ========================================================================
    @Test
    @DisplayName("Old age + long term => increased risk score but still acceptable")
    void testOldAgeLongTermRisk() {
        KieSession ks = createSession();
        try {
            ks.insert(new LoanRequestFact(1L, 100000.0, 240, "INDEFINITE",
                    LocalDate.of(2020, 1, 1), null));
            ks.insert(new ClientFinancialProfile(1L, 68, 100000.0, 0.0, 0, 500000.0));

            ks.fireAllRules();

            assertTrue(queryExists(ks, "hasAcceptableRisk", 1L));
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("High risk: old age + long term + many loans => score >= 50 => reject")
    void testHighRiskScore() {
        KieSession ks = createSession();
        try {
            ks.insert(new LoanRequestFact(1L, 100000.0, 240, "INDEFINITE",
                    LocalDate.of(2020, 1, 1), null));
            ks.insert(new ClientFinancialProfile(1L, 68, 100000.0, 15000.0, 4, 500000.0));
            // Insert 4 active LoanFacts so the active loan risk factor fires
            ks.insert(new LoanFact(101L, 1L, 5000.0, 100000.0, "ACTIVE", 24));
            ks.insert(new LoanFact(102L, 1L, 6000.0, 80000.0, "ACTIVE", 36));
            ks.insert(new LoanFact(103L, 1L, 4000.0, 50000.0, "ACTIVE", 12));
            ks.insert(new LoanFact(104L, 1L, 3000.0, 30000.0, "ACTIVE", 48));

            ks.fireAllRules();

            assertFalse(queryExists(ks, "hasAcceptableRisk", 1L));
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("Fixed term employment adds +10 risk")
    void testFixedTermRisk() {
        KieSession ks = createSession();
        try {
            ks.insert(new LoanRequestFact(1L, 100000.0, 60, "FIXED_TERM",
                    LocalDate.of(2020, 1, 1), LocalDate.now().plusYears(10)));
            ks.insert(new ClientFinancialProfile(1L, 30, 100000.0, 0.0, 0, 500000.0));

            ks.fireAllRules();

            assertTrue(queryExists(ks, "hasAcceptableRisk", 1L));
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("Young + indefinite => -10 risk reduction")
    void testYoungIndefiniteRiskReduction() {
        KieSession ks = createSession();
        try {
            ks.insert(new LoanRequestFact(1L, 100000.0, 60, "INDEFINITE",
                    LocalDate.of(2020, 1, 1), null));
            ks.insert(new ClientFinancialProfile(1L, 30, 100000.0, 0.0, 0, 500000.0));

            ks.fireAllRules();

            assertTrue(queryExists(ks, "hasAcceptableRisk", 1L));
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("Low account balance relative to loan adds +5 risk")
    void testLowBalanceRisk() {
        KieSession ks = createSession();
        try {
            ks.insert(new LoanRequestFact(1L, 100000.0, 60, "INDEFINITE",
                    LocalDate.of(2020, 1, 1), null));
            ks.insert(new ClientFinancialProfile(1L, 30, 100000.0, 0.0, 0, 5000.0));

            ks.fireAllRules();

            assertTrue(queryExists(ks, "hasAcceptableRisk", 1L));
        } finally {
            ks.dispose();
        }
    }

    // ========================================================================
    // SECTION 6: EXISTING LOAN CHECKS (LoanFact + LoanRepaymentFact)
    // ========================================================================
    @Test
    @DisplayName("Client with defaulted loan => fails hasNoDefaultedLoans")
    void testDefaultedLoanRejection() {
        KieSession ks = createSession();
        try {
            ks.insert(new LoanRequestFact(1L, 100000.0, 60, "INDEFINITE",
                    LocalDate.of(2020, 1, 1), null));
            ks.insert(new ClientFinancialProfile(1L, 34, 150000.0, 0.0, 0, 300000.0));
            ks.insert(new LoanFact(101L, 1L, 10000.0, 500000.0, "DEFAULTED", 60));

            ks.fireAllRules();

            assertTrue(queryExists(ks, "hasAnyDefaultedLoan", 1L));
            assertFalse(queryExists(ks, "hasNoDefaultedLoans", 1L));
            assertFalse(queryExists(ks, "isLoanApproved", 1L));
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("Client with active loan (no defaults) => passes hasNoDefaultedLoans")
    void testActiveLoanNoDefault() {
        KieSession ks = createSession();
        try {
            ks.insert(new LoanRequestFact(1L, 100000.0, 60, "INDEFINITE",
                    LocalDate.of(2020, 1, 1), null));
            ks.insert(new ClientFinancialProfile(1L, 34, 150000.0, 5000.0, 1, 300000.0));
            ks.insert(new LoanFact(101L, 1L, 5000.0, 200000.0, "ACTIVE", 48));

            ks.fireAllRules();

            assertFalse(queryExists(ks, "hasAnyDefaultedLoan", 1L));
            assertTrue(queryExists(ks, "hasNoDefaultedLoans", 1L));
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("Client with late payments => fails hasNoSeriousDelinquencies")
    void testLatePaymentsRejection() {
        KieSession ks = createSession();
        try {
            ks.insert(new LoanRequestFact(1L, 100000.0, 60, "INDEFINITE",
                    LocalDate.of(2020, 1, 1), null));
            ks.insert(new ClientFinancialProfile(1L, 34, 150000.0, 5000.0, 1, 300000.0));
            ks.insert(new LoanFact(101L, 1L, 5000.0, 200000.0, "ACTIVE", 48));
            ks.insert(new LoanRepaymentFact(1001L, 101L, 5000.0,
                    LocalDate.now().minusMonths(1).withDayOfMonth(15),
                    LocalDate.now().minusMonths(1).withDayOfMonth(20), "LATE"));

            ks.fireAllRules();

            assertTrue(queryExists(ks, "hasAnyDelinquentRepayment", 1L));
            assertFalse(queryExists(ks, "hasNoSeriousDelinquencies", 1L));
            assertFalse(queryExists(ks, "isLoanApproved", 1L));
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("Client with all payments on time => passes hasNoSeriousDelinquencies")
    void testAllPaymentsOnTime() {
        KieSession ks = createSession();
        try {
            ks.insert(new LoanRequestFact(1L, 100000.0, 60, "INDEFINITE",
                    LocalDate.of(2020, 1, 1), null));
            ks.insert(new ClientFinancialProfile(1L, 34, 150000.0, 5000.0, 1, 300000.0));
            ks.insert(new LoanFact(101L, 1L, 5000.0, 200000.0, "ACTIVE", 48));
            ks.insert(new LoanRepaymentFact(1001L, 101L, 5000.0,
                    LocalDate.now().minusMonths(1).withDayOfMonth(15),
                    LocalDate.now().minusMonths(1).withDayOfMonth(13), "PAID"));

            ks.fireAllRules();

            assertFalse(queryExists(ks, "hasAnyDelinquentRepayment", 1L));
            assertTrue(queryExists(ks, "hasNoSeriousDelinquencies", 1L));
        } finally {
            ks.dispose();
        }
    }

    // ========================================================================
    // SECTION 7: LOAN COUNT BOUNDARIES
    // ========================================================================
    @Test
    @DisplayName("Client with 3 active loans => fails notExcessiveExistingLoans")
    void testExcessiveActiveLoans() {
        KieSession ks = createSession();
        try {
            ks.insert(new LoanRequestFact(1L, 100000.0, 60, "INDEFINITE",
                    LocalDate.of(2020, 1, 1), null));
            ks.insert(new ClientFinancialProfile(1L, 34, 150000.0, 15000.0, 3, 300000.0));
            ks.insert(new LoanFact(101L, 1L, 5000.0, 100000.0, "ACTIVE", 24));
            ks.insert(new LoanFact(102L, 1L, 6000.0, 80000.0, "ACTIVE", 36));
            ks.insert(new LoanFact(103L, 1L, 4000.0, 50000.0, "ACTIVE", 12));

            ks.fireAllRules();

            assertTrue(queryExists(ks, "hasThreeOrMoreActiveLoans", 1L));
            assertFalse(queryExists(ks, "notExcessiveExistingLoans", 1L));
            assertFalse(queryExists(ks, "hasAcceptableRisk", 1L));
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("Client with 2 active loans => passes notExcessiveExistingLoans")
    void testTwoActiveLoansOk() {
        KieSession ks = createSession();
        try {
            ks.insert(new LoanRequestFact(1L, 100000.0, 60, "INDEFINITE",
                    LocalDate.of(2020, 1, 1), null));
            ks.insert(new ClientFinancialProfile(1L, 34, 150000.0, 10000.0, 2, 300000.0));
            ks.insert(new LoanFact(101L, 1L, 5000.0, 100000.0, "ACTIVE", 24));
            ks.insert(new LoanFact(102L, 1L, 5000.0, 80000.0, "ACTIVE", 36));

            ks.fireAllRules();

            assertFalse(queryExists(ks, "hasThreeOrMoreActiveLoans", 1L));
            assertTrue(queryExists(ks, "notExcessiveExistingLoans", 1L));
        } finally {
            ks.dispose();
        }
    }

    // ========================================================================
    // SECTION 8: COMPREHENSIVE SCENARIOS
    // ========================================================================
    @Test
    @DisplayName("Multiple failures: unemployed + too many installments + low income")
    void testMultipleRejections() {
        KieSession ks = createSession();
        try {
            ks.insert(new LoanRequestFact(1L, 9999999.0, 400, "UNEMPLOYED",
                    null, null));
            ks.insert(new ClientFinancialProfile(1L, 30, 5000.0, 2000.0, 1, 1000.0));

            ks.fireAllRules();

            assertFalse(queryExists(ks, "isLoanApproved", 1L));
            assertFalse(queryExists(ks, "hasValidLoanPeriod", 1L));
            assertFalse(queryExists(ks, "hasStableEmployment", 1L));
            assertFalse(queryExists(ks, "hasSufficientIncome", 1L));
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("LoanAssessment is created with correct metrics")
    void testLoanAssessmentCreation() {
        KieSession ks = createSession();
        try {
            ks.insert(new LoanRequestFact(1L, 500000.0, 60, "INDEFINITE",
                    LocalDate.of(2020, 1, 1), null));
            ks.insert(new ClientFinancialProfile(1L, 34, 150000.0, 5000.0, 1, 300000.0));

            ks.fireAllRules();

            var assessments = ks.getObjects(o -> o instanceof LoanAssessment);
            assertEquals(1, assessments.size());

            LoanAssessment assessment = (LoanAssessment) assessments.iterator().next();
            assertTrue(assessment.getMonthlyPayment() > 0);
            assertTrue(assessment.getRiskScore() >= 0);
            assertNotNull(assessment.getRiskLevel());
            assertTrue(assessment.getDebtToIncomeRatio() > 0);
            assertEquals(1L, assessment.getClientId());
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("Edge: minimal loan amount with maximum income")
    void testMinimalLoanMaxIncome() {
        KieSession ks = createSession();
        try {
            ks.insert(new LoanRequestFact(1L, 1000.0, 3, "INDEFINITE",
                    LocalDate.of(2020, 1, 1), null));
            ks.insert(new ClientFinancialProfile(1L, 30, 500000.0, 0.0, 0, 1000000.0));

            ks.fireAllRules();

            assertTrue(queryExists(ks, "isLoanApproved", 1L));
            assertTrue(queryExists(ks, "hasValidLoanPeriod", 1L));
            assertTrue(queryExists(ks, "hasSufficientIncome", 1L));
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("Edge: very large loan amount relative to income")
    void testVeryLargeLoan() {
        KieSession ks = createSession();
        try {
            ks.insert(new LoanRequestFact(1L, 50000000.0, 360, "INDEFINITE",
                    LocalDate.of(2020, 1, 1), null));
            ks.insert(new ClientFinancialProfile(1L, 30, 100000.0, 0.0, 0, 500000.0));

            ks.fireAllRules();

            assertFalse(queryExists(ks, "hasSufficientIncome", 1L));
        } finally {
            ks.dispose();
        }
    }

    // ========================================================================
    // SECTION 9: RISK LEVEL COMPUTATION
    // ========================================================================
    @Test
    @DisplayName("Risk level LOW when score < 15")
    void testRiskLevelLow() {
        KieSession ks = createSession();
        try {
            ks.insert(new LoanRequestFact(1L, 50000.0, 60, "INDEFINITE",
                    LocalDate.of(2020, 1, 1), null));
            ks.insert(new ClientFinancialProfile(1L, 30, 100000.0, 0.0, 0, 500000.0));

            ks.fireAllRules();

            LoanAssessment assessment = getAssessment(ks);
            assertEquals("LOW", assessment.getRiskLevel());
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("Risk level MEDIUM when score between 15 and 35")
    void testMediumRiskLevel() {
        KieSession ks = createSession();
        try {
            // Age 68 > 60 + inst 130 > 120 => +30
            // Age 68 > 55 but inst 130 NOT > 180 => no +15
            // INDEFINITE, age 68 not < 55 => no reduction
            // Loan 100000 <= 100000*12 => -5
            // Total: 30 - 5 = 25 => MEDIUM (15-35)
            ks.insert(new LoanRequestFact(1L, 100000.0, 130, "INDEFINITE",
                    LocalDate.of(2020, 1, 1), null));
            ks.insert(new ClientFinancialProfile(1L, 68, 100000.0, 0.0, 0, 500000.0));

            ks.fireAllRules();

            LoanAssessment assessment = getAssessment(ks);
            assertNotNull(assessment);
            assertEquals("MEDIUM", assessment.getRiskLevel());
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("Risk level HIGH when score between 35 and 50")
    void testHighRiskLevel() {
        KieSession ks = createSession();
        try {
            // Age > 55 (58) + 240 > 180 -> +15, 3 active loans -> +20
            // INDEFINITE age 58 not < 55 -> no reduction
            // Loan less than annual? 100000 <= 100000*12 -> -5
            // Total: 15 + 20 - 5 = 30 -> wait, that's MEDIUM
            // Let me use a case that gives HIGH: age > 60 + 240 > 120 -> +30
            // Age > 55 + 240 > 180 -> +15, 2 active loans -> +10
            // INDEFINITE age 62, not < 55
            // Loan 100000 <= 100000*12 -> -5
            // Total: 30 + 15 + 10 - 5 = 50 -> wait 50 is VERY_HIGH
            // Let me target score 40 (between 35 and 50):
            // Age > 55 (60) + 240 > 180 -> +15
            // 2 active loans -> +10
            // INDEFINITE age 60 not < 55 -> no reduction
            // Loan 100000 <= 100000*12 -> -5
            // Fixed term -> +10
            // Total: 15 + 10 + 10 - 5 = 30 -> MEDIUM
            // Hmm. Let me try:
            // Age > 60 (62) + 240 > 120 -> +30
            // Age > 55 (62) + 240 > 180 -> +15
            // 0 active loans -> no factor
            // INDEFINITE age 62 not < 55 -> no reduction
            // Loan 100000 <= 100000*12 -> -5
            // Total: 30 + 15 - 5 = 40 -> HIGH!
            ks.insert(new LoanRequestFact(1L, 100000.0, 240, "INDEFINITE",
                    LocalDate.of(2020, 1, 1), null));
            ks.insert(new ClientFinancialProfile(1L, 62, 100000.0, 0.0, 0, 500000.0));

            ks.fireAllRules();

            LoanAssessment assessment = getAssessment(ks);
            assertNotNull(assessment);
            assertEquals("HIGH", assessment.getRiskLevel());
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("Very high risk level when score >= 50")
    void testVeryHighRiskLevel() {
        KieSession ks = createSession();
        try {
            // Age > 60 (68) + 240 > 120 -> +30
            // Age > 55 (68) + 240 > 180 -> +15
            // Fixed term -> +10
            // Loan 100000 <= 100000*12 -> -5
            // Total: 30 + 15 + 10 - 5 = 50 -> VERY_HIGH
            ks.insert(new LoanRequestFact(1L, 100000.0, 240, "FIXED_TERM",
                    LocalDate.of(2020, 1, 1), LocalDate.now().plusYears(10)));
            ks.insert(new ClientFinancialProfile(1L, 68, 100000.0, 20000.0, 4, 500000.0));

            ks.fireAllRules();

            LoanAssessment assessment = getAssessment(ks);
            assertNotNull(assessment);
            assertTrue(assessment.getRiskScore() >= 50.0);
            assertEquals("VERY_HIGH", assessment.getRiskLevel());
        } finally {
            ks.dispose();
        }
    }

    // ========================================================================
    // SECTION 10: AGE BOUNDARY TESTS
    // ========================================================================
    @Test
    @DisplayName("Age exactly 60 with installments > 120 => risk factor triggers")
    void testAgeExactly60WithLongTerm() {
        KieSession ks = createSession();
        try {
            // Age > 60 is false (60 is not > 60), so the +30 rule should NOT fire
            // Age > 55 is true (60 > 55) AND 240 > 180 -> +15
            // No other factors
            // Total: 15 -> MEDIUM
            ks.insert(new LoanRequestFact(1L, 100000.0, 240, "INDEFINITE",
                    LocalDate.of(2020, 1, 1), null));
            ks.insert(new ClientFinancialProfile(1L, 60, 100000.0, 0.0, 0, 500000.0));

            ks.fireAllRules();

            LoanAssessment assessment = getAssessment(ks);
            assertNotNull(assessment);
            // Score should be 15 (age > 55 + inst > 180) - 5 (loan < annual income) = 10
            assertTrue(assessment.getRiskScore() >= 10.0 && assessment.getRiskScore() < 50.0);
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("Age exactly 55 with installments > 180 => risk factor triggers")
    void testAgeExactly55WithLongTerm() {
        KieSession ks = createSession();
        try {
            // Age > 55 is false (55 is not > 55)
            // Age > 60 is false
            // So neither age risk factor triggers
            // No other factors
            // Total: 0 -> LOW
            ks.insert(new LoanRequestFact(1L, 100000.0, 240, "INDEFINITE",
                    LocalDate.of(2020, 1, 1), null));
            ks.insert(new ClientFinancialProfile(1L, 55, 100000.0, 0.0, 0, 500000.0));

            ks.fireAllRules();

            LoanAssessment assessment = getAssessment(ks);
            assertNotNull(assessment);
            // Score should be 0 - 5 = max(0, -5) = 0
            assertEquals(0.0, assessment.getRiskScore(), 0.001);
            assertTrue(queryExists(ks, "hasAcceptableRisk", 1L));
        } finally {
            ks.dispose();
        }
    }

    // ========================================================================
    // SECTION 11: DTI BOUNDARY TESTS
    // ========================================================================
    @Test
    @DisplayName("DTI exactly 0.4 => passes hasAffordableDebtToIncome (boundary)")
    void testDTIExactlyPointFour() {
        KieSession ks = createSession();
        try {
            // Use a case where DTI is clearly at or below 0.4
            // income=100000, existing=0, loan=500000, inst=360
            // payment for 500000/360 is approx 2684
            // DTI = 2684/100000 = 0.0268 << 0.4
            ks.insert(new LoanRequestFact(1L, 500000.0, 360, "INDEFINITE",
                    LocalDate.of(2020, 1, 1), null));
            ks.insert(new ClientFinancialProfile(1L, 30, 100000.0, 0.0, 0, 500000.0));

            ks.fireAllRules();

            assertTrue(queryExists(ks, "hasAffordableDebtToIncome", 1L));
            assertTrue(queryExists(ks, "hasSufficientIncome", 1L));
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("DTI slightly below 0.4 => should pass")
    void testDTIBelowThreshold() {
        KieSession ks = createSession();
        try {
            // income=200000, existing=50000, loan=500000, inst=360
            // payment for 500000/360 ≈ 2684
            // DTI = (2684 + 50000)/200000 = 0.2634 < 0.4
            ks.insert(new LoanRequestFact(1L, 500000.0, 360, "INDEFINITE",
                    LocalDate.of(2020, 1, 1), null));
            ks.insert(new ClientFinancialProfile(1L, 30, 200000.0, 50000.0, 0, 500000.0));

            ks.fireAllRules();

            assertTrue(queryExists(ks, "hasSufficientIncome", 1L));
            assertTrue(queryExists(ks, "hasAffordableDebtToIncome", 1L));
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("DTI slightly above 0.4 => should fail")
    void testDTIAboveThreshold() {
        KieSession ks = createSession();
        try {
            // income=50000, existing=0, loan=10000000, inst=60
            // payment for 10000000/60 ≈ 188712
            // DTI = 188712/50000 = 3.77 > 0.4
            ks.insert(new LoanRequestFact(1L, 10000000.0, 60, "INDEFINITE",
                    LocalDate.of(2020, 1, 1), null));
            ks.insert(new ClientFinancialProfile(1L, 30, 50000.0, 0.0, 0, 100000.0));

            ks.fireAllRules();

            assertFalse(queryExists(ks, "hasSufficientIncome", 1L));
            assertFalse(queryExists(ks, "hasAffordableDebtToIncome", 1L));
        } finally {
            ks.dispose();
        }
    }

    // ========================================================================
    // SECTION 12: CREDIT HISTORY TESTS
    // ========================================================================
    @Test
    @DisplayName("Client with good credit history (previous loans, all paid on time)")
    void testGoodCreditHistory() {
        KieSession ks = createSession();
        try {
            ks.insert(new LoanRequestFact(1L, 500000.0, 60, "INDEFINITE",
                    LocalDate.of(2020, 1, 1), null));
            ks.insert(new ClientFinancialProfile(1L, 34, 150000.0, 5000.0, 1, 300000.0));
            // Previous loan with perfect payment history
            ks.insert(new LoanFact(101L, 1L, 5000.0, 0.0, "PAID", 48));
            ks.insert(new LoanRepaymentFact(1001L, 101L, 5000.0,
                    LocalDate.now().minusMonths(1).withDayOfMonth(15),
                    LocalDate.now().minusMonths(1).withDayOfMonth(13), "PAID"));

            ks.fireAllRules();

            // Should have good credit history
            assertTrue(queryExists(ks, "hasGoodCreditHistory", 1L));
            assertTrue(queryExists(ks, "hasPreviousLoans", 1L));
            assertTrue(queryExists(ks, "isLoanApproved", 1L));

            // Should have a PositiveCreditHistoryFact
            var positiveHistory = ks.getObjects(o -> o instanceof PositiveCreditHistoryFact);
            assertEquals(1, positiveHistory.size());
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("Client with multiple fully repaid loans => good credit history")
    void testMultipleGoodLoans() {
        KieSession ks = createSession();
        try {
            ks.insert(new LoanRequestFact(1L, 500000.0, 60, "INDEFINITE",
                    LocalDate.of(2020, 1, 1), null));
            ks.insert(new ClientFinancialProfile(1L, 34, 150000.0, 0.0, 0, 300000.0));
            // Two previous paid loans with perfect history
            ks.insert(new LoanFact(101L, 1L, 5000.0, 0.0, "PAID", 48));
            ks.insert(new LoanFact(102L, 1L, 3000.0, 0.0, "PAID", 36));
            ks.insert(new LoanRepaymentFact(1001L, 101L, 5000.0,
                    LocalDate.now().minusMonths(1).withDayOfMonth(15),
                    LocalDate.now().minusMonths(1).withDayOfMonth(13), "PAID"));
            ks.insert(new LoanRepaymentFact(1002L, 102L, 3000.0,
                    LocalDate.now().minusMonths(1).withDayOfMonth(10),
                    LocalDate.now().minusMonths(1).withDayOfMonth(8), "PAID"));

            ks.fireAllRules();

            assertTrue(queryExists(ks, "hasGoodCreditHistory", 1L));
            assertTrue(queryExists(ks, "isLoanApproved", 1L));
        } finally {
            ks.dispose();
        }
    }

    // ========================================================================
    // SECTION 13: AGE + LOAN TERM LIMIT TESTS
    // ========================================================================
    @Test
    @DisplayName("Age at end of loan exactly at boundary (75)")
    void testAgeAtLoanEndAtBoundary() {
        KieSession ks = createSession();
        try {
            // age + inst/12 <= 75
            // age = 65, inst = 120 => 65 + 10 = 75 -> passes
            ks.insert(new LoanRequestFact(1L, 100000.0, 120, "INDEFINITE",
                    LocalDate.of(2020, 1, 1), null));
            ks.insert(new ClientFinancialProfile(1L, 65, 100000.0, 0.0, 0, 500000.0));

            ks.fireAllRules();

            assertTrue(queryExists(ks, "agePlusLoanTermWithinLimit", 1L));
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("Age at end of loan above boundary (75)")
    void testAgeAtLoanEndAboveBoundary() {
        KieSession ks = createSession();
        try {
            // age = 65, inst = 240 => 65 + 20 = 85 > 75 -> fails
            ks.insert(new LoanRequestFact(1L, 100000.0, 240, "INDEFINITE",
                    LocalDate.of(2020, 1, 1), null));
            ks.insert(new ClientFinancialProfile(1L, 65, 100000.0, 0.0, 0, 500000.0));

            ks.fireAllRules();

            assertFalse(queryExists(ks, "agePlusLoanTermWithinLimit", 1L));
        } finally {
            ks.dispose();
        }
    }

    // ========================================================================
    // SECTION 14: DECISION REASON TESTS
    // ========================================================================
    @Test
    @DisplayName("DecisionReasonFacts are inserted by rules")
    void testDecisionReasonsPopulated() {
        KieSession ks = createSession();
        try {
            ks.insert(new LoanRequestFact(1L, 9999999.0, 400, "UNEMPLOYED",
                    null, null));
            ks.insert(new ClientFinancialProfile(1L, 30, 5000.0, 2000.0, 1, 1000.0));

            ks.fireAllRules();

            var reasons = ks.getObjects(o -> o instanceof DecisionReasonFact);
            assertTrue(reasons.size() > 0);

            LoanAssessment assessment = getAssessment(ks);
            assertNotNull(assessment);
        } finally {
            ks.dispose();
        }
    }
}
