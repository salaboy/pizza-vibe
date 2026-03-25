package com.pizzavibe.delivery.controller;

import com.pizzavibe.delivery.agent.DeliveryAgentService;
import com.pizzavibe.delivery.client.StoreEventClient;
import com.pizzavibe.delivery.model.DeliveryRequest;
import com.pizzavibe.delivery.model.StoreOrderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/deliver")
public class DeliveryController {

    private static final Logger log = LoggerFactory.getLogger(DeliveryController.class);

    private final DeliveryAgentService deliveryAgentService;
    private final StoreEventClient storeEventClient;

    public DeliveryController(DeliveryAgentService deliveryAgentService, StoreEventClient storeEventClient) {
        this.deliveryAgentService = deliveryAgentService;
        this.storeEventClient = storeEventClient;
    }

    @GetMapping
    public String hello() {
        return "Hello from Delivery Agent (Spring AI)";
    }

    @PostMapping
    public String deliverOrder(@RequestBody DeliveryRequest request) {
        try {
            return deliveryAgentService.deliverOrder(request.orderId());
        } catch (Exception e) {
            log.error("Error delivering order for orderId={}", request.orderId(), e);
            storeEventClient.sendEvent(new StoreOrderEvent(request.orderId(), "DELIVERY_ERROR", "delivery", e.getMessage()));
            throw e;
        }
    }
}
