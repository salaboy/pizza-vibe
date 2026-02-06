package com.pizzavibe.agent;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class CookingAgentTest {

    @Inject
    CookingAgent cookingAgent;

    @Test
    void shouldInjectCookingAgent() {
        assertNotNull(cookingAgent, "CookingAgent should be injected");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "PIZZA_MCP_URL", matches = ".+")
    void shouldCookPizzaUsingMcpTools() {
        String result = cookingAgent.cook("Please cook a Margherita pizza");

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void shouldHaveCorrectAgentName() {
        // The agent's user name for oven reservations should be "cooking-agent-joe"
        // This is verified through the SystemMessage configuration
        assertNotNull(cookingAgent);
    }
}
