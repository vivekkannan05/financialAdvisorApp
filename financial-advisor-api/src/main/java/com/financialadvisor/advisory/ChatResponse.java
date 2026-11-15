package com.financialadvisor.advisory;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Chat response body from POST /api/v1/chat.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatResponse(
        String message,
        String source,
        boolean aiAvailable,
        List<String> validationWarnings,
        Map<String, Object> engineData,
        String timestamp
) {
    public static ChatResponse aiResponse(String message, Map<String, Object> engineData,
                                           List<String> warnings) {
        return new ChatResponse(message, "ai", true, warnings.isEmpty() ? null : warnings,
                engineData, Instant.now().toString());
    }

    public static ChatResponse fallbackResponse(String message, Map<String, Object> engineData) {
        return new ChatResponse(message, "rule-engine", false, null,
                engineData, Instant.now().toString());
    }

    public static ChatResponse guardBlocked(String reason) {
        return new ChatResponse(reason, "guard", false, null, null, Instant.now().toString());
    }
}
