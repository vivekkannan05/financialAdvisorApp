package com.financialadvisor.advisory;

import com.financialadvisor.engine.*;
import com.financialadvisor.engine.model.*;
import com.financialadvisor.engine.risk.CompositeRiskResult;
import com.financialadvisor.engine.risk.RiskAssessor;
import com.financialadvisor.engine.risk.RiskContext;
import com.financialadvisor.profile.ProfileService;
import com.financialadvisor.profile.ProfileSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.financialadvisor.math.FinancialMathUtil;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.financialadvisor.engine.ProfileDataExtractor.*;

/**
 * Spring AI tool functions registered for LLM function calling (FR-11.x, FR-12.3).
 *
 * These tools let the LLM invoke deterministic Java rule engines to get verified
 * financial data — the LLM never fabricates numbers (FR-12.4).
 */
@Component
public class FinancialAdvisoryTools {

    private static final Logger log = LoggerFactory.getLogger(FinancialAdvisoryTools.class);

    private final ProfileService profileService;
    private final HealthAssessmentEngine healthEngine;
    private final GoalPlanningEngine goalEngine;
    private final CashFlowEngine cashFlowEngine;
    private final RiskAssessor riskAssessor;
    private final StressTestEngine stressTestEngine;
    private final ScenarioEngine scenarioEngine;

    public FinancialAdvisoryTools(ProfileService profileService,
                                   HealthAssessmentEngine healthEngine,
                                   GoalPlanningEngine goalEngine,
                                   CashFlowEngine cashFlowEngine,
                                   RiskAssessor riskAssessor,
                                   StressTestEngine stressTestEngine,
                                   ScenarioEngine scenarioEngine) {
        this.profileService = profileService;
        this.healthEngine = healthEngine;
        this.goalEngine = goalEngine;
        this.cashFlowEngine = cashFlowEngine;
        this.riskAssessor = riskAssessor;
        this.stressTestEngine = stressTestEngine;
        this.scenarioEngine = scenarioEngine;
    }

    @Tool(description = "Get the user's complete financial health assessment including net worth " +
            "(liquid and illiquid breakdown), cash flow surplus/deficit, and emergency fund coverage. " +
            "Call this when the user asks about their financial health, net worth, cash flow, or emergency fund.")
    public Map<String, Object> getFinancialHealth(
            @ToolParam(description = "The user's ID") String userId) {
        log.info("Tool call: getFinancialHealth for user {}", userId);
        Map<String, Object> profileData = loadProfileData(userId);

        NetWorthResult netWorth = healthEngine.calculateNetWorth(profileData);
        CashFlowResult cashFlow = cashFlowEngine.calculateCashFlow(profileData);
        EmergencyCoverageResult emergency = healthEngine.calculateEmergencyCoverage(profileData);
        BigDecimal investable = healthEngine.calculateInvestableCapital(profileData);

        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, String> nwMap = new LinkedHashMap<>();
        nwMap.put("totalAssets", netWorth.totalAssets().toPlainString());
        nwMap.put("liquidAssets", netWorth.liquidAssets().toPlainString());
        nwMap.put("marketLinkedAssets", netWorth.marketLinkedAssets().toPlainString());
        nwMap.put("restrictedAssets", netWorth.restrictedAssets().toPlainString());
        nwMap.put("illiquidAssets", netWorth.illiquidAssets().toPlainString());
        nwMap.put("totalLiabilities", netWorth.totalLiabilities().toPlainString());
        nwMap.put("netWorth", netWorth.netWorth().toPlainString());
        result.put("netWorth", nwMap);
        result.put("cashFlow", Map.of(
                "grossMonthlyIncome", cashFlow.grossMonthlyIncome().toPlainString(),
                "monthlyExpenses", cashFlow.monthlyExpenses().toPlainString(),
                "monthlyEMIs", cashFlow.monthlyEMIs().toPlainString(),
                "monthlyInsurance", cashFlow.monthlyInsurance().toPlainString(),
                "monthlyInvestments", cashFlow.monthlyInvestments().toPlainString(),
                "monthlySurplus", cashFlow.monthlySurplus().toPlainString(),
                "hasSurplus", cashFlow.hasSurplus()
        ));
        result.put("emergencyCoverage", Map.of(
                "coverageMonths", emergency.coverageMonths().toPlainString(),
                "thresholdMonths", emergency.thresholdMonths(),
                "isBelowThreshold", emergency.isBelowThreshold(),
                "emergencyAssets", emergency.emergencyAssets().toPlainString(),
                "mandatoryMonthly", emergency.mandatoryMonthlyExpense().toPlainString()
        ));
        result.put("investableCapital", investable.toPlainString());

        return result;
    }

