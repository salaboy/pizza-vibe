package com.pizzavibe.mcp.client;

import com.pizzavibe.mcp.model.Oven;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

@RegisterRestClient(configKey = "oven-api")
@Path("/ovens")
public interface OvenClient {

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    List<Oven> getAll();

    @GET
    @Path("/{ovenId}")
    @Produces(MediaType.APPLICATION_JSON)
    Oven getById(@PathParam("ovenId") String ovenId);

    @POST
    @Path("/{ovenId}")
    @Produces(MediaType.APPLICATION_JSON)
    Oven reserve(@PathParam("ovenId") String ovenId, @QueryParam("user") String user);
}
