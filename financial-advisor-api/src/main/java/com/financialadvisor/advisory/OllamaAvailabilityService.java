package com.financialadvisor.advisory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Checks Ollama availability at startup and provides runtime status (NFR-3.8).
 *
 * When Ollama is unreachable, the system operates in rule-engine-only mode.
 * The service periodically rechecks so it can recover when Ollama comes online.
 */
@Service
public class OllamaAvailabilityService {

    private static final Logger log = LoggerFactory.getLogger(OllamaAvailabilityService.class);

    private final String baseUrl;
    private final String modelName;
    private final AtomicBoolean available = new AtomicBoolean(false);
    private final HttpClient httpClient;

    public OllamaAvailabilityService(
            @Value("${spring.ai.ollama.base-url}") String baseUrl,
            @Value("${spring.ai.ollama.chat.options.model}") String modelName) {
        this.baseUrl = baseUrl;
        this.modelName = modelName;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void checkOnStartup() {
        boolean reachable = checkAvailability();
        if (reachable) {
            log.info("Ollama is available at {} with model {}", baseUrl, modelName);
        } else {
            log.warn("Ollama is NOT available at {}. Operating in rule-engine-only mode (NFR-3.8). " +
                    "AI advisory will resume when Ollama becomes reachable.", baseUrl);
        }
    }

    public boolean isAvailable() {
        return available.get();
    }

    /**
     * Probes Ollama's /api/tags endpoint to verify reachability and model presence.
     */
    public boolean checkAvailability() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/tags"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                boolean modelLoaded = response.body().contains(modelName.split(":")[0]);
                available.set(true);
                if (!modelLoaded) {
                    log.warn("Ollama reachable but model '{}' may not be loaded yet. " +
                            "Pull it with: ollama pull {}", modelName, modelName);
                }
                return true;
            }
        } catch (Exception e) {
            log.debug("Ollama availability check failed: {}", e.getMessage());
        }
        available.set(false);
        return false;
    }

    /**
     * Refreshes availability status. Call before each chat request for
     * up-to-date fallback decisions.
     */
    public void refresh() {
        checkAvailability();
    }
}
