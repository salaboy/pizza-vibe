package com.pizzavibe.delivery.model;

public record StoreOrderEvent(String orderId, String status, String source, String message) {
}