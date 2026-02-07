package com.pizzavibe.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DeliveryUpdate(
    String type,
    String action,
    String message,
    String toolName,
    String toolInput
) {
    public static DeliveryUpdate action(String action, String message) {
        return new DeliveryUpdate("action", action, message, null, null);
    }

    public static DeliveryUpdate toolExecution(String toolName, String toolInput) {
        String action = mapToolToAction(toolName);
        String message = generateToolMessage(toolName, toolInput);
        return new DeliveryUpdate("action", action, message, toolName, toolInput);
    }

    public static DeliveryUpdate partial(String message) {
        return new DeliveryUpdate("partial", null, message, null, null);
    }

    public static DeliveryUpdate result(String message) {
        return new DeliveryUpdate("result", "completed", message, null, null);
    }

    private static String mapToolToAction(String toolName) {
        if (toolName == null) {
            return "unknown";
        }
        return switch (toolName.toLowerCase()) {
            case "getbikes" -> "checking_bikes";
            case "getbike" -> "checking_bike_status";
            case "reservebike" -> "reserving_bike";
            default -> "processing";
        };
    }

    private static String generateToolMessage(String toolName, String toolInput) {
        if (toolName == null) {
            return "Processing...";
        }
        return switch (toolName.toLowerCase()) {
            case "getbikes" -> "Checking available bikes for delivery";
            case "getbike" -> "Checking bike status: " + toolInput;
            case "reservebike" -> "Reserving bike for delivery: " + toolInput;
            default -> "Processing: " + toolName;
        };
    }
}
