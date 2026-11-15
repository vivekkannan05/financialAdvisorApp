package com.financialadvisor.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financialadvisor.config.AppProperties;
import com.financialadvisor.encryption.FieldEncryptionService;
import com.financialadvisor.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests for ProfileService:
 * - UT-17: Snapshot versioning — new snapshot preserves old
 * - UT-25: Idempotent snapshot — identical data produces no new snapshot
 */
@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private ProfileRepository profileRepository;

    private FieldEncryptionService encryptionService;
    private ProfileService profileService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        var jwt = new AppProperties.Security.Jwt(
                "test-secret-must-be-at-least-32-bytes-long-for-hmac-sha256", 60);
        var cors = new AppProperties.Security.Cors("http://localhost:5173");
        var password = new AppProperties.Security.Password("bcrypt", 4);
        var encryption = new AppProperties.Security.Encryption("test-encryption-key-must-be-32b!");
        var security = new AppProperties.Security(jwt, cors, password, encryption);
        var financial = new AppProperties.Financial(RoundingMode.HALF_UP, 2, 7, 6);
        var props = new AppProperties(security, financial);

        encryptionService = new FieldEncryptionService(props);
        profileService = new ProfileService(profileRepository, encryptionService, objectMapper);
    }

    @Test
    @DisplayName("UT-17: New snapshot preserves old — update creates new and marks old as not-latest")
    void updateProfile_createsNewSnapshot_marksOldNotLatest() {
        String userId = "user1";
        Map<String, Object> existingData = Map.of("personalInfo", Map.of("age", 40));
        ProfileSnapshot existing = new ProfileSnapshot(
                userId, "snap-001", Instant.now(), true, existingData, List.of()
        );
        when(profileRepository.findLatest(userId)).thenReturn(Optional.of(existing));

        Map<String, Object> newData = new LinkedHashMap<>();
        newData.put("personalInfo", Map.of("age", 41));

        ProfileService.UpdateResult result = profileService.updateProfile(userId, newData);

        assertTrue(result.created());
        verify(profileRepository).markNotLatest(userId, "snap-001");

        ArgumentCaptor<ProfileSnapshot> captor = ArgumentCaptor.forClass(ProfileSnapshot.class);
        verify(profileRepository).save(captor.capture());
        ProfileSnapshot saved = captor.getValue();
        assertTrue(saved.isLatest());
        assertNotEquals("snap-001", saved.snapshotId());
    }

    @Test
    @DisplayName("UT-25: Idempotent snapshot — identical data does not create a new snapshot")
    void updateProfile_identicalData_noNewSnapshot() {
        String userId = "user1";
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("personalInfo", new LinkedHashMap<>(Map.of("age", 40)));

        ProfileSnapshot existing = new ProfileSnapshot(
                userId, "snap-001", Instant.now(), true, data, List.of()
        );
        when(profileRepository.findLatest(userId)).thenReturn(Optional.of(existing));

        Map<String, Object> sameData = new LinkedHashMap<>();
        sameData.put("personalInfo", new LinkedHashMap<>(Map.of("age", 40)));

        ProfileService.UpdateResult result = profileService.updateProfile(userId, sameData);

        assertFalse(result.created());
        assertEquals("snap-001", result.snapshotId());
        assertEquals("No changes detected", result.message());
        verify(profileRepository, never()).save(any());
        verify(profileRepository, never()).markNotLatest(any(), any());
    }

    @Test
    @DisplayName("First profile creation saves a snapshot with isLatest=true")
    void updateProfile_firstProfile_savesNewSnapshot() {
        String userId = "user1";
        when(profileRepository.findLatest(userId)).thenReturn(Optional.empty());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("personalInfo", Map.of("age", 40));

        ProfileService.UpdateResult result = profileService.updateProfile(userId, data);

        assertTrue(result.created());
        verify(profileRepository, never()).markNotLatest(any(), any());

        ArgumentCaptor<ProfileSnapshot> captor = ArgumentCaptor.forClass(ProfileSnapshot.class);
        verify(profileRepository).save(captor.capture());
        assertTrue(captor.getValue().isLatest());
    }

    @Test
    @DisplayName("Update with sensitive data encrypts fields before saving")
    void updateProfile_withSensitiveData_encryptsBeforeSave() {
        String userId = "user1";
        when(profileRepository.findLatest(userId)).thenReturn(Optional.empty());

        Map<String, Object> data = new LinkedHashMap<>();
        Map<String, Object> income = new LinkedHashMap<>();
        income.put("monthlySalary", 150000);
        data.put("income", income);

        profileService.updateProfile(userId, data);

        ArgumentCaptor<ProfileSnapshot> captor = ArgumentCaptor.forClass(ProfileSnapshot.class);
        verify(profileRepository).save(captor.capture());
        ProfileSnapshot saved = captor.getValue();

        assertFalse(saved.encryptedFields().isEmpty(), "Should have encrypted fields");
        assertTrue(saved.encryptedFields().contains("income.monthlySalary"));

        @SuppressWarnings("unchecked")
        Map<String, Object> savedIncome = (Map<String, Object>) saved.data().get("income");
        assertTrue(savedIncome.get("monthlySalary").toString().startsWith("ENC:"));
    }

    @Test
    @DisplayName("createProfile throws if profile already exists")
    void createProfile_alreadyExists_throwsException() {
        String userId = "user1";
        ProfileSnapshot existing = new ProfileSnapshot(
                userId, "snap-001", Instant.now(), true, Map.of(), List.of()
        );
        when(profileRepository.findLatest(userId)).thenReturn(Optional.of(existing));

        assertThrows(AppException.class,
                () -> profileService.createProfile(userId, Map.of("test", "data")));
    }

    @Test
    @DisplayName("isDataIdentical returns true for equivalent data structures")
    void isDataIdentical_equivalentMaps_returnsTrue() {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("age", 40);
        a.put("name", "test");

        Map<String, Object> b = new LinkedHashMap<>();
        b.put("age", 40);
        b.put("name", "test");

        assertTrue(profileService.isDataIdentical(a, b));
    }

    @Test
    @DisplayName("isDataIdentical returns false for different data")
    void isDataIdentical_differentMaps_returnsFalse() {
        Map<String, Object> a = Map.of("age", 40);
        Map<String, Object> b = Map.of("age", 41);

        assertFalse(profileService.isDataIdentical(a, b));
    }

    @Test
    @DisplayName("Multiple updates create sequential snapshots")
    void multipleUpdates_createSequentialSnapshots() {
        String userId = "user1";

        when(profileRepository.findLatest(userId))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(new ProfileSnapshot(
                        userId, "snap-001", Instant.now(), true,
                        Map.of("personalInfo", Map.of("age", 40)), List.of()
                )));

        profileService.updateProfile(userId, new LinkedHashMap<>(Map.of(
                "personalInfo", Map.of("age", 40))));

        profileService.updateProfile(userId, new LinkedHashMap<>(Map.of(
                "personalInfo", Map.of("age", 41))));

        verify(profileRepository, times(2)).save(any());
        verify(profileRepository, times(1)).markNotLatest(eq(userId), eq("snap-001"));
    }
}
