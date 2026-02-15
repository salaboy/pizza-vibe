package com.pizzavibe.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MgmtUpdateTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldCreateActionUpdate() {
        MgmtUpdate update = MgmtUpdate.action("sending_to_kitchen", "Sending order to Kitchen");

        assertEquals("action", update.type());
        assertEquals("sending_to_kitchen", update.action());
        assertEquals("Sending order to Kitchen", update.message());
        assertNull(update.toolName());
        assertNull(update.toolInput());
    }

    @Test
    void shouldCreateToolExecutionForCookOrder() {
        MgmtUpdate update = MgmtUpdate.toolExecution("cookOrder", "{\"orderId\":\"order-1\"}");

        assertEquals("action", update.type());
        assertEquals("sending_to_kitchen", update.action());
        assertEquals("Sending order to Kitchen service", update.message());
        assertEquals("cookOrder", update.toolName());
        assertEquals("{\"orderId\":\"order-1\"}", update.toolInput());
    }

    @Test
    void shouldCreateToolExecutionForDeliverOrder() {
        MgmtUpdate update = MgmtUpdate.toolExecution("deliverOrder", "{\"orderId\":\"order-1\"}");

        assertEquals("action", update.type());
        assertEquals("sending_to_delivery", update.action());
        assertEquals("Sending order to Delivery service", update.message());
        assertEquals("deliverOrder", update.toolName());
    }

    @Test
    void shouldCreatePartialUpdate() {
        MgmtUpdate update = MgmtUpdate.partial("Processing your order...");

        assertEquals("partial", update.type());
        assertNull(update.action());
        assertEquals("Processing your order...", update.message());
    }

    @Test
    void shouldCreateResultUpdate() {
        MgmtUpdate update = MgmtUpdate.result("Order completed successfully!");

        assertEquals("result", update.type());
        assertEquals("completed", update.action());
        assertEquals("Order completed successfully!", update.message());
    }

    @Test
    void shouldHandleUnknownTool() {
        MgmtUpdate update = MgmtUpdate.toolExecution("unknownTool", "param");

        assertEquals("processing", update.action());
        assertTrue(update.message().contains("unknownTool"));
    }

    @Test
    void shouldOmitNullFieldsInJson() throws Exception {
        MgmtUpdate update = MgmtUpdate.partial("test");
        String json = objectMapper.writeValueAsString(update);

        assertTrue(json.contains("\"type\":\"partial\""));
        assertTrue(json.contains("\"message\":\"test\""));
        assertFalse(json.contains("\"toolName\""));
        assertFalse(json.contains("\"toolInput\""));
        assertFalse(json.contains("\"action\""));
    }

    @Test
    void shouldSerializeToValidJson() throws Exception {
        MgmtUpdate update = MgmtUpdate.toolExecution("cookOrder", "{\"orderId\":\"123\"}");
        String json = objectMapper.writeValueAsString(update);

        var node = objectMapper.readTree(json);
        assertEquals("action", node.get("type").asText());
        assertEquals("sending_to_kitchen", node.get("action").asText());
        assertNotNull(node.get("message"));
        assertEquals("cookOrder", node.get("toolName").asText());
    }
}
