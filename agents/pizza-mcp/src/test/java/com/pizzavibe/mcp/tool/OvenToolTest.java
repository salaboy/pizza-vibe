package com.pizzavibe.mcp.tool;

import com.pizzavibe.mcp.client.OvenClient;
import com.pizzavibe.mcp.client.StoreClient;
import com.pizzavibe.mcp.model.Oven;
import com.pizzavibe.mcp.model.StoreOrderEvent;
import io.quarkus.test.Mock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class OvenToolTest {

    @Inject
    OvenTool ovenTool;

    @BeforeEach
    void resetCounters() {
        MockStoreClient.eventCount.set(0);
    }

    @Test
    void shouldGetAllOvens() {
        String result = ovenTool.getOvens();

        assertNotNull(result);
        assertTrue(result.contains("oven-1"));
        assertTrue(result.contains("AVAILABLE"));
    }

    @Test
    void shouldGetOvenById() {
        String result = ovenTool.getOven("oven-1", "order-123");

        assertNotNull(result);
        assertTrue(result.contains("oven-1"));
        assertTrue(result.contains("AVAILABLE"));
    }

    @Test
    void shouldReserveOven() {
        String result = ovenTool.reserveOven("oven-1", "pizza-cook-1");

        assertNotNull(result);
        assertTrue(result.contains("oven-1"));
        assertTrue(result.contains("RESERVED"));
        assertTrue(result.contains("pizza-cook-1"));
    }

    @Test
    void shouldReserveOvenWithCookingAgentJoe() {
        String result = ovenTool.reserveOven("oven-1", "cooking-agent-joe");

        assertNotNull(result);
        assertTrue(result.contains("oven-1"));
        assertTrue(result.contains("RESERVED"));
        assertTrue(result.contains("cooking-agent-joe"),
            "Oven reservation must record 'cooking-agent-joe' as the user");
    }

    @Test
    void shouldSendProgressEventToStoreOnPolling() {
        ovenTool.getOven("oven-1", "order-456");

        assertTrue(MockStoreClient.eventCount.get() > 0,
            "StoreClient.sendEvent() must be called at least once while polling the oven");
    }

    @Mock
    @ApplicationScoped
    @RestClient
    public static class MockOvenClient implements OvenClient {

        @Override
        public List<Oven> getAll() {
            return List.of(
                new Oven("oven-1", "AVAILABLE", null, 0, Instant.now()),
                new Oven("oven-2", "AVAILABLE", null, 0, Instant.now()),
                new Oven("oven-3", "AVAILABLE", null, 0, Instant.now()),
                new Oven("oven-4", "AVAILABLE", null, 0, Instant.now())
            );
        }

        @Override
        public Oven getById(String ovenId) {
            return new Oven(ovenId, "AVAILABLE", null, 100, Instant.now());
        }

        @Override
        public Oven reserve(String ovenId, String user) {
            return new Oven(ovenId, "RESERVED", user, 0, Instant.now());
        }
    }

    @Mock
    @ApplicationScoped
    @RestClient
    public static class MockStoreClient implements StoreClient {

        static final AtomicInteger eventCount = new AtomicInteger(0);

        @Override
        public void sendEvent(StoreOrderEvent event) {
            eventCount.incrementAndGet();
        }
    }
}
