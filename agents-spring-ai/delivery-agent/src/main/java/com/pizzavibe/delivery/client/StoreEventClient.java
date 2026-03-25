package com.pizzavibe.delivery.client;

import com.pizzavibe.delivery.model.StoreOrderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class StoreEventClient {

    private static final Logger log = LoggerFactory.getLogger(StoreEventClient.class);
    private final RestClient restClient;

    public StoreEventClient(@Value("${store.service.url:http://localhost:8080}") String storeServiceUrl) {
        this.restClient = RestClient.builder().baseUrl(storeServiceUrl).build();
    }

    public void sendEvent(StoreOrderEvent event) {
        try {
            restClient.post()
                    .uri("/events")
                    .body(event)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Failed to send event to store for orderId={}: {}", event.orderId(), e.getMessage());
        }
    }
}
