package com.pizzavibe.store.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChatResponseTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldDeserializeFromJson() throws Exception {
        String json = """
                {
                  "sessionId": "session-123",
                  "response": "Welcome to Pizza Vibe!"
                }
                """;
        ChatResponse resp = mapper.readValue(json, ChatResponse.class);
        assertEquals("session-123", resp.sessionId());
        assertEquals("Welcome to Pizza Vibe!", resp.response());
    }

    @Test
    void shouldSerializeToJson() throws Exception {
        ChatResponse resp = new ChatResponse("session-456", "Your order has been placed!");
        String json = mapper.writeValueAsString(resp);
        assertTrue(json.contains("\"sessionId\""));
        assertTrue(json.contains("\"session-456\""));
        assertTrue(json.contains("\"response\""));
        assertTrue(json.contains("Your order has been placed!"));
    }

    @Test
    void shouldRoundTrip() throws Exception {
        ChatResponse original = new ChatResponse("rt-id", "Here are the available pizzas");
        String json = mapper.writeValueAsString(original);
        ChatResponse deserialized = mapper.readValue(json, ChatResponse.class);
        assertEquals(original.sessionId(), deserialized.sessionId());
        assertEquals(original.response(), deserialized.response());
    }
}