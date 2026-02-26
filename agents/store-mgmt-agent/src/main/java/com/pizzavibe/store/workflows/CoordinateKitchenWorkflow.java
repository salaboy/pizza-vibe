package com.pizzavibe.store.workflows;

import com.pizzavibe.store.agent.CookingRemoteAgent;
import com.pizzavibe.store.agent.DrinksAgent;
import com.pizzavibe.store.model.KitchenOrderStatus;
import com.pizzavibe.store.model.KitchenStatus;
import dev.langchain4j.agentic.declarative.Output;
import dev.langchain4j.agentic.declarative.ParallelAgent;

public interface CoordinateKitchenWorkflow {

  @ParallelAgent(outputKey = "kitchenReport",
      subAgents = { CookingRemoteAgent.class, DrinksAgent.class })
  KitchenOrderStatus coordinateKitchenAndDrinks();

  @Output
  static KitchenOrderStatus output(String cookingReport, String drinksReport) {
    boolean cookingFailed = false;
    boolean drinksFailed = false;
    KitchenStatus status = KitchenStatus.SUCCESS;
    if (cookingReport == null || cookingReport.contains("ERROR") || cookingReport.contains("FAILED")) {
      cookingFailed = true;
    }
    if (drinksReport == null || drinksReport.contains("ERROR") || drinksReport.contains("FAILED")) {
      drinksFailed = true;
    }
    if (cookingFailed || drinksFailed) {
      status = KitchenStatus.FAILED;
    }
    return new KitchenOrderStatus(status, cookingReport, drinksReport);
  }

}
