package com.pizzavibe.mcp.tool;

import com.pizzavibe.mcp.client.OvenClient;
import com.pizzavibe.mcp.client.StoreClient;
import com.pizzavibe.mcp.model.Oven;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that OvenTool polls every 1 second (not 2) when waiting for an oven to finish.
 */
class OvenToolPollingTest {

    @Test
    void getOvenShouldPollEveryOneSecond() throws Exception {
        // Mock that returns RESERVED twice, then AVAILABLE
        AtomicInteger callCount = new AtomicInteger(0);
        OvenClient mockOvenClient = new OvenClient() {
            @Override
            public List<Oven> getAll() {
                return List.of();
            }

            @Override
            public Oven getById(String ovenId) {
                int count = callCount.incrementAndGet();
                if (count <= 2) {
                    return new Oven(ovenId, "RESERVED", "test-user", 50, Instant.now());
                }
                return new Oven(ovenId, "AVAILABLE", null, 100, Instant.now());
            }

            @Override
            public Oven reserve(String ovenId, String user) {
                return new Oven(ovenId, "RESERVED", user, 0, Instant.now());
            }
        };

        StoreClient mockStoreClient = event -> { /* no-op */ };

        OvenTool tool = new OvenTool();
        setField(tool, "ovenClient", mockOvenClient);
        setField(tool, "storeClient", mockStoreClient);

        long start = System.currentTimeMillis();
        String result = tool.getOven("oven-1", "order-123");
        long elapsed = System.currentTimeMillis() - start;

        // 2 polls with sleep = 2 seconds total (at 1s each)
        // With the old 2-second interval this would take ~4 seconds
        assertTrue(elapsed < 3000,
            "Polling should take ~2 seconds (1s interval x 2 polls), but took " + elapsed + "ms");
        assertTrue(elapsed >= 1800,
            "Polling should take at least ~2 seconds, but took only " + elapsed + "ms");
        assertEquals(3, callCount.get(), "Should have polled 3 times (2 RESERVED + 1 AVAILABLE)");
        assertTrue(result.contains("AVAILABLE"));
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
