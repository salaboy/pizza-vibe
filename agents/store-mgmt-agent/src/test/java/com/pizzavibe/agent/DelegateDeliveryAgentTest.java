package com.pizzavibe.agent;

import com.pizzavibe.tools.DeliveryTools;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import io.quarkiverse.langchain4j.ToolBox;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DelegateDeliveryAgentTest {

    @Test
    void shouldHaveToolBoxWithDeliveryTools() throws NoSuchMethodException {
        var method = DelegateDeliveryAgent.class.getMethod("delegateToDeliveryStream", String.class);
        ToolBox annotation = method.getAnnotation(ToolBox.class);

        assertNotNull(annotation, "delegateToDeliveryStream should have @ToolBox annotation");
        assertEquals(1, annotation.value().length);
        assertEquals(DeliveryTools.class, annotation.value()[0]);
    }

    @Test
    void shouldHaveAgentAnnotationWithOutputKey() throws NoSuchMethodException {
        var method = DelegateDeliveryAgent.class.getMethod("delegateToDeliveryStream", String.class);
        Agent agentAnnotation = method.getAnnotation(Agent.class);

        assertNotNull(agentAnnotation, "delegateToDeliveryStream should have @Agent annotation");
        assertTrue(agentAnnotation.description().contains("Delivery"));
        assertEquals("deliveryStatus", agentAnnotation.outputKey());
    }

    @Test
    void shouldHaveSystemMessageWithToolInstructions() throws NoSuchMethodException {
        var method = DelegateDeliveryAgent.class.getMethod("delegateToDeliveryStream", String.class);
        SystemMessage annotation = method.getAnnotation(SystemMessage.class);

        assertNotNull(annotation, "delegateToDeliveryStream should have @SystemMessage");
        String message = String.join("\n", annotation.value());
        assertTrue(message.contains("Delivery"), "System message should mention Delivery");
        assertTrue(message.contains("deliverOrder"), "System message should mention deliverOrder tool");
    }
}
