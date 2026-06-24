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
import org.kie.api.runtime.rule.EntryPoint;
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

    private EntryPoint ep(KieSession ks) {
        return ks.getEntryPoint("card-transactions");
    }

    private List<SuspiciousTransactionFact> getSuspiciousList(KieSession ks) {
        Collection<?> facts = ks.getObjects(o -> o instanceof SuspiciousTransactionFact);
        List<SuspiciousTransactionFact> result = new ArrayList<>();
        for (Object f : facts) {
            result.add((SuspiciousTransactionFact) f);
        }
        return result;
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
                ep(ks).insert(new CardTransactionEvent((long) i + 1, 1L, 100L,
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
                ep(ks).insert(new CardTransactionEvent((long) i + 1, 1L, 100L,
                    "ACC-001", "ACC-002",
                    15.0, now,
                    45.0, 15.0, "Zagreb", "Croatia"));
                clock.advanceTime(1, TimeUnit.MINUTES);
                now = clock.getCurrentTime();
            }

            ks.fireAllRules();
            assertFalse(getReasons(ks).stream().anyMatch(r -> r.contains("Many small")),
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
                ep(ks).insert(new CardTransactionEvent((long) i + 1, 1L, 100L,
                    "ACC-001", "ACC-002",
                    10.0, now,
                    45.0, 15.0, "Zagreb", "Croatia"));
                clock.advanceTime(1, TimeUnit.MINUTES);
                now = clock.getCurrentTime();
            }

            clock.advanceTime(15, TimeUnit.MINUTES);
            now = clock.getCurrentTime();

            ep(ks).insert(new CardTransactionEvent(5L, 1L, 100L,
                "ACC-001", "ACC-002",
                10.0, now,
                45.0, 15.0, "Zagreb", "Croatia"));

            ks.fireAllRules();
            assertFalse(getReasons(ks).stream().anyMatch(r -> r.contains("Many small")),
                "Expected no MANY_SMALL when events outside 10-minute window");
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("Large transactions (>20) do not contribute to MANY_SMALL count")
    void testManySmallOnlyCountsSmallTransactions() {
        KieSessionConfiguration config = KieServices.Factory.get().newKieSessionConfiguration();
        config.setOption(ClockTypeOption.get("pseudo"));
        KieSession ks = fraudKieBase.newKieSession(config, null);
        try {
            SessionPseudoClock clock = ks.getSessionClock();
            long now = clock.getCurrentTime();

            for (int i = 0; i < 3; i++) {
                ep(ks).insert(new CardTransactionEvent((long) i + 1, 1L, 100L,
                    "ACC-001", "ACC-002",
                    10.0, now,
                    45.0, 15.0, "Zagreb", "Croatia"));
                clock.advanceTime(1, TimeUnit.MINUTES);
                now = clock.getCurrentTime();
            }
            // 2 large transactions (>=20) should not count
            ep(ks).insert(new CardTransactionEvent(4L, 1L, 100L,
                "ACC-001", "ACC-002",
                100.0, now,
                45.0, 15.0, "Zagreb", "Croatia"));
            clock.advanceTime(1, TimeUnit.MINUTES);
            now = clock.getCurrentTime();
            ep(ks).insert(new CardTransactionEvent(5L, 1L, 100L,
                "ACC-001", "ACC-002",
                200.0, now,
                45.0, 15.0, "Zagreb", "Croatia"));

            ks.fireAllRules();
            assertFalse(getReasons(ks).stream().anyMatch(r -> r.contains("Many small")),
                "Expected no MANY_SMALL when large transactions don't count");
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
            ep(ks).insert(new CardTransactionEvent(1L, 1L, 100L,
                "ACC-001", "ACC-002",
                6000.0, epoch(LocalDateTime.of(2026, 6, 15, 3, 0)),
                45.0, 15.0, "Zagreb", "Croatia"));
            ks.fireAllRules();
            assertTrue(getReasons(ks).stream().anyMatch(r -> r.contains("Large night")),
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
            ep(ks).insert(new CardTransactionEvent(1L, 1L, 100L,
                "ACC-001", "ACC-002",
                6000.0, epoch(LocalDateTime.of(2026, 6, 15, 15, 0)),
                45.0, 15.0, "Zagreb", "Croatia"));
            ks.fireAllRules();
            assertFalse(getReasons(ks).stream().anyMatch(r -> r.contains("Large night")),
                "Expected no LARGE_NIGHT for daytime");
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("5000.01 EUR at 04:59 => LARGE_NIGHT triggered (boundary)")
    void testLargeNightTransactionBoundary() {
        KieSession ks = createRealtimeSession();
        try {
            ep(ks).insert(new CardTransactionEvent(1L, 1L, 100L,
                "ACC-001", "ACC-002",
                5000.01, epoch(LocalDateTime.of(2026, 6, 15, 4, 59)),
                45.0, 15.0, "Zagreb", "Croatia"));
            ks.fireAllRules();
            assertTrue(getReasons(ks).stream().anyMatch(r -> r.contains("Large night")),
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
            ep(ks).insert(new CardTransactionEvent(1L, 1L, 100L,
                "ACC-001", "ACC-002",
                6000.0, epoch(LocalDateTime.of(2026, 6, 15, 5, 0)),
                45.0, 15.0, "Zagreb", "Croatia"));
            ks.fireAllRules();
            assertFalse(getReasons(ks).stream().anyMatch(r -> r.contains("Large night")),
                "Expected no LARGE_NIGHT at 05:00");
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("5000.00 EUR at 03:00 => LARGE_NIGHT not triggered (exact threshold)")
    void testLargeNightTransactionExactThreshold() {
        KieSession ks = createRealtimeSession();
        try {
            ep(ks).insert(new CardTransactionEvent(1L, 1L, 100L,
                "ACC-001", "ACC-002",
                5000.0, epoch(LocalDateTime.of(2026, 6, 15, 3, 0)),
                45.0, 15.0, "Zagreb", "Croatia"));
            ks.fireAllRules();
            assertFalse(getReasons(ks).stream().anyMatch(r -> r.contains("Large night")),
                "Expected no LARGE_NIGHT for exactly 5000.00");
        } finally {
            ks.dispose();
        }
    }

    // ========================================================================
    // UNUSUAL_TRANSACTION_AMOUNT — amount >= 5x average, min 5 history events
    // ========================================================================

    @Test
    @DisplayName("30 normal transactions then 1 large (6x avg) => UNUSUAL_AMOUNT triggered")
    void testUnusualAmountTriggered() {
        KieSession ks = createRealtimeSession();
        try {
            long baseTime = epoch(LocalDateTime.of(2026, 6, 1, 10, 0));
            for (int i = 0; i < 30; i++) {
                ep(ks).insert(new CardTransactionEvent((long) i + 1, 1L, 100L,
                    "ACC-001", "ACC-002",
                    100.0, baseTime + i * 60000L,
                    45.0, 15.0, "Zagreb", "Croatia"));
            }

            ep(ks).insert(new CardTransactionEvent(31L, 1L, 100L,
                "ACC-001", "ACC-002",
                600.0, baseTime + 30 * 60000L,
                45.0, 15.0, "Zagreb", "Croatia"));

            ks.fireAllRules();
            assertTrue(getReasons(ks).stream().anyMatch(r -> r.contains("5x the average")),
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
                ep(ks).insert(new CardTransactionEvent((long) i + 1, 1L, 100L,
                    "ACC-001", "ACC-002",
                    100.0, baseTime + i * 60000L,
                    45.0, 15.0, "Zagreb", "Croatia"));
            }

            ep(ks).insert(new CardTransactionEvent(31L, 1L, 100L,
                "ACC-001", "ACC-002",
                400.0, baseTime + 30 * 60000L,
                45.0, 15.0, "Zagreb", "Croatia"));

            ks.fireAllRules();
            assertFalse(getReasons(ks).stream().anyMatch(r -> r.contains("5x the average")),
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
                ep(ks).insert(new CardTransactionEvent((long) i + 1, 1L, 100L,
                    "ACC-001", "ACC-002",
                    100.0, baseTime + i * 60000L,
                    45.0, 15.0, "Zagreb", "Croatia"));
            }

            ep(ks).insert(new CardTransactionEvent(31L, 1L, 100L,
                "ACC-001", "ACC-002",
                500.0, baseTime + 30 * 60000L,
                45.0, 15.0, "Zagreb", "Croatia"));

            ks.fireAllRules();
            assertTrue(getReasons(ks).stream().anyMatch(r -> r.contains("5x the average")),
                "Expected UNUSUAL_AMOUNT for exactly 5x avg");
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("Only 4 historical transactions + large current => UNUSUAL_AMOUNT NOT triggered (min 5 required)")
    void testUnusualAmountFewTransactions() {
        KieSession ks = createRealtimeSession();
        try {
            long baseTime = epoch(LocalDateTime.of(2026, 6, 1, 10, 0));
            // only 4 history events (below the min 5 threshold)
            for (int i = 0; i < 4; i++) {
                ep(ks).insert(new CardTransactionEvent((long) i + 1, 1L, 100L,
                    "ACC-001", "ACC-002",
                    50.0, baseTime + i * 60000L,
                    45.0, 15.0, "Zagreb", "Croatia"));
            }

            ep(ks).insert(new CardTransactionEvent(5L, 1L, 100L,
                "ACC-001", "ACC-002",
                1000.0, baseTime + 4 * 60000L,
                45.0, 15.0, "Zagreb", "Croatia"));

            ks.fireAllRules();
            assertFalse(getReasons(ks).stream().anyMatch(r -> r.contains("5x the average")),
                "Expected no UNUSUAL_AMOUNT with only 4 history events (min 5 required)");
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("Exactly 5 historical events + large current => UNUSUAL_AMOUNT triggered (boundary)")
    void testUnusualAmountExactlyFiveHistory() {
        KieSession ks = createRealtimeSession();
        try {
            long baseTime = epoch(LocalDateTime.of(2026, 6, 1, 10, 0));
            for (int i = 0; i < 5; i++) {
                ep(ks).insert(new CardTransactionEvent((long) i + 1, 1L, 100L,
                    "ACC-001", "ACC-002",
                    50.0, baseTime + i * 60000L,
                    45.0, 15.0, "Zagreb", "Croatia"));
            }

            ep(ks).insert(new CardTransactionEvent(6L, 1L, 100L,
                "ACC-001", "ACC-002",
                500.0, baseTime + 5 * 60000L,
                45.0, 15.0, "Zagreb", "Croatia"));

            ks.fireAllRules();
            assertTrue(getReasons(ks).stream().anyMatch(r -> r.contains("5x the average")),
                "Expected UNUSUAL_AMOUNT with exactly 5 history events (10x avg)");
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("First transaction (no history) => not flagged by UNUSUAL_AMOUNT")
    void testUnusualAmountFirstTransaction() {
        KieSession ks = createRealtimeSession();
        try {
            ep(ks).insert(new CardTransactionEvent(1L, 1L, 100L,
                "ACC-001", "ACC-002",
                10000.0, epoch(LocalDateTime.of(2026, 6, 1, 10, 0)),
                45.0, 15.0, "Zagreb", "Croatia"));

            ks.fireAllRules();
            assertFalse(getReasons(ks).stream().anyMatch(r -> r.contains("5x the average")),
                "Expected no UNUSUAL_AMOUNT for first transaction");
        } finally {
            ks.dispose();
        }
    }

    // ========================================================================
    // NEW_LOCATION — GPS-based: no prior transaction within 50km in 90d
    // ========================================================================

    @Test
    @DisplayName("First transaction ever => NOT flagged as NEW_LOCATION (requires prior transaction)")
    void testNewLocationFirstTransaction() {
        KieSession ks = createRealtimeSession();
        try {
            ep(ks).insert(new CardTransactionEvent(1L, 1L, 100L,
                "ACC-001", "ACC-002",
                100.0, epoch(LocalDateTime.of(2026, 6, 1, 10, 0)),
                45.0, 15.0, "Zagreb", "Croatia"));

            ks.fireAllRules();
            List<String> reasons = getReasons(ks);
            assertFalse(reasons.stream().anyMatch(r -> r.contains("new location")),
                "Expected NO NEW_LOCATION for first-ever transaction");
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("Two transactions at same GPS location => neither flagged")
    void testNewLocationSameLocationNotTriggered() {
        KieSession ks = createRealtimeSession();
        try {
            long t = epoch(LocalDateTime.of(2026, 6, 1, 10, 0));
            ep(ks).insert(new CardTransactionEvent(1L, 1L, 100L,
                "ACC-001", "ACC-002",
                100.0, t,
                45.0, 15.0, "Zagreb", "Croatia"));
            ep(ks).insert(new CardTransactionEvent(2L, 1L, 100L,
                "ACC-001", "ACC-002",
                200.0, t + 3600000L,
                45.0, 15.0, "Zagreb", "Croatia"));

            ks.fireAllRules();
            List<String> reasons = getReasons(ks);
            long newLocCount = reasons.stream().filter(r -> r.contains("new location")).count();
            assertEquals(0, newLocCount,
                "Expected NO NEW_LOCATION: first is first transaction, second is within 50km of first");
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("Transaction within 50km of prior location => NOT flagged")
    void testNewLocationWithin50km() {
        KieSession ks = createRealtimeSession();
        try {
            long t = epoch(LocalDateTime.of(2026, 6, 1, 10, 0));
            // Zagreb center ~45.815, 15.982
            ep(ks).insert(new CardTransactionEvent(1L, 1L, 100L,
                "ACC-001", "ACC-002",
                100.0, t,
                45.815, 15.982, "Zagreb", "Croatia"));
            // ~30km away (Samobor area) -> within 50km
            ep(ks).insert(new CardTransactionEvent(2L, 1L, 100L,
                "ACC-001", "ACC-002",
                200.0, t + 3600000L,
                45.803, 15.710, "Samobor", "Croatia"));

            ks.fireAllRules();
            List<String> reasons = getReasons(ks);
            long newLocCount = reasons.stream().filter(r -> r.contains("new location")).count();
            assertEquals(0, newLocCount,
                "Expected NO NEW_LOCATION: first is first tx, second is within 50km");
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("Second transaction >50km from first => NEW_LOCATION flagged")
    void testNewLocationFarAway() {
        KieSession ks = createRealtimeSession();
        try {
            long t = epoch(LocalDateTime.of(2026, 6, 1, 10, 0));
            // Zagreb
            ep(ks).insert(new CardTransactionEvent(1L, 1L, 100L,
                "ACC-001", "ACC-002",
                100.0, t,
                45.815, 15.982, "Zagreb", "Croatia"));
            // Paris (~1000km away)
            ep(ks).insert(new CardTransactionEvent(2L, 1L, 100L,
                "ACC-001", "ACC-002",
                200.0, t + 3600000L,
                48.857, 2.352, "Paris", "France"));

            ks.fireAllRules();
            List<String> reasons = getReasons(ks);
            long newLocCount = reasons.stream().filter(r -> r.contains("new location")).count();
            assertEquals(1, newLocCount,
                "Expected NEW_LOCATION only for second transaction (far from first)");
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("Transaction >90d since last visit to same area => NEW_LOCATION flagged again")
    void testNewLocationAfter90Days() {
        KieSession ks = createRealtimeSession();
        try {
            long t = epoch(LocalDateTime.of(2026, 1, 1, 10, 0));
            ep(ks).insert(new CardTransactionEvent(1L, 1L, 100L,
                "ACC-001", "ACC-002",
                100.0, t,
                45.815, 15.982, "Zagreb", "Croatia"));

            // 100 days later, same location (outside 90d window)
            ep(ks).insert(new CardTransactionEvent(2L, 1L, 100L,
                "ACC-001", "ACC-002",
                200.0, t + 100L * 24 * 3600000L,
                45.815, 15.982, "Zagreb", "Croatia"));

            ks.fireAllRules();
            List<String> reasons = getReasons(ks);
            long newLocCount = reasons.stream().filter(r -> r.contains("new location")).count();
            assertEquals(1, newLocCount,
                "Expected NEW_LOCATION only for second transaction (first is >90d old)");
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("Different clients: NEW_LOCATION computed independently per client")
    void testNewLocationPerClient() {
        KieSession ks = createRealtimeSession();
        try {
            long t = epoch(LocalDateTime.of(2026, 6, 1, 10, 0));
            // Client 1: transaction in Zagreb (first tx, not flagged)
            ep(ks).insert(new CardTransactionEvent(1L, 1L, 100L,
                "ACC-001", "ACC-002",
                100.0, t,
                45.815, 15.982, "Zagreb", "Croatia"));
            // Client 1: second transaction near Zagreb (within 50km, not flagged)
            ep(ks).insert(new CardTransactionEvent(2L, 1L, 100L,
                "ACC-001", "ACC-002",
                150.0, t + 3600000L,
                45.900, 16.100, "Zagreb", "Croatia"));
            // Client 2: first transaction in Paris (first tx, not flagged)
            ep(ks).insert(new CardTransactionEvent(3L, 2L, 200L,
                "ACC-010", "ACC-020",
                200.0, t,
                48.857, 2.352, "Paris", "France"));
            // Client 2: second transaction near Paris (within 50km, not flagged)
            ep(ks).insert(new CardTransactionEvent(4L, 2L, 200L,
                "ACC-010", "ACC-020",
                250.0, t + 3600000L,
                48.900, 2.400, "Paris", "France"));
            // Client 1: third transaction in Lima, Peru (>50km from Zagreb, flagged!)
            ep(ks).insert(new CardTransactionEvent(5L, 1L, 100L,
                "ACC-001", "ACC-002",
                300.0, t + 7200000L,
                -12.046, -77.043, "Lima", "Peru"));

            ks.fireAllRules();
            List<String> reasons = getReasons(ks);
            long newLocCount = reasons.stream().filter(r -> r.contains("new location")).count();
            assertEquals(1, newLocCount,
                "Expected NEW_LOCATION only for Client 1 third transaction (Lima, far from prior)");
        } finally {
            ks.dispose();
        }
    }

    // ========================================================================
    // IMPOSSIBLE_TRAVEL — speed > 1000 km/h, min distance 100km, within 2h
    // ========================================================================

    @Test
    @DisplayName("Two transactions within 2h, 2000km apart => IMPOSSIBLE_TRAVEL triggered")
    void testImpossibleTravelTriggered() {
        KieSession ks = createRealtimeSession();
        try {
            long t = epoch(LocalDateTime.of(2026, 6, 1, 10, 0));
            ep(ks).insert(new CardTransactionEvent(1L, 1L, 100L,
                "ACC-001", "ACC-002",
                100.0, t,
                51.5, -0.1, "London", "UK"));
            // 1 hour later, Zagreb (~1300km) => speed ~1300 km/h > 1000
            ep(ks).insert(new CardTransactionEvent(2L, 1L, 100L,
                "ACC-001", "ACC-002",
                200.0, t + 3600000L,
                45.8, 16.0, "Zagreb", "Croatia"));

            ks.fireAllRules();
            List<String> reasons = getReasons(ks);
            assertTrue(reasons.stream().anyMatch(r -> r.contains("Impossible travel")),
                "Expected IMPOSSIBLE_TRAVEL for high-speed travel");
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("Two transactions within 2h, 10km apart => no IMPOSSIBLE_TRAVEL (min distance 100km)")
    void testImpossibleTravelCloseDistance() {
        KieSession ks = createRealtimeSession();
        try {
            long t = epoch(LocalDateTime.of(2026, 6, 1, 10, 0));
            ep(ks).insert(new CardTransactionEvent(1L, 1L, 100L,
                "ACC-001", "ACC-002",
                100.0, t,
                45.85, 16.0, "Zagreb", "Croatia"));
            ep(ks).insert(new CardTransactionEvent(2L, 1L, 100L,
                "ACC-001", "ACC-002",
                200.0, t + 3600000L,
                45.81, 15.95, "Zagreb", "Croatia"));

            ks.fireAllRules();
            assertFalse(getReasons(ks).stream().anyMatch(r -> r.contains("Impossible travel")),
                "Expected no IMPOSSIBLE_TRAVEL for close transactions (< 100km)");
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("Two transactions >2h apart, 2000km apart => no IMPOSSIBLE_TRAVEL")
    void testImpossibleTravelTooFarApart() {
        KieSession ks = createRealtimeSession();
        try {
            long t = epoch(LocalDateTime.of(2026, 6, 1, 8, 0));
            ep(ks).insert(new CardTransactionEvent(1L, 1L, 100L,
                "ACC-001", "ACC-002",
                100.0, t,
                51.5, -0.1, "London", "UK"));
            // 5 hours later => speed = 1300/5 = 260 km/h < 1000, OK
            ep(ks).insert(new CardTransactionEvent(2L, 1L, 100L,
                "ACC-001", "ACC-002",
                200.0, t + 5 * 3600000L,
                45.8, 16.0, "Zagreb", "Croatia"));

            ks.fireAllRules();
            assertFalse(getReasons(ks).stream().anyMatch(r -> r.contains("Impossible travel")),
                "Expected no IMPOSSIBLE_TRAVEL for >2h apart transactions");
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("Two transactions within 2h, 500km apart (speed ~500 km/h) => no IMPOSSIBLE_TRAVEL")
    void testImpossibleTravelBelowSpeedThreshold() {
        KieSession ks = createRealtimeSession();
        try {
            long t = epoch(LocalDateTime.of(2026, 6, 1, 10, 0));
            // Frankfurt (50.1, 8.7) to Munich (48.1, 11.6) ~300km
            ep(ks).insert(new CardTransactionEvent(1L, 1L, 100L,
                "ACC-001", "ACC-002",
                100.0, t,
                50.1, 8.7, "Frankfurt", "Germany"));
            // 1 hour later -> speed ~300 km/h (below 1000)
            ep(ks).insert(new CardTransactionEvent(2L, 1L, 100L,
                "ACC-001", "ACC-002",
                200.0, t + 3600000L,
                48.1, 11.6, "Munich", "Germany"));

            ks.fireAllRules();
            assertFalse(getReasons(ks).stream().anyMatch(r -> r.contains("Impossible travel")),
                "Expected no IMPOSSIBLE_TRAVEL for sub-1000 km/h speed");
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("Two transactions at same timestamp => no exception and no IMPOSSIBLE_TRAVEL")
    void testImpossibleTravelSameTimestamp() {
        KieSession ks = createRealtimeSession();
        try {
            long t = epoch(LocalDateTime.of(2026, 6, 1, 10, 0));
            ep(ks).insert(new CardTransactionEvent(1L, 1L, 100L,
                "ACC-001", "ACC-002",
                100.0, t,
                51.5, -0.1, "London", "UK"));
            // Same timestamp - should not cause division by zero
            ep(ks).insert(new CardTransactionEvent(2L, 1L, 100L,
                "ACC-001", "ACC-002",
                200.0, t,
                45.8, 16.0, "Zagreb", "Croatia"));

            // Must not throw ArithmeticException
            assertDoesNotThrow(() -> ks.fireAllRules(),
                "Should not throw exception when two events have same timestamp");
            List<String> reasons = getReasons(ks);
            assertFalse(reasons.stream().anyMatch(r -> r.contains("Impossible travel")),
                "Expected no IMPOSSIBLE_TRAVEL for identical timestamps");
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("No duplicate IMPOSSIBLE_TRAVEL for same pair on re-fire")
    void testImpossibleTravelDuplicatePrevention() {
        KieSession ks = createRealtimeSession();
        try {
            long t = epoch(LocalDateTime.of(2026, 6, 1, 10, 0));
            ep(ks).insert(new CardTransactionEvent(1L, 1L, 100L,
                "ACC-001", "ACC-002",
                100.0, t,
                51.5, -0.1, "London", "UK"));
            ep(ks).insert(new CardTransactionEvent(2L, 1L, 100L,
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
            assertEquals(1, secondCount, "Expected no duplicate on re-fire");
        } finally {
            ks.dispose();
        }
    }

    // ========================================================================
    // MULTI-ACCOUNT / MULTI-CARD — same client, different cards/accounts
    // ========================================================================

    @Test
    @DisplayName("Small transactions across different cards of same client => MANY_SMALL triggered")
    void testManySmallTransactionsAcrossCards() {
        KieSessionConfiguration config = KieServices.Factory.get().newKieSessionConfiguration();
        config.setOption(ClockTypeOption.get("pseudo"));
        KieSession ks = fraudKieBase.newKieSession(config, null);
        try {
            SessionPseudoClock clock = ks.getSessionClock();
            long now = clock.getCurrentTime();

            for (int i = 0; i < 5; i++) {
                ep(ks).insert(new CardTransactionEvent((long) i + 1, 1L, (long) 100 + i,
                    "ACC-00" + (i + 1), "ACC-100",
                    10.0, now,
                    45.0, 15.0, "Zagreb", "Croatia"));
                clock.advanceTime(1, TimeUnit.MINUTES);
                now = clock.getCurrentTime();
            }

            ks.fireAllRules();
            assertTrue(getReasons(ks).stream().anyMatch(r -> r.contains("Many small")),
                "Expected MANY_SMALL across different cards of same client");
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("Impossible travel across different cards/accounts of same client")
    void testImpossibleTravelDifferentCards() {
        KieSession ks = createRealtimeSession();
        try {
            long t = epoch(LocalDateTime.of(2026, 6, 1, 10, 0));
            ep(ks).insert(new CardTransactionEvent(1L, 1L, 101L,
                "ACC-001", "ACC-002",
                100.0, t,
                51.5, -0.1, "London", "UK"));
            ep(ks).insert(new CardTransactionEvent(2L, 1L, 102L,
                "ACC-003", "ACC-004",
                200.0, t + 3600000L,
                45.8, 16.0, "Zagreb", "Croatia"));

            ks.fireAllRules();
            assertTrue(getReasons(ks).stream().anyMatch(r -> r.contains("Impossible travel")),
                "Expected IMPOSSIBLE_TRAVEL across different cards of same client");
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("Different client ID => no cross-client contamination")
    void testDifferentClientsIndependent() {
        KieSession ks = createRealtimeSession();
        try {
            long t = epoch(LocalDateTime.of(2026, 6, 1, 10, 0));
            // Client 1: London -> Zagreb (impossible travel)
            ep(ks).insert(new CardTransactionEvent(1L, 1L, 100L,
                "ACC-001", "ACC-002",
                100.0, t,
                51.5, -0.1, "London", "UK"));
            ep(ks).insert(new CardTransactionEvent(2L, 1L, 100L,
                "ACC-001", "ACC-002",
                200.0, t + 3600000L,
                45.8, 16.0, "Zagreb", "Croatia"));

            // Client 2: same timestamps, same locations -> should also be flagged independently
            ep(ks).insert(new CardTransactionEvent(3L, 2L, 200L,
                "ACC-010", "ACC-020",
                150.0, t,
                51.5, -0.1, "London", "UK"));
            ep(ks).insert(new CardTransactionEvent(4L, 2L, 200L,
                "ACC-010", "ACC-020",
                250.0, t + 3600000L,
                45.8, 16.0, "Zagreb", "Croatia"));

            ks.fireAllRules();
            List<String> reasons = getReasons(ks);
            long impossibleCount = reasons.stream().filter(r -> r.contains("Impossible travel")).count();
            assertEquals(2, impossibleCount,
                "Expected IMPOSSIBLE_TRAVEL for BOTH clients (independent)");
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
            long t = epoch(LocalDateTime.of(2026, 6, 1, 10, 0));
            // Client 1: first transaction (not a new location)
            ep(ks).insert(new CardTransactionEvent(0L, 1L, 100L,
                "ACC-001", "ACC-002",
                150.0, t,
                45.815, 15.982, "Zagreb", "Croatia"));
            // Client 1: second event within 50km (not a new location)
            ep(ks).insert(new CardTransactionEvent(1L, 1L, 100L,
                "ACC-001", "ACC-002",
                150.0, t + 7200000L,
                45.820, 15.990, "Zagreb", "Croatia"));

            // Client 2: first transaction (not a new location)
            ep(ks).insert(new CardTransactionEvent(2L, 2L, 200L,
                "ACC-010", "ACC-020",
                300.0, t,
                48.857, 2.352, "Paris", "France"));
            // Client 2: second event within 50km (not a new location)
            ep(ks).insert(new CardTransactionEvent(3L, 2L, 200L,
                "ACC-010", "ACC-020",
                300.0, t + 7200000L,
                48.860, 2.355, "Paris", "France"));

            ks.fireAllRules();
            List<String> reasons = getReasons(ks);
            assertEquals(0, reasons.stream().filter(r -> r.contains("new location")).count(),
                "Expected no NEW_LOCATION (all are first tx or within 50km of first)");
            assertFalse(reasons.stream().anyMatch(r -> r.contains("Many small")), "No MANY_SMALL");
            assertFalse(reasons.stream().anyMatch(r -> r.contains("Large night")), "No LARGE_NIGHT");
            assertFalse(reasons.stream().anyMatch(r -> r.contains("5x the average")), "No UNUSUAL_AMOUNT");
            assertFalse(reasons.stream().anyMatch(r -> r.contains("Impossible travel")), "No IMPOSSIBLE_TRAVEL");
        } finally {
            ks.dispose();
        }
    }

    // ========================================================================
    // COMBINATION — multiple fraud patterns triggered for same transaction
    // ========================================================================

    @Test
    @DisplayName("Transaction can trigger multiple patterns simultaneously")
    void testMultiplePatternsSimultaneously() {
        KieSession ks = createRealtimeSession();
        try {
            long t = epoch(LocalDateTime.of(2026, 6, 1, 10, 0));
            // Insert 5 small transactions first (all same Zagreb location)
            for (int i = 0; i < 5; i++) {
                ep(ks).insert(new CardTransactionEvent((long) i + 1, 1L, 100L,
                    "ACC-001", "ACC-002",
                    10.0, t + i * 60000L,
                    45.815, 15.982, "Zagreb", "Croatia"));
            }

            // 6th transaction: 6000 EUR at midnight from new location (Paris)
            // This should trigger: LARGE_NIGHT (6000 EUR at 3am), NEW_LOCATION (has prior tx in Zagreb, Paris is far)
            long nightTime = epoch(LocalDateTime.of(2026, 6, 2, 3, 0));
            ep(ks).insert(new CardTransactionEvent(6L, 1L, 100L,
                "ACC-001", "ACC-002",
                6000.0, nightTime,
                48.857, 2.352, "Paris", "France"));

            ks.fireAllRules();
            List<String> reasons = getReasons(ks);
            assertTrue(reasons.stream().anyMatch(r -> r.contains("Large night")), "Expected LARGE_NIGHT");
            assertTrue(reasons.stream().anyMatch(r -> r.contains("new location")), "Expected NEW_LOCATION");
            // For this specific transaction (id=6)
            long tx6patterns = reasons.stream()
                .filter(r -> !r.contains("Many small"))
                .count();
            assertTrue(tx6patterns >= 2, "Expected transaction 6 to trigger at least 2 patterns");
        } finally {
            ks.dispose();
        }
    }

    // ========================================================================
    // @expires verification — events beyond 90d are ignored
    // ========================================================================

    @Test
    @DisplayName("Events older than 90d do not prevent NEW_LOCATION for re-visited location")
    void testExpiresOldEvents() {
        KieSessionConfiguration config = KieServices.Factory.get().newKieSessionConfiguration();
        config.setOption(ClockTypeOption.get("pseudo"));
        KieSession ks = fraudKieBase.newKieSession(config, null);
        try {
            SessionPseudoClock clock = ks.getSessionClock();
            long now = clock.getCurrentTime();

            // Insert event at time 0
            ep(ks).insert(new CardTransactionEvent(1L, 1L, 100L,
                "ACC-001", "ACC-002",
                100.0, now,
                45.815, 15.982, "Zagreb", "Croatia"));

            // Fire rules so event 1 gets evaluated before it expires
            ks.fireAllRules();
            List<String> reasonsBefore = getReasons(ks);
            assertFalse(reasonsBefore.stream().anyMatch(r -> r.contains("new location")),
                "Expected no NEW_LOCATION for first event");

            // Advance clock by 100 days (exceeds @expires(90d))
            clock.advanceTime(100, TimeUnit.DAYS);
            now = clock.getCurrentTime();

            // Insert new event at same location
            ep(ks).insert(new CardTransactionEvent(2L, 1L, 100L,
                "ACC-001", "ACC-002",
                200.0, now,
                45.815, 15.982, "Zagreb", "Croatia"));

            ks.fireAllRules();
            List<String> reasons = getReasons(ks);
            long newLocCount = reasons.stream().filter(r -> r.contains("new location")).count();
            assertEquals(1, newLocCount,
                "Expected NEW_LOCATION for second event (first has expired)");
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("MANY_SMALL does not count events beyond the 10-minute window")
    void testManySmallWindowIsTimeBased() {
        KieSessionConfiguration config = KieServices.Factory.get().newKieSessionConfiguration();
        config.setOption(ClockTypeOption.get("pseudo"));
        KieSession ks = fraudKieBase.newKieSession(config, null);
        try {
            SessionPseudoClock clock = ks.getSessionClock();
            long now = clock.getCurrentTime();

            // 3 small events
            for (int i = 0; i < 3; i++) {
                ep(ks).insert(new CardTransactionEvent((long) i + 1, 1L, 100L,
                    "ACC-001", "ACC-002",
                    10.0, now,
                    45.0, 15.0, "Zagreb", "Croatia"));
                clock.advanceTime(8, TimeUnit.MINUTES); // 24 minutes total for 3 events
                now = clock.getCurrentTime();
            }

            // After 24 minutes, the earliest events are beyond the 10min window
            // But the rule requires 4+ events in a 10 minute window, so with only 3 events spread over 24 minutes,
            // the 3rd event should not see 4 prior events in its window
            ks.fireAllRules();
            assertFalse(getReasons(ks).stream().anyMatch(r -> r.contains("Many small")),
                "Expected no MANY_SMALL when events are spread beyond the 10min window");
        } finally {
            ks.dispose();
        }
    }

    // ========================================================================
    // DUPLICATE PREVENTION — ensure each rule prevents re-insertion
    // ========================================================================

    @Test
    @DisplayName("No duplicate SuspiciousTransactionFact for any rule on re-fire")
    void testNoDuplicateOnReFire() {
        KieSession ks = createRealtimeSession();
        try {
            long t = epoch(LocalDateTime.of(2026, 6, 1, 10, 0));
            ep(ks).insert(new CardTransactionEvent(1L, 1L, 100L,
                "ACC-001", "ACC-002",
                100.0, t,
                45.0, 15.0, "Zagreb", "Croatia"));

            ks.fireAllRules();
            int before = getSuspiciousList(ks).size();
            ks.fireAllRules();
            int after = getSuspiciousList(ks).size();

            assertEquals(before, after, "Expected no new SuspiciousTransactionFacts on re-fire");
        } finally {
            ks.dispose();
        }
    }

    // ========================================================================
    // CEP SESSION — events accumulate in session, no DB reload needed
    // ========================================================================

    @Test
    @DisplayName("Events stay in session between fireAllRules calls")
    void testEventsPersistInSession() {
        KieSession ks = createRealtimeSession();
        try {
            long t = epoch(LocalDateTime.of(2026, 6, 1, 10, 0));

            // Insert first event and fire
            ep(ks).insert(new CardTransactionEvent(1L, 1L, 100L,
                "ACC-001", "ACC-002",
                100.0, t,
                45.815, 15.982, "Zagreb", "Croatia"));
            ks.fireAllRules();

            // Insert second event at new location (should see first event as history)
            ep(ks).insert(new CardTransactionEvent(2L, 1L, 100L,
                "ACC-001", "ACC-002",
                200.0, t + 3600000L,
                48.857, 2.352, "Paris", "France"));
            ks.fireAllRules();

            List<String> reasons = getReasons(ks);
            // First tx: no prior → no NEW_LOCATION
            // Second tx: has prior (tx1) but far away → NEW_LOCATION
            long newLocCount = reasons.stream().filter(r -> r.contains("new location")).count();
            assertEquals(1, newLocCount,
                "Expected NEW_LOCATION for second tx (sees first tx in session without DB reload)");
        } finally {
            ks.dispose();
        }
    }

    @Test
    @DisplayName("Session retains events across multiple rule firings")
    void testSessionRetainsEvents() {
        KieSession ks = createRealtimeSession();
        try {
            long t = epoch(LocalDateTime.of(2026, 6, 1, 10, 0));

            // Insert 4 small events, fire after each
            for (int i = 0; i < 4; i++) {
                ep(ks).insert(new CardTransactionEvent((long) i + 1, 1L, 100L,
                    "ACC-001", "ACC-002",
                    10.0, t + i * 60000L,
                    45.0, 15.0, "Zagreb", "Croatia"));
                ks.fireAllRules();
            }

            // 5th event should see the accumulated 4 events
            ep(ks).insert(new CardTransactionEvent(5L, 1L, 100L,
                "ACC-001", "ACC-002",
                10.0, t + 5 * 60000L,
                45.0, 15.0, "Zagreb", "Croatia"));
            ks.fireAllRules();

            List<String> reasons = getReasons(ks);
            assertTrue(reasons.stream().anyMatch(r -> r.contains("Many small")),
                "Expected MANY_SMALL: 5th event sees prior 4 events in session");
        } finally {
            ks.dispose();
        }
    }
}
