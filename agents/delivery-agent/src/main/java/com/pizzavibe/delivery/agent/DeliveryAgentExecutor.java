package com.pizzavibe.delivery.agent;

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
            public void execute(RequestContext context, AgentEmitter emitter) throws A2AError {
                System.out.println("🚴 ========================================");
                System.out.println("🚴 REMOTE A2A Delivery AGENT CALLED!");
                System.out.println("🚴 ========================================");

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