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
    @DisplayName("Fixed term with short contract => fails employmentCoversFullTerm")
    void testFixedTermShortContract() {
        KieSession ks = createSession();
        try {
            ks.insert(new LoanRequestFact(1L, 100000.0, 60, "FIXED_TERM",
                    LocalDate.of(2023, 1, 1), LocalDate.of(2024, 6, 1)));
            ks.insert(new ClientFinancialProfile(1L, 30, 50000.0, 0.0, 0, 100000.0));

            ks.fireAllRules();

            assertFalse(queryExists(ks, "isLoanApproved", 1L));
            assertFalse(queryExists(ks, "hasStableEmployment", 1L));
            assertFalse(queryExists(ks, "fixedTermEmploymentCoversLoan", 1L));
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("Fixed term covering full loan term => stable employment")
    void testFixedTermLongContract() {
        KieSession ks = createSession();
        try {
            ks.insert(new LoanRequestFact(1L, 100000.0, 60, "FIXED_TERM",
                    LocalDate.of(2020, 1, 1), LocalDate.now().plusYears(10)));
            ks.insert(new ClientFinancialProfile(1L, 30, 50000.0, 0.0, 0, 100000.0));

            ks.fireAllRules();

            assertTrue(queryExists(ks, "hasStableEmployment", 1L));
            assertTrue(queryExists(ks, "employmentCoversFullTerm", 1L));
        } finally {
            ks.dispose();
        }
    }

    // ========================================================================
    // SECTION 4: INCOME SUFFICIENCY
    // ========================================================================
    @Test
    @DisplayName("Monthly payment exceeds 70% of income => fails paymentIsAffordable")
    void testPaymentExceedsIncome() {
        KieSession ks = createSession();
        try {
            ks.insert(new LoanRequestFact(1L, 10000000.0, 60, "INDEFINITE",
                    LocalDate.of(2020, 1, 1), null));
            ks.insert(new ClientFinancialProfile(1L, 30, 50000.0, 0.0, 0, 100000.0));

            ks.fireAllRules();

            assertFalse(queryExists(ks, "isLoanApproved", 1L));
            assertFalse(queryExists(ks, "hasSufficientIncome", 1L));
            assertFalse(queryExists(ks, "paymentIsAffordable", 1L));
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("Existing debt + new payment > 50% income => fails totalDebtIsAcceptable")
    void testTotalDebtTooHigh() {
        KieSession ks = createSession();
        try {
            // income=50000, payment for 300000/60 approx 5661, existing=45000
            // (5661 + 45000)/50000 = 1.013 > 0.5 => fail
            ks.insert(new LoanRequestFact(1L, 300000.0, 60, "INDEFINITE",
                    LocalDate.of(2020, 1, 1), null));
            ks.insert(new ClientFinancialProfile(1L, 30, 50000.0, 45000.0, 2, 100000.0));

            ks.fireAllRules();

            assertFalse(queryExists(ks, "isLoanApproved", 1L));
            assertFalse(queryExists(ks, "hasSufficientIncome", 1L));
            assertFalse(queryExists(ks, "totalDebtIsAcceptable", 1L));
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("Zero income => fails paymentIsAffordable and totalDebtIsAcceptable")
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
    @DisplayName("Old age + long term => increased risk score")
    void testOldAgeLongTermRisk() {
        KieSession ks = createSession();
        try {
            // 68 > 60, 240 > 120 => +30 risk, no existing loans => total 30 < 50 => acceptable
            ks.insert(new LoanRequestFact(1L, 100000.0, 240, "INDEFINITE",
                    LocalDate.of(2020, 1, 1), null));
            ks.insert(new ClientFinancialProfile(1L, 68, 100000.0, 0.0, 0, 500000.0));
            // No LoanFacts inserted, so existingLoanCount=0

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
            // 68>60 + 240>120 => +30, existingLoanCount=4 => +20, total=50 => NOT < 50
            ks.insert(new LoanRequestFact(1L, 100000.0, 240, "INDEFINITE",
                    LocalDate.of(2020, 1, 1), null));
            ks.insert(new ClientFinancialProfile(1L, 68, 100000.0, 15000.0, 4, 500000.0));

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
            // FIXED_TERM => +10 risk, age 30 < 55 and INDEFINITE check doesn't apply
            // total risk = 10 < 50 => acceptable
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
            // INDEFINITE + age 30 < 55 => -10, loan <= income*12 => -5, total = -15 => clamped to 0
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
            // loan 100000, balance 5000 < 10000 (10% of loan) => +5 risk
            // total = 5 < 50 => acceptable
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

    @Test
    @DisplayName("Client with 3+ active loans => fails notExcessiveExistingLoans")
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
    // SECTION 7: COMPREHENSIVE SCENARIOS
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
            assertFalse(queryExists(ks, "paymentIsAffordable", 1L));
        } finally {
            ks.dispose();
        }
    }

    // ========================================================================
    // SECTION 8: RISK LEVEL COMPUTATION
    // ========================================================================
    @Test
    @DisplayName("Risk level computation via LoanAssessment")
    void testRiskLevels() {
        KieSession ks = createSession();
        try {
            // Score = 0 => LOW
            ks.insert(new LoanRequestFact(1L, 50000.0, 60, "INDEFINITE",
                    LocalDate.of(2020, 1, 1), null));
            ks.insert(new ClientFinancialProfile(1L, 30, 100000.0, 0.0, 0, 500000.0));

            ks.fireAllRules();

            var assessments = ks.getObjects(o -> o instanceof LoanAssessment);
            LoanAssessment assessment = (LoanAssessment) assessments.iterator().next();
            assertEquals("LOW", assessment.getRiskLevel());
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("Medium risk level when score between 15 and 35")
    void testMediumRiskLevel() {
        KieSession ks = createSession();
        try {
            // 1 existing loan => +10, old age 55+ with 180+ term => +15, total=25 => MEDIUM
            ks.insert(new LoanRequestFact(1L, 100000.0, 240, "INDEFINITE",
                    LocalDate.of(2020, 1, 1), null));
            ks.insert(new ClientFinancialProfile(1L, 58, 100000.0, 5000.0, 1, 500000.0));

            ks.fireAllRules();

            var assessments = ks.getObjects(o -> o instanceof LoanAssessment);
            LoanAssessment assessment = (LoanAssessment) assessments.iterator().next();
            assertEquals("MEDIUM", assessment.getRiskLevel());
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("Very high risk level when score >= 50")
    void testVeryHighRiskLevel() {
        KieSession ks = createSession();
        try {
            // 68>60 + 240>120 => +30, 4 loans => +20, fixed term => +10, total=60 => VERY_HIGH
            ks.insert(new LoanRequestFact(1L, 100000.0, 240, "FIXED_TERM",
                    LocalDate.of(2020, 1, 1), LocalDate.now().plusYears(10)));
            ks.insert(new ClientFinancialProfile(1L, 68, 100000.0, 20000.0, 4, 500000.0));

            ks.fireAllRules();

            var assessments = ks.getObjects(o -> o instanceof LoanAssessment);
            LoanAssessment assessment = (LoanAssessment) assessments.iterator().next();
            assertTrue(assessment.getRiskScore() >= 50.0);
            assertEquals("VERY_HIGH", assessment.getRiskLevel());
        } finally {
            ks.dispose();
        }
    }
}
