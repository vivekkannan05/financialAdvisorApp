package com.financialadvisor.profile;

import java.util.List;

/**
 * Response DTO for profile history queries.
 * Returns all snapshots in chronological order with their metadata.
 */
public record ProfileHistoryResponse(
        String userId,
        int snapshotCount,
        List<SnapshotSummary> snapshots
) {
    public record SnapshotSummary(
            String snapshotId,
            String snapshotDate,
            boolean isLatest
    ) {
        public static SnapshotSummary from(ProfileSnapshot snapshot) {
            return new SnapshotSummary(
                    snapshot.snapshotId(),
                    snapshot.snapshotDate().toString(),
                    snapshot.isLatest()
            );
        }
    }
}
