package com.pizzavibe.store.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import io.quarkiverse.langchain4j.mcp.runtime.McpToolBox;

public interface DrinksAgent {
  @SystemMessage("""
        You are an agent in charge of fetching the drinks to be delivered.
       
        Get all the drinkItems and fetch them from the drinks stock service.
        
        Call getDrinksStock() once. Then call getDrinkItem() for each drink.
                If a drink is unavailable, report failure and STOP.
        """)
  @Agent(name = "Drinks Agent", description = "Get drinks for delivery.",
      outputKey = "drinksReport")
  @McpToolBox("pizza-mcp")
  @UserMessage("Fetch drinks for order {{orderId}} with drink items: {{drinkItems}}")
  String fetchDrinks(@V("orderId") String orderId, @V("drinkItems") String drinkItems);
}
