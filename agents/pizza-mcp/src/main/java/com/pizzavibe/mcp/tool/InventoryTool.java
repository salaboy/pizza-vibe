package com.pizzavibe.mcp.tool;

import com.pizzavibe.mcp.client.InventoryClient;
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
public class InventoryTool {

    private static final Logger LOG = Logger.getLogger(InventoryTool.class);

    @Inject
    @RestClient
    InventoryClient inventoryClient;

    @Inject
    @RestClient
    StoreClient storeClient;

    @Tool(description = "Get the current inventory of all pizza ingredients with their quantities")
    public String getInventory() {
        Map<String, Integer> inventory = inventoryClient.getAll();
        return inventory.entrySet().stream()
            .map(e -> e.getKey() + ": " + e.getValue())
            .collect(Collectors.joining(", "));
    }

    @Tool(description = "Get the quantity of a specific inventory item")
    public String getItem(@ToolArg(description = "The name of the inventory item (e.g., Pepperoni, PizzaDough, Mozzarella, Sauce, Pineapple)") String item) {
        ItemResponse response = inventoryClient.getItem(item);
        return "Item: " + response.item() + ", Quantity: " + response.quantity();
    }

    @Tool(description = "Acquire one unit of an inventory item for pizza making. Returns ACQUIRED if successful or EMPTY if not available.")
    public String acquireItem(
            @ToolArg(description = "The name of the inventory item to acquire") String item,
            @ToolArg(description = "The order ID to track this acquisition") String orderId) {
        AcquireResponse response = inventoryClient.acquireItem(item);
        sendEventToStore(orderId, item, response.status());
        return "Item: " + response.item() + ", Status: " + response.status() + ", Remaining: " + response.remainingQuantity();
    }

    private void sendEventToStore(String orderId, String item, String status) {
        try {
            String cleanOrderId = orderId != null ? orderId.trim() : "";
            String message = "Ingredient " + item + " " + status.toLowerCase();
            StoreOrderEvent event = new StoreOrderEvent(cleanOrderId, status, "inventory", message);
            storeClient.sendEvent(event);
        } catch (Exception e) {
            LOG.warn("Failed to send inventory event to store (orderId=" + orderId + "): " + e.getMessage());
        }
    }
}
