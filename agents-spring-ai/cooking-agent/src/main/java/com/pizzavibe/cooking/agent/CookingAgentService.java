package com.pizzavibe.cooking.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.stereotype.Service;

@Service
public class CookingAgentService {

    private static final String SYSTEM_PROMPT = """
            You are a pizza cooking agent. Your name is "cooking-agent-joe".
            You cook exactly ONE pizza per request and then STOP.

            # Workflow — follow these 4 steps exactly, in order:

            STEP 1: Call getInventory() once. Then call acquireItem() for each needed ingredient.
                    If any ingredient is unavailable, report failure and STOP.

            STEP 2: Call getOvens() once. Pick the first oven with status AVAILABLE.
                    Call reserveOven() once with the chosen ovenId and your name ("cooking-agent-joe").
                    If none are available, call getOvens() once more. If still none, report failure and STOP.

            STEP 3: Call getOven() once with the ovenId AND the orderId from the request.

            STEP 4: Notify the caller that the pizza was correctly cooked.

            The response to the user must include:
            - The tools that were called and the number of times, for example getInventory: 1, getIngredients: 3, getOvens: 1, reserverOven: 1
            - The ingredients used
            - The oven that was used and the time it took to be cooked
            - If the pizza was cooked correctly.
            """;

    private final ChatClient chatClient;

    public CookingAgentService(ChatClient.Builder chatClientBuilder,
                               SyncMcpToolCallbackProvider toolCallbackProvider) {
        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(toolCallbackProvider.getToolCallbacks())
                .build();
    }

    public String cook(String orderId, String orderItems) {
        String userMessage = "Pizza: " + orderItems + "\nOrder Id: " + orderId;
        return chatClient.prompt()
                .user(userMessage)
                .call()
                .content();
    }
}
