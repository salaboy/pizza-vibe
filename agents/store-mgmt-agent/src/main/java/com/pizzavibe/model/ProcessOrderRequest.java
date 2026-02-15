package com.pizzavibe.model;

import java.util.List;

public record ProcessOrderRequest(String orderId, List<OrderItem> orderItems) {
}
