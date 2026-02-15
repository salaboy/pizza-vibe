package com.pizzavibe.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CookResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldCreateCookResponse() {
        CookResponse response = new CookResponse("order-123", "cooking", "Started cooking 2 item(s)");

        assertEquals("order-123", response.orderId());
        assertEquals("cooking", response.status());
        assertEquals("Started cooking 2 item(s)", response.message());
    }

    @Test
    void shouldDeserializeFromKitchenServiceResponse() throws Exception {
        // This matches the Go kitchen service response format
        String json = "{\"orderId\":\"550e8400-e29b-41d4-a716-446655440000\",\"status\":\"cooking\",\"message\":\"Started cooking 1 item(s)\"}";
        CookResponse response = objectMapper.readValue(json, CookResponse.class);

        assertEquals("550e8400-e29b-41d4-a716-446655440000", response.orderId());
        assertEquals("cooking", response.status());
        assertEquals("Started cooking 1 item(s)", response.message());
    }

    @Test
    void shouldOmitNullMessageInSerialization() throws Exception {
        CookResponse response = new CookResponse("order-123", "cooking", null);
        String json = objectMapper.writeValueAsString(response);

        assertFalse(json.contains("\"message\""));
    }
}
