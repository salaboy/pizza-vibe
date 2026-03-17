package com.pizzavibe.cooking.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AgentEventTest {

    @Test
    void shouldCreateRequestEvent() {
        AgentEvent event = AgentEvent.request("cooking-agent", "order-123", "Cook a pepperoni pizza");

        assertEquals("cooking-agent", event.agentId());
        assertEquals("request", event.kind());
        assertEquals("Cook a pepperoni pizza", event.text());
        assertEquals("order-123", event.orderId());
        assertNotNull(event.timestamp());
    }

    @Test
    void shouldCreateResponseEvent() {
        AgentEvent event = AgentEvent.response("cooking-agent", "order-123", "Pizza is ready!");

        assertEquals("cooking-agent", event.agentId());
        assertEquals("response", event.kind());
        assertEquals("Pizza is ready!", event.text());
        assertEquals("order-123", event.orderId());
        assertNotNull(event.timestamp());
    }

    @Test
    void shouldCreateErrorEvent() {
        AgentEvent event = AgentEvent.error("cooking-agent", "order-456", "Oven unavailable");

        assertEquals("cooking-agent", event.agentId());
        assertEquals("error", event.kind());
        assertEquals("Oven unavailable", event.text());
        assertEquals("order-456", event.orderId());
        assertNotNull(event.timestamp());
    }

    @Test
    void shouldHandleEmptyOrderId() {
        AgentEvent event = AgentEvent.request("cooking-agent", "", "hello");

        assertEquals("", event.orderId());
        assertEquals("request", event.kind());
    }
}