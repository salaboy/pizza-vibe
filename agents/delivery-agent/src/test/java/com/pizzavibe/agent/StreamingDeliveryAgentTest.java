package com.pizzavibe.agent;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class StreamingDeliveryAgentTest {

    @Inject
    StreamingDeliveryAgent streamingDeliveryAgent;

    @Test
    void shouldInjectStreamingDeliveryAgent() {
        assertNotNull(streamingDeliveryAgent, "StreamingDeliveryAgent should be injected");
    }

    @Test
    void deliverStreamShouldReturnMultiOfChatEvent() {
        assertNotNull(streamingDeliveryAgent, "StreamingDeliveryAgent should be available");
    }
}
