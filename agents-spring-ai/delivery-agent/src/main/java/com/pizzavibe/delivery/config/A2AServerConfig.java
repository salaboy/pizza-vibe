package com.pizzavibe.delivery.config;

import com.pizzavibe.delivery.agent.DeliveryAgentExecutor;
import io.a2a.server.events.InMemoryQueueManager;
import io.a2a.server.events.MainEventBus;
import io.a2a.server.events.MainEventBusProcessor;
import io.a2a.server.requesthandlers.DefaultRequestHandler;
import io.a2a.server.requesthandlers.RequestHandler;
import io.a2a.server.tasks.InMemoryPushNotificationConfigStore;
import io.a2a.server.tasks.InMemoryTaskStore;
import io.a2a.transport.jsonrpc.handler.JSONRPCHandler;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentInterface;
import io.a2a.spec.AgentSkill;
import io.a2a.spec.TransportProtocol;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.Executors;

@Configuration
public class A2AServerConfig {

    @Bean
    public AgentCard agentCard() {
        return AgentCard.builder()
                .name("Delivery Agent")
                .description("Delivery orders using the bikes skill.")
                .version("1.0.0")
                .capabilities(AgentCapabilities.builder()
                        .streaming(true)
                        .pushNotifications(false)
                        .build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of(AgentSkill.builder()
                        .id("delivery")
                        .name("Delivery agent")
                        .description("Delivers orders using the bikes skill.")
                        .tags(List.of("deliver", "pizza"))
                        .build()))
                .supportedInterfaces(List.of(
                        new AgentInterface(TransportProtocol.JSONRPC.asString(), "http://delivery-agent:8089/")))
                .build();
    }

    @Bean
    public JSONRPCHandler jsonRPCHandler(AgentCard agentCard, DeliveryAgentExecutor agentExecutor) {
        var executor = Executors.newVirtualThreadPerTaskExecutor();
        var taskStore = new InMemoryTaskStore();
        var mainEventBus = new MainEventBus();
        var queueManager = new InMemoryQueueManager(taskStore, mainEventBus);
        var pushNotifStore = new InMemoryPushNotificationConfigStore();
        var eventBusProcessor = new MainEventBusProcessor(mainEventBus, taskStore, null, queueManager);

        RequestHandler requestHandler = DefaultRequestHandler.create(
                agentExecutor, taskStore, queueManager, pushNotifStore, eventBusProcessor,
                executor, executor);

        return new JSONRPCHandler(agentCard, requestHandler, executor);
    }
}
