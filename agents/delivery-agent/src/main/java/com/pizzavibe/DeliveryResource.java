package com.pizzavibe;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pizzavibe.agent.StreamingDeliveryAgent;
import com.pizzavibe.model.DeliveryRequest;
import com.pizzavibe.model.DeliveryUpdate;
import io.quarkiverse.langchain4j.runtime.aiservice.ChatEvent;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/deliver")
public class DeliveryResource {

    @Inject
    StreamingDeliveryAgent streamingDeliveryAgent;

    @Inject
    ObjectMapper objectMapper;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello from Delivery Agent";
    }

    @POST
    @Path("/stream")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Multi<String> deliverOrderStream(DeliveryRequest request) {
        return streamingDeliveryAgent.deliverStream("Please deliver the following order: " + request.orderId())
            .map(this::convertToJson);
    }

    private String convertToJson(ChatEvent event) {
        DeliveryUpdate update = switch (event) {
            case ChatEvent.BeforeToolExecutionEvent toolEvent ->
                DeliveryUpdate.toolExecution(toolEvent.getRequest().name(), toolEvent.getRequest().arguments());
            case ChatEvent.ToolExecutedEvent toolExecuted ->
                DeliveryUpdate.action("tool_completed", "Tool execution completed: " + toolExecuted.getExecution().request().name());
            case ChatEvent.PartialResponseEvent partial ->
                DeliveryUpdate.partial(partial.getChunk());
            case ChatEvent.ChatCompletedEvent completed ->
                DeliveryUpdate.result(completed.getChatResponse().aiMessage().text());
            default -> DeliveryUpdate.partial("Processing...");
        };

        try {
            return objectMapper.writeValueAsString(update);
        } catch (JsonProcessingException e) {
            return "{\"type\":\"error\",\"message\":\"" + e.getMessage() + "\"}";
        }
    }
}
