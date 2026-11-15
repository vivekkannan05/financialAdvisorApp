package com.financialadvisor.engine;

import com.financialadvisor.engine.model.GoalAssessment;
import com.financialadvisor.math.FinancialMathUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.financialadvisor.engine.ProfileDataExtractor.*;

/**
 * Goal Planning Engine (FR-3.x).
 *
 * Calculates: future goal values, funding status, shortfall,
 * required monthly SIP, and progress tracking.
 */
@Service
public class GoalPlanningEngine {

    private static final BigDecimal DEFAULT_INFLATION = new BigDecimal("0.06");
    private static final BigDecimal DEFAULT_RETURN_RATE = new BigDecimal("0.10");

    /**
     * Assesses all goals in the profile (FR-3.1 to FR-3.4).
     */
    public List<GoalAssessment> assessGoals(Map<String, Object> profileData) {
        List<Map<String, Object>> goals = getListSection(profileData, "goals");
        Map<String, Object> assets = getSection(profileData, "assets");
        List<GoalAssessment> assessments = new ArrayList<>();

        for (Map<String, Object> goal : goals) {
            assessments.add(assessSingleGoal(goal, assets));
        }

        return assessments;
    }

    /**
     * Assesses a single goal: future value, current funding, shortfall, required SIP.
     */
    GoalAssessment assessSingleGoal(Map<String, Object> goal, Map<String, Object> assets) {
        String goalId = getString(goal, "id");
        String goalName = getString(goal, "name");
        if (goalId == null && goalName != null) {
            goalId = goalName.toLowerCase().replaceAll("\\s+", "-");
        }

        BigDecimal targetToday = getBigDecimal(goal, "targetAmountToday");
        if (targetToday == null) {
            targetToday = getBigDecimal(goal, "targetAmount");
        }
        if (targetToday == null) {
            targetToday = BigDecimal.ZERO;
        }

        Integer targetYear = getInteger(goal, "targetYear");
        int yearsRemaining = (targetYear != null) ? Math.max(0, targetYear - java.time.Year.now().getValue()) : 10;

        BigDecimal inflationRate = getBigDecimal(goal, "inflationRate");
        if (inflationRate == null) inflationRate = DEFAULT_INFLATION;

        BigDecimal returnRate = getBigDecimal(goal, "expectedReturnRate");
        if (returnRate == null) returnRate = DEFAULT_RETURN_RATE;

        BigDecimal targetFuture = FinancialMathUtil.futureValue(targetToday, inflationRate, yearsRemaining);

        BigDecimal currentFunding = calculateGoalFunding(goalId, assets);

        BigDecimal projectedFunding = FinancialMathUtil.futureValue(currentFunding, returnRate, yearsRemaining);
        BigDecimal shortfall = targetFuture.subtract(projectedFunding);
        if (shortfall.signum() < 0) shortfall = BigDecimal.ZERO;

        BigDecimal fundingPercent = FinancialMathUtil.goalFundingPercent(
                currentFunding, projectedFunding, targetFuture);

        BigDecimal requiredSIP = BigDecimal.ZERO;
        if (shortfall.signum() > 0 && yearsRemaining > 0) {
            requiredSIP = FinancialMathUtil.requiredMonthlyContribution(
                    targetFuture, currentFunding, returnRate, yearsRemaining);
        }

        String status;
        if (fundingPercent.compareTo(BigDecimal.valueOf(100)) >= 0) {
            status = "on-track";
        } else if (fundingPercent.compareTo(BigDecimal.valueOf(75)) >= 0) {
            status = "near-target";
        } else if (fundingPercent.compareTo(BigDecimal.valueOf(50)) >= 0) {
            status = "needs-attention";
        } else {
            status = "at-risk";
        }

        return new GoalAssessment(
                goalId, goalName, targetToday, targetFuture, currentFunding,
                shortfall, fundingPercent, requiredSIP, yearsRemaining, status
        );
    }

    /**
     * Calculates current funding for a goal by summing assets tagged with the goalId (FR-3.7).
     * Each asset has exactly one goalTag — no double-counting.
     */
    @SuppressWarnings("unchecked")
    private BigDecimal calculateGoalFunding(String goalId, Map<String, Object> assets) {
        if (goalId == null) return BigDecimal.ZERO;

        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<String, Object> entry : assets.entrySet()) {
            if (entry.getValue() instanceof Map<?, ?> assetMap) {
                Map<String, Object> asset = (Map<String, Object>) assetMap;
                String tag = getString(asset, "goalTag");
                if (goalId.equals(tag)) {
                    BigDecimal value = getBigDecimal(asset, "value");
                    if (value != null) total = total.add(value);
                }
            }
        }
        return total;
    }
}
