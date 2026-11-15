package com.financialadvisor.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financialadvisor.encryption.FieldEncryptionService;
import com.financialadvisor.exception.AppException;
import com.financialadvisor.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Profile business logic (FR-1.x, NFR-2.2, NFR-3.6).
 *
 * Responsibilities:
 * - CRUD operations on financial profile
 * - Snapshot versioning: each update creates a new dated snapshot (FR-1.3)
 * - Idempotent saves: identical data does not create a new snapshot (NFR-3.6)
 * - Field-level encryption before storage (NFR-1.2)
 * - Flexible schema: accepts any profile structure (FR-1.2)
 */
@Service
public class ProfileService {

    private static final Logger log = LoggerFactory.getLogger(ProfileService.class);

    private final ProfileRepository profileRepository;
    private final FieldEncryptionService encryptionService;
    private final ObjectMapper objectMapper;

    public ProfileService(ProfileRepository profileRepository,
                          FieldEncryptionService encryptionService,
                          ObjectMapper objectMapper) {
        this.profileRepository = profileRepository;
        this.encryptionService = encryptionService;
        this.objectMapper = objectMapper;
    }

    /**
     * Gets the current (latest) profile snapshot, decrypted.
     *
     * @param userId the user ID
     * @return the decrypted profile snapshot, or empty if no profile exists
     */
    public Optional<ProfileSnapshot> getCurrentProfile(String userId) {
        Optional<ProfileSnapshot> snapshot = profileRepository.findLatest(userId);
        snapshot.ifPresent(s -> {
            Map<String, Object> mutableData = new LinkedHashMap<>(s.data());
            encryptionService.decryptSensitiveFields(mutableData, s.encryptedFields());
        });
        return snapshot;
    }

    /**
     * Gets the full snapshot history for a user, decrypted.
     *
     * @param userId the user ID
     * @return list of all snapshots in chronological order
     */
    public List<ProfileSnapshot> getProfileHistory(String userId) {
        List<ProfileSnapshot> snapshots = profileRepository.findHistory(userId);
        for (ProfileSnapshot s : snapshots) {
            Map<String, Object> mutableData = new LinkedHashMap<>(s.data());
            encryptionService.decryptSensitiveFields(mutableData, s.encryptedFields());
        }
        return snapshots;
    }

    /**
     * Updates the profile, creating a new snapshot.
     * Idempotent: if the new data is identical to the current snapshot, no new snapshot is created (NFR-3.6).
     *
     * @param userId the user ID
     * @param newData the updated profile data (flexible schema)
     * @return the result of the update operation
     */
    public UpdateResult updateProfile(String userId, Map<String, Object> newData) {
        Optional<ProfileSnapshot> currentOpt = profileRepository.findLatest(userId);

        if (currentOpt.isPresent()) {
            ProfileSnapshot current = currentOpt.get();

            Map<String, Object> currentDecrypted = deepCopy(current.data());
            encryptionService.decryptSensitiveFields(currentDecrypted, current.encryptedFields());

            if (isDataIdentical(currentDecrypted, newData)) {
                log.info("Profile data identical to current snapshot for user {}; no new snapshot created (NFR-3.6)",
                        userId);
                return new UpdateResult(false, current.snapshotId(), "No changes detected");
            }

            profileRepository.markNotLatest(userId, current.snapshotId());
        }

        ProfileSnapshot newSnapshot = ProfileSnapshot.createNew(userId, deepCopy(newData));

        Map<String, Object> dataToEncrypt = deepCopy(newData);
        List<String> encryptedPaths = encryptionService.encryptSensitiveFields(dataToEncrypt);
        newSnapshot = new ProfileSnapshot(
                newSnapshot.userId(),
                newSnapshot.snapshotId(),
                newSnapshot.snapshotDate(),
                true,
                dataToEncrypt,
                encryptedPaths
        );

        profileRepository.save(newSnapshot);

        log.info("Created new profile snapshot {} for user {} (encrypted {} fields)",
                newSnapshot.snapshotId(), userId, encryptedPaths.size());

        return new UpdateResult(true, newSnapshot.snapshotId(), "Profile updated");
    }

    /**
     * Creates the initial profile for a new user.
     *
     * @param userId the user ID
     * @param initialData the initial profile data
     * @return the created snapshot
     */
    public ProfileSnapshot createProfile(String userId, Map<String, Object> initialData) {
        Optional<ProfileSnapshot> existing = profileRepository.findLatest(userId);
        if (existing.isPresent()) {
            throw new AppException(ErrorCode.BAD_REQUEST,
                    "Profile already exists for user " + userId + ". Use PUT to update.");
        }

        ProfileSnapshot snapshot = ProfileSnapshot.createNew(userId, deepCopy(initialData));

        Map<String, Object> dataToEncrypt = deepCopy(initialData);
        List<String> encryptedPaths = encryptionService.encryptSensitiveFields(dataToEncrypt);
        snapshot = new ProfileSnapshot(
                snapshot.userId(),
                snapshot.snapshotId(),
                snapshot.snapshotDate(),
                true,
                dataToEncrypt,
                encryptedPaths
        );

        profileRepository.save(snapshot);
        log.info("Created initial profile snapshot {} for user {}", snapshot.snapshotId(), userId);
        return snapshot;
    }

    /**
     * Deep-compares two data maps for idempotent save check (NFR-3.6).
     */
    boolean isDataIdentical(Map<String, Object> existing, Map<String, Object> incoming) {
        try {
            String existingJson = objectMapper.writeValueAsString(
                    objectMapper.readTree(objectMapper.writeValueAsString(existing)));
            String incomingJson = objectMapper.writeValueAsString(
                    objectMapper.readTree(objectMapper.writeValueAsString(incoming)));
            return existingJson.equals(incomingJson);
        } catch (Exception e) {
            log.warn("Failed to compare profile data; treating as different", e);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> deepCopy(Map<String, Object> original) {
        try {
            String json = objectMapper.writeValueAsString(original);
            return objectMapper.readValue(json, LinkedHashMap.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deep-copy profile data", e);
        }
    }

    /**
     * Result of a profile update operation.
     */
    public record UpdateResult(boolean created, String snapshotId, String message) {}
}
