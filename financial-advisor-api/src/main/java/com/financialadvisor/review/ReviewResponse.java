package com.financialadvisor.review;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Periodic review response — flags material drift (FR-9.2).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReviewResponse(
        String userId,
        String currentSnapshotId,
        String snapshotDate,
        boolean staleProfile,
        int daysSinceLastUpdate,
        List<DriftWarning> driftWarnings,
        List<String> recommendedActions,
        String timestamp
) {
    public record DriftWarning(
            String dimension,
            String description,
            String severity
    ) {}

    public static ReviewResponse create(
            String userId, String snapshotId, String snapshotDate,
            boolean stale, int daysSince,
            List<DriftWarning> warnings, List<String> actions) {
        return new ReviewResponse(userId, snapshotId, snapshotDate,
                stale, daysSince, warnings, actions, Instant.now().toString());
    }
}
