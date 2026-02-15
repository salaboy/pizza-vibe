package com.pizzavibe.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.Tool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.*;

class DeliveryToolsTest {

    private HttpServer mockServer;
    private DeliveryTools deliveryTools;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private String lastRequestBody;
    private String lastRequestContentType;

    @BeforeEach
    void setUp() throws IOException {
        mockServer = HttpServer.create(new InetSocketAddress(0), 0);
        int port = mockServer.getAddress().getPort();
        String baseUrl = "http://localhost:" + port;

        deliveryTools = new DeliveryTools(
                java.net.http.HttpClient.newHttpClient(),
                objectMapper,
                baseUrl
        );

        lastRequestBody = null;
        lastRequestContentType = null;
    }

    @AfterEach
    void tearDown() {
        if (mockServer != null) {
            mockServer.stop(0);
        }
    }

    @Test
    void shouldHaveToolAnnotation() throws NoSuchMethodException {
        var method = DeliveryTools.class.getMethod("deliverOrder", String.class, String.class);
        Tool toolAnnotation = method.getAnnotation(Tool.class);

        assertNotNull(toolAnnotation, "deliverOrder should have @Tool annotation");
        assertTrue(toolAnnotation.value()[0].contains("Delivery"), "Tool description should mention Delivery");
    }

    @Test
    void shouldSendDeliverRequestToDeliveryService() {
        // Given: mock delivery service that returns 202 Accepted
        String responseJson = "{\"orderId\":\"order-123\",\"status\":\"delivering\",\"message\":\"Started delivering 1 item(s)\"}";
        mockServer.createContext("/deliver", exchange -> {
            lastRequestBody = new String(exchange.getRequestBody().readAllBytes());
            lastRequestContentType = exchange.getRequestHeaders().getFirst("Content-Type");

            exchange.sendResponseHeaders(202, responseJson.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseJson.getBytes());
            }
        });
        mockServer.start();

        // When: pass orderItems as JSON string (as the LLM would)
        String result = deliveryTools.deliverOrder("order-123", "[{\"pizzaType\":\"Margherita\",\"quantity\":1}]");

        // Then
        assertEquals(responseJson, result);
        assertNotNull(lastRequestBody);
        assertTrue(lastRequestBody.contains("\"orderId\":\"order-123\""));
        assertTrue(lastRequestBody.contains("\"orderItems\""));
        assertTrue(lastRequestBody.contains("\"pizzaType\":\"Margherita\""));
        assertEquals("application/json", lastRequestContentType);
    }

    @Test
    void shouldSendCorrectPayloadWithMultipleItems() throws Exception {
        // Given
        mockServer.createContext("/deliver", exchange -> {
            lastRequestBody = new String(exchange.getRequestBody().readAllBytes());
            String resp = "{\"orderId\":\"order-456\",\"status\":\"delivering\",\"message\":\"Started delivering 3 item(s)\"}";
            exchange.sendResponseHeaders(202, resp.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp.getBytes());
            }
        });
        mockServer.start();

        // When
        String orderItems = "[{\"pizzaType\":\"Margherita\",\"quantity\":2},{\"pizzaType\":\"Pepperoni\",\"quantity\":1}]";
        deliveryTools.deliverOrder("order-456", orderItems);

        // Then: verify payload matches Go DeliverRequest structure
        var requestNode = objectMapper.readTree(lastRequestBody);
        assertEquals("order-456", requestNode.get("orderId").asText());
        assertEquals(2, requestNode.get("orderItems").size());
        assertEquals("Margherita", requestNode.get("orderItems").get(0).get("pizzaType").asText());
        assertEquals(2, requestNode.get("orderItems").get(0).get("quantity").asInt());
        assertEquals("Pepperoni", requestNode.get("orderItems").get(1).get("pizzaType").asText());
        assertEquals(1, requestNode.get("orderItems").get(1).get("quantity").asInt());
    }

    @Test
    void shouldHandleDeliveryServiceError() {
        // Given: delivery service returns 400 Bad Request
        mockServer.createContext("/deliver", exchange -> {
            String errorBody = "Order must contain at least one item";
            exchange.sendResponseHeaders(400, errorBody.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(errorBody.getBytes());
            }
        });
        mockServer.start();

        // When
        String result = deliveryTools.deliverOrder("order-789", "[{\"pizzaType\":\"Margherita\",\"quantity\":1}]");

        // Then
        assertTrue(result.contains("\"error\""));
        assertTrue(result.contains("400"));
    }

    @Test
    void shouldHandleConnectionFailure() {
        // Given: no server running
        deliveryTools = new DeliveryTools(
                java.net.http.HttpClient.newHttpClient(),
                objectMapper,
                "http://localhost:1"
        );

        // When
        String result = deliveryTools.deliverOrder("order-000", "[{\"pizzaType\":\"Margherita\",\"quantity\":1}]");

        // Then
        assertTrue(result.contains("\"error\""));
        assertTrue(result.contains("Failed to call delivery service"));
    }

    @Test
    void shouldHandleHttp200AsSuccess() {
        // Given: delivery service returns 200 instead of 202
        String responseJson = "{\"orderId\":\"order-123\",\"status\":\"delivering\"}";
        mockServer.createContext("/deliver", exchange -> {
            exchange.sendResponseHeaders(200, responseJson.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseJson.getBytes());
            }
        });
        mockServer.start();

        // When
        String result = deliveryTools.deliverOrder("order-123", "[{\"pizzaType\":\"Margherita\",\"quantity\":1}]");

        // Then
        assertEquals(responseJson, result);
    }

    @Test
    void shouldHandleInvalidJsonOrderItems() {
        // Given: malformed JSON string for orderItems
        String result = deliveryTools.deliverOrder("order-bad", "not-valid-json");

        // Then
        assertTrue(result.contains("\"error\""));
        assertTrue(result.contains("Failed to call delivery service"));
    }
}
