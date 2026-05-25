package com.example.bookstore.rules;

import com.example.bookstore.model.Order;
import com.example.bookstore.model.OrderItem;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Results;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.internal.io.ResourceFactory;
import org.springframework.stereotype.Service;

@Service
public class OrderDiscountService {

    private final KieContainer kieContainer;

    public OrderDiscountService() {
        KieServices kieServices = KieServices.Factory.get();
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
        kieFileSystem.write(ResourceFactory.newClassPathResource("rules/order-discount-rules.drl"));

        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
        kieBuilder.buildAll();

        Results results = kieBuilder.getResults();
        if (results.hasMessages(org.kie.api.builder.Message.Level.ERROR)) {
            throw new IllegalStateException("Drools build errors: " + results.getMessages());
        }

        this.kieContainer = kieServices.newKieContainer(kieServices.getRepository().getDefaultReleaseId());
    }

    public DiscountContext evaluate(Order order) {
        DiscountContext context = new DiscountContext(order);
        KieSession kieSession = kieContainer.newKieSession();
        try {
            kieSession.insert(order);
            kieSession.insert(context);
            for (OrderItem item : order.getItems()) {
                kieSession.insert(item);
            }
            kieSession.fireAllRules();
        } finally {
            kieSession.dispose();
        }
        return context;
    }
}
