package com.financialadvisor.profile;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST controller for profile management (§4.2 API Endpoints).
 *
 * Endpoints:
 *   GET  /api/v1/profile          — current profile snapshot
 *   GET  /api/v1/profile/history  — all snapshots in chronological order
 *   PUT  /api/v1/profile          — update profile (creates new snapshot; idempotent)
 *   POST /api/v1/profile          — create initial profile
 */
@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {

    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    /**
     * GET /api/v1/profile — returns the current (latest) profile snapshot.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getCurrentProfile(Authentication auth) {
        String userId = auth.getName();
        Optional<ProfileSnapshot> snapshot = profileService.getCurrentProfile(userId);

        if (snapshot.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        ProfileSnapshot s = snapshot.get();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("userId", s.userId());
        response.put("snapshotId", s.snapshotId());
        response.put("snapshotDate", s.snapshotDate().toString());
        response.put("isLatest", s.isLatest());
        response.put("data", s.data());

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/profile/history — returns all snapshots for the user.
     */
    @GetMapping("/history")
    public ResponseEntity<ProfileHistoryResponse> getProfileHistory(Authentication auth) {
        String userId = auth.getName();
        List<ProfileSnapshot> history = profileService.getProfileHistory(userId);

        List<ProfileHistoryResponse.SnapshotSummary> summaries = history.stream()
                .map(ProfileHistoryResponse.SnapshotSummary::from)
                .toList();

        return ResponseEntity.ok(new ProfileHistoryResponse(userId, summaries.size(), summaries));
    }

    /**
     * PUT /api/v1/profile — update profile (creates new snapshot; idempotent per NFR-3.6).
     */
    @PutMapping
    public ResponseEntity<Map<String, Object>> updateProfile(
            Authentication auth,
            @Valid @RequestBody ProfileUpdateRequest request) {
        String userId = auth.getName();

        log.info("Profile update requested for user: {}", userId);
        ProfileService.UpdateResult result = profileService.updateProfile(userId, request.data());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("snapshotId", result.snapshotId());
        response.put("created", result.created());
        response.put("message", result.message());

        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }

    /**
     * POST /api/v1/profile — create initial profile.
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createProfile(
            Authentication auth,
            @Valid @RequestBody ProfileUpdateRequest request) {
        String userId = auth.getName();

        log.info("Initial profile creation requested for user: {}", userId);
        ProfileSnapshot snapshot = profileService.createProfile(userId, request.data());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("snapshotId", snapshot.snapshotId());
        response.put("snapshotDate", snapshot.snapshotDate().toString());
        response.put("message", "Profile created");

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
