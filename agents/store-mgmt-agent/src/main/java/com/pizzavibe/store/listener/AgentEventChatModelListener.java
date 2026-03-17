package com.pizzavibe.store.listener;

import com.pizzavibe.store.client.AgentEventClient;
import com.pizzavibe.store.model.AgentEvent;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
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

import java.util.List;
import java.util.Map;

@ApplicationScoped
public class AgentEventChatModelListener implements ChatModelListener {

    private static final Logger log = LoggerFactory.getLogger(AgentEventChatModelListener.class);
    private static final String DEFAULT_AGENT_ID = "store-mgmt-agent";

    static final Map<String, String> SYSTEM_MESSAGE_AGENT_MAP = Map.of(
            "pizza ordering assistant", "chat-agent",
            "fetching the drinks", "drinks-agent"
    );

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
            String agentId = resolveAgentId(messages);
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

            agentEventClient.sendEvent(AgentEvent.request(agentId, orderId, text));
            log.debug("Sent agent request event: agent={}, order={}", agentId, orderId);
        } catch (Exception e) {
            log.warn("Failed to send agent request event: {}", e.getMessage());
        }
    }

    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        try {
            String orderId = resolveOrderId();
            var messages = responseContext.chatRequest().messages();
            String agentId = resolveAgentId(messages);
            String text = truncate(responseContext.chatResponse().aiMessage().text(), 500);

            agentEventClient.sendEvent(AgentEvent.response(agentId, orderId, text));
            log.debug("Sent agent response event: agent={}, order={}", agentId, orderId);
        } catch (Exception e) {
            log.warn("Failed to send agent response event: {}", e.getMessage());
        }
    }

    @Override
    public void onError(ChatModelErrorContext errorContext) {
        try {
            String orderId = resolveOrderId();
            var messages = errorContext.chatRequest().messages();
            String agentId = resolveAgentId(messages);
            String text = errorContext.error().getMessage();

            agentEventClient.sendEvent(AgentEvent.error(agentId, orderId, text));
            log.debug("Sent agent error event: agent={}, order={}", agentId, orderId);
        } catch (Exception e) {
            log.warn("Failed to send agent error event: {}", e.getMessage());
        }
    }

    String resolveAgentId(List<ChatMessage> messages) {
        // Try to detect the specific sub-agent from the system message
        for (ChatMessage message : messages) {
            if (message instanceof SystemMessage sm) {
                String sysText = sm.text().toLowerCase();
                for (var entry : SYSTEM_MESSAGE_AGENT_MAP.entrySet()) {
                    if (sysText.contains(entry.getKey())) {
                        return entry.getValue();
                    }
                }
            }
        }
        // Fall back to the agent name set in the request context
        try {
            if (agentContext.isResolvable()) {
                return agentContext.get().getAgentName();
            }
        } catch (Exception e) {
            // No request scope active
        }
        return DEFAULT_AGENT_ID;
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

    static String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}
