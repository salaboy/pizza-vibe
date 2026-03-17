package com.pizzavibe.store.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AgentEventTest {

    @Test
    void shouldCreateRequestEvent() {
        AgentEvent event = AgentEvent.request("store-mgmt-agent", "order-123", "I want a pepperoni pizza");

        assertEquals("store-mgmt-agent", event.agentId());
        assertEquals("request", event.kind());
        assertEquals("I want a pepperoni pizza", event.text());
        assertEquals("order-123", event.orderId());
        assertNotNull(event.timestamp());
    }

    @Test
    void shouldCreateResponseEvent() {
        AgentEvent event = AgentEvent.response("store-mgmt-agent", "order-123", "Your pizza is on the way!");

        assertEquals("store-mgmt-agent", event.agentId());
        assertEquals("response", event.kind());
        assertEquals("Your pizza is on the way!", event.text());
        assertEquals("order-123", event.orderId());
        assertNotNull(event.timestamp());
    }

    @Test
    void shouldCreateErrorEvent() {
        AgentEvent event = AgentEvent.error("store-mgmt-agent", "order-456", "Connection timeout");

        assertEquals("store-mgmt-agent", event.agentId());
        assertEquals("error", event.kind());
        assertEquals("Connection timeout", event.text());
        assertEquals("order-456", event.orderId());
        assertNotNull(event.timestamp());
    }

    @Test
    void shouldHandleEmptyOrderId() {
        AgentEvent event = AgentEvent.request("store-mgmt-agent", "", "hello");

        assertEquals("", event.orderId());
        assertEquals("request", event.kind());
    }
}