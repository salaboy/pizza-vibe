package com.pizzavibe.store.model;

public record PizzaOrderStatus(OrderFinalStatus status, KitchenOrderStatus kitchenReport, String deliveryReport) {
}

