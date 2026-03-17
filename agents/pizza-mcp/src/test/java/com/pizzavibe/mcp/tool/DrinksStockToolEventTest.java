package com.pizzavibe.mcp.tool;

import com.pizzavibe.mcp.client.DrinksStockClient;
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

class DrinksStockToolEventTest {

    private DrinksStockTool tool;
    private List<StoreOrderEvent> capturedEvents;

    @BeforeEach
    void setUp() throws Exception {
        capturedEvents = new ArrayList<>();

        DrinksStockClient mockDrinksClient = new DrinksStockClient() {
            @Override
            public Map<String, Integer> getAll() {
                return Map.of("Beer", 10, "Coke", 10);
            }

            @Override
            public ItemResponse getItem(String item) {
                return new ItemResponse(item, 10);
            }

            @Override
            public AcquireResponse acquireItem(String item) {
                if ("EmptyDrink".equals(item)) {
                    return new AcquireResponse(item, "EMPTY", 0);
                }
                return new AcquireResponse(item, "ACQUIRED", 9);
            }
        };

        StoreClient mockStoreClient = event -> capturedEvents.add(event);

        tool = new DrinksStockTool();
        setField(tool, "drinksStockClient", mockDrinksClient);
        setField(tool, "storeClient", mockStoreClient);
    }

    @Test
    void acquireDrinkShouldSendEventToStore() {
        tool.acquireDrink("Beer", "order-123");

        assertEquals(1, capturedEvents.size(), "Should send exactly one event when acquiring a drink");
        StoreOrderEvent event = capturedEvents.get(0);
        assertEquals("order-123", event.orderId());
        assertEquals("ACQUIRED", event.status());
        assertEquals("drinks", event.source());
        assertTrue(event.message().contains("Beer"));
    }

    @Test
    void acquireDrinkShouldSendEventEvenWhenEmpty() {
        tool.acquireDrink("EmptyDrink", "order-456");

        assertEquals(1, capturedEvents.size(), "Should send event even when drink is empty");
        StoreOrderEvent event = capturedEvents.get(0);
        assertEquals("order-456", event.orderId());
        assertEquals("EMPTY", event.status());
        assertEquals("drinks", event.source());
        assertTrue(event.message().contains("EmptyDrink"));
    }

    @Test
    void acquireDrinkShouldNotFailWhenStoreClientThrows() {
        StoreClient failingClient = event -> { throw new RuntimeException("connection refused"); };
        try {
            setField(tool, "storeClient", failingClient);
        } catch (Exception e) {
            fail("Setup failed: " + e.getMessage());
        }

        String result = tool.acquireDrink("Beer", "order-789");

        assertNotNull(result);
        assertTrue(result.contains("ACQUIRED"), "Tool should still return result even if event sending fails");
    }

    @Test
    void acquireDrinkShouldStillWorkWithoutBlocking() {
        long start = System.currentTimeMillis();
        tool.acquireDrink("Beer", "order-100");
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed < 500, "acquireDrink should not block/sleep, took " + elapsed + "ms");
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
