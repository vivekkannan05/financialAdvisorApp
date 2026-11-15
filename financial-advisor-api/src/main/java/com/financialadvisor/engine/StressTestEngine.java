package com.financialadvisor.engine;

import com.financialadvisor.engine.model.*;
import com.financialadvisor.math.FinancialMathUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.financialadvisor.engine.ProfileDataExtractor.*;

/**
 * Stress Testing Engine (FR-7.x).
 *
 * Models adverse scenarios and shows impact on portfolio, goals,
 * emergency coverage, and EMI serviceability.
 */
@Service
public class StressTestEngine {

    private final HealthAssessmentEngine healthEngine;
    private final GoalPlanningEngine goalEngine;

    public StressTestEngine(HealthAssessmentEngine healthEngine,
                            GoalPlanningEngine goalEngine) {
        this.healthEngine = healthEngine;
        this.goalEngine = goalEngine;
    }

    /**
     * Runs a stress scenario against the profile and returns impact analysis (FR-7.5).
     */
    public StressTestResult runStressTest(Map<String, Object> profileData,
                                           StressScenario scenario) {
        NetWorthResult beforeNw = healthEngine.calculateNetWorth(profileData);
        EmergencyCoverageResult beforeEc = healthEngine.calculateEmergencyCoverage(profileData);
        List<GoalAssessment> beforeGoals = goalEngine.assessGoals(profileData);

        Map<String, Object> stressedProfile = applyStress(profileData, scenario);

        NetWorthResult afterNw = healthEngine.calculateNetWorth(stressedProfile);
        EmergencyCoverageResult afterEc = healthEngine.calculateEmergencyCoverage(stressedProfile);
        List<GoalAssessment> afterGoals = goalEngine.assessGoals(stressedProfile);

        BigDecimal portfolioBefore = beforeNw.liquidAssets();
        BigDecimal portfolioAfter = afterNw.liquidAssets();
        BigDecimal impact = portfolioAfter.subtract(portfolioBefore);

        List<StressTestResult.GoalImpact> goalImpacts = new ArrayList<>();
        for (int i = 0; i < beforeGoals.size(); i++) {
            GoalAssessment before = beforeGoals.get(i);
            GoalAssessment after = (i < afterGoals.size()) ? afterGoals.get(i) : before;

            boolean wasViable = before.fundingPercent().compareTo(BigDecimal.valueOf(50)) >= 0;
            boolean stillViable = after.fundingPercent().compareTo(BigDecimal.valueOf(50)) >= 0;

            String status;
            if (wasViable && !stillViable) status = "at-risk";
            else if (!wasViable && !stillViable) status = "was-already-at-risk";
            else if (after.fundingPercent().compareTo(before.fundingPercent()) < 0) status = "impacted";
            else status = "unaffected";

            goalImpacts.add(new StressTestResult.GoalImpact(
                    before.goalId(), before.goalName(),
                    before.fundingPercent(), after.fundingPercent(),
                    wasViable, stillViable, status
            ));
        }

        List<String> warnings = new ArrayList<>();
        if (afterEc.isBelowThreshold()) {
            warnings.add("Emergency coverage drops to " + afterEc.coverageMonths()
                    + " months — below safe threshold of " + afterEc.thresholdMonths() + " months");
        }
        if (impact.compareTo(BigDecimal.ZERO) < 0) {
            warnings.add("Portfolio value decreases by ₹" + impact.abs().toPlainString());
        }
        for (StressTestResult.GoalImpact gi : goalImpacts) {
            if ("at-risk".equals(gi.status())) {
                warnings.add("Goal '" + gi.goalName() + "' becomes at risk (funding drops from "
                        + gi.fundingPercentBefore() + "% to " + gi.fundingPercentAfter() + "%)");
            }
        }

        return new StressTestResult(
                scenario, portfolioBefore, portfolioAfter, impact,
                beforeEc.coverageMonths(), afterEc.coverageMonths(),
                goalImpacts, warnings
        );
    }

    /**
     * Applies a stress scenario to a copy of the profile data.
     */
    @SuppressWarnings("unchecked")
    Map<String, Object> applyStress(Map<String, Object> profileData, StressScenario scenario) {
        Map<String, Object> stressed = deepCopyMap(profileData);

        switch (scenario.type()) {
            case INCOME_LOSS -> applyIncomeLoss(stressed, scenario.parameter());
            case MARKET_FALL -> applyMarketFall(stressed, scenario.parameter());
            case INTEREST_RATE_CHANGE -> applyInterestRateChange(stressed, scenario.parameter());
            case LARGE_EXPENSE -> applyLargeExpense(stressed, scenario.parameter());
        }

        return stressed;
    }

    /**
     * FR-7.1: Income loss — reduces monthly income by the given fraction.
     */
    private void applyIncomeLoss(Map<String, Object> profile, BigDecimal lossFraction) {
        Map<String, Object> income = getMutableSection(profile, "income");
        BigDecimal salary = getBigDecimal(income, "monthlySalary");
        if (salary != null) {
            BigDecimal remaining = BigDecimal.ONE.subtract(lossFraction);
            income.put("monthlySalary", salary.multiply(remaining));
        }
    }

