package com.pizzavibe.store.model;

import java.time.Instant;

public record AgentEvent(
        String agentId,
        String kind,
        String text,
        Instant timestamp
) {
    public static AgentEvent request(String agentId, String text) {
        return new AgentEvent(agentId, "request", text, Instant.now());
    }

    public static AgentEvent response(String agentId, String text) {
        return new AgentEvent(agentId, "response", text, Instant.now());
    }

    public static AgentEvent error(String agentId, String text) {
        return new AgentEvent(agentId, "error", text, Instant.now());
    }
}
