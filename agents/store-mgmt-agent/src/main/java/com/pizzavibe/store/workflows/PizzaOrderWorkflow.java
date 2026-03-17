package com.pizzavibe.store.workflows;

import com.pizzavibe.store.agent.DeliveryRemoteAgent;
import com.pizzavibe.store.model.KitchenOrderStatus;
import com.pizzavibe.store.model.KitchenStatus;
import com.pizzavibe.store.model.OrderFinalStatus;
import com.pizzavibe.store.model.PizzaOrderStatus;
import dev.langchain4j.agentic.declarative.Output;
import dev.langchain4j.agentic.declarative.SequenceAgent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface PizzaOrderWorkflow {

  @SequenceAgent(outputKey = "pizzaOrderAgentResult",
      subAgents = {CoordinateKitchenWorkflow.class, DeliveryRemoteAgent.class})
  @UserMessage("Process pizza order {{orderId}} with items {{orderItems}} and drinks {{drinkItems}}")
  PizzaOrderStatus processPizzaOrder(@V("orderId") String orderId,
                                     @V("orderItems") String orderItems,
                                     @V("drinkItems") String drinkItems);

  @Output
  static PizzaOrderStatus output(KitchenOrderStatus kitchenReport, String deliveryReport) {
    boolean kitchenFailed = false;
    boolean deliveryFailed = false;
    OrderFinalStatus status = OrderFinalStatus.SUCCESS;
    if (kitchenReport == null || kitchenReport.status() ==  KitchenStatus.FAILED) {
      kitchenFailed = true;
    }
    if (deliveryReport == null || deliveryReport.contains("ERROR") || deliveryReport.contains("FAILED")) {
      deliveryFailed = true;
    }
    if (kitchenFailed || deliveryFailed) {
      status = OrderFinalStatus.FAILED;
    }
    System.out.println(">>>>>> Order Status: " + status);
    System.out.println("Kitchen Report: " + kitchenReport);
    System.out.println("Delivery Report: " + deliveryReport);
    return new PizzaOrderStatus(status, kitchenReport, deliveryReport);
  }
}
