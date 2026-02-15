package com.pizzavibe.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeliverResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldCreateDeliverResponse() {
        DeliverResponse response = new DeliverResponse("order-123", "delivering", "Started delivering 1 item(s)");

        assertEquals("order-123", response.orderId());
        assertEquals("delivering", response.status());
        assertEquals("Started delivering 1 item(s)", response.message());
    }

    @Test
    void shouldDeserializeFromDeliveryServiceResponse() throws Exception {
        // This matches the Go delivery service response format
        String json = "{\"orderId\":\"550e8400-e29b-41d4-a716-446655440000\",\"status\":\"delivering\",\"message\":\"Started delivering 2 item(s)\"}";
        DeliverResponse response = objectMapper.readValue(json, DeliverResponse.class);

        assertEquals("550e8400-e29b-41d4-a716-446655440000", response.orderId());
        assertEquals("delivering", response.status());
        assertEquals("Started delivering 2 item(s)", response.message());
    }

    @Test
    void shouldOmitNullMessageInSerialization() throws Exception {
        DeliverResponse response = new DeliverResponse("order-123", "delivering", null);
        String json = objectMapper.writeValueAsString(response);

        assertFalse(json.contains("\"message\""));
    }
}
