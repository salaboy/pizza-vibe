package com.pizzavibe.cooking.controller;

import com.pizzavibe.cooking.agent.CookingAgentService;
import com.pizzavibe.cooking.client.StoreEventClient;
import com.pizzavibe.cooking.model.CookRequest;
import com.pizzavibe.cooking.model.StoreOrderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
@RequestMapping("/cook")
public class CookingController {

    private static final Logger log = LoggerFactory.getLogger(CookingController.class);

    private final CookingAgentService cookingAgentService;
    private final StoreEventClient storeEventClient;

    public CookingController(CookingAgentService cookingAgentService, StoreEventClient storeEventClient) {
        this.cookingAgentService = cookingAgentService;
        this.storeEventClient = storeEventClient;
    }

    @GetMapping
    public String hello() {
        return "Hello from Cooking Agent (Spring AI)";
    }

    @PostMapping
    public String cookPizza(@RequestBody CookRequest request) {
        try {
            return cookingAgentService.cook(request.orderId(), Arrays.toString(request.orderItems().toArray()));
        } catch (Exception e) {
            log.error("Error cooking pizza for orderId={}", request.orderId(), e);
            storeEventClient.sendEvent(new StoreOrderEvent(request.orderId(), "COOKING_ERROR", "kitchen", e.getMessage()));
            throw e;
        }
    }
}
