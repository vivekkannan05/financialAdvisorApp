package com.financialadvisor.engine;

import com.financialadvisor.engine.model.ScenarioComparison;
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

/**
 * REST controller for scenario comparison (§4.2: POST /api/v1/scenario/compare).
 */
@RestController
@RequestMapping("/api/v1/scenario")
public class ScenarioController {

    private static final Logger log = LoggerFactory.getLogger(ScenarioController.class);

    private final ScenarioEngine scenarioEngine;
    private final ProfileService profileService;

    public ScenarioController(ScenarioEngine scenarioEngine,
                               ProfileService profileService) {
        this.scenarioEngine = scenarioEngine;
        this.profileService = profileService;
    }

    public record SipVsPrepayRequest(
            @NotNull BigDecimal sipIncrease,
            @NotNull BigDecimal prepaymentAmount,
            @NotNull BigDecimal loanInterestRate,
            @NotNull Integer yearsRemaining
    ) {}

    @PostMapping("/compare")
    public ResponseEntity<ScenarioComparison> compareSipVsPrepay(
            Authentication auth,
            @Valid @RequestBody SipVsPrepayRequest request) {
        String userId = auth.getName();

        ProfileSnapshot profile = profileService.getCurrentProfile(userId)
                .orElseThrow(() -> new AppException(ErrorCode.INSUFFICIENT_DATA,
                        "No profile found. Create a profile before comparing scenarios."));

        log.info("Running SIP vs Prepayment comparison for user {}", userId);
        ScenarioComparison result = scenarioEngine.compareSipVsPrepayment(
                profile.data(),
                request.sipIncrease(),
                request.prepaymentAmount(),
                request.loanInterestRate(),
                request.yearsRemaining()
        );

        return ResponseEntity.ok(result);
    }
}
