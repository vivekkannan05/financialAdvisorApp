package com.financialadvisor.engine;

import com.financialadvisor.engine.model.StressScenario;
import com.financialadvisor.engine.model.StressTestResult;
import com.financialadvisor.exception.AppException;
import com.financialadvisor.exception.ErrorCode;
import com.financialadvisor.profile.ProfileService;
import com.financialadvisor.profile.ProfileSnapshot;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * REST controller for stress testing (§4.2: POST /api/v1/stress-test).
 */
@RestController
@RequestMapping("/api/v1/stress-test")
public class StressTestController {

    private static final Logger log = LoggerFactory.getLogger(StressTestController.class);

    private final StressTestEngine stressTestEngine;
    private final ProfileService profileService;

    public StressTestController(StressTestEngine stressTestEngine,
                                 ProfileService profileService) {
        this.stressTestEngine = stressTestEngine;
        this.profileService = profileService;
    }

    public record StressTestRequest(
            @NotNull StressScenario.StressType type,
            @NotNull BigDecimal parameter,
            Integer durationMonths
    ) {}

    @PostMapping
    public ResponseEntity<StressTestResult> runStressTest(
            Authentication auth,
            @Valid @RequestBody StressTestRequest request) {
        String userId = auth.getName();

        ProfileSnapshot profile = profileService.getCurrentProfile(userId)
                .orElseThrow(() -> new AppException(ErrorCode.INSUFFICIENT_DATA,
                        "No profile found. Create a profile before running stress tests."));

        StressScenario scenario = new StressScenario(
                request.type(), request.parameter(), request.durationMonths(),
                "User-requested " + request.type() + " stress test"
        );

        log.info("Running stress test for user {}: {}", userId, scenario.type());
        StressTestResult result = stressTestEngine.runStressTest(profile.data(), scenario);

        return ResponseEntity.ok(result);
    }
}
