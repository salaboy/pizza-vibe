package com.pizzavibe.cooking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pizzavibe.cooking.agent.CookingAgent;
import com.pizzavibe.cooking.client.StoreClient;
import com.pizzavibe.cooking.listener.AgentContext;
import com.pizzavibe.cooking.model.CookRequest;
import com.pizzavibe.cooking.model.StoreOrderEvent;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.Arrays;

@Path("/cook")
public class CookingResource {

  private static final Logger LOG = Logger.getLogger(CookingResource.class);

  @Inject
  CookingAgent cookingAgent;

  @Inject
  ObjectMapper objectMapper;

  @Inject
  @RestClient
  StoreClient storeClient;

  @Inject
  AgentContext agentContext;

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  public String hello() {
    return "Hello from Cooking Agent";
  }


  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public String cookPizza(CookRequest request) {
    try {
      agentContext.setOrderId(request.orderId());
      return cookingAgent.cook(request.orderId(), Arrays.toString(request.orderItems().toArray()));
    } catch (Exception e) {
      LOG.error("Error cooking pizza for orderId=" + request.orderId(), e);
      try {
        storeClient.sendEvent(new StoreOrderEvent(request.orderId(), "COOKING_ERROR", "kitchen", e.getMessage()));
      } catch (Exception ex) {
        LOG.warn("Failed to send COOKING_ERROR event to store for orderId=" + request.orderId(), ex);
      }
      throw e;
    }
  }

}
