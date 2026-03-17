package com.pizzavibe.store;

import com.pizzavibe.store.listener.AgentContext;
import com.pizzavibe.store.model.DrinkItem;
import com.pizzavibe.store.model.OrderItem;
import com.pizzavibe.store.model.PizzaOrderStatus;
import com.pizzavibe.store.model.ProcessOrderRequest;
import com.pizzavibe.store.workflows.PizzaOrderWorkflow;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

@Path("/mgmt")
public class StoreMgmtResource {

  private static final Logger log = LoggerFactory.getLogger(StoreMgmtResource.class);
  @Inject
    PizzaOrderWorkflow pizzaOrderWorkflowAgent;

  @Inject
    AgentContext agentContext;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello from Store Management Agent";
    }

    @POST
    @Path("/processOrder")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public PizzaOrderStatus processOrder(ProcessOrderRequest request) {
        log.info(request.toString());
      agentContext.setOrderId(request.orderId());
      agentContext.setAgentName("pizza-order-workflow");
      String pizzas = "";
      if (request.orderItems() != null) {
        pizzas = Arrays.toString(request.orderItems().toArray());
      }
      String drinks = "";
      if (request.drinkItems() != null) {
        drinks = Arrays.toString(request.drinkItems().toArray());
      }
      return pizzaOrderWorkflowAgent.processPizzaOrder(request.orderId(), pizzas, drinks);
    }


}
