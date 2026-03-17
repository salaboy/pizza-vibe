package com.pizzavibe.delivery;

import com.pizzavibe.delivery.agent.DeliveryAgent;
import com.pizzavibe.delivery.client.StoreClient;
import com.pizzavibe.delivery.listener.AgentContext;
import com.pizzavibe.delivery.model.DeliveryRequest;
import com.pizzavibe.delivery.model.StoreOrderEvent;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@Path("/deliver")
public class DeliveryResource {

    private static final Logger LOG = Logger.getLogger(DeliveryResource.class);

    @Inject
    DeliveryAgent deliveryAgent;

    @Inject
    @RestClient
    StoreClient storeClient;

    @Inject
    AgentContext agentContext;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello from Delivery Agent";
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public String deliverOrderStream(DeliveryRequest request) {
        agentContext.setOrderId(request.orderId());
        try {
            return deliveryAgent.deliverOrder(request.orderId());
        } catch (Exception e) {
            LOG.error("Error delivering order for orderId=" + request.orderId(), e);
            try {
                storeClient.sendEvent(new StoreOrderEvent(request.orderId(), "DELIVERY_ERROR", "delivery", e.getMessage()));
            } catch (Exception ex) {
                LOG.warn("Failed to send DELIVERY_ERROR event to store for orderId=" + request.orderId(), ex);
            }
            throw e;
        }
    }

}
