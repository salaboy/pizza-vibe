package com.pizzavibe.store.model;

public record StoreOrderEvent(String orderId, String status, String source, String message) {
}
