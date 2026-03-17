package com.pizzavibe.store.model;

import java.util.List;

public record ProcessOrderRequest(String orderId, List<OrderItem> orderItems, List<DrinkItem> drinkItems) {
}
