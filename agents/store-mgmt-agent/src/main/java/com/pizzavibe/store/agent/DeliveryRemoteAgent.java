package com.pizzavibe.store.agent;


import dev.langchain4j.agentic.declarative.A2AClientAgent;
import dev.langchain4j.service.UserMessage;

public interface DeliveryRemoteAgent {
  @A2AClientAgent(a2aServerUrl = "http://delivery-agent:8089",
      outputKey = "deliveryReport",
      name = "Delivery Agent (Remote)",
      description = "Agent that delivers an order.")
  @UserMessage("Deliver order {{orderId}}")
  String deliverOrder(String orderId);

}
