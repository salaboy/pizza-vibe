package com.pizzavibe.mcp.client;

import com.pizzavibe.mcp.model.AcquireResponse;
import com.pizzavibe.mcp.model.ItemResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.Map;

@RegisterRestClient(configKey = "inventory-api")
@Path("/inventory")
public interface InventoryClient {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    Map<String, Integer> getAll();

    @GET
    @Path("/{item}")
    @Produces(MediaType.APPLICATION_JSON)
    ItemResponse getItem(@PathParam("item") String item);

    @POST
    @Path("/{item}")
    @Produces(MediaType.APPLICATION_JSON)
    AcquireResponse acquireItem(@PathParam("item") String item);
}
