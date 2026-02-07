package com.pizzavibe;

import com.pizzavibe.agent.StreamingDeliveryAgent;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class DeliveryResourceTest {

    @Inject
    StreamingDeliveryAgent streamingDeliveryAgent;

    @Test
    void testHelloEndpoint() {
        given()
          .when().get("/deliver")
          .then()
             .statusCode(200)
             .body(is("Hello from Delivery Agent"));
    }

    @Test
    void testStreamingDeliveryAgentShouldBeInjected() {
        assertNotNull(streamingDeliveryAgent, "StreamingDeliveryAgent should be injected");
    }
}
