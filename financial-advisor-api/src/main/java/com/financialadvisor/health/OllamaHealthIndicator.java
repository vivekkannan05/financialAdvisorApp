package com.financialadvisor.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Custom health indicator for Ollama LLM service (NFR-5.2, NFR-3.8).
 * Checks if Ollama is reachable and the configured model is available.
 * When unhealthy, the system operates in rule-engine-only mode.
 */
@Component("ollama")
public class OllamaHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(OllamaHealthIndicator.class);

    private final String baseUrl;
    private final String model;
    private final HttpClient httpClient;

    public OllamaHealthIndicator(
            @Value("${spring.ai.ollama.base-url}") String baseUrl,
            @Value("${spring.ai.ollama.chat.options.model}") String model) {
        this.baseUrl = baseUrl;
        this.model = model;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public Health health() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/tags"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                boolean modelLoaded = response.body().contains(model.split(":")[0]);
                if (modelLoaded) {
                    return Health.up()
                            .withDetail("url", baseUrl)
                            .withDetail("model", model)
                            .withDetail("modelLoaded", true)
                            .build();
                } else {
                    return Health.up()
                            .withDetail("url", baseUrl)
                            .withDetail("model", model)
                            .withDetail("modelLoaded", false)
                            .withDetail("note", "Ollama reachable but model not yet loaded; operating in rule-engine-only mode")
                            .build();
                }
            }

            return Health.down()
                    .withDetail("url", baseUrl)
                    .withDetail("httpStatus", response.statusCode())
                    .build();
        } catch (Exception e) {
            log.warn("Ollama health check failed: {}", e.getMessage());
            return Health.down()
                    .withDetail("url", baseUrl)
                    .withDetail("error", e.getMessage())
                    .withDetail("note", "Operating in rule-engine-only mode")
                    .build();
        }
    }
}
