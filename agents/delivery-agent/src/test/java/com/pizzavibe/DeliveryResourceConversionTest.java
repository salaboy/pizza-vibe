package com.pizzavibe;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pizzavibe.model.DeliveryUpdate;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.tool.ToolExecution;
import io.quarkiverse.langchain4j.runtime.aiservice.ChatEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeliveryResourceConversionTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void shouldConvertBeforeToolExecutionEventToAction() throws JsonProcessingException {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
            .name("getBikes")
            .arguments("{}")
            .build();
        ChatEvent.BeforeToolExecutionEvent event = new ChatEvent.BeforeToolExecutionEvent(request);

        DeliveryUpdate update = convertEvent(event);

        assertEquals("action", update.type());
        assertEquals("checking_bikes", update.action());
        assertEquals("Checking available bikes for delivery", update.message());
        assertEquals("getBikes", update.toolName());
    }

    @Test
    void shouldConvertReserveBikeEventToReservingBikeAction() throws JsonProcessingException {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
            .name("reserveBike")
            .arguments("{\"bikeId\": \"bike-1\", \"user\": \"delivery-agent-dave\"}")
            .build();
        ChatEvent.BeforeToolExecutionEvent event = new ChatEvent.BeforeToolExecutionEvent(request);

        DeliveryUpdate update = convertEvent(event);

        assertEquals("action", update.type());
        assertEquals("reserving_bike", update.action());
        assertTrue(update.message().contains("Reserving bike"));
        assertEquals("reserveBike", update.toolName());
    }

    @Test
    void shouldConvertGetBikeEventToCheckingBikeStatusAction() throws JsonProcessingException {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
            .name("getBike")
            .arguments("bike-1")
            .build();
        ChatEvent.BeforeToolExecutionEvent event = new ChatEvent.BeforeToolExecutionEvent(request);

        DeliveryUpdate update = convertEvent(event);

        assertEquals("action", update.type());
        assertEquals("checking_bike_status", update.action());
        assertTrue(update.message().contains("bike-1"));
    }

    @Test
    void shouldConvertToolExecutedEventToCompletedAction() throws JsonProcessingException {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
            .name("getBikes")
            .arguments("{}")
            .build();
        ToolExecution execution = ToolExecution.builder()
            .request(request)
            .result("Bikes fetched successfully")
            .build();
        ChatEvent.ToolExecutedEvent event = new ChatEvent.ToolExecutedEvent(execution);

        DeliveryUpdate update = convertEvent(event);

        assertEquals("action", update.type());
        assertEquals("tool_completed", update.action());
        assertTrue(update.message().contains("getBikes"));
    }

    @Test
    void shouldConvertPartialResponseEventToPartialUpdate() throws JsonProcessingException {
        ChatEvent.PartialResponseEvent event = new ChatEvent.PartialResponseEvent("Delivering your order...");

        DeliveryUpdate update = convertEvent(event);

        assertEquals("partial", update.type());
        assertEquals("Delivering your order...", update.message());
    }

    @Test
    void shouldConvertChatCompletedEventToResultUpdate() throws JsonProcessingException {
        AiMessage aiMessage = AiMessage.from("Order order-123 has been delivered successfully!");
        ChatResponse response = ChatResponse.builder()
            .aiMessage(aiMessage)
            .build();
        ChatEvent.ChatCompletedEvent event = new ChatEvent.ChatCompletedEvent(response);

        DeliveryUpdate update = convertEvent(event);

        assertEquals("result", update.type());
        assertEquals("completed", update.action());
        assertEquals("Order order-123 has been delivered successfully!", update.message());
    }

    @Test
    void shouldSerializeToValidJson() throws JsonProcessingException {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
            .name("getBikes")
            .arguments("{}")
            .build();
        ChatEvent.BeforeToolExecutionEvent event = new ChatEvent.BeforeToolExecutionEvent(request);

        DeliveryUpdate update = convertEvent(event);
        String json = objectMapper.writeValueAsString(update);

        JsonNode node = objectMapper.readTree(json);
        assertEquals("action", node.get("type").asText());
        assertEquals("checking_bikes", node.get("action").asText());
        assertNotNull(node.get("message"));
    }

    private DeliveryUpdate convertEvent(ChatEvent event) {
        return switch (event) {
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
    }
}
