package com.example.bank.rules;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieSessionConfiguration;
import org.kie.api.runtime.conf.ClockTypeOption;
import org.kie.api.time.SessionPseudoClock;
import org.kie.internal.io.ResourceFactory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class FraudDetectionRulesTest {

    private static KieBase fraudKieBase;
    private static KieContainer fraudKieContainer;

    @BeforeAll
    static void buildFraudKieContainer() {
        KieServices ks = KieServices.Factory.get();
        KieFileSystem kfs = ks.newKieFileSystem();

        String kmodule = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<kmodule xmlns=\"http://www.drools.org/xsd/kmodule\">\n" +
            "  <kbase name=\"fraudKieBase\" packages=\"rules.fraud\" eventProcessingMode=\"stream\">\n" +
            "    <ksession name=\"fraudKieSession\" clockType=\"realtime\"/>\n" +
            "    <ksession name=\"fraudPseudoClockKieSession\" clockType=\"pseudo\"/>\n" +
            "  </kbase>\n" +
            "</kmodule>";

        kfs.writeKModuleXML(kmodule);
        kfs.write(ResourceFactory.newClassPathResource("rules/fraud-detection-rules.drl"));

        KieBuilder kb = ks.newKieBuilder(kfs);
        kb.buildAll();

        assertFalse(kb.getResults().hasMessages(Message.Level.ERROR),
            "DRL build errors: " + kb.getResults().getMessages());

        fraudKieContainer = ks.newKieContainer(ks.getRepository().getDefaultReleaseId());
        fraudKieBase = fraudKieContainer.getKieBase("fraudKieBase");
    }

    private KieSession createRealtimeSession() {
        return fraudKieContainer.newKieSession("fraudKieSession");
    }

    private List<SuspiciousTransactionFact> getSuspiciousList(KieSession ks) {
        Collection<?> facts = ks.getObjects(o -> o instanceof SuspiciousTransactionFact);
        List<SuspiciousTransactionFact> result = new ArrayList<>();
        for (Object f : facts) {
            result.add((SuspiciousTransactionFact) f);
        }
        return result;
    }

    private long countSuspicious(KieSession ks) {
        return getSuspiciousList(ks).size();
    }

    private List<String> getReasons(KieSession ks) {
        return getSuspiciousList(ks).stream()
            .map(SuspiciousTransactionFact::getReason)
            .collect(Collectors.toList());
    }

    private static long epoch(LocalDateTime ldt) {
        return ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    // ========================================================================
    // MANY_SMALL_TRANSACTIONS — >= 5 transactions under 20 EUR within 10 min
    // ========================================================================

    @Test
    @DisplayName("5 small transactions within 10 minutes => 5th triggers MANY_SMALL")
    void testManySmallTransactionsTriggered() {
        KieSessionConfiguration config = KieServices.Factory.get().newKieSessionConfiguration();
        config.setOption(ClockTypeOption.get("pseudo"));
        KieSession ks = fraudKieBase.newKieSession(config, null);
        try {
            SessionPseudoClock clock = ks.getSessionClock();
            long now = clock.getCurrentTime();

            for (int i = 0; i < 5; i++) {
                ks.insert(new CardTransactionEvent((long) i + 1, 1L, 100L,
                    "ACC-001", "ACC-002",
                    10.0, now,
                    45.0, 15.0, "Zagreb", "Croatia"));
                clock.advanceTime(1, TimeUnit.MINUTES);
                now = clock.getCurrentTime();
            }

            ks.fireAllRules();
            List<String> reasons = getReasons(ks);
            assertTrue(reasons.stream().anyMatch(r -> r.contains("Many small")),
                "Expected MANY_SMALL to fire for event 5");
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("4 small transactions only => MANY_SMALL not triggered")
    void testManySmallTransactionsNotTriggered() {
        KieSessionConfiguration config = KieServices.Factory.get().newKieSessionConfiguration();
        config.setOption(ClockTypeOption.get("pseudo"));
        KieSession ks = fraudKieBase.newKieSession(config, null);
        try {
            SessionPseudoClock clock = ks.getSessionClock();
            long now = clock.getCurrentTime();

            for (int i = 0; i < 4; i++) {
                ks.insert(new CardTransactionEvent((long) i + 1, 1L, 100L,
                    "ACC-001", "ACC-002",
                    15.0, now,
                    45.0, 15.0, "Zagreb", "Croatia"));
                clock.advanceTime(1, TimeUnit.MINUTES);
                now = clock.getCurrentTime();
            }

            ks.fireAllRules();
            List<String> reasons = getReasons(ks);
            assertFalse(reasons.stream().anyMatch(r -> r.contains("Many small")),
                "Expected no MANY_SMALL with only 4 events");
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("5 small transactions but time gap exceeds 10 minutes => not triggered")
    void testManySmallTransactionsOutsideWindow() {
        KieSessionConfiguration config = KieServices.Factory.get().newKieSessionConfiguration();
        config.setOption(ClockTypeOption.get("pseudo"));
        KieSession ks = fraudKieBase.newKieSession(config, null);
        try {
            SessionPseudoClock clock = ks.getSessionClock();
            long now = clock.getCurrentTime();

            for (int i = 0; i < 4; i++) {
                ks.insert(new CardTransactionEvent((long) i + 1, 1L, 100L,
                    "ACC-001", "ACC-002",
                    10.0, now,
                    45.0, 15.0, "Zagreb", "Croatia"));
                clock.advanceTime(1, TimeUnit.MINUTES);
                now = clock.getCurrentTime();
            }

            clock.advanceTime(15, TimeUnit.MINUTES);
            now = clock.getCurrentTime();

            ks.insert(new CardTransactionEvent(5L, 1L, 100L,
                "ACC-001", "ACC-002",
                10.0, now,
                45.0, 15.0, "Zagreb", "Croatia"));

            ks.fireAllRules();
            List<String> reasons = getReasons(ks);
            assertFalse(reasons.stream().anyMatch(r -> r.contains("Many small")),
                "Expected no MANY_SMALL when events outside 10-minute window");
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("6th small transaction also flagged for MANY_SMALL, no cross-rule duplicate")
    void testManySmallTransactionsMultipleFires() {
        KieSessionConfiguration config = KieServices.Factory.get().newKieSessionConfiguration();
        config.setOption(ClockTypeOption.get("pseudo"));
        KieSession ks = fraudKieBase.newKieSession(config, null);
        try {
            SessionPseudoClock clock = ks.getSessionClock();
            long now = clock.getCurrentTime();

            for (int i = 0; i < 6; i++) {
                ks.insert(new CardTransactionEvent((long) i + 1, 1L, 100L,
                    "ACC-001", "ACC-002",
                    10.0, now,
                    45.0, 15.0, "Zagreb", "Croatia"));
                clock.advanceTime(1, TimeUnit.MINUTES);
                now = clock.getCurrentTime();
            }

            ks.fireAllRules();
            // Events 5 and 6 should trigger MANY_SMALL (each has 4+ prior events in 10m)
            // Event 1 triggers NEW_LOCATION
            List<String> reasons = getReasons(ks);
            long manySmallCount = reasons.stream().filter(r -> r.contains("Many small")).count();
            assertTrue(manySmallCount >= 2, "Expected MANY_SMALL for at least events 5 and 6, got " + manySmallCount);
        } finally {
            ks.dispose();
        }
    }

    // ========================================================================
    // LARGE_NIGHT_TRANSACTION — amount > 5000 EUR between 00:00 and 05:00
    // ========================================================================

    @Test
    @DisplayName("6000 EUR at 03:00 => LARGE_NIGHT_TRANSACTION triggered")
    void testLargeNightTransactionTriggered() {
        KieSession ks = createRealtimeSession();
        try {
            ks.insert(new CardTransactionEvent(1L, 1L, 100L,
                "ACC-001", "ACC-002",
                6000.0, epoch(LocalDateTime.of(2026, 6, 15, 3, 0)),
                45.0, 15.0, "Zagreb", "Croatia"));
            ks.fireAllRules();
            List<String> reasons = getReasons(ks);
            assertTrue(reasons.stream().anyMatch(r -> r.contains("Large night")),
                "Expected LARGE_NIGHT for 6000 EUR at 03:00");
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("6000 EUR at 15:00 => LARGE_NIGHT not triggered")
    void testLargeNightTransactionDaytime() {
        KieSession ks = createRealtimeSession();
        try {
            ks.insert(new CardTransactionEvent(1L, 1L, 100L,
                "ACC-001", "ACC-002",
                6000.0, epoch(LocalDateTime.of(2026, 6, 15, 15, 0)),
                45.0, 15.0, "Zagreb", "Croatia"));
            ks.fireAllRules();
            List<String> reasons = getReasons(ks);
            assertFalse(reasons.stream().anyMatch(r -> r.contains("Large night")),
                "Expected no LARGE_NIGHT for daytime");
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("3000 EUR at 03:00 => LARGE_NIGHT not triggered (amount not > 5000)")
    void testLargeNightTransactionSmallAmount() {
        KieSession ks = createRealtimeSession();
        try {
            ks.insert(new CardTransactionEvent(1L, 1L, 100L,
                "ACC-001", "ACC-002",
                3000.0, epoch(LocalDateTime.of(2026, 6, 15, 3, 0)),
                45.0, 15.0, "Zagreb", "Croatia"));
            ks.fireAllRules();
            List<String> reasons = getReasons(ks);
            assertFalse(reasons.stream().anyMatch(r -> r.contains("Large night")),
                "Expected no LARGE_NIGHT for 3000 EUR");
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("5000.01 EUR at 04:59 => LARGE_NIGHT triggered (boundary)")
    void testLargeNightTransactionBoundaryAmount() {
        KieSession ks = createRealtimeSession();
        try {
            ks.insert(new CardTransactionEvent(1L, 1L, 100L,
                "ACC-001", "ACC-002",
                5000.01, epoch(LocalDateTime.of(2026, 6, 15, 4, 59)),
                45.0, 15.0, "Zagreb", "Croatia"));
            ks.fireAllRules();
            List<String> reasons = getReasons(ks);
            assertTrue(reasons.stream().anyMatch(r -> r.contains("Large night")),
                "Expected LARGE_NIGHT at boundary");
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("6000 EUR at 05:00 => LARGE_NIGHT not triggered (hour boundary)")
    void testLargeNightTransactionBoundaryHour() {
        KieSession ks = createRealtimeSession();
        try {
            ks.insert(new CardTransactionEvent(1L, 1L, 100L,
                "ACC-001", "ACC-002",
                6000.0, epoch(LocalDateTime.of(2026, 6, 15, 5, 0)),
                45.0, 15.0, "Zagreb", "Croatia"));
            ks.fireAllRules();
            List<String> reasons = getReasons(ks);
            assertFalse(reasons.stream().anyMatch(r -> r.contains("Large night")),
                "Expected no LARGE_NIGHT at 05:00");
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("5000.00 EUR at 03:00 => LARGE_NIGHT not triggered (amount = 5000)")
    void testLargeNightTransactionExactThreshold() {
        KieSession ks = createRealtimeSession();
        try {
            ks.insert(new CardTransactionEvent(1L, 1L, 100L,
                "ACC-001", "ACC-002",
                5000.0, epoch(LocalDateTime.of(2026, 6, 15, 3, 0)),
                45.0, 15.0, "Zagreb", "Croatia"));
            ks.fireAllRules();
            List<String> reasons = getReasons(ks);
            assertFalse(reasons.stream().anyMatch(r -> r.contains("Large night")),
                "Expected no LARGE_NIGHT for exactly 5000.00");
        } finally {
            ks.dispose();
        }
    }

    // ========================================================================
    // UNUSUAL_TRANSACTION_AMOUNT — amount >= 5x average of last 30 transactions
    // ========================================================================

    @Test
    @DisplayName("30 normal transactions then 1 large (6x avg) => UNUSUAL_AMOUNT triggered")
    void testUnusualAmountTriggered() {
        KieSession ks = createRealtimeSession();
        try {
            long baseTime = epoch(LocalDateTime.of(2026, 6, 1, 10, 0));
            for (int i = 0; i < 30; i++) {
                ks.insert(new CardTransactionEvent((long) i + 1, 1L, 100L,
                    "ACC-001", "ACC-002",
                    100.0, baseTime + i * 60000L,
                    45.0, 15.0, "Zagreb", "Croatia"));
            }

            ks.insert(new CardTransactionEvent(31L, 1L, 100L,
                "ACC-001", "ACC-002",
                600.0, baseTime + 30 * 60000L,
                45.0, 15.0, "Zagreb", "Croatia"));

            ks.fireAllRules();
            List<String> reasons = getReasons(ks);
            assertTrue(reasons.stream().anyMatch(r -> r.contains("5x the average")),
                "Expected UNUSUAL_AMOUNT for 6x avg transaction");
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("30 normal transactions then 1 slightly large (4x avg) => not triggered")
    void testUnusualAmountNotTriggered() {
        KieSession ks = createRealtimeSession();
        try {
            long baseTime = epoch(LocalDateTime.of(2026, 6, 1, 10, 0));
            for (int i = 0; i < 30; i++) {
                ks.insert(new CardTransactionEvent((long) i + 1, 1L, 100L,
                    "ACC-001", "ACC-002",
                    100.0, baseTime + i * 60000L,
                    45.0, 15.0, "Zagreb", "Croatia"));
            }

            ks.insert(new CardTransactionEvent(31L, 1L, 100L,
                "ACC-001", "ACC-002",
                400.0, baseTime + 30 * 60000L,
                45.0, 15.0, "Zagreb", "Croatia"));

            ks.fireAllRules();
            List<String> reasons = getReasons(ks);
            assertFalse(reasons.stream().anyMatch(r -> r.contains("5x the average")),
                "Expected no UNUSUAL_AMOUNT for 4x avg");
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("30 transactions of 100 then 1 of 500 (exactly 5x) => triggered")
    void testUnusualAmountBoundary() {
        KieSession ks = createRealtimeSession();
        try {
            long baseTime = epoch(LocalDateTime.of(2026, 6, 1, 10, 0));
            for (int i = 0; i < 30; i++) {
                ks.insert(new CardTransactionEvent((long) i + 1, 1L, 100L,
                    "ACC-001", "ACC-002",
                    100.0, baseTime + i * 60000L,
                    45.0, 15.0, "Zagreb", "Croatia"));
            }

            ks.insert(new CardTransactionEvent(31L, 1L, 100L,
                "ACC-001", "ACC-002",
                500.0, baseTime + 30 * 60000L,
                45.0, 15.0, "Zagreb", "Croatia"));

            ks.fireAllRules();
            List<String> reasons = getReasons(ks);
            assertTrue(reasons.stream().anyMatch(r -> r.contains("5x the average")),
                "Expected UNUSUAL_AMOUNT for exactly 5x avg");
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("Fewer than 30 transactions, 1 large => triggered (avg over available)")
    void testUnusualAmountFewTransactions() {
        KieSession ks = createRealtimeSession();
        try {
            long baseTime = epoch(LocalDateTime.of(2026, 6, 1, 10, 0));
            ks.insert(new CardTransactionEvent(1L, 1L, 100L,
                "ACC-001", "ACC-002",
                50.0, baseTime,
                45.0, 15.0, "Zagreb", "Croatia"));
            ks.insert(new CardTransactionEvent(2L, 1L, 100L,
                "ACC-001", "ACC-002",
                50.0, baseTime + 60000L,
                45.0, 15.0, "Zagreb", "Croatia"));

            ks.insert(new CardTransactionEvent(3L, 1L, 100L,
                "ACC-001", "ACC-002",
                1000.0, baseTime + 120000L,
                45.0, 15.0, "Zagreb", "Croatia"));

            ks.fireAllRules();
            List<String> reasons = getReasons(ks);
            assertTrue(reasons.stream().anyMatch(r -> r.contains("5x the average")),
                "Expected UNUSUAL_AMOUNT for 20x avg with few transactions");
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("First transaction (no history) => not flagged by UNUSUAL_AMOUNT (avg is null)")
    void testUnusualAmountFirstTransaction() {
        KieSession ks = createRealtimeSession();
        try {
            ks.insert(new CardTransactionEvent(1L, 1L, 100L,
                "ACC-001", "ACC-002",
                10000.0, epoch(LocalDateTime.of(2026, 6, 1, 10, 0)),
                45.0, 15.0, "Zagreb", "Croatia"));

            ks.fireAllRules();
            List<String> reasons = getReasons(ks);
            assertFalse(reasons.stream().anyMatch(r -> r.contains("5x the average")),
                "Expected no UNUSUAL_AMOUNT for first transaction");
        } finally {
            ks.dispose();
        }
    }

    // ========================================================================
    // NEW_LOCATION — transaction from city not seen in previous 90 days
    // ========================================================================

    @Test
    @DisplayName("First transaction from unknown city => NEW_LOCATION triggered")
    void testNewLocationTriggered() {
        KieSession ks = createRealtimeSession();
        try {
            ks.insert(new CardTransactionEvent(1L, 1L, 100L,
                "ACC-001", "ACC-002",
                100.0, epoch(LocalDateTime.of(2026, 6, 1, 10, 0)),
                45.0, 15.0, "Zagreb", "Croatia"));

            ks.fireAllRules();
            List<String> reasons = getReasons(ks);
            assertTrue(reasons.stream().anyMatch(r -> r.contains("new location")),
                "Expected NEW_LOCATION for first city visit");
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("Transaction from different city than previous => NEW_LOCATION for both")
    void testNewLocationDifferentCity() {
        KieSession ks = createRealtimeSession();
        try {
            ks.insert(new CardTransactionEvent(1L, 1L, 100L,
                "ACC-001", "ACC-002",
                100.0, epoch(LocalDateTime.of(2026, 6, 1, 10, 0)),
                45.0, 15.0, "Zagreb", "Croatia"));
            ks.insert(new CardTransactionEvent(2L, 1L, 100L,
                "ACC-001", "ACC-002",
                200.0, epoch(LocalDateTime.of(2026, 6, 1, 11, 0)),
                48.0, 2.0, "Paris", "France"));

            ks.fireAllRules();
            List<String> reasons = getReasons(ks);
            assertEquals(2, reasons.stream().filter(r -> r.contains("new location")).count(),
                "Expected NEW_LOCATION for both cities");
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("Second transaction from same city => NEW_LOCATION not triggered")
    void testNewLocationSameCityNotTriggered() {
        KieSession ks = createRealtimeSession();
        try {
            ks.insert(new CardTransactionEvent(1L, 1L, 100L,
                "ACC-001", "ACC-002",
                100.0, epoch(LocalDateTime.of(2026, 6, 1, 10, 0)),
                45.0, 15.0, "Zagreb", "Croatia"));
            ks.insert(new CardTransactionEvent(2L, 1L, 100L,
                "ACC-001", "ACC-002",
                200.0, epoch(LocalDateTime.of(2026, 6, 1, 12, 0)),
                45.0, 15.0, "Zagreb", "Croatia"));

            ks.fireAllRules();
            List<String> reasons = getReasons(ks);
            assertEquals(1, reasons.stream().filter(r -> r.contains("new location")).count(),
                "Expected NEW_LOCATION only for first event in same city");
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("Same transaction evaluated twice => no duplicate NEW_LOCATION")
    void testNewLocationDuplicatePrevention() {
        KieSession ks = createRealtimeSession();
        try {
            ks.insert(new CardTransactionEvent(1L, 1L, 100L,
                "ACC-001", "ACC-002",
                100.0, epoch(LocalDateTime.of(2026, 6, 1, 10, 0)),
                45.0, 15.0, "Zagreb", "Croatia"));
            ks.fireAllRules();
            ks.fireAllRules();

            long newLocationCount = getReasons(ks).stream()
                .filter(r -> r.contains("new location")).count();
            assertEquals(1, newLocationCount,
                "Expected no duplicate NEW_LOCATION");
        } finally {
            ks.dispose();
        }
    }

    // ========================================================================
    // IMPOSSIBLE_TRAVEL — 2 transactions within 2h, distance > 500 km
    // ========================================================================

    @Test
    @DisplayName("Two transactions within 2h, 2000km apart => IMPOSSIBLE_TRAVEL triggered")
    void testImpossibleTravelTriggered() {
        KieSession ks = createRealtimeSession();
        try {
            long t = epoch(LocalDateTime.of(2026, 6, 1, 10, 0));
            ks.insert(new CardTransactionEvent(1L, 1L, 100L,
                "ACC-001", "ACC-002",
                100.0, t,
                51.5, -0.1, "London", "UK"));
            ks.insert(new CardTransactionEvent(2L, 1L, 100L,
                "ACC-001", "ACC-002",
                200.0, t + 3600000L,
                45.8, 16.0, "Zagreb", "Croatia"));

            ks.fireAllRules();
            List<String> reasons = getReasons(ks);
            assertTrue(reasons.stream().anyMatch(r -> r.contains("Impossible travel")),
                "Expected IMPOSSIBLE_TRAVEL for distant transactions");
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("Two transactions within 2h, 10km apart => IMPOSSIBLE_TRAVEL not triggered")
    void testImpossibleTravelCloseDistance() {
        KieSession ks = createRealtimeSession();
        try {
            long t = epoch(LocalDateTime.of(2026, 6, 1, 10, 0));
            ks.insert(new CardTransactionEvent(1L, 1L, 100L,
                "ACC-001", "ACC-002",
                100.0, t,
                45.85, 16.0, "Zagreb", "Croatia"));
            ks.insert(new CardTransactionEvent(2L, 1L, 100L,
                "ACC-001", "ACC-002",
                200.0, t + 3600000L,
                45.81, 15.95, "Zagreb", "Croatia"));

            ks.fireAllRules();
            List<String> reasons = getReasons(ks);
            assertFalse(reasons.stream().anyMatch(r -> r.contains("Impossible travel")),
                "Expected no IMPOSSIBLE_TRAVEL for close transactions");
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("Two transactions > 2h apart, 2000km apart => IMPOSSIBLE_TRAVEL not triggered")
    void testImpossibleTravelTooFarApart() {
        KieSession ks = createRealtimeSession();
        try {
            long t = epoch(LocalDateTime.of(2026, 6, 1, 8, 0));
            ks.insert(new CardTransactionEvent(1L, 1L, 100L,
                "ACC-001", "ACC-002",
                100.0, t,
                51.5, -0.1, "London", "UK"));
            ks.insert(new CardTransactionEvent(2L, 1L, 100L,
                "ACC-001", "ACC-002",
                200.0, t + 5 * 3600000L,
                45.8, 16.0, "Zagreb", "Croatia"));

            ks.fireAllRules();
            List<String> reasons = getReasons(ks);
            assertFalse(reasons.stream().anyMatch(r -> r.contains("Impossible travel")),
                "Expected no IMPOSSIBLE_TRAVEL for transactions >2h apart");
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("Same pair evaluated twice => no duplicate IMPOSSIBLE_TRAVEL")
    void testImpossibleTravelDuplicatePrevention() {
        KieSession ks = createRealtimeSession();
        try {
            long t = epoch(LocalDateTime.of(2026, 6, 1, 10, 0));
            ks.insert(new CardTransactionEvent(1L, 1L, 100L,
                "ACC-001", "ACC-002",
                100.0, t,
                51.5, -0.1, "London", "UK"));
            ks.insert(new CardTransactionEvent(2L, 1L, 100L,
                "ACC-001", "ACC-002",
                200.0, t + 3600000L,
                45.8, 16.0, "Zagreb", "Croatia"));

            ks.fireAllRules();
            long firstCount = getReasons(ks).stream()
                .filter(r -> r.contains("Impossible travel")).count();

            ks.fireAllRules();
            long secondCount = getReasons(ks).stream()
                .filter(r -> r.contains("Impossible travel")).count();

            assertEquals(1, firstCount, "Expected 1 IMPOSSIBLE_TRAVEL");
            assertEquals(1, secondCount, "Expected no duplicate");
        } finally {
            ks.dispose();
        }
    }

    // ========================================================================
    // MULTIPLE ACCOUNTS / CARDS — same client, different cards/accounts
    // ========================================================================

    @Test
    @DisplayName("Small transactions across different cards from same client still count together")
    void testManySmallTransactionsAcrossCards() {
        KieSessionConfiguration config = KieServices.Factory.get().newKieSessionConfiguration();
        config.setOption(ClockTypeOption.get("pseudo"));
        KieSession ks = fraudKieBase.newKieSession(config, null);
        try {
            SessionPseudoClock clock = ks.getSessionClock();
            long now = clock.getCurrentTime();

            for (int i = 0; i < 5; i++) {
                ks.insert(new CardTransactionEvent((long) i + 1, 1L, (long) 100 + i,
                    "ACC-00" + (i + 1), "ACC-100",
                    10.0, now,
                    45.0, 15.0, "Zagreb", "Croatia"));
                clock.advanceTime(1, TimeUnit.MINUTES);
                now = clock.getCurrentTime();
            }

            ks.fireAllRules();
            List<String> reasons = getReasons(ks);
            assertTrue(reasons.stream().anyMatch(r -> r.contains("Many small")),
                "Expected MANY_SMALL across different cards");
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("Impossible travel: different cards, different accounts, same client")
    void testImpossibleTravelDifferentCards() {
        KieSession ks = createRealtimeSession();
        try {
            long t = epoch(LocalDateTime.of(2026, 6, 1, 10, 0));
            ks.insert(new CardTransactionEvent(1L, 1L, 101L,
                "ACC-001", "ACC-002",
                100.0, t,
                51.5, -0.1, "London", "UK"));
            ks.insert(new CardTransactionEvent(2L, 1L, 102L,
                "ACC-003", "ACC-004",
                200.0, t + 3600000L,
                45.8, 16.0, "Zagreb", "Croatia"));

            ks.fireAllRules();
            List<String> reasons = getReasons(ks);
            assertTrue(reasons.stream().anyMatch(r -> r.contains("Impossible travel")),
                "Expected IMPOSSIBLE_TRAVEL across different cards");
        } finally {
            ks.dispose();
        }
    }

    // ========================================================================
    // NO SUSPICIOUS — scenario where no pattern matches
    // ========================================================================

    @Test
    @DisplayName("Regular transactions with no suspicious pattern => no pattern rule fires")
    void testNoSuspiciousTransactions() {
        KieSession ks = createRealtimeSession();
        try {
            // Insert a previous transaction to suppress NEW_LOCATION for client 1
            long t = epoch(LocalDateTime.of(2026, 6, 1, 10, 0));
            ks.insert(new CardTransactionEvent(0L, 1L, 100L,
                "ACC-001", "ACC-002",
                150.0, t,
                45.0, 15.0, "Zagreb", "Croatia"));
            ks.insert(new CardTransactionEvent(1L, 1L, 100L,
                "ACC-001", "ACC-002",
                150.0, t + 7200000L,
                45.0, 15.0, "Zagreb", "Croatia"));
            // Client 2: insert prior from Paris
            ks.insert(new CardTransactionEvent(2L, 2L, 200L,
                "ACC-010", "ACC-020",
                300.0, t,
                48.0, 2.0, "Paris", "France"));
            ks.insert(new CardTransactionEvent(3L, 2L, 200L,
                "ACC-010", "ACC-020",
                300.0, t + 7200000L,
                48.0, 2.0, "Paris", "France"));

            ks.fireAllRules();
            // Only NEW_LOCATION fires: event 0 (Zagreb first) and event 2 (Paris first)
            // But those are seed events with transactionId 0 and 2.
            List<String> reasons = getReasons(ks);
            long newLocCount = reasons.stream().filter(r -> r.contains("new location")).count();
            assertEquals(2, newLocCount, "Expected NEW_LOCATION for seed events only");
            assertFalse(reasons.stream().anyMatch(r -> r.contains("Many small")), "No MANY_SMALL");
            assertFalse(reasons.stream().anyMatch(r -> r.contains("Large night")), "No LARGE_NIGHT");
            assertFalse(reasons.stream().anyMatch(r -> r.contains("5x the average")), "No UNUSUAL_AMOUNT");
            assertFalse(reasons.stream().anyMatch(r -> r.contains("Impossible travel")), "No IMPOSSIBLE_TRAVEL");
        } finally {
            ks.dispose();
        }
    }
}
