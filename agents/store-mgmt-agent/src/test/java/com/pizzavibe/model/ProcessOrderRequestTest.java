package com.pizzavibe.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProcessOrderRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldCreateProcessOrderRequest() {
        List<OrderItem> items = List.of(new OrderItem("Margherita", 2));
        ProcessOrderRequest request = new ProcessOrderRequest("order-123", items);

        assertEquals("order-123", request.orderId());
        assertEquals(1, request.orderItems().size());
        assertEquals("Margherita", request.orderItems().get(0).pizzaType());
    }

    @Test
    void shouldDeserializeFromJson() throws Exception {
        String json = "{\"orderId\":\"order-456\",\"orderItems\":[{\"pizzaType\":\"Pepperoni\",\"quantity\":1}]}";
        ProcessOrderRequest request = objectMapper.readValue(json, ProcessOrderRequest.class);

        assertEquals("order-456", request.orderId());
        assertEquals(1, request.orderItems().size());
        assertEquals("Pepperoni", request.orderItems().get(0).pizzaType());
        assertEquals(1, request.orderItems().get(0).quantity());
    }

    @Test
    void shouldSerializeToJson() throws Exception {
        List<OrderItem> items = List.of(
                new OrderItem("Margherita", 1),
                new OrderItem("Hawaiian", 2)
        );
        ProcessOrderRequest request = new ProcessOrderRequest("order-789", items);
        String json = objectMapper.writeValueAsString(request);

        assertTrue(json.contains("\"orderId\":\"order-789\""));
        assertTrue(json.contains("\"orderItems\""));
        assertTrue(json.contains("\"pizzaType\":\"Margherita\""));
        assertTrue(json.contains("\"pizzaType\":\"Hawaiian\""));
    }
}
