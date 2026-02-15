package com.pizzavibe.agent;

import com.pizzavibe.tools.KitchenTools;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.ToolBox;
import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public interface DelegateKitchenAgent {
  @SystemMessage("""
      Your task is to delegate to the Kitchen Service the order sent by the store manager.
      You have a tool called cookOrder that sends a cook request to the Kitchen service.
      Extract the orderId and orderItems from the request and call the cookOrder tool with them.
      Each order item must have a pizzaType (string) and quantity (integer).
      You can call the Kitchen service only once per order. If the request fails report back a detailed error, 
      but do not call it again.
      """)

  @Agent(description = "Kitchen Delegate Agent", outputKey = "kitchenStatus")
  @ToolBox(KitchenTools.class)
  String delegateToKitchenStream(@UserMessage String request);
}
