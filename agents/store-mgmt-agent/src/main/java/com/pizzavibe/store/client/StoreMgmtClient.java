package com.pizzavibe.store.client;

import com.pizzavibe.store.model.PizzaOrderStatus;
import com.pizzavibe.store.model.ProcessOrderRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "store-mgmt-api")
@Path("/mgmt")
public interface StoreMgmtClient {

    @POST
    @Path("/processOrder")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    PizzaOrderStatus processOrder(ProcessOrderRequest request);
}
