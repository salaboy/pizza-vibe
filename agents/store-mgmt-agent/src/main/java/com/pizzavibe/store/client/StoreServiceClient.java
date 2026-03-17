package com.pizzavibe.store.client;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "store-api")
@Path("/events")
public interface StoreServiceClient {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    void sendEvent(StoreOrderEvent event);

    record StoreOrderEvent(String orderId, String status, String source, String message) {
    }
}