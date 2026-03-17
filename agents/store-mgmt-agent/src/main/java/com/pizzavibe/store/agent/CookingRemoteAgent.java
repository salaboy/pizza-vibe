package com.pizzavibe.store.agent;

import dev.langchain4j.agentic.declarative.A2AClientAgent;
import dev.langchain4j.service.UserMessage;

public interface CookingRemoteAgent {

  @A2AClientAgent(a2aServerUrl = "http://cooking-agent:8087",
      outputKey = "cookingReport",
      name = "Cooking Agent (Remote)",
      description = "Agent that coordinate the cooking of an order.")
  @UserMessage("Cook order {{orderId}} with items {{orderItems}}")
  String cook(String orderId, String orderItems);
}
