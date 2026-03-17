package com.pizzavibe.store.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChatMessageTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldDeserializeFromJson() throws Exception {
        String json = """
                {
                  "sessionId": "session-123",
                  "message": "I want a pepperoni pizza"
                }
                """;
        ChatMessage msg = mapper.readValue(json, ChatMessage.class);
        assertEquals("session-123", msg.sessionId());
        assertEquals("I want a pepperoni pizza", msg.message());
    }

    @Test
    void shouldSerializeToJson() throws Exception {
        ChatMessage msg = new ChatMessage("session-456", "Two margherita pizzas please");
        String json = mapper.writeValueAsString(msg);
        assertTrue(json.contains("\"sessionId\""));
        assertTrue(json.contains("\"session-456\""));
        assertTrue(json.contains("\"message\""));
        assertTrue(json.contains("Two margherita pizzas please"));
    }

    @Test
    void shouldRoundTrip() throws Exception {
        ChatMessage original = new ChatMessage("round-trip-id", "Hello, I'd like to order");
        String json = mapper.writeValueAsString(original);
        ChatMessage deserialized = mapper.readValue(json, ChatMessage.class);
        assertEquals(original.sessionId(), deserialized.sessionId());
        assertEquals(original.message(), deserialized.message());
    }

    @Test
    void shouldHandleNullSessionId() throws Exception {
        String json = """
                {
                  "sessionId": null,
                  "message": "test"
                }
                """;
        ChatMessage msg = mapper.readValue(json, ChatMessage.class);
        assertNull(msg.sessionId());
        assertEquals("test", msg.message());
    }

    @Test
    void shouldHandleEmptyMessage() throws Exception {
        String json = """
                {
                  "sessionId": "s1",
                  "message": ""
                }
                """;
        ChatMessage msg = mapper.readValue(json, ChatMessage.class);
        assertEquals("s1", msg.sessionId());
        assertEquals("", msg.message());
    }
}