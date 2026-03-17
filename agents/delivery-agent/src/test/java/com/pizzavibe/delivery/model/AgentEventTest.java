package com.pizzavibe.delivery.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AgentEventTest {

    @Test
    void shouldCreateRequestEvent() {
        AgentEvent event = AgentEvent.request("delivery-agent", "order-123", "Deliver pepperoni pizza");

        assertEquals("delivery-agent", event.agentId());
        assertEquals("request", event.kind());
        assertEquals("Deliver pepperoni pizza", event.text());
        assertEquals("order-123", event.orderId());
        assertNotNull(event.timestamp());
    }

    @Test
    void shouldCreateResponseEvent() {
        AgentEvent event = AgentEvent.response("delivery-agent", "order-123", "Pizza delivered!");

        assertEquals("delivery-agent", event.agentId());
        assertEquals("response", event.kind());
        assertEquals("Pizza delivered!", event.text());
        assertEquals("order-123", event.orderId());
        assertNotNull(event.timestamp());
    }

    @Test
    void shouldCreateErrorEvent() {
        AgentEvent event = AgentEvent.error("delivery-agent", "order-456", "Connection timeout");

        assertEquals("delivery-agent", event.agentId());
        assertEquals("error", event.kind());
        assertEquals("Connection timeout", event.text());
        assertEquals("order-456", event.orderId());
        assertNotNull(event.timestamp());
    }

    @Test
    void shouldHandleEmptyOrderId() {
        AgentEvent event = AgentEvent.request("delivery-agent", "", "hello");

        assertEquals("", event.orderId());
        assertEquals("request", event.kind());
    }
}
