package com.pizzavibe;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pizzavibe.agent.CookingAgent;
import com.pizzavibe.agent.StreamingCookingAgent;
import com.pizzavibe.model.CookRequest;
import com.pizzavibe.model.CookingUpdate;
import io.quarkiverse.langchain4j.runtime.aiservice.ChatEvent;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/cook")
public class CookingResource {


    @Inject
    CookingAgent cookingAgent;

    @Inject
    StreamingCookingAgent streamingCookingAgent;

    @Inject
    ObjectMapper objectMapper;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello from Cooking Agent";
    }


    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String cookPizzasWithAgent(CookRequest request) {
        String pizzaList = String.join(", ", request.pizzas());
        return cookingAgent.cook("Please cook the following pizzas: " + pizzaList);
    }

    @POST
    @Path("/stream")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Multi<String> cookPizzasStream(CookRequest request) {
        String pizzaList = String.join(", ", request.pizzas());
        return streamingCookingAgent.cookStream("Please cook the following pizzas: " + pizzaList)
            .map(this::convertToJson);
    }

    private String convertToJson(ChatEvent event) {
        CookingUpdate update = switch (event) {
            case ChatEvent.BeforeToolExecutionEvent toolEvent ->
                CookingUpdate.toolExecution(toolEvent.getRequest().name(), toolEvent.getRequest().arguments());
            case ChatEvent.ToolExecutedEvent toolExecuted ->
                CookingUpdate.action("tool_completed", "Tool execution completed: " + toolExecuted.getExecution().request().name());
            case ChatEvent.PartialResponseEvent partial ->
                CookingUpdate.partial(partial.getChunk());
            case ChatEvent.ChatCompletedEvent completed ->
                CookingUpdate.result(completed.getChatResponse().aiMessage().text());
            default -> CookingUpdate.partial("Processing...");
        };

        try {
            return objectMapper.writeValueAsString(update);
        } catch (JsonProcessingException e) {
            return "{\"type\":\"error\",\"message\":\"" + e.getMessage() + "\"}";
        }
    }
}
