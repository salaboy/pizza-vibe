package com.pizzavibe.store.agent;

import com.pizzavibe.store.client.AgentEventClient;
import com.pizzavibe.store.client.StoreEventClient;
import com.pizzavibe.store.model.AgentEvent;
import com.pizzavibe.store.model.DrinkItem;
import com.pizzavibe.store.model.OrderItem;
import com.pizzavibe.store.model.PizzaOrderStatus;
import com.pizzavibe.store.model.ProcessOrderRequest;
import com.pizzavibe.store.model.StoreOrderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class OrderSubmissionTool {

    private static final Logger log = LoggerFactory.getLogger(OrderSubmissionTool.class);
    private static final String AGENT_ID = "store-mgmt-agent";

    private final RestClient storeMgmtClient;
    private final StoreEventClient storeEventClient;
    private final AgentEventClient agentEventClient;

    public OrderSubmissionTool(
            @Value("${store.mgmt.url:http://localhost:9999}") String storeMgmtUrl,
            StoreEventClient storeEventClient,
            AgentEventClient agentEventClient) {
        this.storeMgmtClient = RestClient.builder().baseUrl(storeMgmtUrl).build();
        this.storeEventClient = storeEventClient;
        this.agentEventClient = agentEventClient;
    }

    @Tool(description = "Submit a validated pizza order for processing. " +
            "Call this only when the customer has confirmed their order and stock has been validated. " +
            "orderItems is a string describing pizza items e.g. 'OrderItem[pizzaType=Pepperoni, quantity=2], OrderItem[pizzaType=Margherita, quantity=1]'. " +
            "drinkItems is a string describing drink items e.g. 'DrinkItem[drinkType=Beer, quantity=1], DrinkItem[drinkType=Coke, quantity=2]'. " +
            "Pass empty string if no drinks are ordered.")
    public String submitOrder(
            @ToolParam(description = "String describing pizza items") String orderItems,
            @ToolParam(description = "String describing drink items") String drinkItems) {
        String orderId = UUID.randomUUID().toString();
        log.info("Submitting order {} asynchronously. Items: {}, Drinks: {}", orderId, orderItems, drinkItems);

        List<OrderItem> parsedOrderItems = parseOrderItems(orderItems);
        List<DrinkItem> parsedDrinkItems = parseDrinkItems(drinkItems);
        ProcessOrderRequest request = new ProcessOrderRequest(orderId, parsedOrderItems, parsedDrinkItems);

        CompletableFuture.runAsync(() -> {
            try {
                log.info("Processing order {} via HTTP call to /mgmt/processOrder", orderId);
                PizzaOrderStatus status = storeMgmtClient.post()
                        .uri("/mgmt/processOrder")
                        .body(request)
                        .retrieve()
                        .body(PizzaOrderStatus.class);

                String message = "Order " + status.status()
                        + ". Kitchen: " + (status.kitchenReport() != null ? status.kitchenReport().status() : "N/A")
                        + ". Delivery: " + (status.deliveryReport() != null ? status.deliveryReport() : "N/A");
                storeEventClient.sendEvent(new StoreOrderEvent(orderId, status.status().name(), AGENT_ID, message));
                agentEventClient.sendEvent(AgentEvent.response(AGENT_ID, message));
                log.info("Order {} processing completed with status {}", orderId, status.status());
            } catch (Exception e) {
                log.error("Order {} processing failed", orderId, e);
                String errorMessage = "Order processing failed: " + e.getMessage();
                storeEventClient.sendEvent(new StoreOrderEvent(orderId, "FAILED", AGENT_ID, errorMessage));
                agentEventClient.sendEvent(AgentEvent.error(AGENT_ID, errorMessage));
            }
        });

        return "Order " + orderId + " has been submitted and is now being processed. "
                + "The customer can track the progress in real-time.";
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