    @Tool(description = "Calculate goal funding status for all of the user's financial goals. " +
            "Shows current funding percentage, shortfall, required monthly SIP for each goal. " +
            "Call this when the user asks about goals, retirement, education funding, or savings targets.")
    public Map<String, Object> calculateGoalFunding(
            @ToolParam(description = "The user's ID") String userId) {
        log.info("Tool call: calculateGoalFunding for user {}", userId);
        Map<String, Object> profileData = loadProfileData(userId);

        List<GoalAssessment> goals = goalEngine.assessGoals(profileData);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("goalCount", goals.size());
        result.put("goals", goals.stream().map(g -> {
            Map<String, Object> goalMap = new LinkedHashMap<>();
            goalMap.put("goalId", g.goalId());
            goalMap.put("goalName", g.goalName());
            goalMap.put("targetAmountToday", g.targetAmountToday().toPlainString());
            goalMap.put("targetAmountFuture", g.targetAmountFuture().toPlainString());
            goalMap.put("currentFunding", g.currentFunding().toPlainString());
            goalMap.put("shortfall", g.shortfall().toPlainString());
            goalMap.put("requiredMonthlySIP", g.requiredMonthlySIP().toPlainString());
            goalMap.put("fundingPercent", g.fundingPercent().toPlainString());
            goalMap.put("isFullyFunded", g.isFullyFunded());
            goalMap.put("yearsRemaining", g.yearsRemaining());
            goalMap.put("status", g.status());
            return goalMap;
        }).toList());

        return result;
    }

    @Tool(description = "Assess the user's composite risk profile across 5 dimensions: " +
            "tolerance, capacity, requirement, horizon, life stage. The safer constraint wins. " +
            "Call this when the user asks about risk tolerance, investment suitability, or asset allocation.")
    public Map<String, Object> assessRisk(
            @ToolParam(description = "The user's ID") String userId) {
        log.info("Tool call: assessRisk for user {}", userId);
        Map<String, Object> profileData = loadProfileData(userId);

        Map<String, Object> riskProfile = getSection(profileData, "riskProfile");
        String tolerance = getString(riskProfile, "behaviouralTolerance");
        Integer age = getInteger(riskProfile, "age");
        Integer retirementAge = getInteger(riskProfile, "retirementAge");

        NetWorthResult nw = healthEngine.calculateNetWorth(profileData);
        CashFlowResult cf = cashFlowEngine.calculateCashFlow(profileData);
        EmergencyCoverageResult ec = healthEngine.calculateEmergencyCoverage(profileData);
        List<GoalAssessment> goals = goalEngine.assessGoals(profileData);

        int shortestHorizon = goals.stream()
                .mapToInt(GoalAssessment::yearsRemaining)
                .min().orElse(20);

        BigDecimal requiredReturn = goals.stream()
                .map(GoalAssessment::fundingPercent)
                .filter(fp -> fp.compareTo(new BigDecimal("100")) < 0)
                .findFirst().map(_ -> new BigDecimal("0.12")).orElse(null);

        BigDecimal accessible = nw.accessibleAssets();
        RiskContext ctx = new RiskContext(
                tolerance, cf.monthlySurplus(), nw.totalLiabilities(),
                accessible, cf.grossMonthlyIncome(),
                ec.coverageMonths(), requiredReturn,
                shortestHorizon, age != null ? age : 35,
                retirementAge, accessible, nw.totalLiabilities()
        );

        CompositeRiskResult risk = riskAssessor.assess(ctx);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("effectiveRiskLevel", risk.effectiveLevel().name());
        result.put("constrainingDimension", risk.constrainingDimension());
        result.put("summary", risk.summary());
        result.put("dimensionScores", risk.dimensionScores());

        return result;
    }

