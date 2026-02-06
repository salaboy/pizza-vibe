package com.pizzavibe.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Represents a streaming update from the cooking agent.
 * These updates inform the client about the current action being performed.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CookingUpdate(
    String type,        // Type of update: "action", "progress", "result", "partial"
    String action,      // The action being performed: "checking_inventory", "acquiring_ingredients",
                        // "reserving_oven", "waiting_for_oven", "cooking", "completed"
    String message,     // Human-readable message describing the update
    String toolName,    // The name of the tool being executed (if applicable)
    String toolInput    // The input to the tool (if applicable)
) {
    public static CookingUpdate action(String action, String message) {
        return new CookingUpdate("action", action, message, null, null);
    }

    public static CookingUpdate toolExecution(String toolName, String toolInput) {
        String action = mapToolToAction(toolName);
        String message = generateToolMessage(toolName, toolInput);
        return new CookingUpdate("action", action, message, toolName, toolInput);
    }

    public static CookingUpdate partial(String message) {
        return new CookingUpdate("partial", null, message, null, null);
    }

    public static CookingUpdate result(String message) {
        return new CookingUpdate("result", "completed", message, null, null);
    }

    private static String mapToolToAction(String toolName) {
        if (toolName == null) {
            return "unknown";
        }
        return switch (toolName.toLowerCase()) {
            case "getinventory" -> "checking_inventory";
            case "getitem" -> "checking_item";
            case "acquireitem" -> "acquiring_ingredients";
            case "getovens" -> "checking_ovens";
            case "getoven" -> "checking_oven_status";
            case "reserveoven" -> "reserving_oven";
            default -> "processing";
        };
    }

    private static String generateToolMessage(String toolName, String toolInput) {
        if (toolName == null) {
            return "Processing...";
        }
        return switch (toolName.toLowerCase()) {
            case "getinventory" -> "Checking available ingredients in inventory";
            case "getitem" -> "Checking specific item: " + toolInput;
            case "acquireitem" -> "Acquiring ingredients: " + toolInput;
            case "getovens" -> "Checking available ovens";
            case "getoven" -> "Checking oven status: " + toolInput;
            case "reserveoven" -> "Reserving oven for cooking: " + toolInput;
            default -> "Processing: " + toolName;
        };
    }
}
