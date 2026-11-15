package com.financialadvisor.profile;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Domain model for a profile snapshot (FR-1.3, NFR-2.2).
 *
 * Each snapshot is an immutable point-in-time view of the user's financial profile.
 * The latest snapshot is the "current" profile; all prior snapshots remain
 * queryable for trend analysis and audit.
 *
 * Uses a flexible schema — the {@code data} map accepts any structure the user
 * provides, and the engine handles missing fields gracefully (FR-1.2).
 */
public record ProfileSnapshot(
        String userId,
        String snapshotId,
        Instant snapshotDate,
        boolean isLatest,
        Map<String, Object> data,
        List<String> encryptedFields
) {
    /**
     * Creates a new snapshot with a generated ID and current timestamp.
     */
    public static ProfileSnapshot createNew(String userId, Map<String, Object> data) {
        String snapshotId = generateSnapshotId();
        return new ProfileSnapshot(userId, snapshotId, Instant.now(), true, data, List.of());
    }

    /**
     * Creates a copy marked as not-latest (for archiving the previous snapshot).
     */
    public ProfileSnapshot markNotLatest() {
        return new ProfileSnapshot(userId, snapshotId, snapshotDate, false, data, encryptedFields);
    }

    /**
     * Creates a copy with encrypted fields list set.
     */
    public ProfileSnapshot withEncryptedFields(List<String> fields) {
        return new ProfileSnapshot(userId, snapshotId, snapshotDate, isLatest, data, fields);
    }

    /**
     * Generates a time-sortable snapshot ID.
     * Format: epoch millis (zero-padded to 15 digits) + random suffix.
     * This ensures chronological ordering when used as DynamoDB sort key.
     */
    private static String generateSnapshotId() {
        long epochMillis = Instant.now().toEpochMilli();
        String random = java.util.UUID.randomUUID().toString().substring(0, 8);
        return String.format("%015d-%s", epochMillis, random);
    }
}
