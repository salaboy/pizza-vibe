package com.pizzavibe.store.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class ChatAgentService {

    private static final String SYSTEM_PROMPT = """
            You are a friendly pizza ordering assistant for Pizza Vibe.
            Your ONLY purpose is to help customers create valid pizza orders with pizza items and drink items.

            When a customer wants to order:
            1. Help them choose pizzas and drinks.
            2. Use getInventory() to check available pizza ingredients and their quantities.
            3. Use getDrinksStock() to check available drinks and their quantities.
            4. Validate that there is enough stock to fulfill the requested items before confirming.
            5. Once the customer confirms the order, use the submitOrder tool to send it for processing.

            Important rules:
            - Always check stock availability BEFORE confirming an order.
            - If an item is out of stock or has insufficient quantity, inform the customer and suggest alternatives.
            - Each pizza requires ingredients from the inventory (e.g., PizzaDough, Mozzarella, Sauce, plus toppings).
            - Drinks are separate items from the drinks stock.
            - If the customer asks about anything NOT related to creating a pizza order,
              politely decline and redirect them to ordering pizzas and drinks.
            - Be helpful, concise, and guide the customer through the ordering process.
            - As part of the order confirmation keep the text short, and highlight the items in the order so the user can approve or change the order
            """;

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    public ChatAgentService(ChatClient.Builder chatClientBuilder,
                            SyncMcpToolCallbackProvider toolCallbackProvider,
                            OrderSubmissionTool orderSubmissionTool) {
        this.chatMemory = MessageWindowChatMemory.builder()
                .maxMessages(20)
                .build();
        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(toolCallbackProvider.getToolCallbacks())
                .defaultTools(orderSubmissionTool)
                .build();
    }

    public Flux<String> chat(String sessionId, String message) {
        return chatClient.prompt()
                .user(message)
                .advisors(MessageChatMemoryAdvisor.builder(chatMemory)
                        .conversationId(sessionId)
                        .build())
                .stream()
                .content();
    }
}
