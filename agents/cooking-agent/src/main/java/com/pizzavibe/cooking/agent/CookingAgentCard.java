package com.pizzavibe.cooking.agent;

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
public class CookingAgentCard {

    @Produces
    @PublicAgentCard
    public AgentCard agentCard() {
        return AgentCard.builder()
                .name("Cooking Agent")
                .description("Cooks pizza using the inventory and oven services via MCP.")
                .version("1.0.0")
                .capabilities(AgentCapabilities.builder()
                        .streaming(true)
                        .pushNotifications(false)
                        .build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of(AgentSkill.builder()
                                .id("cooking")
                                .name("Cooking agent")
                                .description("Cooks pizza using the inventory and oven services via MCP.")
                                .tags(List.of("cook", "pizza"))
                                .build()))
                .supportedInterfaces(List.of(
                        new AgentInterface(TransportProtocol.JSONRPC.asString(), "http://cooking-agent:8087/")))
                .build();
    }
}