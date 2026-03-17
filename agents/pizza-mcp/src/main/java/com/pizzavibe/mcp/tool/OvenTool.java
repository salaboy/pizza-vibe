package com.pizzavibe.mcp.tool;

import com.pizzavibe.mcp.client.OvenClient;
import com.pizzavibe.mcp.client.StoreClient;
import com.pizzavibe.mcp.model.Oven;
import com.pizzavibe.mcp.model.StoreOrderEvent;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class OvenTool {

    private static final Logger LOG = Logger.getLogger(OvenTool.class);

    @Inject
    @RestClient
    OvenClient ovenClient;

    @Inject
    @RestClient
    StoreClient storeClient;

    @Tool(description = "Get all pizza ovens with their current status (AVAILABLE or RESERVED)")
    public String getOvens() {
        List<Oven> ovens = ovenClient.getAll();
        return ovens.stream()
            .map(o -> "Oven: " + o.id() + ", Status: " + o.status() + (o.user() != null ? ", User: " + o.user() : ""))
            .collect(Collectors.joining("\n"));
    }

    @Tool(description = "Wait for a pizza oven to finish cooking. Polls every 1 second and returns only when the oven is AVAILABLE again. Returns progress percentage while cooking. Call this once after reserving an oven.")
    public String getOven(
            @ToolArg(description = "The oven ID (e.g., oven-1, oven-2, oven-3, oven-4)") String ovenId,
            @ToolArg(description = "The order ID to track progress for") String orderId) {
        int maxAttempts = 30;

        sendMessageEventToStore( orderId, "RESERVING_OVEN", "Preparing pizza in oven: "+ ovenId);
        for (int i = 0; i < maxAttempts; i++) {
            Oven oven = ovenClient.getById(ovenId);
            // Send progress event to store
            sendProgressToStore(orderId, ovenId, oven.progress(), "COOKING");
            if (Oven.STATUS_AVAILABLE.equals(oven.status())) {
              sendProgressToStore(orderId, ovenId, 100, "COOKING");
              sendMessageEventToStore( orderId, "COOKED", "Pizza cooked!");
              sendMessageEventToStore( orderId, "RELEASING_OVEN", "Releasing oven: "+ ovenId);
              return "Oven: " + oven.id() + ", Status: " + oven.status() + ", Progress: 100%";
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "Error: polling interrupted for oven " + ovenId;
            }
        }
        return "Error: Oven " + ovenId + " did not become AVAILABLE after " + (maxAttempts * 2) + " seconds";
    }

    @Tool(description = "Reserve a pizza oven for cooking. The oven will be automatically released after 5-20 seconds. Returns the reserved oven or an error if already reserved.")
    public String reserveOven(
            @ToolArg(description = "The oven ID to reserve (e.g., oven-1)") String ovenId,
            @ToolArg(description = "The user/cook name reserving the oven") String user) {
        Oven oven = ovenClient.reserve(ovenId, user);
        return "Oven: " + oven.id() + ", Status: " + oven.status() + ", User: " + oven.user();
    }

    private void sendProgressToStore(String orderId, String ovenId, int progress, String status) {
        try {
            String cleanOrderId = orderId != null ? orderId.trim() : "";
            String message = "Oven " + ovenId + " progress: " + progress + "%";
            StoreOrderEvent event = new StoreOrderEvent(cleanOrderId, status, "kitchen", message);
            storeClient.sendEvent(event);
        } catch (Exception e) {
            LOG.warn("Failed to send progress to store (orderId=" + orderId + "): " + e.getMessage());
        }
    }

    private void sendMessageEventToStore(String orderId, String status, String message) {
        try {
            String cleanOrderId = orderId != null ? orderId.trim() : "";
            StoreOrderEvent event = new StoreOrderEvent(cleanOrderId, status, "kitchen", message);
            storeClient.sendEvent(event);
        } catch (Exception e) {
            LOG.warn("Failed to send event to store (orderId=" + orderId + "): " + e.getMessage());
        }
    }
}
