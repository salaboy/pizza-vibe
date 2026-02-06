package com.pizzavibe.mcp.tool;

import com.pizzavibe.mcp.client.OvenClient;
import com.pizzavibe.mcp.model.Oven;
import io.quarkus.test.Mock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class OvenToolTest {

    @Inject
    OvenTool ovenTool;

    @Test
    void shouldGetAllOvens() {
        String result = ovenTool.getOvens();

        assertNotNull(result);
        assertTrue(result.contains("oven-1"));
        assertTrue(result.contains("AVAILABLE"));
    }

    @Test
    void shouldGetOvenById() {
        String result = ovenTool.getOven("oven-1");

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

    @Mock
    @ApplicationScoped
    @RestClient
    public static class MockOvenClient implements OvenClient {

        @Override
        public List<Oven> getAll() {
            return List.of(
                new Oven("oven-1", "AVAILABLE", null, Instant.now()),
                new Oven("oven-2", "AVAILABLE", null, Instant.now()),
                new Oven("oven-3", "AVAILABLE", null, Instant.now()),
                new Oven("oven-4", "AVAILABLE", null, Instant.now())
            );
        }

        @Override
        public Oven getById(String ovenId) {
            return new Oven(ovenId, "AVAILABLE", null, Instant.now());
        }

        @Override
        public Oven reserve(String ovenId, String user) {
            return new Oven(ovenId, "RESERVED", user, Instant.now());
        }
    }
}
