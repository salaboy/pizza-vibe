package com.pizzavibe.tools;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pizzavibe.model.DeliverRequest;
import com.pizzavibe.model.OrderItem;
import dev.langchain4j.agent.tool.Tool;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

@ApplicationScoped
public class DeliveryTools {

    @ConfigProperty(name = "delivery.service.url", defaultValue = "http://localhost:8082")
    String deliveryServiceUrl;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public DeliveryTools() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    DeliveryTools(HttpClient httpClient, ObjectMapper objectMapper, String deliveryServiceUrl) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.deliveryServiceUrl = deliveryServiceUrl;
    }

    @Tool("Send a delivery request to the Delivery service. Provide the orderId as a string and orderItems as a JSON array string, e.g. '[{\"pizzaType\":\"Margherita\",\"quantity\":2}]'. The delivery service will start delivering asynchronously and return an accepted response with status.")
    public String deliverOrder(String orderId, String orderItems) {
        try {
            List<OrderItem> items = objectMapper.readValue(orderItems, new TypeReference<>() {});
            DeliverRequest deliverRequest = new DeliverRequest(orderId, items);
            String body = objectMapper.writeValueAsString(deliverRequest);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(deliveryServiceUrl + "/deliver"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 202 || response.statusCode() == 200) {
                return response.body();
            }
            return "{\"error\": \"Delivery service returned status " + response.statusCode() + ": " + response.body() + "\"}";
        } catch (Exception e) {
            return "{\"error\": \"Failed to call delivery service: " + e.getMessage() + "\"}";
        }
    }
}
