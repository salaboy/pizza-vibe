package com.pizzavibe.mcp.tool;

import com.pizzavibe.mcp.client.DrinksStockClient;
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
class DrinksStockToolTest {

    @Inject
    DrinksStockTool drinksStockTool;

    @Test
    void shouldGetAllDrinksStock() {
        String result = drinksStockTool.getDrinksStock();

        assertNotNull(result);
        assertTrue(result.contains("Beer"));
        assertTrue(result.contains("Coke"));
        assertTrue(result.contains("DietCoke"));
        assertTrue(result.contains("OrangeJuice"));
    }

    @Test
    void shouldGetSpecificDrinkItem() {
        String result = drinksStockTool.getDrinkItem("Beer");

        assertNotNull(result);
        assertTrue(result.contains("Beer"));
        assertTrue(result.contains("10"));
    }

    @Test
    void shouldAcquireDrinkItem() {
        String result = drinksStockTool.acquireDrink("Coke", "order-test");

        assertNotNull(result);
        assertTrue(result.contains("ACQUIRED"));
        assertTrue(result.contains("Coke"));
    }

    @Test
    void shouldReportEmptyWhenDrinkNotAvailable() {
        String result = drinksStockTool.acquireDrink("EmptyDrink", "order-test");

        assertNotNull(result);
        assertTrue(result.contains("EMPTY"));
    }

    @Mock
    @ApplicationScoped
    @RestClient
    public static class MockDrinksStockClient implements DrinksStockClient {

        @Override
        public Map<String, Integer> getAll() {
            return Map.of(
                "Beer", 10,
                "Coke", 10,
                "DietCoke", 10,
                "OrangeJuice", 10
            );
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
    }
}