package com.pizzavibe.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BikeToolsTest {

    @Test
    void shouldHaveGetBikesMethod() throws NoSuchMethodException {
        var method = BikeTools.class.getMethod("getBikes");
        assertNotNull(method);
        assertTrue(method.isAnnotationPresent(dev.langchain4j.agent.tool.Tool.class));
    }

    @Test
    void shouldHaveGetBikeMethod() throws NoSuchMethodException {
        var method = BikeTools.class.getMethod("getBike", String.class);
        assertNotNull(method);
        assertTrue(method.isAnnotationPresent(dev.langchain4j.agent.tool.Tool.class));
    }

    @Test
    void shouldHaveReserveBikeMethod() throws NoSuchMethodException {
        var method = BikeTools.class.getMethod("reserveBike", String.class, String.class);
        assertNotNull(method);
        assertTrue(method.isAnnotationPresent(dev.langchain4j.agent.tool.Tool.class));
    }

    @Test
    void shouldReturnErrorWhenScriptNotFound() {
        var tools = new BikeTools();
        tools.bikesServiceUrl = "http://localhost:8088";
        tools.scriptsPath = "/nonexistent/path";
        String result = tools.getBikes();
        assertTrue(result.contains("error"), "Should return error JSON when script is not found");
    }

    @Test
    void runScriptShouldCaptureOutput() throws Exception {
        var tools = new BikeTools();
        // Create a temporary script that echoes a value
        var tmpScript = java.nio.file.Files.createTempFile("test-script", ".sh");
        java.nio.file.Files.writeString(tmpScript, "#!/bin/bash\necho \"hello\"");
        tmpScript.toFile().setExecutable(true);
        String result = tools.runScript(tmpScript.toString());
        assertEquals("hello", result);
        java.nio.file.Files.delete(tmpScript);
    }
}
