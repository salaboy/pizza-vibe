package com.pizzavibe.mcp.tool;

import com.pizzavibe.mcp.client.InventoryClient;
import com.pizzavibe.mcp.client.StoreClient;
import com.pizzavibe.mcp.model.AcquireResponse;
import com.pizzavibe.mcp.model.ItemResponse;
import com.pizzavibe.mcp.model.StoreOrderEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InventoryToolEventTest {

    private InventoryTool tool;
    private List<StoreOrderEvent> capturedEvents;

    @BeforeEach
    void setUp() throws Exception {
        capturedEvents = new ArrayList<>();

        InventoryClient mockInventoryClient = new InventoryClient() {
            @Override
            public Map<String, Integer> getAll() {
                return Map.of("Pepperoni", 10, "Mozzarella", 10, "PizzaDough", 10);
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
        };

        StoreClient mockStoreClient = event -> capturedEvents.add(event);

        tool = new InventoryTool();
        setField(tool, "inventoryClient", mockInventoryClient);
        setField(tool, "storeClient", mockStoreClient);
    }

    @Test
    void acquireItemShouldSendEventToStore() {
        tool.acquireItem("Pepperoni", "order-123");

        assertEquals(1, capturedEvents.size(), "Should send exactly one event when acquiring an item");
        StoreOrderEvent event = capturedEvents.get(0);
        assertEquals("order-123", event.orderId());
        assertEquals("ACQUIRED", event.status());
        assertEquals("inventory", event.source());
        assertTrue(event.message().contains("Pepperoni"));
    }

    @Test
    void acquireItemShouldSendEventEvenWhenEmpty() {
        tool.acquireItem("EmptyItem", "order-456");

        assertEquals(1, capturedEvents.size(), "Should send event even when item is empty");
        StoreOrderEvent event = capturedEvents.get(0);
        assertEquals("order-456", event.orderId());
        assertEquals("EMPTY", event.status());
        assertEquals("inventory", event.source());
        assertTrue(event.message().contains("EmptyItem"));
    }

    @Test
    void acquireItemShouldNotFailWhenStoreClientThrows() {
        StoreClient failingClient = event -> { throw new RuntimeException("connection refused"); };
        try {
            setField(tool, "storeClient", failingClient);
        } catch (Exception e) {
            fail("Setup failed: " + e.getMessage());
        }

        String result = tool.acquireItem("Pepperoni", "order-789");

        assertNotNull(result);
        assertTrue(result.contains("ACQUIRED"), "Tool should still return result even if event sending fails");
    }

    @Test
    void acquireItemShouldStillWorkWithoutBlocking() {
        long start = System.currentTimeMillis();
        tool.acquireItem("Pepperoni", "order-100");
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed < 500, "acquireItem should not block/sleep, took " + elapsed + "ms");
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}