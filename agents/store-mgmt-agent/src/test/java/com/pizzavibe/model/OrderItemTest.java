package com.pizzavibe.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderItemTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldCreateOrderItem() {
        OrderItem item = new OrderItem("Margherita", 2);

        assertEquals("Margherita", item.pizzaType());
        assertEquals(2, item.quantity());
    }

    @Test
    void shouldSerializeToJson() throws Exception {
        OrderItem item = new OrderItem("Pepperoni", 1);
        String json = objectMapper.writeValueAsString(item);

        assertTrue(json.contains("\"pizzaType\":\"Pepperoni\""));
        assertTrue(json.contains("\"quantity\":1"));
    }

    @Test
    void shouldDeserializeFromJson() throws Exception {
        String json = "{\"pizzaType\":\"Hawaiian\",\"quantity\":3}";
        OrderItem item = objectMapper.readValue(json, OrderItem.class);

        assertEquals("Hawaiian", item.pizzaType());
        assertEquals(3, item.quantity());
    }
}
