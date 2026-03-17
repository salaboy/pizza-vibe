package com.pizzavibe.delivery.agent;

import io.a2a.server.PublicAgentCard;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentInterface;
import io.a2a.spec.AgentSkill;
import io.a2a.spec.TransportProtocol;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import java.util.List;

@ApplicationScoped
public class DeliveryAgentCard {

    @Produces
    @PublicAgentCard
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
}