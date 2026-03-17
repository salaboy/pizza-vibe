package com.pizzavibe.mcp.model;

public record StoreOrderEvent(String orderId, String status, String source, String message) {
}
