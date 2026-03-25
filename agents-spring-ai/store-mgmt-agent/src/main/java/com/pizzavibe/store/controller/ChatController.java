package com.pizzavibe.store.controller;

import com.pizzavibe.store.agent.ChatAgentService;
import com.pizzavibe.store.model.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/mgmt/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatAgentService chatAgentService;

    public ChatController(ChatAgentService chatAgentService) {
        this.chatAgentService = chatAgentService;
    }

    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestBody ChatMessage message) {
        log.info("Chat message received: sessionId={}, message={}", message.sessionId(), message.message());
        return chatAgentService.chat(message.sessionId(), message.message());
    }
}
