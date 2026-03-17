package com.pizzavibe.cooking.model;

import java.time.Instant;

public record AgentEvent(
    String agentId,
    String kind,
    String text,
    Instant timestamp,
    String orderId
) {
    public static AgentEvent request(String agentId, String orderId, String text) {
        return new AgentEvent(agentId, "request", text, Instant.now(), orderId);
    }

    public static AgentEvent response(String agentId, String orderId, String text) {
        return new AgentEvent(agentId, "response", text, Instant.now(), orderId);
    }

    public static AgentEvent error(String agentId, String orderId, String text) {
        return new AgentEvent(agentId, "error", text, Instant.now(), orderId);
    }

  @Override
  public String toString() {
    return "AgentEvent{" +
        "agentId='" + agentId + '\'' +
        ", kind='" + kind + '\'' +
        ", text='" + text + '\'' +
        ", timestamp=" + timestamp +
        ", orderId='" + orderId + '\'' +
        '}';
  }
}