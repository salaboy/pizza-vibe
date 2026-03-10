package com.pizzavibe.store.agent;

import com.pizzavibe.store.client.StoreServiceClient;
import com.pizzavibe.store.client.StoreServiceClient.StoreOrderEvent;
import com.pizzavibe.store.model.PizzaOrderStatus;
import com.pizzavibe.store.workflows.PizzaOrderWorkflow;
import dev.langchain4j.agent.tool.Tool;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@ApplicationScoped
public class OrderSubmissionTool {

    private static final Logger log = LoggerFactory.getLogger(OrderSubmissionTool.class);

    @Inject
    PizzaOrderWorkflow pizzaOrderWorkflow;

    @Inject
    @RestClient
    StoreServiceClient storeServiceClient;

    @Tool("Submit a validated pizza order for processing. " +
          "Call this only when the customer has confirmed their order and stock has been validated. " +
          "orderItems is a string describing pizza items e.g. 'OrderItem[pizzaType=Pepperoni, quantity=2], OrderItem[pizzaType=Margherita, quantity=1]'. " +
          "drinkItems is a string describing drink items e.g. 'DrinkItem[drinkType=Beer, quantity=1], DrinkItem[drinkType=Coke, quantity=2]'. " +
          "Pass empty string if no drinks are ordered.")
    public String submitOrder(String orderItems, String drinkItems) {
        String orderId = UUID.randomUUID().toString();
        log.info("Submitting order {} asynchronously. Items: {}, Drinks: {}", orderId, orderItems, drinkItems);

        CompletableFuture.runAsync(() -> {
            try {
                log.info("Processing order {} in background", orderId);
                PizzaOrderStatus status = pizzaOrderWorkflow.processPizzaOrder(orderId, orderItems, drinkItems);
                String message = "Order " + status.status()
                        + ". Kitchen: " + (status.kitchenReport() != null ? status.kitchenReport().status() : "N/A")
                        + ". Delivery: " + (status.deliveryReport() != null ? status.deliveryReport() : "N/A");
                sendEventToStore(orderId, status.status().name(), message);
                log.info("Order {} processing completed with status {}", orderId, status.status());
            } catch (Exception e) {
                log.error("Order {} processing failed", orderId, e);
                sendEventToStore(orderId, "FAILED", "Order processing failed: " + e.getMessage());
            }
        });

        return "Order " + orderId + " has been submitted and is now being processed. "
                + "The customer can track the progress in real-time.";
    }

    private void sendEventToStore(String orderId, String status, String message) {
        try {
            storeServiceClient.sendEvent(new StoreOrderEvent(orderId, status, "store-mgmt-agent", message));
        } catch (Exception e) {
            log.warn("Failed to send event to store for orderId={}: {}", orderId, e.getMessage());
        }
    }
}