package com.pizzavibe.model;

import java.util.List;

public record CookRequest(String orderId, List<OrderItem> orderItems) {
}
