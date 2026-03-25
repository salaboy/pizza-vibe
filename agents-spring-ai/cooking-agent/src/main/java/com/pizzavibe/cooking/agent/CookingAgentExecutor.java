package com.pizzavibe.cooking.agent;

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.tasks.AgentEmitter;
import io.a2a.spec.A2AError;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.TextPart;
import io.a2a.spec.UnsupportedOperationError;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CookingAgentExecutor implements AgentExecutor {

    private final CookingAgentService cookingAgentService;

    public CookingAgentExecutor(CookingAgentService cookingAgentService) {
        this.cookingAgentService = cookingAgentService;
    }

    @Override
    public void execute(RequestContext context, AgentEmitter emitter) throws A2AError {
        System.out.println("========================================");
        System.out.println("REMOTE A2A Cooking AGENT CALLED!");
        System.out.println("========================================");

        if (context.getTask() == null) {
            emitter.submit();
        }
        emitter.startWork();

        List<String> inputs = new ArrayList<>();

        Message message = context.getMessage();
        System.out.println("Processing message with " + (message.parts() != null ? message.parts().size() : 0) + " parts");
        if (message.parts() != null) {
            for (Part<?> part : message.parts()) {
                if (part instanceof TextPart textPart) {
                    System.out.println("Text part: " + textPart.text());
                    inputs.add(textPart.text());
                }
            }
        }

        System.out.println("Calling CookingAgent with " + inputs.size() + " parameters:");
        System.out.println("   - orderId: " + inputs.get(0));
        System.out.println("   - OrderItems: " + inputs.get(1));

        String agentResponse = cookingAgentService.cook(inputs.get(0), inputs.get(1));

        System.out.println("CookingAgent response: " + agentResponse);
        System.out.println("========================================");

        TextPart responsePart = new TextPart(agentResponse, null);
        List<Part<?>> parts = List.of(responsePart);
        emitter.addArtifact(parts, null, null, null);
        emitter.complete();
    }

    @Override
    public void cancel(RequestContext context, AgentEmitter emitter) throws A2AError {
        throw new UnsupportedOperationError();
    }
}
