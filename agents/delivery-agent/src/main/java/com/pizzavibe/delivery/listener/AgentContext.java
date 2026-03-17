package com.pizzavibe.delivery.listener;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class AgentContext {

    private String orderId = "";

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
}