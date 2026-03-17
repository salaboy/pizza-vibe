package com.pizzavibe.store.model;

import java.util.List;

public record CookRequest(String orderId, List<OrderItem> orderItems) {
}