    @Tool(description = "Run a financial stress test scenario: income loss, market fall, " +
            "interest rate change, or large unplanned expense. Shows impact on portfolio and goals. " +
            "Call this when the user asks 'what if I lose my job', 'what if markets crash', etc.")
    public Map<String, Object> runStressTest(
            @ToolParam(description = "The user's ID") String userId,
            @ToolParam(description = "Stress type: INCOME_LOSS, MARKET_FALL, INTEREST_RATE_CHANGE, or LARGE_EXPENSE") String stressType,
            @ToolParam(description = "Parameter: fraction for income/market (e.g. 0.5 for 50%), rate change (e.g. 0.02), or amount for expense") double parameter) {
        log.info("Tool call: runStressTest for user {} — type={}, param={}", userId, stressType, parameter);
        Map<String, Object> profileData = loadProfileData(userId);

        StressScenario scenario = new StressScenario(
                StressScenario.StressType.valueOf(stressType),
                BigDecimal.valueOf(parameter), null,
                "Advisory tool stress test: " + stressType
        );

        StressTestResult result = stressTestEngine.runStressTest(profileData, scenario);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("stressType", stressType);
        response.put("portfolioBefore", result.portfolioValueBefore().toPlainString());
        response.put("portfolioAfter", result.portfolioValueAfter().toPlainString());
        response.put("portfolioImpact", result.portfolioImpact().toPlainString());
        response.put("emergencyCoverageBefore", result.emergencyCoverageBefore().toPlainString());
        response.put("emergencyCoverageAfter", result.emergencyCoverageAfter().toPlainString());
        response.put("goalImpacts", result.goalImpacts().stream().map(gi -> Map.of(
                "goalName", gi.goalName(),
                "fundingBefore", gi.fundingPercentBefore().toPlainString(),
                "fundingAfter", gi.fundingPercentAfter().toPlainString(),
                "status", gi.status()
        )).toList());
        response.put("warnings", result.warnings());

        return response;
    }

    @Tool(description = "Compare SIP increase vs mortgage prepayment scenarios side by side. " +
            "Shows projected corpus growth, interest saved, and tradeoffs. " +
            "Call this when the user asks whether to invest more or prepay their home loan.")
    public Map<String, Object> compareScenarios(
            @ToolParam(description = "The user's ID") String userId,
            @ToolParam(description = "Monthly SIP increase amount in INR") double sipIncrease,
            @ToolParam(description = "Lump sum prepayment amount in INR") double prepaymentAmount,
            @ToolParam(description = "Annual home loan interest rate as decimal (e.g. 0.085 for 8.5%)") double loanRate,
            @ToolParam(description = "Remaining loan tenure in years") int years) {
        log.info("Tool call: compareScenarios for user {} — SIP=₹{}, prepay=₹{}", userId, sipIncrease, prepaymentAmount);
        Map<String, Object> profileData = loadProfileData(userId);

        ScenarioComparison comparison = scenarioEngine.compareSipVsPrepayment(
                profileData,
                BigDecimal.valueOf(sipIncrease),
                BigDecimal.valueOf(prepaymentAmount),
                BigDecimal.valueOf(loanRate),
                years
        );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("scenarioA", comparison.scenarioAName());
        response.put("scenarioB", comparison.scenarioBName());
        response.put("recommendation", comparison.recommendation());
        response.put("tradeoffs", comparison.tradeoffs());
        response.put("sipCorpus", comparison.scenarioA().projectedCorpus().toPlainString());
        response.put("interestSaved", comparison.scenarioB().totalInterestSaved().toPlainString());

        return response;
    }

