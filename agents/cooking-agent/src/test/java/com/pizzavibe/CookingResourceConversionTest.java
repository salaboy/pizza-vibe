package com.pizzavibe;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pizzavibe.model.CookingUpdate;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.tool.ToolExecution;
import io.quarkiverse.langchain4j.runtime.aiservice.ChatEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the ChatEvent to CookingUpdate conversion logic.
 */
class CookingResourceConversionTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void shouldConvertBeforeToolExecutionEventToAction() throws JsonProcessingException {
        // Given
        ToolExecutionRequest request = ToolExecutionRequest.builder()
            .name("getInventory")
            .arguments("{}")
            .build();
        ChatEvent.BeforeToolExecutionEvent event = new ChatEvent.BeforeToolExecutionEvent(request);

        // When
        CookingUpdate update = convertEvent(event);

        // Then
        assertEquals("action", update.type());
        assertEquals("checking_inventory", update.action());
        assertEquals("Checking available ingredients in inventory", update.message());
        assertEquals("getInventory", update.toolName());
    }

    @Test
    void shouldConvertReserveOvenEventToReservingOvenAction() throws JsonProcessingException {
        // Given
        ToolExecutionRequest request = ToolExecutionRequest.builder()
            .name("reserveOven")
            .arguments("{\"ovenId\": \"oven-1\", \"reservedBy\": \"cooking-agent-joe\"}")
            .build();
        ChatEvent.BeforeToolExecutionEvent event = new ChatEvent.BeforeToolExecutionEvent(request);

        // When
        CookingUpdate update = convertEvent(event);

        // Then
        assertEquals("action", update.type());
        assertEquals("reserving_oven", update.action());
        assertTrue(update.message().contains("Reserving oven"));
        assertEquals("reserveOven", update.toolName());
    }

    @Test
    void shouldConvertGetOvensEventToCheckingOvensAction() throws JsonProcessingException {
        // Given
        ToolExecutionRequest request = ToolExecutionRequest.builder()
            .name("getOvens")
            .arguments("{}")
            .build();
        ChatEvent.BeforeToolExecutionEvent event = new ChatEvent.BeforeToolExecutionEvent(request);

        // When
        CookingUpdate update = convertEvent(event);

        // Then
        assertEquals("action", update.type());
        assertEquals("checking_ovens", update.action());
        assertEquals("Checking available ovens", update.message());
    }

    @Test
    void shouldConvertGetOvenEventToCheckingOvenStatusAction() throws JsonProcessingException {
        // Given
        ToolExecutionRequest request = ToolExecutionRequest.builder()
            .name("getOven")
            .arguments("oven-1")
            .build();
        ChatEvent.BeforeToolExecutionEvent event = new ChatEvent.BeforeToolExecutionEvent(request);

        // When
        CookingUpdate update = convertEvent(event);

        // Then
        assertEquals("action", update.type());
        assertEquals("checking_oven_status", update.action());
        assertTrue(update.message().contains("oven-1"));
    }

    @Test
    void shouldConvertAcquireItemEventToAcquiringIngredientsAction() throws JsonProcessingException {
        // Given
        ToolExecutionRequest request = ToolExecutionRequest.builder()
            .name("acquireItem")
            .arguments("{\"itemName\": \"mozzarella\", \"quantity\": 2}")
            .build();
        ChatEvent.BeforeToolExecutionEvent event = new ChatEvent.BeforeToolExecutionEvent(request);

        // When
        CookingUpdate update = convertEvent(event);

        // Then
        assertEquals("action", update.type());
        assertEquals("acquiring_ingredients", update.action());
        assertTrue(update.message().contains("mozzarella"));
    }

    @Test
    void shouldConvertToolExecutedEventToCompletedAction() throws JsonProcessingException {
        // Given
        ToolExecutionRequest request = ToolExecutionRequest.builder()
            .name("getInventory")
            .arguments("{}")
            .build();
        ToolExecution execution = ToolExecution.builder()
            .request(request)
            .result("Inventory fetched successfully")
            .build();
        ChatEvent.ToolExecutedEvent event = new ChatEvent.ToolExecutedEvent(execution);

        // When
        CookingUpdate update = convertEvent(event);

        // Then
        assertEquals("action", update.type());
        assertEquals("tool_completed", update.action());
        assertTrue(update.message().contains("getInventory"));
    }

    @Test
    void shouldConvertPartialResponseEventToPartialUpdate() throws JsonProcessingException {
        // Given
        ChatEvent.PartialResponseEvent event = new ChatEvent.PartialResponseEvent("Processing your pizza order...");

        // When
        CookingUpdate update = convertEvent(event);

        // Then
        assertEquals("partial", update.type());
        assertEquals("Processing your pizza order...", update.message());
    }

    @Test
    void shouldConvertChatCompletedEventToResultUpdate() throws JsonProcessingException {
        // Given
        AiMessage aiMessage = AiMessage.from("Your Margherita pizza has been cooked successfully!");
        ChatResponse response = ChatResponse.builder()
            .aiMessage(aiMessage)
            .build();
        ChatEvent.ChatCompletedEvent event = new ChatEvent.ChatCompletedEvent(response);

        // When
        CookingUpdate update = convertEvent(event);

        // Then
        assertEquals("result", update.type());
        assertEquals("completed", update.action());
        assertEquals("Your Margherita pizza has been cooked successfully!", update.message());
    }

    @Test
    void shouldSerializeToValidJson() throws JsonProcessingException {
        // Given
        ToolExecutionRequest request = ToolExecutionRequest.builder()
            .name("getOvens")
            .arguments("{}")
            .build();
        ChatEvent.BeforeToolExecutionEvent event = new ChatEvent.BeforeToolExecutionEvent(request);

        // When
        CookingUpdate update = convertEvent(event);
        String json = objectMapper.writeValueAsString(update);

        // Then
        JsonNode node = objectMapper.readTree(json);
        assertEquals("action", node.get("type").asText());
        assertEquals("checking_ovens", node.get("action").asText());
        assertNotNull(node.get("message"));
    }

    private CookingUpdate convertEvent(ChatEvent event) {
        return switch (event) {
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
    }
}
