package com.pizzavibe.store.agent;

import com.pizzavibe.store.client.AgentEventClient;
import com.pizzavibe.store.client.StoreMgmtClient;
import com.pizzavibe.store.client.StoreServiceClient;
import com.pizzavibe.store.client.StoreServiceClient.StoreOrderEvent;
import com.pizzavibe.store.model.AgentEvent;
import com.pizzavibe.store.model.DrinkItem;
import com.pizzavibe.store.model.OrderItem;
import com.pizzavibe.store.model.PizzaOrderStatus;
import com.pizzavibe.store.model.ProcessOrderRequest;
import dev.langchain4j.agent.tool.Tool;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.microprofile.context.ManagedExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
public class OrderSubmissionTool {

    private static final Logger log = LoggerFactory.getLogger(OrderSubmissionTool.class);
    private static final String AGENT_ID = "store-mgmt-agent";

    @Inject
    @RestClient
    StoreMgmtClient storeMgmtClient;

    @Inject
    @RestClient
    StoreServiceClient storeServiceClient;

    @Inject
    @RestClient
    AgentEventClient agentEventClient;

    @Inject
    ManagedExecutor managedExecutor;

    @Tool("Submit a validated pizza order for processing. " +
          "Call this only when the customer has confirmed their order and stock has been validated. " +
          "orderItems is a string describing pizza items e.g. 'OrderItem[pizzaType=Pepperoni, quantity=2], OrderItem[pizzaType=Margherita, quantity=1]'. " +
          "drinkItems is a string describing drink items e.g. 'DrinkItem[drinkType=Beer, quantity=1], DrinkItem[drinkType=Coke, quantity=2]'. " +
          "Pass empty string if no drinks are ordered.")
    public String submitOrder(String orderItems, String drinkItems) {
        String orderId = UUID.randomUUID().toString();
        log.info("Submitting order {} asynchronously. Items: {}, Drinks: {}", orderId, orderItems, drinkItems);

        List<OrderItem> parsedOrderItems = parseOrderItems(orderItems);
        List<DrinkItem> parsedDrinkItems = parseDrinkItems(drinkItems);
        ProcessOrderRequest request = new ProcessOrderRequest(orderId, parsedOrderItems, parsedDrinkItems);

        managedExecutor.runAsync(() -> {
            try {
                log.info("Processing order {} via HTTP call to /mgmt/processOrder", orderId);
                PizzaOrderStatus status = storeMgmtClient.processOrder(request);
                String message = "Order " + status.status()
                        + ". Kitchen: " + (status.kitchenReport() != null ? status.kitchenReport().status() : "N/A")
                        + ". Delivery: " + (status.deliveryReport() != null ? status.deliveryReport() : "N/A");
                sendEventToStore(orderId, status.status().name(), message);
                sendAgentEvent(AgentEvent.response(AGENT_ID, orderId, message));
                log.info("Order {} processing completed with status {}", orderId, status.status());
            } catch (Exception e) {
                log.error("Order {} processing failed", orderId, e);
                String errorMessage = "Order processing failed: " + e.getMessage();
                sendEventToStore(orderId, "FAILED", errorMessage);
                sendAgentEvent(AgentEvent.error(AGENT_ID, orderId, errorMessage));
            }
        });

        return "Order " + orderId + " has been submitted and is now being processed. "
                + "The customer can track the progress in real-time.";
    }

    private void sendEventToStore(String orderId, String status, String message) {
        try {
            storeServiceClient.sendEvent(new StoreOrderEvent(orderId, status, AGENT_ID, message));
        } catch (Exception e) {
            log.warn("Failed to send event to store for orderId={}: {}", orderId, e.getMessage());
        }
    }

    private void sendAgentEvent(AgentEvent event) {
        try {
            agentEventClient.sendEvent(event);
        } catch (Exception e) {
            log.warn("Failed to send agent event for orderId={}: {}", event.orderId(), e.getMessage());
        }
    }

    private static final Pattern ORDER_ITEM_PATTERN =
            Pattern.compile("pizzaType\\s*=\\s*([\\w]+).*?quantity\\s*=\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

    private static final Pattern DRINK_ITEM_PATTERN =
            Pattern.compile("drinkType\\s*=\\s*([\\w]+).*?quantity\\s*=\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

    static List<OrderItem> parseOrderItems(String text) {
        List<OrderItem> items = new ArrayList<>();
        if (text == null || text.isBlank()) return items;
        Matcher m = ORDER_ITEM_PATTERN.matcher(text);
        while (m.find()) {
            items.add(new OrderItem(m.group(1), Integer.parseInt(m.group(2))));
        }
        return items;
    }

    static List<DrinkItem> parseDrinkItems(String text) {
        List<DrinkItem> items = new ArrayList<>();
        if (text == null || text.isBlank()) return items;
        Matcher m = DRINK_ITEM_PATTERN.matcher(text);
        while (m.find()) {
            items.add(new DrinkItem(m.group(1), Integer.parseInt(m.group(2))));
        }
        return items;
    }
}
