package com.pizzavibe.store.model;

import java.util.List;

public record DeliverRequest(String orderId, List<OrderItem> orderItems) {
}
