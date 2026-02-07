package com.pizzavibe.tools;

import dev.langchain4j.agent.tool.Tool;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

@ApplicationScoped
public class BikeTools {

    @ConfigProperty(name = "bikes.service.url", defaultValue = "http://localhost:8088")
    String bikesServiceUrl;

    @ConfigProperty(name = "bikes.skill.scripts.path", defaultValue = "skills/bikes/scripts")
    String scriptsPath;

    @Tool("List all available bikes and their current status. Returns JSON array of bikes with id, status, user, and updatedAt fields.")
    public String getBikes() {
        return runScript(scriptsPath + "/list-bikes.sh", bikesServiceUrl);
    }

    @Tool("Get the status of a specific bike by its ID. Returns JSON with id, status, user, and updatedAt fields.")
    public String getBike(String bikeId) {
        return runScript(scriptsPath + "/get-bike.sh", bikeId, bikesServiceUrl);
    }

    @Tool("Reserve a bike for delivery. The bike must be AVAILABLE. Requires bikeId and user name. The bike will automatically become AVAILABLE again after 10-20 seconds when the delivery is complete.")
    public String reserveBike(String bikeId, String user) {
        return runScript(scriptsPath + "/reserve-bike.sh", bikeId, user, bikesServiceUrl);
    }

    String runScript(String scriptPath, String... args) {
        try {
            var command = new java.util.ArrayList<String>();
            command.add("bash");
            command.add(scriptPath);
            command.addAll(java.util.List.of(args));
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                output = reader.lines().collect(Collectors.joining("\n"));
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                return "{\"error\": \"Script exited with code " + exitCode + ": " + output + "\"}";
            }
            return output;
        } catch (IOException | InterruptedException e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
}
