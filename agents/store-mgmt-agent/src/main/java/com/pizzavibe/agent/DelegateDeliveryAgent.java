package com.pizzavibe.agent;

import com.pizzavibe.tools.DeliveryTools;
import com.pizzavibe.tools.KitchenTools;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkiverse.langchain4j.ToolBox;
import io.quarkiverse.langchain4j.runtime.aiservice.ChatEvent;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public interface DelegateDeliveryAgent {

  @SystemMessage("""
      Your task is to delegate to the Delivery Service the order sent by the store manager.
      You have a tool called deliverOrder that sends a delivery request to the Delivery service.
      Extract the orderId and orderItems from the request and call the deliverOrder tool with them.
      Each order item must have a pizzaType (string) and quantity (integer).
      Only call the delivery service once per order, if the request fails report back a detailed error, 
      but do not call it again.
      """)

  @Agent(description = "Delivery Delegate Agent", outputKey = "deliveryStatus")
  @ToolBox(DeliveryTools.class)
  String delegateToDeliveryStream(@UserMessage String request);
}
