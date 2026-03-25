package com.pizzavibe.store.a2a;

import io.a2a.client.transport.jsonrpc.JSONRPCTransport;
import io.a2a.spec.Artifact;
import io.a2a.spec.EventKind;
import io.a2a.spec.Message;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.Part;
import io.a2a.spec.Task;
import io.a2a.spec.TextPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CookingA2AClient {

    private static final Logger log = LoggerFactory.getLogger(CookingA2AClient.class);

    private final String cookingAgentUrl;

    public CookingA2AClient(@Value("${cooking.agent.url:http://localhost:8087}") String cookingAgentUrl) {
        this.cookingAgentUrl = cookingAgentUrl;
    }

    public String cook(String orderId, String orderItems) {
        try {
            JSONRPCTransport transport = new JSONRPCTransport(cookingAgentUrl);

            List<Part<?>> parts = List.of(
                    new TextPart(orderId, null),
                    new TextPart(orderItems, null)
            );

            Message message = Message.builder()
                    .role(Message.Role.ROLE_USER)
                    .parts(parts)
                    .build();

            MessageSendParams params = MessageSendParams.builder()
                    .message(message)
                    .build();

            EventKind result = transport.sendMessage(params, null);

            if (result instanceof Task task) {
                return extractTextFromTask(task);
            }

            return "Cooking completed for order " + orderId;
        } catch (Exception e) {
            log.error("Error calling cooking agent via A2A for orderId={}", orderId, e);
            return "COOKING ERROR: " + e.getMessage();
        }
    }

    private String extractTextFromTask(Task task) {
        if (task.artifacts() != null) {
            for (Artifact artifact : task.artifacts()) {
                if (artifact.parts() != null) {
                    for (Part<?> part : artifact.parts()) {
                        if (part instanceof TextPart textPart) {
                            return textPart.text();
                        }
                    }
                }
            }
        }
        return "Task completed";
    }
}
