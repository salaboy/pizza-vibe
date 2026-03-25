package com.pizzavibe.store.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.stereotype.Service;

@Service
public class DrinksAgentService {

    private static final String SYSTEM_PROMPT = """
            You are an agent in charge of fetching the drinks to be delivered.

            Get all the drinkItems and fetch them from the drinks stock service.

            Call getDrinksStock() once. Then call getDrinkItem() for each drink.
                    If a drink is unavailable, report failure and STOP.
            """;

    private final ChatClient chatClient;

    public DrinksAgentService(ChatClient.Builder chatClientBuilder,
                              SyncMcpToolCallbackProvider toolCallbackProvider) {
        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(toolCallbackProvider.getToolCallbacks())
                .build();
    }

    public String fetchDrinks(String orderId, String drinkItems) {
        String userMessage = "Fetch drinks for order " + orderId + " with drink items: " + drinkItems;
        return chatClient.prompt()
                .user(userMessage)
                .call()
                .content();
    }
}
