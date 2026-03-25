package com.pizzavibe.delivery.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class BikeTools {

    private static final Logger log = LoggerFactory.getLogger(BikeTools.class);

    @Value("${bikes.service.url:http://localhost:8088}")
    private String bikesServiceUrl;

    @Value("${bikes.skill.scripts.path:skills/bikes/scripts}")
    private String scriptsPath;

    @Tool(description = "List all available bikes and their current status. Returns JSON array of bikes with id, status, user, and updatedAt fields.")
    public String getBikes() {
        return runScript(scriptsPath + "/list-bikes.sh", bikesServiceUrl);
    }

    @Tool(description = "Wait for a bike to finish its delivery. Polls the bike every 2 seconds and returns only when the bike status is AVAILABLE. Call this once after reserving a bike.")
    public String getBike(@ToolParam(description = "The ID of the bike to check") String bikeId) {
        return runScript(scriptsPath + "/get-bike.sh", bikeId, bikesServiceUrl);
    }

    @Tool(description = "Reserve a bike for delivery. The bike must be AVAILABLE. Requires bikeId, user name, and orderId. The bike will automatically become AVAILABLE again after 10-20 seconds when the delivery is complete.")
    public String reserveBike(
            @ToolParam(description = "The ID of the bike to reserve") String bikeId,
            @ToolParam(description = "The name of the user reserving the bike") String user,
            @ToolParam(description = "The order ID for the delivery") String orderId) {
        return runScript(scriptsPath + "/reserve-bike.sh", bikeId, user, orderId, bikesServiceUrl);
    }

    String runScript(String scriptPath, String... args) {
        String scriptName = Path.of(scriptPath).getFileName().toString();
        log.info("Executing bike script: {} with args count: {}", scriptName, args.length);

        try {
            List<String> command = new ArrayList<>();
            command.add("bash");
            command.add(scriptPath);
            command.addAll(List.of(args));
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);

            Process process = pb.start();
            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                output = reader.lines().collect(Collectors.joining("\n"));
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("Script {} exited with code {}: {}", scriptName, exitCode, output);
                return "{\"error\": \"Script exited with code " + exitCode + ": " + output + "\"}";
            }
            return output;
        } catch (IOException | InterruptedException e) {
            log.error("Error executing script {}: {}", scriptName, e.getMessage());
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
}
