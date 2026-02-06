package com.pizzavibe.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CookingUpdateTest {

    @Test
    void shouldCreateActionUpdate() {
        CookingUpdate update = CookingUpdate.action("reserving_oven", "Reserving oven for cooking");

        assertEquals("action", update.type());
        assertEquals("reserving_oven", update.action());
        assertEquals("Reserving oven for cooking", update.message());
        assertNull(update.toolName());
        assertNull(update.toolInput());
    }

    @Test
    void shouldCreateToolExecutionUpdate() {
        CookingUpdate update = CookingUpdate.toolExecution("getInventory", null);

        assertEquals("action", update.type());
        assertEquals("checking_inventory", update.action());
        assertEquals("Checking available ingredients in inventory", update.message());
        assertEquals("getInventory", update.toolName());
    }

    @Test
    void shouldMapGetOvensToCheckingOvens() {
        CookingUpdate update = CookingUpdate.toolExecution("getOvens", null);

        assertEquals("checking_ovens", update.action());
        assertEquals("Checking available ovens", update.message());
    }

    @Test
    void shouldMapReserveOvenToReservingOven() {
        CookingUpdate update = CookingUpdate.toolExecution("reserveOven", "{\"ovenId\": \"oven-1\"}");

        assertEquals("reserving_oven", update.action());
        assertTrue(update.message().contains("Reserving oven"));
        assertEquals("{\"ovenId\": \"oven-1\"}", update.toolInput());
    }

    @Test
    void shouldMapGetOvenToCheckingOvenStatus() {
        CookingUpdate update = CookingUpdate.toolExecution("getOven", "oven-1");

        assertEquals("checking_oven_status", update.action());
        assertTrue(update.message().contains("oven-1"));
    }

    @Test
    void shouldMapAcquireItemToAcquiringIngredients() {
        CookingUpdate update = CookingUpdate.toolExecution("acquireItem", "tomatoes");

        assertEquals("acquiring_ingredients", update.action());
        assertTrue(update.message().contains("tomatoes"));
    }

    @Test
    void shouldCreatePartialUpdate() {
        CookingUpdate update = CookingUpdate.partial("Processing your request...");

        assertEquals("partial", update.type());
        assertNull(update.action());
        assertEquals("Processing your request...", update.message());
    }

    @Test
    void shouldCreateResultUpdate() {
        CookingUpdate update = CookingUpdate.result("Pizza cooked successfully!");

        assertEquals("result", update.type());
        assertEquals("completed", update.action());
        assertEquals("Pizza cooked successfully!", update.message());
    }

    @Test
    void shouldHandleUnknownTool() {
        CookingUpdate update = CookingUpdate.toolExecution("unknownTool", "param");

        assertEquals("processing", update.action());
        assertTrue(update.message().contains("unknownTool"));
    }
}
