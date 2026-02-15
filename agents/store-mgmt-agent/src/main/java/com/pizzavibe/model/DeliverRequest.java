package com.pizzavibe.model;

import java.util.List;

public record DeliverRequest(String orderId, List<OrderItem> orderItems) {
}
