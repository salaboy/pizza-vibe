package com.pizzavibe.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CookResponse(String orderId, String status, String message) {
}
