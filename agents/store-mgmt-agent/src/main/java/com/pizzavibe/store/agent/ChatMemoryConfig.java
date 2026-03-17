package com.pizzavibe.store.agent;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ChatMemoryConfig implements ChatMemoryProvider {

    @Override
    public dev.langchain4j.memory.ChatMemory get(Object memoryId) {
        return MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(20)
                .build();
    }
}