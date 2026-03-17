package com.pizzavibe.mcp.tool;

import com.pizzavibe.mcp.client.DrinksStockClient;
import com.pizzavibe.mcp.client.StoreClient;
import com.pizzavibe.mcp.model.AcquireResponse;
import com.pizzavibe.mcp.model.ItemResponse;
import com.pizzavibe.mcp.model.StoreOrderEvent;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class DrinksStockTool {

    private static final Logger LOG = Logger.getLogger(DrinksStockTool.class);

    @Inject
    @RestClient
    DrinksStockClient drinksStockClient;

    @Inject
    @RestClient
    StoreClient storeClient;

    @Tool(description = "Get the current stock of all available drinks with their quantities")
    public String getDrinksStock() {
        Map<String, Integer> stock = drinksStockClient.getAll();
        return stock.entrySet().stream()
            .map(e -> e.getKey() + ": " + e.getValue())
            .collect(Collectors.joining(", "));
    }

    @Tool(description = "Get the quantity of a specific drink item in stock")
    public String getDrinkItem(@ToolArg(description = "The name of the drink (e.g., Beer, Coke, DietCoke, OrangeJuice)") String item) {
        ItemResponse response = drinksStockClient.getItem(item);
        return "Item: " + response.item() + ", Quantity: " + response.quantity();
    }

    @Tool(description = "Acquire one unit of a drink from stock. Returns ACQUIRED if successful or EMPTY if not available.")
    public String acquireDrink(
            @ToolArg(description = "The name of the drink to acquire") String item,
            @ToolArg(description = "The order ID to track this acquisition") String orderId) {
        AcquireResponse response = drinksStockClient.acquireItem(item);
        sendEventToStore(orderId, item, response.status());
        return "Item: " + response.item() + ", Status: " + response.status() + ", Remaining: " + response.remainingQuantity();
    }

    private void sendEventToStore(String orderId, String item, String status) {
        try {
            String cleanOrderId = orderId != null ? orderId.trim() : "";
            String message = "Drink " + item + " " + status.toLowerCase();
            StoreOrderEvent event = new StoreOrderEvent(cleanOrderId, status, "drinks", message);
            storeClient.sendEvent(event);
        } catch (Exception e) {
            LOG.warn("Failed to send drink event to store (orderId=" + orderId + "): " + e.getMessage());
        }
    }
}