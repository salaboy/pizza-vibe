package com.pizzavibe.model;

public record DeliveryResult(
    String orderId,
    String bikeId,
    String status,
    String message
) {
    public static DeliveryResult success(String orderId, String bikeId) {
        return new DeliveryResult(orderId, bikeId, "DELIVERED", "Order " + orderId + " delivered successfully using " + bikeId);
    }

    public static DeliveryResult failure(String orderId, String reason) {
        return new DeliveryResult(orderId, null, "FAILED", reason);
    }
}
