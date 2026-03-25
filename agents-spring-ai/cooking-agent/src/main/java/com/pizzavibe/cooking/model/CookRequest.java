package com.pizzavibe.cooking.model;

import java.util.List;

public record CookRequest(String orderId, List<OrderItem> orderItems) {
}
