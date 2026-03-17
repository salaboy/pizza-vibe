package com.pizzavibe.cooking.listener;

import com.pizzavibe.cooking.client.AgentEventClient;
import com.pizzavibe.cooking.model.AgentEvent;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.ModelProvider;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import jakarta.enterprise.inject.Instance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentEventChatModelListenerTest {

    @Mock
    AgentEventClient agentEventClient;

    @Mock
    Instance<AgentContext> agentContextInstance;

    @InjectMocks
    AgentEventChatModelListener listener;

    private final ArgumentCaptor<AgentEvent> eventCaptor = ArgumentCaptor.forClass(AgentEvent.class);

    @BeforeEach
    void setUp() {
        AgentContext context = new AgentContext();
        context.setOrderId("order-123");
        lenient().when(agentContextInstance.isResolvable()).thenReturn(true);
        lenient().when(agentContextInstance.get()).thenReturn(context);
    }

    @Test
    void onRequestShouldSendRequestEventWithUserMessage() {
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(List.of(UserMessage.from("Cook a margherita pizza")))
                .build();
        ChatModelRequestContext requestContext = new ChatModelRequestContext(chatRequest, ModelProvider.ANTHROPIC, Map.of());

        listener.onRequest(requestContext);

        verify(agentEventClient).sendEvent(eventCaptor.capture());
        AgentEvent event = eventCaptor.getValue();
        assertEquals("cooking-agent", event.agentId());
        assertEquals("request", event.kind());
        assertEquals("Cook a margherita pizza", event.text());
        assertEquals("order-123", event.orderId());
        assertNotNull(event.timestamp());
    }

    @Test
    void onRequestShouldUseLastMessageFromList() {
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(List.of(
                        UserMessage.from("first message"),
                        UserMessage.from("second message")))
                .build();
        ChatModelRequestContext requestContext = new ChatModelRequestContext(chatRequest, ModelProvider.ANTHROPIC, Map.of());

        listener.onRequest(requestContext);

        verify(agentEventClient).sendEvent(eventCaptor.capture());
        assertEquals("second message", eventCaptor.getValue().text());
    }

    @Test
    void onResponseShouldSendResponseEventWithAiMessage() {
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(List.of(UserMessage.from("hello")))
                .build();
        ChatResponse chatResponse = ChatResponse.builder()
                .aiMessage(AiMessage.from("Pizza is cooked and ready"))
                .build();
        ChatModelResponseContext responseContext = new ChatModelResponseContext(
                chatResponse, chatRequest, ModelProvider.ANTHROPIC, Map.of());

        listener.onResponse(responseContext);

        verify(agentEventClient).sendEvent(eventCaptor.capture());
        AgentEvent event = eventCaptor.getValue();
        assertEquals("cooking-agent", event.agentId());
        assertEquals("response", event.kind());
        assertEquals("Pizza is cooked and ready", event.text());
        assertEquals("order-123", event.orderId());
    }

    @Test
    void onErrorShouldSendErrorEventWithExceptionMessage() {
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(List.of(UserMessage.from("hello")))
                .build();
        Throwable error = new RuntimeException("Oven malfunction");
        ChatModelErrorContext errorContext = new ChatModelErrorContext(
                error, chatRequest, ModelProvider.ANTHROPIC, Map.of());

        listener.onError(errorContext);

        verify(agentEventClient).sendEvent(eventCaptor.capture());
        AgentEvent event = eventCaptor.getValue();
        assertEquals("cooking-agent", event.agentId());
        assertEquals("error", event.kind());
        assertEquals("Oven malfunction", event.text());
        assertEquals("order-123", event.orderId());
    }

    @Test
    void shouldUseEmptyOrderIdWhenContextNotResolvable() {
        when(agentContextInstance.isResolvable()).thenReturn(false);

        ChatRequest chatRequest = ChatRequest.builder()
                .messages(List.of(UserMessage.from("hello")))
                .build();
        ChatModelRequestContext requestContext = new ChatModelRequestContext(chatRequest, ModelProvider.ANTHROPIC, Map.of());

        listener.onRequest(requestContext);

        verify(agentEventClient).sendEvent(eventCaptor.capture());
        assertEquals("", eventCaptor.getValue().orderId());
    }

    @Test
    void shouldNotThrowWhenClientFails() {
        doThrow(new RuntimeException("connection refused"))
                .when(agentEventClient).sendEvent(any());

        ChatRequest chatRequest = ChatRequest.builder()
                .messages(List.of(UserMessage.from("hello")))
                .build();
        ChatModelRequestContext requestContext = new ChatModelRequestContext(chatRequest, ModelProvider.ANTHROPIC, Map.of());

        assertDoesNotThrow(() -> listener.onRequest(requestContext));
    }

    @Test
    void shouldTruncateLongMessages() {
        String longMessage = "x".repeat(600);
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(List.of(UserMessage.from(longMessage)))
                .build();
        ChatModelRequestContext requestContext = new ChatModelRequestContext(chatRequest, ModelProvider.ANTHROPIC, Map.of());

        listener.onRequest(requestContext);

        verify(agentEventClient).sendEvent(eventCaptor.capture());
        String text = eventCaptor.getValue().text();
        assertEquals(503, text.length()); // 500 chars + "..."
        assertTrue(text.endsWith("..."));
    }

    @Test
    void onRequestShouldHandleNonUserMessage() {
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(List.of(AiMessage.from("previous AI response"), UserMessage.from("user reply")))
                .build();
        ChatModelRequestContext requestContext = new ChatModelRequestContext(chatRequest, ModelProvider.ANTHROPIC, Map.of());

        listener.onRequest(requestContext);

        verify(agentEventClient).sendEvent(eventCaptor.capture());
        assertEquals("user reply", eventCaptor.getValue().text());
    }
}