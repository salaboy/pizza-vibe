package com.pizzavibe.mcp.tool;

import com.pizzavibe.mcp.client.InventoryClient;
import com.pizzavibe.mcp.model.AcquireResponse;
import com.pizzavibe.mcp.model.ItemResponse;
import io.quarkus.test.Mock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class InventoryToolTest {

    @Inject
    InventoryTool inventoryTool;

    @Test
    void shouldGetAllInventoryItems() {
        String result = inventoryTool.getInventory();

        assertNotNull(result);
        assertTrue(result.contains("Pepperoni"));
        assertTrue(result.contains("Mozzarella"));
        assertTrue(result.contains("PizzaDough"));
    }

    @Test
    void shouldGetSpecificInventoryItem() {
        String result = inventoryTool.getItem("Pepperoni");

        assertNotNull(result);
        assertTrue(result.contains("Pepperoni"));
        assertTrue(result.contains("10"));
    }

    @Test
    void shouldAcquireInventoryItem() {
        String result = inventoryTool.acquireItem("Pepperoni", "order-test");

        assertNotNull(result);
        assertTrue(result.contains("ACQUIRED"));
        assertTrue(result.contains("Pepperoni"));
    }

    @Test
    void shouldReportEmptyWhenItemNotAvailable() {
        String result = inventoryTool.acquireItem("EmptyItem", "order-test");

        assertNotNull(result);
        assertTrue(result.contains("EMPTY"));
    }

    @Mock
    @ApplicationScoped
    @RestClient
    public static class MockInventoryClient implements InventoryClient {

        @Override
        public Map<String, Integer> getAll() {
            return Map.of(
                "Pepperoni", 10,
                "Pineapple", 10,
                "PizzaDough", 10,
                "Mozzarella", 10,
                "Sauce", 10
            );
        }

        @Override
        public ItemResponse getItem(String item) {
            return new ItemResponse(item, 10);
        }

        @Override
        public AcquireResponse acquireItem(String item) {
            if ("EmptyItem".equals(item)) {
                return new AcquireResponse(item, "EMPTY", 0);
            }
            return new AcquireResponse(item, "ACQUIRED", 9);
        }
    }
}
