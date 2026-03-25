package com.pizzavibe.store.agent;

import com.pizzavibe.store.a2a.CookingA2AClient;
import com.pizzavibe.store.a2a.DeliveryA2AClient;
import com.pizzavibe.store.model.KitchenOrderStatus;
import com.pizzavibe.store.model.KitchenStatus;
import com.pizzavibe.store.model.OrderFinalStatus;
import com.pizzavibe.store.model.PizzaOrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class PizzaOrderOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(PizzaOrderOrchestrator.class);

    private final CookingA2AClient cookingA2AClient;
    private final DeliveryA2AClient deliveryA2AClient;
    private final DrinksAgentService drinksAgentService;

    public PizzaOrderOrchestrator(CookingA2AClient cookingA2AClient,
                                  DeliveryA2AClient deliveryA2AClient,
                                  DrinksAgentService drinksAgentService) {
        this.cookingA2AClient = cookingA2AClient;
        this.deliveryA2AClient = deliveryA2AClient;
        this.drinksAgentService = drinksAgentService;
    }

    public PizzaOrderStatus processOrder(String orderId, String orderItems, String drinkItems) {
        // STEP 1: Kitchen (parallel) - cooking + drinks
        CompletableFuture<String> cookingFuture = CompletableFuture.supplyAsync(
                () -> cookingA2AClient.cook(orderId, orderItems));
        CompletableFuture<String> drinksFuture = CompletableFuture.supplyAsync(
                () -> drinksAgentService.fetchDrinks(orderId, drinkItems));

        String cookingReport;
        String drinksReport;
        try {
            cookingReport = cookingFuture.join();
            drinksReport = drinksFuture.join();
        } catch (Exception e) {
            log.error("Kitchen phase failed for orderId={}", orderId, e);
            cookingReport = "COOKING FAILED: " + e.getMessage();
            drinksReport = "DRINKS FAILED: " + e.getMessage();
        }

        KitchenOrderStatus kitchenReport = buildKitchenStatus(cookingReport, drinksReport);

        log.info(">>>>>> Coordinate Kitchen Status: {}", kitchenReport.status());
        log.info("Cooking Report: {}", kitchenReport.cookingReport());
        log.info("Drinks Report: {}", kitchenReport.drinksReport());

        // STEP 2: Delivery (sequential)
        String deliveryReport = deliveryA2AClient.deliverOrder(orderId);

        PizzaOrderStatus result = buildOrderStatus(kitchenReport, deliveryReport);

        log.info(">>>>>> Order Status: {}", result.status());
        log.info("Kitchen Report: {}", result.kitchenReport());
        log.info("Delivery Report: {}", result.deliveryReport());

        return result;
    }

    private KitchenOrderStatus buildKitchenStatus(String cookingReport, String drinksReport) {
        boolean cookingFailed = cookingReport == null || cookingReport.contains("ERROR") || cookingReport.contains("FAILED");
        boolean drinksFailed = drinksReport == null || drinksReport.contains("ERROR") || drinksReport.contains("FAILED");
        KitchenStatus status = (cookingFailed || drinksFailed) ? KitchenStatus.FAILED : KitchenStatus.SUCCESS;
        return new KitchenOrderStatus(status, cookingReport, drinksReport);
    }

    private PizzaOrderStatus buildOrderStatus(KitchenOrderStatus kitchenReport, String deliveryReport) {
        boolean kitchenFailed = kitchenReport == null || kitchenReport.status() == KitchenStatus.FAILED;
        boolean deliveryFailed = deliveryReport == null || deliveryReport.contains("ERROR") || deliveryReport.contains("FAILED");
        OrderFinalStatus status = (kitchenFailed || deliveryFailed) ? OrderFinalStatus.FAILED : OrderFinalStatus.SUCCESS;
        return new PizzaOrderStatus(status, kitchenReport, deliveryReport);
    }
}
