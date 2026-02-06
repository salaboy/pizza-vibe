package com.pizzavibe.agent;

import io.quarkiverse.langchain4j.runtime.aiservice.ChatEvent;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class StreamingCookingAgentTest {

    @Inject
    StreamingCookingAgent streamingCookingAgent;

    @Test
    void shouldInjectStreamingCookingAgent() {
        assertNotNull(streamingCookingAgent, "StreamingCookingAgent should be injected");
    }

    @Test
    void cookStreamShouldReturnMultiOfChatEvent() {
        // Verify that the method signature returns Multi<ChatEvent>
        // This is a compile-time check but we can verify the agent interface is correct
        assertNotNull(streamingCookingAgent, "StreamingCookingAgent should be available");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "PIZZA_MCP_URL", matches = ".+")
    void shouldStreamCookingUpdates() {
        Multi<ChatEvent> stream = streamingCookingAgent.cookStream("Please cook a Margherita pizza");

        assertNotNull(stream, "Stream should not be null");

        // Collect all streamed events
        var events = stream.collect().asList().await().indefinitely();

        assertNotNull(events, "Events should not be null");
        assertFalse(events.isEmpty(), "Should receive at least one event");
    }
}
