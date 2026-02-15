package com.pizzavibe;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pizzavibe.agent.StoreMgmtAgent;
import com.pizzavibe.model.MgmtUpdate;
import com.pizzavibe.model.OrderStatus;
import com.pizzavibe.model.ProcessOrderRequest;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/mgmt")
public class StoreMgmtResource {

    @Inject
    StoreMgmtAgent storeMgmtAgent;

    @Inject
    ObjectMapper objectMapper;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello from Store Management Agent";
    }

    @POST
    @Path("/processOrder")
    @Consumes(MediaType.APPLICATION_JSON)
    public OrderStatus processOrder(ProcessOrderRequest request) {
        String userMessage = "Process order " + request.orderId()
                + " with items: " + request.orderItems();
        return storeMgmtAgent.processOrder(userMessage);
    }


}
