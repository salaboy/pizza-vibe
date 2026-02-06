package com.pizzavibe;


import com.pizzavibe.agent.StreamingCookingAgent;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class CookingResourceTest {

    @Inject
    StreamingCookingAgent streamingCookingAgent;

    @Test
    void testHelloEndpoint() {
        given()
          .when().get("/cook")
          .then()
             .statusCode(200)
             .body(is("Hello from Cooking Agent"));
    }

    @Test
    void testStreamingCookingAgentShouldBeInjected() {
        // Test that the streaming agent is properly injected
        assertNotNull(streamingCookingAgent, "StreamingCookingAgent should be injected");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "PIZZA_MCP_URL", matches = ".+")
    void testStreamEndpointExists() {
        // Test that the streaming endpoint exists and accepts POST with JSON
        // Only run when MCP server is available
        given()
          .contentType(ContentType.JSON)
          .body("{\"pizzas\": [\"Margherita\"]}")
          .when().post("/cook/stream")
          .then()
             .statusCode(200)
             .contentType("text/event-stream");
    }
}
