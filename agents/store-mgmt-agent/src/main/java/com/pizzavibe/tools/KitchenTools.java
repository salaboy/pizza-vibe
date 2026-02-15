package com.pizzavibe.tools;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pizzavibe.model.CookRequest;
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
public class KitchenTools {

    @ConfigProperty(name = "kitchen.service.url", defaultValue = "http://localhost:8081")
    String kitchenServiceUrl;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public KitchenTools() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    KitchenTools(HttpClient httpClient, ObjectMapper objectMapper, String kitchenServiceUrl) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.kitchenServiceUrl = kitchenServiceUrl;
    }

    @Tool("Send a cook request to the Kitchen service. Provide the orderId as a string and orderItems as a JSON array string, e.g. '[{\"pizzaType\":\"Margherita\",\"quantity\":2}]'. The kitchen will start cooking asynchronously and return an accepted response with status.")
    public String cookOrder(String orderId, String orderItems) {
        try {
            List<OrderItem> items = objectMapper.readValue(orderItems, new TypeReference<>() {});
            CookRequest cookRequest = new CookRequest(orderId, items);
            String body = objectMapper.writeValueAsString(cookRequest);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(kitchenServiceUrl + "/cook"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 202 || response.statusCode() == 200) {
                return response.body();
            }
            return "{\"error\": \"Kitchen service returned status " + response.statusCode() + ": " + response.body() + "\"}";
        } catch (Exception e) {
            return "{\"error\": \"Failed to call kitchen service: " + e.getMessage() + "\"}";
        }
    }
}
