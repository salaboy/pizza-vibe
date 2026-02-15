package com.pizzavibe.agent;

import com.pizzavibe.model.MgmtUpdate;
import com.pizzavibe.model.OrderStatus;
import dev.langchain4j.agentic.declarative.Output;
import dev.langchain4j.agentic.declarative.SequenceAgent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public interface StoreMgmtAgent {

    @SystemMessage("""
        You are a pizza store manager. Your task is to process one pizza order.
        To do this, you will coordinate with the Kitchen and Delivery services.
        You will receive a pizza order and then delegate the cooking and delivery tasks to the respective services.
        Once the pizza is cooked and delivered, you will mark the order as completed.
        
        """)
    @SequenceAgent(name = "Manage the Pizza store by coordinating the Kitchen and Delivery services",
      subAgents = {DelegateKitchenAgent.class, DelegateDeliveryAgent.class})
    OrderStatus processOrder(@UserMessage String request);

  @Output
  static OrderStatus output(String kitchenStatus, String deliveryStatus) {
    return new OrderStatus(kitchenStatus, deliveryStatus);
  }
}
