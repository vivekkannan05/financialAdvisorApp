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

import java.math.BigDecimal;
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
        result.put("netWorth", Map.of(
                "totalAssets", netWorth.totalAssets().toPlainString(),
                "liquidAssets", netWorth.liquidAssets().toPlainString(),
                "illiquidAssets", netWorth.illiquidAssets().toPlainString(),
                "totalLiabilities", netWorth.totalLiabilities().toPlainString(),
                "netWorth", netWorth.netWorth().toPlainString()
        ));
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

        RiskContext ctx = new RiskContext(
                tolerance, cf.monthlySurplus(), nw.totalLiabilities(),
                nw.liquidAssets(), cf.grossMonthlyIncome(),
                ec.coverageMonths(), requiredReturn,
                shortestHorizon, age != null ? age : 35,
                retirementAge, nw.liquidAssets(), nw.totalLiabilities()
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

    private Map<String, Object> loadProfileData(String userId) {
        Optional<ProfileSnapshot> snapshot = profileService.getCurrentProfile(userId);
        if (snapshot.isEmpty()) {
            return Map.of("_error", "No profile found for user. Please create a profile first.");
        }
        return snapshot.get().data();
    }
}
