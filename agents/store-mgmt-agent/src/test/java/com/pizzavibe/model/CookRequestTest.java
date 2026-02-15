package com.pizzavibe.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CookRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldCreateCookRequest() {
        List<OrderItem> items = List.of(
                new OrderItem("Margherita", 2),
                new OrderItem("Pepperoni", 1)
        );
        CookRequest request = new CookRequest("order-123", items);

        assertEquals("order-123", request.orderId());
        assertEquals(2, request.orderItems().size());
        assertEquals("Margherita", request.orderItems().get(0).pizzaType());
    }

    @Test
    void shouldSerializeToJsonMatchingGoServiceFormat() throws Exception {
        List<OrderItem> items = List.of(new OrderItem("Margherita", 1));
        CookRequest request = new CookRequest("550e8400-e29b-41d4-a716-446655440000", items);
        String json = objectMapper.writeValueAsString(request);

        assertTrue(json.contains("\"orderId\":\"550e8400-e29b-41d4-a716-446655440000\""));
        assertTrue(json.contains("\"orderItems\""));
        assertTrue(json.contains("\"pizzaType\":\"Margherita\""));
        assertTrue(json.contains("\"quantity\":1"));
    }

    @Test
    void shouldDeserializeFromJson() throws Exception {
        String json = "{\"orderId\":\"order-456\",\"orderItems\":[{\"pizzaType\":\"Hawaiian\",\"quantity\":2}]}";
        CookRequest request = objectMapper.readValue(json, CookRequest.class);

        assertEquals("order-456", request.orderId());
        assertEquals(1, request.orderItems().size());
        assertEquals("Hawaiian", request.orderItems().get(0).pizzaType());
        assertEquals(2, request.orderItems().get(0).quantity());
    }
}
