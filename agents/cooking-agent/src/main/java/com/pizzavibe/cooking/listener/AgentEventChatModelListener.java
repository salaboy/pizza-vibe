package com.pizzavibe.cooking.listener;

import com.pizzavibe.cooking.client.AgentEventClient;
import com.pizzavibe.cooking.model.AgentEvent;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class AgentEventChatModelListener implements ChatModelListener {

    private static final Logger log = LoggerFactory.getLogger(AgentEventChatModelListener.class);
    private static final String AGENT_ID = "cooking-agent";

    @Inject
    @RestClient
    AgentEventClient agentEventClient;

    @Inject
    Instance<AgentContext> agentContext;

    @Override
    public void onRequest(ChatModelRequestContext requestContext) {
        try {
            String orderId = resolveOrderId();
            var messages = requestContext.chatRequest().messages();
            String lastUserMessage = "";
            if (!messages.isEmpty()) {
                var last = messages.get(messages.size() - 1);
                if (last instanceof dev.langchain4j.data.message.UserMessage um) {
                    lastUserMessage = um.singleText();
                } else {
                    lastUserMessage = last.toString();
                }
            }
            String text = truncate(lastUserMessage, 500);
            log.info("Sent agent request event for order {}", AgentEvent.request(AGENT_ID, orderId, text));
            agentEventClient.sendEvent(AgentEvent.request(AGENT_ID, orderId, text));
            log.debug("Sent agent request event for order {}", orderId);
        } catch (Exception e) {
            log.warn("Failed to send agent request event: {}", e.getMessage());
        }
    }

    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        try {
            String orderId = resolveOrderId();
            String text = truncate(responseContext.chatResponse().aiMessage().text(), 500);
            log.info("Sent agent response event for order {}", AgentEvent.response(AGENT_ID, orderId, text));
            agentEventClient.sendEvent(AgentEvent.response(AGENT_ID, orderId, text));
            log.debug("Sent agent response event for order {}", orderId);
        } catch (Exception e) {
            log.warn("Failed to send agent response event: {}", e.getMessage());
        }
    }

    @Override
    public void onError(ChatModelErrorContext errorContext) {
        try {
            String orderId = resolveOrderId();
            String text = errorContext.error().getMessage();
            log.info("Sent agent error event for order {}", AgentEvent.error(AGENT_ID, orderId, text));
            agentEventClient.sendEvent(AgentEvent.error(AGENT_ID, orderId, text));
            log.debug("Sent agent error event for order {}", orderId);
        } catch (Exception e) {
            log.warn("Failed to send agent error event: {}", e.getMessage());
        }
    }

    private String resolveOrderId() {
        try {
            if (agentContext.isResolvable()) {
                return agentContext.get().getOrderId();
            }
        } catch (Exception e) {
            // No request scope active (e.g., background processing)
        }
        return "";
    }

    private static String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}