    @Tool(description = "Get the user's insurance coverage details including all policies, " +
            "covered persons, types (term life, health, etc.), cover amounts, and premiums. " +
            "Call this when the user asks about insurance, protection, coverage gaps, or policy adequacy.")
    public Map<String, Object> getInsuranceSummary(
            @ToolParam(description = "The user's ID") String userId) {
        log.info("Tool call: getInsuranceSummary for user {}", userId);
        Map<String, Object> profileData = loadProfileData(userId);

        Map<String, Object> insurance = getSection(profileData, "insurance");
        Map<String, Object> income = getSection(profileData, "income");
        BigDecimal annualIncome = FinancialMathUtil.toBigDecimal(income.get("monthlySalary"))
                .multiply(BigDecimal.valueOf(12));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("annualIncome", annualIncome.toPlainString());

        List<Map<String, Object>> policies = new ArrayList<>();
        BigDecimal totalTermCover = BigDecimal.ZERO;
        BigDecimal totalHealthCover = BigDecimal.ZERO;
        BigDecimal totalAnnualPremium = BigDecimal.ZERO;

        for (Map.Entry<String, Object> entry : insurance.entrySet()) {
            if (entry.getValue() instanceof Map<?, ?> policyMap) {
                @SuppressWarnings("unchecked")
                Map<String, Object> policy = (Map<String, Object>) policyMap;
                Map<String, Object> policyInfo = new LinkedHashMap<>();

                String policyKey = entry.getKey();
                String policyType = (String) policy.getOrDefault("type", "unknown");
                String coveredPerson = (String) policy.getOrDefault("coveredPerson", "Self");
                BigDecimal cover = FinancialMathUtil.toBigDecimal(policy.get("cover"));
                BigDecimal premium = FinancialMathUtil.toBigDecimal(
                        policy.containsKey("premiumYearly") ? policy.get("premiumYearly")
                                : policy.get("premium"));

                policyInfo.put("policyKey", policyKey);
                policyInfo.put("type", policyType);
                policyInfo.put("coveredPerson", coveredPerson);
                policyInfo.put("coverAmount", cover.toPlainString());
                policyInfo.put("annualPremium", premium.toPlainString());
                policies.add(policyInfo);

                if ("term".equalsIgnoreCase(policyType)) {
                    totalTermCover = totalTermCover.add(cover);
                } else if ("health".equalsIgnoreCase(policyType)) {
                    totalHealthCover = totalHealthCover.add(cover);
                }
                totalAnnualPremium = totalAnnualPremium.add(premium);
            }
        }

        result.put("policies", policies);
        result.put("policyCount", policies.size());
        result.put("totalTermLifeCover", totalTermCover.toPlainString());
        result.put("totalHealthCover", totalHealthCover.toPlainString());
        result.put("totalAnnualPremium", totalAnnualPremium.toPlainString());

        BigDecimal termMultiple = annualIncome.signum() > 0
                ? totalTermCover.divide(annualIncome, 1, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        result.put("termLifeMultipleOfIncome", termMultiple.toPlainString());
        result.put("termLifeAdequacy", termMultiple.compareTo(BigDecimal.TEN) >= 0
                ? "adequate" : termMultiple.compareTo(BigDecimal.ZERO) == 0
                ? "none" : "insufficient");
        result.put("healthCoverAdequacy", totalHealthCover.compareTo(new BigDecimal("500000")) >= 0
                ? "adequate" : totalHealthCover.compareTo(BigDecimal.ZERO) == 0
                ? "none" : "insufficient");

        return result;
    }

    /**
     * Generates a compact profile summary for pre-loading into conversation context.
     * Not a tool — called directly by ChatService.
     */
    public String generateProfileSummary(String userId) {
        Map<String, Object> profileData = loadProfileData(userId);
        if (profileData.containsKey("_error")) {
            return "No profile found for this user.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== USER PROFILE SNAPSHOT ===\n");

        Map<String, Object> riskProfile = getSection(profileData, "riskProfile");
        Integer age = getInteger(riskProfile, "age");
        Integer retirementAge = getInteger(riskProfile, "retirementAge");
        String tolerance = getString(riskProfile, "behaviouralTolerance");
        if (age != null) sb.append("Age: ").append(age);
        if (retirementAge != null) sb.append(" | Retirement age: ").append(retirementAge);
        if (tolerance != null) sb.append(" | Risk tolerance: ").append(tolerance);
        if (age != null) sb.append("\n");

        Map<String, Object> income = getSection(profileData, "income");
        BigDecimal salary = FinancialMathUtil.toBigDecimal(income.get("monthlySalary"));
        BigDecimal otherIncome = FinancialMathUtil.toBigDecimal(income.get("otherIncome"));
        sb.append("Monthly income: ₹").append(salary.toPlainString());
        if (otherIncome.signum() > 0) {
            sb.append(" + ₹").append(otherIncome.toPlainString()).append(" other");
        }
        sb.append("\n");

        try {
            NetWorthResult nw = healthEngine.calculateNetWorth(profileData);
            sb.append("Net worth: ₹").append(nw.netWorth().toPlainString())
                    .append(" (Liquid: ₹").append(nw.liquidAssets().toPlainString())
                    .append(", Market-linked: ₹").append(nw.marketLinkedAssets().toPlainString())
                    .append(", Restricted: ₹").append(nw.restrictedAssets().toPlainString())
                    .append(", Illiquid: ₹").append(nw.illiquidAssets().toPlainString())
                    .append(", Loans: ₹").append(nw.totalLiabilities().toPlainString())
                    .append(")\n");

            CashFlowResult cf = cashFlowEngine.calculateCashFlow(profileData);
            sb.append("Monthly surplus: ₹").append(cf.monthlySurplus().toPlainString())
                    .append(cf.hasSurplus() ? " (positive)" : " (DEFICIT)").append("\n");

            EmergencyCoverageResult ec = healthEngine.calculateEmergencyCoverage(profileData);
            sb.append("Emergency cover: ").append(ec.coverageMonths().setScale(1, java.math.RoundingMode.HALF_UP))
                    .append(" months (threshold: ").append(ec.thresholdMonths()).append(")\n");
        } catch (Exception e) {
            log.warn("Could not calculate financial summary for profile context: {}", e.getMessage());
        }

        List<GoalAssessment> goals = goalEngine.assessGoals(profileData);
        if (!goals.isEmpty()) {
            sb.append("Goals: ");
            goals.forEach(g -> sb.append(g.goalName())
                    .append(" (").append(g.fundingPercent().setScale(1, java.math.RoundingMode.HALF_UP))
                    .append("% funded, ").append(g.yearsRemaining()).append("y left), "));
            sb.setLength(sb.length() - 2);
            sb.append("\n");
        }

        Map<String, Object> insurance = getSection(profileData, "insurance");
        if (!insurance.isEmpty()) {
            sb.append("Insurance: ").append(insurance.size()).append(" policies");
            BigDecimal annualIncome = salary.multiply(BigDecimal.valueOf(12));
            BigDecimal termCover = BigDecimal.ZERO;
            for (Object val : insurance.values()) {
                if (val instanceof Map<?, ?> pol) {
                    if ("term".equalsIgnoreCase((String) ((Map<?, ?>) pol).get("type"))) {
                        termCover = termCover.add(FinancialMathUtil.toBigDecimal(((Map<?, ?>) pol).get("cover")));
                    }
                }
            }
            if (termCover.signum() > 0 && annualIncome.signum() > 0) {
                sb.append(" (term life: ").append(
                        termCover.divide(annualIncome, 1, java.math.RoundingMode.HALF_UP))
                        .append("x annual income)");
            }
            sb.append("\n");
        }

        sb.append("=== END PROFILE SNAPSHOT ===");
        return sb.toString();
    }

    private Map<String, Object> loadProfileData(String userId) {
        Optional<ProfileSnapshot> snapshot = profileService.getCurrentProfile(userId);
        if (snapshot.isEmpty()) {
            return Map.of("_error", "No profile found for user. Please create a profile first.");
        }
        return snapshot.get().data();
    }
}
