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
class DeliveryResourceSseFormatTest {

    @InjectMock
    StreamingDeliveryAgent streamingDeliveryAgent;

    @Test
    void shouldProduceSseEventsWithDataPrefixAndBlankLineSeparators() {
        ChatEvent action = new ChatEvent.BeforeToolExecutionEvent(
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
            .thenReturn(Multi.createFrom().items(action, completed));

        String body = given()
            .contentType(ContentType.JSON)
            .body("{\"orderId\": \"order-123\"}")
            .when().post("/deliver/stream")
            .then()
            .statusCode(200)
            .extract().body().asString();

        assertTrue(body.contains("data:"), "SSE body must contain data: lines");

        long dataLineCount = body.lines()
            .filter(line -> line.startsWith("data:"))
            .count();
        assertEquals(2, dataLineCount, "Should have exactly 2 data lines (action + result)");
    }

    @Test
    void shouldProduceValidJsonInEachSseDataLine() {
        ChatEvent action = new ChatEvent.BeforeToolExecutionEvent(
            ToolExecutionRequest.builder()
                .name("reserveBike")
                .arguments("{\"bikeId\": \"bike-1\"}")
                .build()
        );
        ChatEvent completed = new ChatEvent.ChatCompletedEvent(
            ChatResponse.builder()
                .aiMessage(AiMessage.from("Done"))
                .build()
        );

        Mockito.when(streamingDeliveryAgent.deliverStream(Mockito.anyString()))
            .thenReturn(Multi.createFrom().items(action, completed));

        String body = given()
            .contentType(ContentType.JSON)
            .body("{\"orderId\": \"order-123\"}")
            .when().post("/deliver/stream")
            .then()
            .statusCode(200)
            .extract().body().asString();

        body.lines()
            .filter(line -> line.startsWith("data:"))
            .forEach(line -> {
                String json = line.startsWith("data: ") ? line.substring(6) : line.substring(5);
                assertTrue(json.startsWith("{") && json.endsWith("}"),
                    "Data line should contain JSON object: " + json);
                assertTrue(json.contains("\"type\""),
                    "JSON should have type field: " + json);
            });
    }
}
