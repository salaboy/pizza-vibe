package com.pizzavibe.delivery.agent;

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
 * Executor for the DeliveryAgent.
 * Handles the integration between the A2A framework and the DeliveryAgent.
 */
@ApplicationScoped
public class DeliveryAgentExecutor {

    @Inject
    DeliveryAgent deliveryAgent;

    @Produces
    public AgentExecutor agentExecutor(DeliveryAgent deliveryAgent) {
        return new AgentExecutor() {
            @Override
            public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
                System.out.println("🚴 ========================================");
                System.out.println("🚴 REMOTE A2A Delivery AGENT CALLED!");
                System.out.println("🚴 ========================================");

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

                System.out.println("📋 Calling DeliveryAgent with " + inputs.size() + " parameters:");
                System.out.println("   - orderId: " + inputs.get(0));

                // Call the agent with all parameters
                String agentResponse = deliveryAgent.deliverOrder(
                        inputs.get(0));                     // delivery request

                System.out.println("✅ DeliveryAgent response: " + agentResponse);
                System.out.println("🚴 ========================================");

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
