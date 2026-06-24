package com.example.bank.rules;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.internal.io.ResourceFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
public class FraudDetectionService {

    private final KieContainer kieContainer;

    public FraudDetectionService() {
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

    public List<SuspiciousTransactionFact> evaluateTransaction(CardTransactionEvent event) {
        return evaluateTransactionWithHistory(event, List.of());
    }

    public List<SuspiciousTransactionFact> evaluateTransactionWithHistory(
            CardTransactionEvent event, List<CardTransactionEvent> historicalEvents) {
        KieSession kieSession = kieContainer.newKieSession("fraudKieSession");
        try {
            for (CardTransactionEvent histEvent : historicalEvents) {
                kieSession.insert(histEvent);
            }
            kieSession.insert(event);
            kieSession.fireAllRules();

            Collection<?> suspiciousFacts = kieSession.getObjects(
                o -> o instanceof SuspiciousTransactionFact
            );
            List<SuspiciousTransactionFact> result = new ArrayList<>();
            for (Object fact : suspiciousFacts) {
                result.add((SuspiciousTransactionFact) fact);
            }
            return result;
        } finally {
            kieSession.dispose();
        }
    }
}
