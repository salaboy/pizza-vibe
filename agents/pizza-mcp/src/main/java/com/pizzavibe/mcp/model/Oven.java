package com.pizzavibe.mcp.model;

import java.time.Instant;

public record Oven(
    String id,
    String status,
    String user,
    int progress,
    Instant updatedAt
) {
    public static final String STATUS_AVAILABLE = "AVAILABLE";
    public static final String STATUS_RESERVED = "RESERVED";
}
