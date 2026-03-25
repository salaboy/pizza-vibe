package com.pizzavibe.delivery.config;

import io.a2a.jsonrpc.common.json.JsonUtil;
import io.a2a.jsonrpc.common.wrappers.SendMessageRequest;
import io.a2a.jsonrpc.common.wrappers.SendMessageResponse;
import io.a2a.server.ServerCallContext;
import io.a2a.spec.AgentCard;
import io.a2a.transport.jsonrpc.handler.JSONRPCHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
public class A2AController {

    private static final Logger log = LoggerFactory.getLogger(A2AController.class);

    private final JSONRPCHandler jsonRPCHandler;
    private final AgentCard agentCard;

    public A2AController(JSONRPCHandler jsonRPCHandler, AgentCard agentCard) {
        this.jsonRPCHandler = jsonRPCHandler;
        this.agentCard = agentCard;
    }

    @GetMapping(value = "/.well-known/agent.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getAgentCard() throws Exception {
        return JsonUtil.toJson(agentCard);
    }

    @PostMapping(value = "/", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public String handleJsonRpc(@RequestBody String body) throws Exception {
        log.info("Received JSONRPC request");

        Map<?, ?> jsonMap = JsonUtil.fromJson(body, Map.class);
        String method = (String) jsonMap.get("method");

        ServerCallContext callContext = new ServerCallContext(null, Collections.emptyMap(), Collections.emptySet());

        if ("message/send".equals(method)) {
            SendMessageRequest request = JsonUtil.fromJson(body, SendMessageRequest.class);
            SendMessageResponse response = jsonRPCHandler.onMessageSend(request, callContext);
            return JsonUtil.toJson(response);
        }

        return JsonUtil.toJson(Map.of("jsonrpc", "2.0", "error",
                Map.of("code", -32601, "message", "Method not found: " + method)));
    }
}
