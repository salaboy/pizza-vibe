package com.pizzavibe.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderStatusTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldCreateOrderStatus() {
        OrderStatus status = new OrderStatus("cooked", "delivered");

        assertEquals("cooked", status.kitchenStatus());
        assertEquals("delivered", status.deliveryStatus());
    }

    @Test
    void shouldSerializeToJson() throws Exception {
        OrderStatus status = new OrderStatus("cooking", "delivering");
        String json = objectMapper.writeValueAsString(status);

        assertTrue(json.contains("\"kitchenStatus\":\"cooking\""));
        assertTrue(json.contains("\"deliveryStatus\":\"delivering\""));
    }

    @Test
    void shouldDeserializeFromJson() throws Exception {
        String json = "{\"kitchenStatus\":\"done\",\"deliveryStatus\":\"done\"}";
        OrderStatus status = objectMapper.readValue(json, OrderStatus.class);

        assertEquals("done", status.kitchenStatus());
        assertEquals("done", status.deliveryStatus());
    }
}
