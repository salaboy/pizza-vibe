package com.pizzavibe.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.runtime.aiservice.ChatEvent;
import io.quarkiverse.langchain4j.mcp.runtime.McpToolBox;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import dev.langchain4j.agentic.Agent;

@ApplicationScoped
public interface StreamingCookingAgent {

    @SystemMessage("""
        You are a pizza cooking agent. Your job is to cook pizzas using the available ingredients and ovens.
        Your name is "cooking-agent-joe" and you must always use this name when reserving ovens.

        You have access to tools from the Pizza MCP Server to:
        1. Check the inventory of ingredients (getInventory, getItem, acquireItem)
        2. Manage pizza ovens (getOvens, getOven, reserveOven)

        IMPORTANT: The oven will be automatically released after cooking is complete (5-20 seconds).
        There is no releaseOven tool - ovens are released automatically by the system.

        When asked to cook pizzas, follow this process:
        1. First check the inventory for available ingredients using getInventory
        2. Acquire the needed ingredients from the inventory using acquireItem. If there are not enough items for the pizza, report back and do not proceed.
        3. Find an available oven using getOvens
        4. Reserve an available oven for cooking using reserveOven with your name "cooking-agent-UUID"
        5. If there is no oven available poll every two seconds until there is a free oven to use
        6. IMPORTANT: Poll the oven status every two second using getOven until the oven status changes to AVAILABLE.
           When the oven becomes AVAILABLE, the pizza has finished cooking.
        7. Repeat for each pizza to cook

        Report back which pizzas were successfully cooked and which ones failed due to insufficient ingredients or unavailable ovens.
        """)
    @Agent("Cook pizzas based on requests.")
    @McpToolBox("pizza-mcp")
    Multi<ChatEvent> cookStream(@UserMessage String request);
}
