package com.example.bank.rules;

import com.example.bank.model.BankAccount;
import com.example.bank.model.Transaction;
import com.example.bank.repository.BankAccountRepository;
import com.example.bank.repository.TransactionRepository;
import jakarta.annotation.PostConstruct;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.EntryPoint;
import org.kie.internal.io.ResourceFactory;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class FraudDetectionService {

    private final KieContainer kieContainer;
    private final TransactionRepository transactionRepository;
    private final BankAccountRepository bankAccountRepository;
    private final Lock sessionLock = new ReentrantLock();

    private volatile KieSession kieSession;

    public FraudDetectionService(TransactionRepository transactionRepository,
                                 BankAccountRepository bankAccountRepository) {
        this.transactionRepository = transactionRepository;
        this.bankAccountRepository = bankAccountRepository;

        KieServices kieServices = KieServices.Factory.get();
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();

        String kmoduleXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<kmodule xmlns=\"http://www.drools.org/xsd/kmodule\">\n" +
            "  <kbase name=\"fraudKieBase\" packages=\"rules.fraud\" eventProcessingMode=\"stream\">\n" +
            "    <ksession name=\"fraudKieSession\" clockType=\"realtime\"/>\n" +
            "    <ksession name=\"fraudPseudoClockKieSession\" clockType=\"pseudo\"/>\n" +
            "  </kbase>\n" +
            "</kmodule>";

        kieFileSystem.writeKModuleXML(kmoduleXml);
        kieFileSystem.write(ResourceFactory.newClassPathResource("rules/fraud-detection-rules.drl"));

        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
        kieBuilder.buildAll();

        Results results = kieBuilder.getResults();
        if (results.hasMessages(Message.Level.ERROR)) {
            throw new IllegalStateException("Fraud DRL build errors: " + results.getMessages());
        }

        this.kieContainer = kieServices.newKieContainer(kieServices.getRepository().getDefaultReleaseId());
    }

    @PostConstruct
    public void init() {
        this.kieSession = kieContainer.newKieSession("fraudKieSession");
        EntryPoint ep = kieSession.getEntryPoint("card-transactions");

        Set<Long> clientIds = new HashSet<>();
        Map<String, Long> accountToClient = new HashMap<>();
        List<Transaction> history = transactionRepository.findAllCompletedOrApproved();
        for (Transaction tx : history) {
            Long clientId = accountToClient.get(tx.getSenderAccountNumber());
            if (clientId == null) {
                BankAccount acc = bankAccountRepository.findByAccountNumber(tx.getSenderAccountNumber()).orElse(null);
                if (acc != null && acc.getPackageAccount() != null && acc.getPackageAccount().getClient() != null) {
                    clientId = acc.getPackageAccount().getClient().getId();
                    accountToClient.put(tx.getSenderAccountNumber(), clientId);
                }
            }
            if (clientId == null) continue;
            clientIds.add(clientId);

            long txEpoch = tx.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            CardTransactionEvent histEvent = new CardTransactionEvent(
                tx.getId(), clientId, null,
                tx.getSenderAccountNumber(), tx.getReceiverAccountNumber(),
                tx.getAmount(), txEpoch,
                tx.getLatitude(), tx.getLongitude(),
                tx.getCity() != null ? tx.getCity() : "",
                tx.getCountry() != null ? tx.getCountry() : ""
            );
            ep.insert(histEvent);
        }

        // Insert non-expiring markers so existing clients' first new tx can be evaluated
        for (Long clientId : clientIds) {
            kieSession.insert(new ClientActivityMarker(clientId));
        }

        kieSession.fireAllRules();
    }

    public List<SuspiciousTransactionFact> evaluateTransaction(CardTransactionEvent event) {
        sessionLock.lock();
        try {
            EntryPoint ep = kieSession.getEntryPoint("card-transactions");
            ep.insert(event);
            kieSession.fireAllRules();

            // Insert a non-expiring marker for new clients so future transactions
            // are evaluated with knowledge of prior activity
            boolean markerExists = !kieSession.getObjects(o ->
                o instanceof ClientActivityMarker m && m.getClientId().equals(event.getClientId())
            ).isEmpty();
            if (!markerExists) {
                kieSession.insert(new ClientActivityMarker(event.getClientId()));
            }

            Collection<?> suspiciousFacts = kieSession.getObjects(
                o -> o instanceof SuspiciousTransactionFact
            );
            List<SuspiciousTransactionFact> result = new ArrayList<>();
            for (Object fact : suspiciousFacts) {
                result.add((SuspiciousTransactionFact) fact);
            }
            return result;
        } finally {
            sessionLock.unlock();
        }
    }
}
