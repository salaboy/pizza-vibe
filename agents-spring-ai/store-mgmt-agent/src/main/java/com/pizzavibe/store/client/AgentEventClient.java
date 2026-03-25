package com.pizzavibe.store.client;

import com.pizzavibe.store.model.AgentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AgentEventClient {

    private static final Logger log = LoggerFactory.getLogger(AgentEventClient.class);
    private final RestClient restClient;

    public AgentEventClient(@Value("${store.service.url:http://localhost:8080}") String storeServiceUrl) {
        this.restClient = RestClient.builder().baseUrl(storeServiceUrl).build();
    }

    public void sendEvent(AgentEvent event) {
        try {
            restClient.post()
                    .uri("/agents-events")
                    .body(event)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Failed to send agent event for agentId={}: {}", event.agentId(), e.getMessage());
        }
    }
}
