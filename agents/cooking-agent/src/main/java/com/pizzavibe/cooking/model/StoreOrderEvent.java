package com.pizzavibe.cooking.model;

public record StoreOrderEvent(String orderId, String status, String source, String message) {
}