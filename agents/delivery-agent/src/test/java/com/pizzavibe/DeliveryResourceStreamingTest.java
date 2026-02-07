package com.pizzavibe;

import com.pizzavibe.agent.StreamingDeliveryAgent;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import io.quarkiverse.langchain4j.runtime.aiservice.ChatEvent;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Multi;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class DeliveryResourceStreamingTest {

    @InjectMock
    StreamingDeliveryAgent streamingDeliveryAgent;

    @Test
    void shouldStreamToolExecutionEventsAsSse() {
        ChatEvent checkBikes = new ChatEvent.BeforeToolExecutionEvent(
            ToolExecutionRequest.builder()
                .name("getBikes")
                .arguments("{}")
                .build()
        );
        ChatEvent reserveBike = new ChatEvent.BeforeToolExecutionEvent(
            ToolExecutionRequest.builder()
                .name("reserveBike")
                .arguments("{\"bikeId\": \"bike-1\", \"user\": \"delivery-agent-dave\"}")
                .build()
        );
        ChatEvent completed = new ChatEvent.ChatCompletedEvent(
            ChatResponse.builder()
                .aiMessage(AiMessage.from("Order delivered successfully!"))
                .build()
        );

        Mockito.when(streamingDeliveryAgent.deliverStream(Mockito.anyString()))
            .thenReturn(Multi.createFrom().items(checkBikes, reserveBike, completed));

        String body = given()
            .contentType(ContentType.JSON)
            .body("{\"orderId\": \"order-123\"}")
            .when().post("/deliver/stream")
            .then()
            .statusCode(200)
            .contentType("text/event-stream")
            .extract().body().asString();

        assertNotNull(body);
        assertFalse(body.isEmpty(), "SSE body should not be empty");

        assertTrue(body.contains("\"action\":\"checking_bikes\""),
            "Should contain checking_bikes action, got: " + body);
        assertTrue(body.contains("\"action\":\"reserving_bike\""),
            "Should contain reserving_bike action, got: " + body);
        assertTrue(body.contains("\"type\":\"result\""),
            "Should contain result event, got: " + body);
    }

    @Test
    void shouldStreamBikePollingEventsAsSse() {
        ChatEvent checkBikeStatus = new ChatEvent.BeforeToolExecutionEvent(
            ToolExecutionRequest.builder()
                .name("getBike")
                .arguments("bike-1")
                .build()
        );
        ChatEvent completed = new ChatEvent.ChatCompletedEvent(
            ChatResponse.builder()
                .aiMessage(AiMessage.from("Done"))
                .build()
        );

        Mockito.when(streamingDeliveryAgent.deliverStream(Mockito.anyString()))
            .thenReturn(Multi.createFrom().items(checkBikeStatus, completed));

        String body = given()
            .contentType(ContentType.JSON)
            .body("{\"orderId\": \"order-123\"}")
            .when().post("/deliver/stream")
            .then()
            .statusCode(200)
            .extract().body().asString();

        assertTrue(body.contains("\"action\":\"checking_bike_status\""),
            "Should contain checking_bike_status action");
    }

    @Test
    void shouldIncludeToolNameAndInputInSseEvents() {
        ChatEvent event = new ChatEvent.BeforeToolExecutionEvent(
            ToolExecutionRequest.builder()
                .name("reserveBike")
                .arguments("{\"bikeId\": \"bike-2\"}")
                .build()
        );
        ChatEvent completed = new ChatEvent.ChatCompletedEvent(
            ChatResponse.builder()
                .aiMessage(AiMessage.from("Done"))
                .build()
        );

        Mockito.when(streamingDeliveryAgent.deliverStream(Mockito.anyString()))
            .thenReturn(Multi.createFrom().items(event, completed));

        String body = given()
            .contentType(ContentType.JSON)
            .body("{\"orderId\": \"order-123\"}")
            .when().post("/deliver/stream")
            .then()
            .statusCode(200)
            .extract().body().asString();

        assertTrue(body.contains("\"toolName\":\"reserveBike\""),
            "Should contain toolName field");
        assertTrue(body.contains("\"toolInput\":\"{\\\"bikeId\\\": \\\"bike-2\\\"}\""),
            "Should contain toolInput field");
    }

    @Test
    void shouldStreamSseWithDataPrefix() {
        ChatEvent event = new ChatEvent.BeforeToolExecutionEvent(
            ToolExecutionRequest.builder()
                .name("getBikes")
                .arguments("{}")
                .build()
        );
        ChatEvent completed = new ChatEvent.ChatCompletedEvent(
            ChatResponse.builder()
                .aiMessage(AiMessage.from("Done"))
                .build()
        );

        Mockito.when(streamingDeliveryAgent.deliverStream(Mockito.anyString()))
            .thenReturn(Multi.createFrom().items(event, completed));

        String body = given()
            .contentType(ContentType.JSON)
            .body("{\"orderId\": \"order-123\"}")
            .when().post("/deliver/stream")
            .then()
            .statusCode(200)
            .contentType("text/event-stream")
            .extract().body().asString();

        String[] lines = body.split("\n");
        boolean foundDataLine = false;
        for (String line : lines) {
            if (line.startsWith("data:")) {
                foundDataLine = true;
                String json = line.startsWith("data: ") ? line.substring(6) : line.substring(5);
                assertTrue(json.startsWith("{"), "Data line should contain JSON: " + json);
                assertTrue(json.contains("\"type\":"), "JSON should have type field: " + json);
            }
        }
        assertTrue(foundDataLine, "SSE body should contain at least one data: line, got: " + body);
    }
}
