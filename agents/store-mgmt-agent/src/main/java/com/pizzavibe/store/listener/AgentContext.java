package com.pizzavibe.store.listener;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class AgentContext {

    private String orderId = "";
    private String agentName = "store-mgmt-agent";

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }
}