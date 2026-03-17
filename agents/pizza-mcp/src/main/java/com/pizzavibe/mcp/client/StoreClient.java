package com.pizzavibe.mcp.client;

import com.pizzavibe.mcp.model.StoreOrderEvent;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "store-api")
@Path("/events")
public interface StoreClient {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    void sendEvent(StoreOrderEvent event);
}
