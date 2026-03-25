package com.pizzavibe.store.controller;

import com.pizzavibe.store.agent.PizzaOrderOrchestrator;
import com.pizzavibe.store.model.PizzaOrderStatus;
import com.pizzavibe.store.model.ProcessOrderRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
@RequestMapping("/mgmt")
public class StoreMgmtController {

    private static final Logger log = LoggerFactory.getLogger(StoreMgmtController.class);

    private final PizzaOrderOrchestrator pizzaOrderOrchestrator;

    public StoreMgmtController(PizzaOrderOrchestrator pizzaOrderOrchestrator) {
        this.pizzaOrderOrchestrator = pizzaOrderOrchestrator;
    }

    @GetMapping
    public String hello() {
        return "Hello from Store Management Agent (Spring AI)";
    }

    @PostMapping("/processOrder")
    public PizzaOrderStatus processOrder(@RequestBody ProcessOrderRequest request) {
        log.info("{}", request);
        String pizzas = "";
        if (request.orderItems() != null) {
            pizzas = Arrays.toString(request.orderItems().toArray());
        }
        String drinks = "";
        if (request.drinkItems() != null) {
            drinks = Arrays.toString(request.drinkItems().toArray());
        }
        return pizzaOrderOrchestrator.processOrder(request.orderId(), pizzas, drinks);
    }
}
