package com.pizzavibe.mcp.model;

public record AcquireResponse(
    String item,
    String status,
    int remainingQuantity
) {
    public static final String STATUS_ACQUIRED = "ACQUIRED";
    public static final String STATUS_EMPTY = "EMPTY";
}
