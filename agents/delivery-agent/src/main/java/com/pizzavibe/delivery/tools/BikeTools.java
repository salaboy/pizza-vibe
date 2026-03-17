package com.pizzavibe.delivery.tools;

import dev.langchain4j.agent.tool.Tool;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.stream.Collectors;

@ApplicationScoped
public class BikeTools {

    @Inject
    Tracer tracer;

    @ConfigProperty(name = "bikes.service.url", defaultValue = "http://localhost:8088")
    String bikesServiceUrl;

    @ConfigProperty(name = "bikes.skill.scripts.path", defaultValue = "skills/bikes/scripts")
    String scriptsPath;

    @Tool("List all available bikes and their current status. Returns JSON array of bikes with id, status, user, and updatedAt fields.")
    public String getBikes() {
        return runScript(scriptsPath + "/list-bikes.sh", bikesServiceUrl);
    }

    @Tool("Wait for a bike to finish its delivery. Polls the bike every 2 seconds and returns only when the bike status is AVAILABLE. Call this once after reserving a bike.")
    public String getBike(String bikeId) {
        return runScript(scriptsPath + "/get-bike.sh", bikeId, bikesServiceUrl);
    }

    @Tool("Reserve a bike for delivery. The bike must be AVAILABLE. Requires bikeId, user name, and orderId. The bike will automatically become AVAILABLE again after 10-20 seconds when the delivery is complete.")
    public String reserveBike(String bikeId, String user, String orderId) {
        return runScript(scriptsPath + "/reserve-bike.sh", bikeId, user, orderId, bikesServiceUrl);
    }

    String runScript(String scriptPath, String... args) {
        String scriptName = Path.of(scriptPath).getFileName().toString();
        Span skillSpan = tracer.spanBuilder("skill.execute")
                .setAttribute("skill.name", "bikes")
                .setAttribute("skill.script", scriptName)
                .setAttribute("skill.script.path", scriptPath)
                .startSpan();

        try (var scope = skillSpan.makeCurrent()) {
            var command = new java.util.ArrayList<String>();
            command.add("bash");
            command.add(scriptPath);
            command.addAll(java.util.List.of(args));
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);

            // Propagate W3C Trace Context to shell scripts via environment variable
            SpanContext spanCtx = Span.current().getSpanContext();
            if (spanCtx.isValid()) {
                String traceparent = String.format("00-%s-%s-%s",
                        spanCtx.getTraceId(),
                        spanCtx.getSpanId(),
                        spanCtx.getTraceFlags().asHex());
                pb.environment().put("TRACEPARENT", traceparent);
            }

            Process process = pb.start();
            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                output = reader.lines().collect(Collectors.joining("\n"));
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                skillSpan.setStatus(StatusCode.ERROR, "Script exited with code " + exitCode);
                return "{\"error\": \"Script exited with code " + exitCode + ": " + output + "\"}";
            }
            return output;
        } catch (IOException | InterruptedException e) {
            skillSpan.setStatus(StatusCode.ERROR, e.getMessage());
            return "{\"error\": \"" + e.getMessage() + "\"}";
        } finally {
            skillSpan.end();
        }
    }
}
