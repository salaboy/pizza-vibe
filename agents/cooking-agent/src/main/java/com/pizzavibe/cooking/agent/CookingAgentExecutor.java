package com.pizzavibe.cooking.agent;

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.*;
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
            public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
                System.out.println("🍕 ========================================");
                System.out.println("🍕 REMOTE A2A Cooking AGENT CALLED!");
                System.out.println("🍕 ========================================");

                TaskUpdater updater = new TaskUpdater(context, eventQueue);
                if (context.getTask() == null) {
                    updater.submit();
                }
                updater.startWork();

                List<String> inputs = new ArrayList<>();

                // Process the request message
                Message message = context.getMessage();
                System.out.println("📨 Processing message with " + (message.getParts() != null ? message.getParts().size() : 0) + " parts");
                if (message.getParts() != null) {
                    for (Part<?> part : message.getParts()) {
                        if (part instanceof TextPart textPart) {
                            System.out.println("💬 Text part: " + textPart.getText());
                            inputs.add(textPart.getText());
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
                updater.addArtifact(parts, null, null, null);
                updater.complete();
            }

            @Override
            public void cancel(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
                throw new UnsupportedOperationError();
            }
        };
    }
}