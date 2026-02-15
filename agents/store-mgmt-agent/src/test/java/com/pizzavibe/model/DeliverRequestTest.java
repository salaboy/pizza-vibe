package com.pizzavibe.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeliverRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldCreateDeliverRequest() {
        List<OrderItem> items = List.of(new OrderItem("Margherita", 1));
        DeliverRequest request = new DeliverRequest("order-123", items);

        assertEquals("order-123", request.orderId());
        assertEquals(1, request.orderItems().size());
    }

    @Test
    void shouldSerializeToJsonMatchingGoServiceFormat() throws Exception {
        List<OrderItem> items = List.of(
                new OrderItem("Margherita", 2),
                new OrderItem("Pepperoni", 1)
        );
        DeliverRequest request = new DeliverRequest("550e8400-e29b-41d4-a716-446655440000", items);
        String json = objectMapper.writeValueAsString(request);

        assertTrue(json.contains("\"orderId\":\"550e8400-e29b-41d4-a716-446655440000\""));
        assertTrue(json.contains("\"orderItems\""));
        assertTrue(json.contains("\"pizzaType\":\"Margherita\""));
        assertTrue(json.contains("\"pizzaType\":\"Pepperoni\""));
    }

    @Test
    void shouldDeserializeFromJson() throws Exception {
        String json = "{\"orderId\":\"order-789\",\"orderItems\":[{\"pizzaType\":\"Veggie\",\"quantity\":1}]}";
        DeliverRequest request = objectMapper.readValue(json, DeliverRequest.class);

        assertEquals("order-789", request.orderId());
        assertEquals(1, request.orderItems().size());
        assertEquals("Veggie", request.orderItems().get(0).pizzaType());
    }
}
