package com.pizzavibe.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeliveryUpdateTest {

    @Test
    void shouldCreateActionUpdate() {
        DeliveryUpdate update = DeliveryUpdate.action("reserving_bike", "Reserving bike for delivery");

        assertEquals("action", update.type());
        assertEquals("reserving_bike", update.action());
        assertEquals("Reserving bike for delivery", update.message());
        assertNull(update.toolName());
        assertNull(update.toolInput());
    }

    @Test
    void shouldCreateToolExecutionUpdate() {
        DeliveryUpdate update = DeliveryUpdate.toolExecution("getBikes", null);

        assertEquals("action", update.type());
        assertEquals("checking_bikes", update.action());
        assertEquals("Checking available bikes for delivery", update.message());
        assertEquals("getBikes", update.toolName());
    }

    @Test
    void shouldMapGetBikesToCheckingBikes() {
        DeliveryUpdate update = DeliveryUpdate.toolExecution("getBikes", null);

        assertEquals("checking_bikes", update.action());
        assertEquals("Checking available bikes for delivery", update.message());
    }

    @Test
    void shouldMapReserveBikeToReservingBike() {
        DeliveryUpdate update = DeliveryUpdate.toolExecution("reserveBike", "{\"bikeId\": \"bike-1\"}");

        assertEquals("reserving_bike", update.action());
        assertTrue(update.message().contains("Reserving bike"));
        assertEquals("{\"bikeId\": \"bike-1\"}", update.toolInput());
    }

    @Test
    void shouldMapGetBikeToCheckingBikeStatus() {
        DeliveryUpdate update = DeliveryUpdate.toolExecution("getBike", "bike-1");

        assertEquals("checking_bike_status", update.action());
        assertTrue(update.message().contains("bike-1"));
    }

    @Test
    void shouldCreatePartialUpdate() {
        DeliveryUpdate update = DeliveryUpdate.partial("Processing your delivery...");

        assertEquals("partial", update.type());
        assertNull(update.action());
        assertEquals("Processing your delivery...", update.message());
    }

    @Test
    void shouldCreateResultUpdate() {
        DeliveryUpdate update = DeliveryUpdate.result("Delivery completed successfully!");

        assertEquals("result", update.type());
        assertEquals("completed", update.action());
        assertEquals("Delivery completed successfully!", update.message());
    }

    @Test
    void shouldHandleUnknownTool() {
        DeliveryUpdate update = DeliveryUpdate.toolExecution("unknownTool", "param");

        assertEquals("processing", update.action());
        assertTrue(update.message().contains("unknownTool"));
    }
}
