package com.pizzavibe.store.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProcessOrderRequestTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldDeserializeWithOrderItemsOnly() throws Exception {
        String json = """
                {
                  "orderId": "abc-123",
                  "orderItems": [
                    {"pizzaType": "margherita", "quantity": 2}
                  ]
                }
                """;
        ProcessOrderRequest request = mapper.readValue(json, ProcessOrderRequest.class);
        assertEquals("abc-123", request.orderId());
        assertEquals(1, request.orderItems().size());
        assertEquals("margherita", request.orderItems().get(0).pizzaType());
        assertEquals(2, request.orderItems().get(0).quantity());
        assertNull(request.drinkItems());
    }

    @Test
    void shouldDeserializeWithOrderItemsAndDrinkItems() throws Exception {
        String json = """
                {
                  "orderId": "abc-456",
                  "orderItems": [
                    {"pizzaType": "pepperoni", "quantity": 1}
                  ],
                  "drinkItems": [
                    {"drinkType": "cola", "quantity": 3}
                  ]
                }
                """;
        ProcessOrderRequest request = mapper.readValue(json, ProcessOrderRequest.class);
        assertEquals("abc-456", request.orderId());
        assertEquals(1, request.orderItems().size());
        assertEquals("pepperoni", request.orderItems().get(0).pizzaType());
        assertNotNull(request.drinkItems());
        assertEquals(1, request.drinkItems().size());
        assertEquals("cola", request.drinkItems().get(0).drinkType());
        assertEquals(3, request.drinkItems().get(0).quantity());
    }

    @Test
    void shouldSerializeToMatchGoStorePayload() throws Exception {
        ProcessOrderRequest request = new ProcessOrderRequest(
                "order-789",
                List.of(new OrderItem("hawaiian", 1)),
                List.of(new DrinkItem("water", 2))
        );
        String json = mapper.writeValueAsString(request);
        assertTrue(json.contains("\"orderId\""));
        assertTrue(json.contains("\"orderItems\""));
        assertTrue(json.contains("\"drinkItems\""));
        assertTrue(json.contains("\"pizzaType\""));
        assertTrue(json.contains("\"drinkType\""));
    }

    @Test
    void shouldSerializeNullDrinkItemsOmitted() throws Exception {
        ProcessOrderRequest request = new ProcessOrderRequest(
                "order-000",
                List.of(new OrderItem("margherita", 1)),
                null
        );
        String json = mapper.writeValueAsString(request);
        assertTrue(json.contains("\"orderId\""));
        assertTrue(json.contains("\"orderItems\""));
    }

    @Test
    void shouldRoundTripWithAllFields() throws Exception {
        ProcessOrderRequest original = new ProcessOrderRequest(
                "round-trip",
                List.of(new OrderItem("veggie", 2), new OrderItem("pepperoni", 1)),
                List.of(new DrinkItem("lemonade", 1), new DrinkItem("cola", 2))
        );
        String json = mapper.writeValueAsString(original);
        ProcessOrderRequest deserialized = mapper.readValue(json, ProcessOrderRequest.class);
        assertEquals(original.orderId(), deserialized.orderId());
        assertEquals(original.orderItems().size(), deserialized.orderItems().size());
        assertEquals(original.drinkItems().size(), deserialized.drinkItems().size());
        assertEquals("veggie", deserialized.orderItems().get(0).pizzaType());
        assertEquals("lemonade", deserialized.drinkItems().get(0).drinkType());
    }
}