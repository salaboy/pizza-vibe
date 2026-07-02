package com.pizzavibe.store;

import com.pizzavibe.store.listener.AgentContext;
import com.pizzavibe.store.model.PizzaOrderStatus;
import com.pizzavibe.store.model.ProcessOrderRequest;
import com.pizzavibe.store.workflows.PizzaOrderWorkflow;
import dev.openfeature.contrib.providers.flagd.FlagdProvider;
import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.OpenFeatureAPI;
import dev.openfeature.contrib.hooks.otel.TracesHook;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;

@Path("/mgmt")
@ApplicationScoped
public class StoreMgmtResource {

    private static final Logger log = LoggerFactory.getLogger(StoreMgmtResource.class);

    @Inject
    PizzaOrderWorkflow pizzaOrderWorkflowAgent;

    @Inject
    AgentContext agentContext;

    private Client openFeatureClient;

    @PostConstruct
    void initOpenFeature() {
        // Set TracesHook globally
        OpenFeatureAPI.getInstance().addHooks(new TracesHook());
        try {
            OpenFeatureAPI.getInstance().setProvider(new FlagdProvider());
            openFeatureClient = OpenFeatureAPI.getInstance().getClient();
            log.info("OpenFeature initialized with flagd provider");
        } catch (Exception e) {
            log.warn("Failed to initialize OpenFeature flagd provider: {}", e.getMessage());
            openFeatureClient = OpenFeatureAPI.getInstance().getClient();
        }
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello from Store Management Agent";
    }

    @GET
    @Path("/agent-chooser")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> getAgentChooser() {
        String agentVersion = openFeatureClient.getStringValue("agent-chooser", "v1");
        return Map.of("agentVersion", agentVersion);
    }

    @POST
    @Path("/processOrder")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public PizzaOrderStatus processOrder(ProcessOrderRequest request) {
        log.info(request.toString());
        agentContext.setOrderId(request.orderId());
        agentContext.setAgentName("pizza-order-workflow");
        String pizzas = "";
        if (request.orderItems() != null) {
            pizzas = Arrays.toString(request.orderItems().toArray());
        }
        String drinks = "";
        if (request.drinkItems() != null) {
            drinks = Arrays.toString(request.drinkItems().toArray());
        }
        return pizzaOrderWorkflowAgent.processPizzaOrder(request.orderId(), pizzas, drinks);
    }
}
