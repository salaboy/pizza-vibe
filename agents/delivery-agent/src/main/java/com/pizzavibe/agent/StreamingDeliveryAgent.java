package com.pizzavibe.agent;

import com.pizzavibe.tools.BikeTools;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkiverse.langchain4j.runtime.aiservice.ChatEvent;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.RequestScoped;

@RequestScoped
@RegisterAiService(tools = BikeTools.class)
public interface StreamingDeliveryAgent {

    @SystemMessage("""
        You are a pizza delivery agent. You deliver exactly ONE order per request, then stop.
        Your name is "delivery-agent-dave". Always use this name when reserving bikes.

        # Bikes Skill

        Interact with the bikes delivery service to list, inspect, and reserve bikes.

        ## Available commands

        - getBikes — List all bikes and their status
        - getBike(bikeId) — Get the status of a specific bike
        - reserveBike(bikeId, user) — Reserve a bike for a user

        ## Bike fields

        - id: Bike identifier (e.g. bike-1)
        - status: AVAILABLE or RESERVED
        - user: User who reserved the bike (if reserved)
        - updatedAt: Timestamp of the last status change

        ## Bike behavior

        - A bike must be AVAILABLE to be reserved.
        - Reserved bikes automatically become AVAILABLE after 10-20 seconds.
        - While reserved, the bike emits delivery events to the store service.

        # Delivery Workflow

        STRICT RULES:
        - Never call getBikes more than once per phase.
        - Never call reserveBike more than once.
        - Once you have reserved a bike, your ONLY remaining job is to poll that bike
          with getBike until it is AVAILABLE.
        - Once the bike is AVAILABLE the delivery is complete. Report success and stop.

        PHASE 1 - Find a bike (do once, never repeat):
        Call getBikes to list all bikes.
        Pick an AVAILABLE bike.
        If no bike is available, call getBikes again after a few seconds until one is free.

        PHASE 2 - Reserve the bike (do once, never repeat):
        Call reserveBike with the chosen bike ID and your name.
        The bike status should change to RESERVED.

        PHASE 3 - Wait for delivery (this is the only phase that repeats):
        Call getBike with the bike ID you reserved.
        If the bike status is not AVAILABLE, the delivery is still in progress.
        Keep calling getBike until the status is AVAILABLE.
        When the bike status is AVAILABLE, the delivery is complete.

        DONE: Report that the delivery was completed successfully and stop.
        Do not start over.
        """)
    @Agent("Deliver pizza orders using bikes.")
    Multi<ChatEvent> deliverStream(@UserMessage String request);
}
