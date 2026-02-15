package com.pizzavibe.agent;

import com.pizzavibe.tools.KitchenTools;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import io.quarkiverse.langchain4j.ToolBox;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DelegateKitchenAgentTest {

    @Test
    void shouldHaveToolBoxWithKitchenTools() throws NoSuchMethodException {
        var method = DelegateKitchenAgent.class.getMethod("delegateToKitchenStream", String.class);
        ToolBox annotation = method.getAnnotation(ToolBox.class);

        assertNotNull(annotation, "delegateToKitchenStream should have @ToolBox annotation");
        assertEquals(1, annotation.value().length);
        assertEquals(KitchenTools.class, annotation.value()[0]);
    }

    @Test
    void shouldHaveAgentAnnotationWithOutputKey() throws NoSuchMethodException {
        var method = DelegateKitchenAgent.class.getMethod("delegateToKitchenStream", String.class);
        Agent agentAnnotation = method.getAnnotation(Agent.class);

        assertNotNull(agentAnnotation, "delegateToKitchenStream should have @Agent annotation");
        assertTrue(agentAnnotation.description().contains("Kitchen"));
        assertEquals("kitchenStatus", agentAnnotation.outputKey());
    }

    @Test
    void shouldHaveSystemMessageWithToolInstructions() throws NoSuchMethodException {
        var method = DelegateKitchenAgent.class.getMethod("delegateToKitchenStream", String.class);
        SystemMessage annotation = method.getAnnotation(SystemMessage.class);

        assertNotNull(annotation, "delegateToKitchenStream should have @SystemMessage");
        String message = String.join("\n", annotation.value());
        assertTrue(message.contains("Kitchen"), "System message should mention Kitchen");
        assertTrue(message.contains("cookOrder"), "System message should mention cookOrder tool");
    }
}
