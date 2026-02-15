package com.pizzavibe.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MgmtUpdate(
    String type,
    String action,
    String message,
    String toolName,
    String toolInput
) {
    public static MgmtUpdate action(String action, String message) {
        return new MgmtUpdate("action", action, message, null, null);
    }

    public static MgmtUpdate toolExecution(String toolName, String toolInput) {
        String action = mapToolToAction(toolName);
        String message = generateToolMessage(toolName, toolInput);
        return new MgmtUpdate("action", action, message, toolName, toolInput);
    }

    public static MgmtUpdate partial(String message) {
        return new MgmtUpdate("partial", null, message, null, null);
    }

    public static MgmtUpdate result(String message) {
        return new MgmtUpdate("result", "completed", message, null, null);
    }

    private static String mapToolToAction(String toolName) {
        if (toolName == null) {
            return "unknown";
        }
        return switch (toolName.toLowerCase()) {
            case "cookorder" -> "sending_to_kitchen";
            case "deliverorder" -> "sending_to_delivery";
            default -> "processing";
        };
    }

    private static String generateToolMessage(String toolName, String toolInput) {
        if (toolName == null) {
            return "Processing...";
        }
        return switch (toolName.toLowerCase()) {
            case "cookorder" -> "Sending order to Kitchen service";
            case "deliverorder" -> "Sending order to Delivery service";
            default -> "Processing: " + toolName;
        };
    }
}