    /**
     * FR-7.2: Market fall — reduces equity/MF asset values by the drawdown fraction.
     */
    @SuppressWarnings("unchecked")
    private void applyMarketFall(Map<String, Object> profile, BigDecimal drawdown) {
        Map<String, Object> assets = getMutableSection(profile, "assets");
        BigDecimal remaining = BigDecimal.ONE.subtract(drawdown);

        for (Map.Entry<String, Object> entry : assets.entrySet()) {
            String name = entry.getKey().toLowerCase();
            if (isEquityAsset(name)) {
                if (entry.getValue() instanceof Map<?, ?> asset) {
                    Map<String, Object> assetMap = (Map<String, Object>) asset;
                    BigDecimal value = getBigDecimal(assetMap, "value");
                    if (value != null) {
                        assetMap.put("value", FinancialMathUtil.roundInr(value.multiply(remaining)));
                    }
                } else {
                    BigDecimal value = FinancialMathUtil.toBigDecimal(entry.getValue());
                    if (value != null) {
                        assets.put(entry.getKey(), FinancialMathUtil.roundInr(value.multiply(remaining)));
                    }
                }
            }
        }
    }

    /**
     * FR-7.3: Interest rate change — adjusts EMI amounts for variable-rate loans.
     */
    @SuppressWarnings("unchecked")
    private void applyInterestRateChange(Map<String, Object> profile, BigDecimal rateChange) {
        Map<String, Object> liabilities = getMutableSection(profile, "liabilities");

        for (Map.Entry<String, Object> entry : liabilities.entrySet()) {
            if (entry.getValue() instanceof Map<?, ?> liab) {
                Map<String, Object> liabMap = (Map<String, Object>) liab;
                BigDecimal emi = getBigDecimal(liabMap, "emiAmount");
                String emiKey = "emiAmount";
                if (emi == null) {
                    emi = getBigDecimal(liabMap, "emi");
                    emiKey = "emi";
                }
                if (emi != null) {
                    BigDecimal factor = BigDecimal.ONE.add(rateChange.multiply(BigDecimal.valueOf(5)));
                    liabMap.put(emiKey, FinancialMathUtil.roundInr(emi.multiply(factor)));
                }
            }
        }
    }

    /**
     * FR-7.4: Large unplanned expense — deducts from emergency/liquid assets.
     */
    @SuppressWarnings("unchecked")
    private void applyLargeExpense(Map<String, Object> profile, BigDecimal amount) {
        Map<String, Object> assets = getMutableSection(profile, "assets");
        BigDecimal remaining = amount;

        for (Map.Entry<String, Object> entry : assets.entrySet()) {
            if (remaining.signum() <= 0) break;
            if (entry.getValue() instanceof Map<?, ?> asset) {
                Map<String, Object> assetMap = (Map<String, Object>) asset;
                if (getBoolean(assetMap, "locked")) continue;

                BigDecimal value = getBigDecimal(assetMap, "value");
                if (value != null && value.signum() > 0) {
                    BigDecimal deduction = value.min(remaining);
                    assetMap.put("value", FinancialMathUtil.roundInr(value.subtract(deduction)));
                    remaining = remaining.subtract(deduction);
                }
            } else {
                BigDecimal value = FinancialMathUtil.toBigDecimal(entry.getValue());
                if (value != null && value.signum() > 0) {
                    BigDecimal deduction = value.min(remaining);
                    assets.put(entry.getKey(), FinancialMathUtil.roundInr(value.subtract(deduction)));
                    remaining = remaining.subtract(deduction);
                }
            }
        }
    }

    private boolean isEquityAsset(String name) {
        return name.contains("stock") || name.contains("mutual") || name.contains("equity")
                || name.contains("mf") || name.contains("fund");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMutableSection(Map<String, Object> data, String key) {
        Object val = data.get(key);
        if (val instanceof Map<?, ?> map) {
            Map<String, Object> mutable = new LinkedHashMap<>((Map<String, Object>) map);
            data.put(key, mutable);
            return mutable;
        }
        Map<String, Object> empty = new LinkedHashMap<>();
        data.put(key, empty);
        return empty;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> deepCopyMap(Map<String, Object> original) {
        Map<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : original.entrySet()) {
            if (entry.getValue() instanceof Map<?, ?> nested) {
                copy.put(entry.getKey(), deepCopyMap((Map<String, Object>) nested));
            } else if (entry.getValue() instanceof List<?> list) {
                List<Object> listCopy = new ArrayList<>(list.size());
                for (Object item : list) {
                    if (item instanceof Map<?, ?> mapItem) {
                        listCopy.add(deepCopyMap((Map<String, Object>) mapItem));
                    } else {
                        listCopy.add(item);
                    }
                }
                copy.put(entry.getKey(), listCopy);
            } else {
                copy.put(entry.getKey(), entry.getValue());
            }
        }
        return copy;
    }
}
