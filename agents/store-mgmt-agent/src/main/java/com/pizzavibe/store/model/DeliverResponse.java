package com.pizzavibe.store.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DeliverResponse(String orderId, String status, String message) {
}
