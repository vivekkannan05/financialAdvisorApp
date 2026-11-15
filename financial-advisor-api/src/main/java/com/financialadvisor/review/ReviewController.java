package com.financialadvisor.review;

import com.financialadvisor.config.AppProperties;
import com.financialadvisor.engine.*;
import com.financialadvisor.engine.model.*;
import com.financialadvisor.exception.AppException;
import com.financialadvisor.exception.ErrorCode;
import com.financialadvisor.profile.ProfileService;
import com.financialadvisor.profile.ProfileSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.financialadvisor.engine.ProfileDataExtractor.*;

/**
 * Periodic review and drift detection endpoint (FR-9.x).
 * GET /api/v1/review
 */
@RestController
@RequestMapping("/api/v1/review")
public class ReviewController {

    private static final Logger log = LoggerFactory.getLogger(ReviewController.class);

    private final ProfileService profileService;
    private final HealthAssessmentEngine healthEngine;
    private final CashFlowEngine cashFlowEngine;
    private final GoalPlanningEngine goalEngine;
    private final AppProperties appProperties;

    public ReviewController(ProfileService profileService,
                             HealthAssessmentEngine healthEngine,
                             CashFlowEngine cashFlowEngine,
                             GoalPlanningEngine goalEngine,
                             AppProperties appProperties) {
        this.profileService = profileService;
        this.healthEngine = healthEngine;
        this.cashFlowEngine = cashFlowEngine;
        this.goalEngine = goalEngine;
        this.appProperties = appProperties;
    }

    @GetMapping
    public ReviewResponse getReview(Authentication auth) {
        String userId = auth.getName();
        log.info("Periodic review request for user {}", userId);

        ProfileSnapshot profile = profileService.getCurrentProfile(userId)
                .orElseThrow(() -> new AppException(ErrorCode.INSUFFICIENT_DATA,
                        "No profile found. Create a profile to run a periodic review."));

        Map<String, Object> data = profile.data();
        Instant snapshotDate = profile.snapshotDate();
        int daysSince = (int) ChronoUnit.DAYS.between(
                snapshotDate.truncatedTo(ChronoUnit.DAYS),
                Instant.now().truncatedTo(ChronoUnit.DAYS));
        boolean stale = daysSince > appProperties.financial().stalenessThresholdDays();

        List<ReviewResponse.DriftWarning> warnings = new ArrayList<>();
        List<String> actions = new ArrayList<>();

        if (stale) {
            warnings.add(new ReviewResponse.DriftWarning(
                    "Data Freshness",
                    "Profile data is " + daysSince + " days old (threshold: "
                            + appProperties.financial().stalenessThresholdDays() + " days)",
                    "high"
            ));
            actions.add("Update your profile with current income, expenses, and asset values");
        }

        EmergencyCoverageResult emergency = healthEngine.calculateEmergencyCoverage(data);
        if (emergency.isBelowThreshold()) {
            warnings.add(new ReviewResponse.DriftWarning(
                    "Emergency Coverage",
                    "Coverage at " + emergency.coverageMonths().toPlainString()
                            + " months — below threshold of " + emergency.thresholdMonths() + " months",
                    "critical"
            ));
            actions.add("Prioritise building emergency fund to " + emergency.thresholdMonths() + "+ months");
        }

        CashFlowResult cashFlow = cashFlowEngine.calculateCashFlow(data);
        if (cashFlow.isDeficit()) {
            warnings.add(new ReviewResponse.DriftWarning(
                    "Cash Flow",
                    "Monthly deficit of ₹" + cashFlow.monthlySurplus().abs().toPlainString(),
                    "critical"
            ));
            actions.add("Review expenses — spending exceeds income");
        }

        List<GoalAssessment> goals = goalEngine.assessGoals(data);
        for (GoalAssessment goal : goals) {
            if (goal.fundingPercent().compareTo(new BigDecimal("25")) < 0) {
                warnings.add(new ReviewResponse.DriftWarning(
                        "Goal: " + goal.goalName(),
                        "Severely underfunded at " + goal.fundingPercent().toPlainString()
                                + "% — needs ₹" + goal.requiredMonthlySIP().toPlainString() + "/month SIP",
                        "high"
                ));
                actions.add("Increase SIP allocation for '" + goal.goalName() + "'");
            }
        }

        NetWorthResult netWorth = healthEngine.calculateNetWorth(data);
        if (netWorth.totalLiabilities().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal debtToAsset = netWorth.totalAssets().signum() > 0
                    ? netWorth.totalLiabilities().divide(netWorth.totalAssets(), 4,
                            java.math.RoundingMode.HALF_UP)
                    : BigDecimal.ONE;
            if (debtToAsset.compareTo(new BigDecimal("0.5")) > 0) {
                warnings.add(new ReviewResponse.DriftWarning(
                        "Debt Exposure",
                        "Debt-to-asset ratio at " + debtToAsset.multiply(BigDecimal.valueOf(100))
                                .setScale(1, java.math.RoundingMode.HALF_UP) + "%",
                        "high"
                ));
                actions.add("Consider accelerating debt repayment");
            }
        }

        Map<String, Object> insurance = getSection(data, "insurance");
        if (insurance == null || insurance.isEmpty()) {
            warnings.add(new ReviewResponse.DriftWarning(
                    "Insurance",
                    "No insurance data on record",
                    "high"
            ));
            actions.add("Add term life and health insurance details to your profile");
        }

        if (warnings.isEmpty()) {
            actions.add("Your financial health looks good! Review again in "
                    + appProperties.financial().stalenessThresholdDays() + " days.");
        }

        return ReviewResponse.create(
                userId, profile.snapshotId(), profile.snapshotDate().toString(),
                stale, daysSince, warnings, actions
        );
    }
}
