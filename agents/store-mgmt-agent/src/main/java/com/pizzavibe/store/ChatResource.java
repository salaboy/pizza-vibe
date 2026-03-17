package com.pizzavibe.store;

import com.pizzavibe.store.agent.ChatAgent;
import com.pizzavibe.store.listener.AgentContext;
import com.pizzavibe.store.model.ChatMessage;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestStreamElementType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/mgmt/chat")
public class ChatResource {

    private static final Logger log = LoggerFactory.getLogger(ChatResource.class);

    @Inject
    ChatAgent chatAgent;

    @Inject
    AgentContext agentContext;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.TEXT_PLAIN)
    public Multi<String> chat(ChatMessage message) {
        log.info("Chat message received: sessionId={}, message={}", message.sessionId(), message.message());
        agentContext.setAgentName("chat-agent");
        return chatAgent.chat(message.sessionId(), message.message());
    }
}