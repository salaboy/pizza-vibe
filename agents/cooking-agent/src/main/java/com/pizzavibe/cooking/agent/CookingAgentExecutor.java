package com.pizzavibe.cooking.agent;

import com.pizzavibe.cooking.model.OrderItem;
import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.tasks.AgentEmitter;
import io.a2a.spec.A2AError;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.TextPart;
import io.a2a.spec.UnsupportedOperationError;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

/**
 * Executor for the CookingAgent.
 * Handles the integration between the A2A framework and the CookingAgent.
 */
@ApplicationScoped
public class CookingAgentExecutor {

    @Inject
    CookingAgent cookingAgent;

    @Produces
    public AgentExecutor agentExecutor(CookingAgent cookingAgent) {
        return new AgentExecutor() {
            @Override
            public void execute(RequestContext context, AgentEmitter emitter) throws A2AError {
                System.out.println("🍕 ========================================");
                System.out.println("🍕 REMOTE A2A Cooking AGENT CALLED!");
                System.out.println("🍕 ========================================");

                if (context.getTask() == null) {
                    emitter.submit();
                }
                emitter.startWork();

                List<String> inputs = new ArrayList<>();

                // Process the request message
                Message message = context.getMessage();
                System.out.println("📨 Processing message with " + (message.parts() != null ? message.parts().size() : 0) + " parts");
                if (message.parts() != null) {
                    for (Part<?> part : message.parts()) {
                        if (part instanceof TextPart textPart) {
                            System.out.println("💬 Text part: " + textPart.text());
                            inputs.add(textPart.text());
                        }
                    }
                }

                System.out.println("📋 Calling CookingAgent with " + inputs.size() + " parameters:");
                System.out.println("   - orderId: " + inputs.get(0));
                System.out.println("   - OrderItems: " + inputs.get(1));

                // Call the agent with all parameters
                String agentResponse = cookingAgent.cook(
                        inputs.get(0),
                        inputs.get(1));                     // cooking request

                System.out.println("✅ CookingAgent response: " + agentResponse);
                System.out.println("🍕 ========================================");

                // Return the result
                TextPart responsePart = new TextPart(agentResponse, null);
                List<Part<?>> parts = List.of(responsePart);
                emitter.addArtifact(parts, null, null, null);
                emitter.complete();
            }

            @Override
            public void cancel(RequestContext context, AgentEmitter emitter) throws A2AError {
                throw new UnsupportedOperationError();
            }
        };
    }
}