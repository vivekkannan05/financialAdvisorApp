package com.financialadvisor.profile;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ProfileSnapshot domain model.
 */
class ProfileSnapshotTest {

    @Test
    @DisplayName("createNew generates unique snapshot IDs")
    void createNew_generatesUniqueIds() {
        ProfileSnapshot s1 = ProfileSnapshot.createNew("user1", Map.of("test", "data1"));
        ProfileSnapshot s2 = ProfileSnapshot.createNew("user1", Map.of("test", "data2"));

        assertNotEquals(s1.snapshotId(), s2.snapshotId());
    }

    @Test
    @DisplayName("createNew sets isLatest to true")
    void createNew_setsIsLatestTrue() {
        ProfileSnapshot snapshot = ProfileSnapshot.createNew("user1", Map.of("test", "data"));
        assertTrue(snapshot.isLatest());
    }

    @Test
    @DisplayName("createNew sets snapshotDate to current time")
    void createNew_setsCurrentTimestamp() {
        ProfileSnapshot snapshot = ProfileSnapshot.createNew("user1", Map.of("test", "data"));
        assertNotNull(snapshot.snapshotDate());
    }

    @Test
    @DisplayName("markNotLatest returns a copy with isLatest=false")
    void markNotLatest_returnsCopyWithFalse() {
        ProfileSnapshot original = ProfileSnapshot.createNew("user1", Map.of("test", "data"));
        assertTrue(original.isLatest());

        ProfileSnapshot archived = original.markNotLatest();
        assertFalse(archived.isLatest());
        assertEquals(original.snapshotId(), archived.snapshotId());
        assertEquals(original.userId(), archived.userId());
    }

    @Test
    @DisplayName("Snapshot IDs are chronologically sortable")
    void snapshotIds_areChronologicallySortable() throws InterruptedException {
        ProfileSnapshot s1 = ProfileSnapshot.createNew("user1", Map.of());
        Thread.sleep(5);
        ProfileSnapshot s2 = ProfileSnapshot.createNew("user1", Map.of());

        assertTrue(s1.snapshotId().compareTo(s2.snapshotId()) < 0,
                "Earlier snapshot ID should sort before later one");
    }
}
