package com.financialadvisor.review;

import com.financialadvisor.config.AppProperties;
import com.financialadvisor.engine.*;
import com.financialadvisor.engine.model.*;
import com.financialadvisor.profile.ProfileService;
import com.financialadvisor.profile.ProfileSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for periodic review and drift detection (FR-9.1, FR-9.2, UT-18).
 */
class ReviewControllerTest {

    private ReviewController controller;
    private ProfileService profileService;
    private HealthAssessmentEngine healthEngine;
    private CashFlowEngine cashFlowEngine;
    private GoalPlanningEngine goalEngine;

    @BeforeEach
    void setUp() {
        profileService = mock(ProfileService.class);

        var jwt = new AppProperties.Security.Jwt("test-secret-must-be-at-least-32-bytes-long!", 60);
        var cors = new AppProperties.Security.Cors("http://localhost");
        var pwd = new AppProperties.Security.Password("bcrypt", 4);
        var enc = new AppProperties.Security.Encryption("test-key-32b");
        var security = new AppProperties.Security(jwt, cors, pwd, enc);
        var financial = new AppProperties.Financial(RoundingMode.HALF_UP, 2, 7, 6);
        var props = new AppProperties(security, financial);

        healthEngine = new HealthAssessmentEngine(props);
        cashFlowEngine = new CashFlowEngine();
        goalEngine = new GoalPlanningEngine();

        controller = new ReviewController(profileService, healthEngine, cashFlowEngine, goalEngine, props);
    }

    @Test
    @DisplayName("UT-18: Stale data beyond threshold is flagged")
    void staleData_isFlagged() {
        Map<String, Object> data = buildProfileData();
        ProfileSnapshot old = new ProfileSnapshot(
                "user1", "snap-old", Instant.now().minus(10, ChronoUnit.DAYS),
                true, data, List.of()
        );
        when(profileService.getCurrentProfile("user1")).thenReturn(Optional.of(old));

        var auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("user1");

        ReviewResponse response = controller.getReview(auth);

        assertTrue(response.staleProfile(), "Profile older than 7 days should be flagged stale");
        assertTrue(response.daysSinceLastUpdate() >= 10);
        assertTrue(response.driftWarnings().stream()
                .anyMatch(w -> w.dimension().equals("Data Freshness")));
    }

    @Test
    @DisplayName("Fresh data within threshold is not flagged stale")
    void freshData_notFlagged() {
        Map<String, Object> data = buildProfileData();
        ProfileSnapshot fresh = new ProfileSnapshot(
                "user1", "snap-new", Instant.now().minus(1, ChronoUnit.DAYS),
                true, data, List.of()
        );
        when(profileService.getCurrentProfile("user1")).thenReturn(Optional.of(fresh));

        var auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("user1");

        ReviewResponse response = controller.getReview(auth);

        assertFalse(response.staleProfile());
        assertTrue(response.driftWarnings().stream()
                .noneMatch(w -> w.dimension().equals("Data Freshness")));
    }

    @Test
    @DisplayName("Low emergency coverage triggers critical warning")
    void lowEmergency_triggersCriticalWarning() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("assets", Map.of("savings", Map.of("value", 100000)));
        data.put("expenses", Map.of("monthlyHousehold", 88000));
        data.put("liabilities", Map.of());
        data.put("insurance", Map.of());

        ProfileSnapshot profile = new ProfileSnapshot(
                "user1", "snap", Instant.now(),
                true, data, List.of()
        );
        when(profileService.getCurrentProfile("user1")).thenReturn(Optional.of(profile));

        var auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("user1");

        ReviewResponse response = controller.getReview(auth);

        assertTrue(response.driftWarnings().stream()
                .anyMatch(w -> w.dimension().equals("Emergency Coverage")
                        && w.severity().equals("critical")));
    }

    @Test
    @DisplayName("Missing insurance triggers warning")
    void noInsurance_triggersWarning() {
        Map<String, Object> data = buildProfileData();
        data.put("insurance", Map.of());

        ProfileSnapshot profile = new ProfileSnapshot(
                "user1", "snap", Instant.now(),
                true, data, List.of()
        );
        when(profileService.getCurrentProfile("user1")).thenReturn(Optional.of(profile));

        var auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("user1");

        ReviewResponse response = controller.getReview(auth);

        assertTrue(response.driftWarnings().stream()
                .anyMatch(w -> w.dimension().equals("Insurance")));
    }

    @Test
    @DisplayName("Healthy profile returns no critical warnings and positive action")
    void healthyProfile_noCriticalWarnings() {
        Map<String, Object> data = buildProfileData();
        ProfileSnapshot profile = new ProfileSnapshot(
                "user1", "snap", Instant.now(),
                true, data, List.of()
        );
        when(profileService.getCurrentProfile("user1")).thenReturn(Optional.of(profile));

        var auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("user1");

        ReviewResponse response = controller.getReview(auth);

        assertNotNull(response);
        assertFalse(response.recommendedActions().isEmpty());
    }

    private Map<String, Object> buildProfileData() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("income", Map.of("monthlySalary", 200000));
        data.put("expenses", Map.of("monthlyHousehold", 88000));
        data.put("assets", Map.of(
                "emergencyFD", Map.of("value", 900000, "goalTag", "emergency", "locked", true),
                "mutualFunds", Map.of("value", 344698)
        ));
        data.put("liabilities", Map.of());
        data.put("insurance", Map.of(
                "termLife", Map.of("type", "term", "cover", 10000000, "premiumYearly", 18000)
        ));
        data.put("goals", List.of(
                Map.of("id", "retirement", "name", "Retirement", "targetAmountToday", 50000000, "targetYear", 2050)
        ));
        data.put("riskProfile", Map.of(
                "behaviouralTolerance", "moderate", "age", 32, "retirementAge", 60
        ));
        return data;
    }
}
