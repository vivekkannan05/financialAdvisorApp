package com.financialadvisor.dashboard;

import com.financialadvisor.engine.*;
import com.financialadvisor.engine.model.*;
import com.financialadvisor.engine.risk.CompositeRiskResult;
import com.financialadvisor.engine.risk.RiskAssessor;
import com.financialadvisor.engine.risk.RiskContext;
import com.financialadvisor.math.FinancialMathUtil;
import com.financialadvisor.exception.AppException;
import com.financialadvisor.exception.ErrorCode;
import com.financialadvisor.profile.ProfileService;
import com.financialadvisor.profile.ProfileSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static com.financialadvisor.engine.ProfileDataExtractor.*;

/**
 * REST controller for the 10-dimension financial health dashboard (FR-10.x).
 * GET /api/v1/health/dashboard
 */
@RestController
@RequestMapping("/api/v1/health")
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    private final ProfileService profileService;
    private final HealthAssessmentEngine healthEngine;
    private final GoalPlanningEngine goalEngine;
    private final CashFlowEngine cashFlowEngine;
    private final RiskAssessor riskAssessor;

    public DashboardController(ProfileService profileService,
                                HealthAssessmentEngine healthEngine,
                                GoalPlanningEngine goalEngine,
                                CashFlowEngine cashFlowEngine,
                                RiskAssessor riskAssessor) {
        this.profileService = profileService;
        this.healthEngine = healthEngine;
        this.goalEngine = goalEngine;
        this.cashFlowEngine = cashFlowEngine;
        this.riskAssessor = riskAssessor;
    }

    @GetMapping("/dashboard")
    public DashboardResponse getDashboard(Authentication auth) {
        String userId = auth.getName();
        log.info("Dashboard request for user {}", userId);

        ProfileSnapshot profile = profileService.getCurrentProfile(userId)
                .orElseThrow(() -> new AppException(ErrorCode.INSUFFICIENT_DATA,
                        "No profile found. Create a profile to view dashboard."));

        Map<String, Object> data = profile.data();

        NetWorthResult netWorth = healthEngine.calculateNetWorth(data);
        CashFlowResult cashFlow = cashFlowEngine.calculateCashFlow(data);
        EmergencyCoverageResult emergency = healthEngine.calculateEmergencyCoverage(data);
        List<GoalAssessment> goals = goalEngine.assessGoals(data);
        BigDecimal investable = healthEngine.calculateInvestableCapital(data);

        CompositeRiskResult risk = assessRisk(data, netWorth, cashFlow, emergency, goals);

        List<DashboardResponse.DimensionStatus> dimensions = build10Dimensions(
                netWorth, cashFlow, emergency, goals, risk, investable, data);

        int goalsOnTrack = (int) goals.stream().filter(g -> g.fundingPercent().compareTo(new BigDecimal("50")) >= 0).count();
        int goalsAtRisk = goals.size() - goalsOnTrack;

        var summary = new DashboardResponse.FinancialSummary(
                netWorth.netWorth(), netWorth.liquidAssets(),
                netWorth.marketLinkedAssets(), netWorth.restrictedAssets(),
                netWorth.illiquidAssets(),
                netWorth.totalLiabilities(), cashFlow.monthlySurplus(),
                emergency.coverageMonths(), risk.effectiveLevel().name(),
                goals.size(), goalsOnTrack, goalsAtRisk
        );

        return DashboardResponse.create(userId, profile.snapshotId(),
                profile.snapshotDate().toString(), dimensions, summary);
    }

    private List<DashboardResponse.DimensionStatus> build10Dimensions(
            NetWorthResult nw, CashFlowResult cf, EmergencyCoverageResult ec,
            List<GoalAssessment> goals, CompositeRiskResult risk,
            BigDecimal investable, Map<String, Object> data) {

        List<DashboardResponse.DimensionStatus> dims = new ArrayList<>();

        // 1. Liquidity (4-tier view)
        String liquidStatus = nw.liquidAssets().compareTo(BigDecimal.valueOf(100_000)) >= 0 ? "healthy" : "warning";
        dims.add(new DashboardResponse.DimensionStatus(1, "Liquidity Status",
                liquidStatus,
                "Liquid: ₹" + nw.liquidAssets().toPlainString()
                        + " | Market-linked: ₹" + nw.marketLinkedAssets().toPlainString()
                        + " | Restricted: ₹" + nw.restrictedAssets().toPlainString()
                        + " | Illiquid: ₹" + nw.illiquidAssets().toPlainString(),
                liquidStatus.equals("warning") ? List.of("Build liquid emergency reserves") : List.of()));

        // 2. Insurance — evaluate coverage adequacy
        Map<String, Object> insurance = getSection(data, "insurance");
        InsuranceAssessment insAssessment = assessInsurance(insurance, cf.grossMonthlyIncome());
        dims.add(new DashboardResponse.DimensionStatus(2, "Insurance Cover",
                insAssessment.status,
                insAssessment.description,
                insAssessment.actions));

        // 3. Debt
        String debtStatus = nw.totalLiabilities().compareTo(BigDecimal.ZERO) == 0 ? "debt-free"
                : nw.totalLiabilities().compareTo(nw.totalAssets().multiply(new BigDecimal("0.5"))) > 0 ? "high" : "manageable";
        dims.add(new DashboardResponse.DimensionStatus(3, "Debt Exposure",
                debtStatus,
                "Total liabilities: ₹" + nw.totalLiabilities().toPlainString(),
                debtStatus.equals("high") ? List.of("Consider debt reduction strategy") : List.of()));

        // 4. Retirement
        Optional<GoalAssessment> retirement = goals.stream()
                .filter(g -> g.goalId() != null && g.goalId().toLowerCase().contains("retire")).findFirst();
        String retStatus = retirement.map(g -> g.fundingPercent().compareTo(new BigDecimal("50")) >= 0 ? "on-track" : "at-risk")
                .orElse("not-set");
        dims.add(new DashboardResponse.DimensionStatus(4, "Retirement Trajectory",
                retStatus,
                retirement.map(g -> "Funding: " + g.fundingPercent() + "%").orElse("No retirement goal defined"),
                retStatus.equals("not-set") ? List.of("Define a retirement goal") : List.of()));

        // 5. Education
        Optional<GoalAssessment> education = goals.stream()
                .filter(g -> g.goalId() != null && g.goalId().toLowerCase().contains("educ")).findFirst();
        String eduStatus = education.map(g -> g.fundingPercent().compareTo(new BigDecimal("50")) >= 0 ? "on-track" : "at-risk")
                .orElse("not-set");
        dims.add(new DashboardResponse.DimensionStatus(5, "Education Trajectory",
                eduStatus,
                education.map(g -> "Funding: " + g.fundingPercent() + "%").orElse("No education goal defined"),
                eduStatus.equals("not-set") ? List.of("Define education goals if applicable") : List.of()));

        // 6. Portfolio Risk
        dims.add(new DashboardResponse.DimensionStatus(6, "Portfolio Risk",
                risk.effectiveLevel().name().toLowerCase(),
                risk.summary(),
                risk.constrainingDimension() != null
                        ? List.of("Constraining factor: " + risk.constrainingDimension())
                        : List.of()));

        // 7. Income Concentration — only count sources with a positive value
        Map<String, Object> income = getSection(data, "income");
        int incomeSources = 0;
        if (income != null) {
            for (Object val : income.values()) {
                BigDecimal amount = FinancialMathUtil.toBigDecimal(val);
                if (amount != null && amount.signum() > 0) incomeSources++;
            }
        }
        String concentrationStatus = incomeSources <= 1 ? "concentrated" : "diversified";
        dims.add(new DashboardResponse.DimensionStatus(7, "Income Concentration",
                concentrationStatus,
                "Income sources: " + incomeSources,
                concentrationStatus.equals("concentrated")
                        ? List.of("Consider building additional income streams")
                        : List.of()));

        // 8. Emergency Coverage
        String ecStatus = ec.isBelowThreshold() ? "critical" : "healthy";
        dims.add(new DashboardResponse.DimensionStatus(8, "Emergency Coverage",
                ecStatus,
                ec.coverageMonths().toPlainString() + " months (threshold: " + ec.thresholdMonths() + ")",
                ec.isBelowThreshold()
                        ? List.of("Build emergency fund to " + ec.thresholdMonths() + "+ months")
                        : List.of()));

        // 9. Housing Commitment
        Map<String, Object> liabilities = getSection(data, "liabilities");
        BigDecimal housingEMI = BigDecimal.ZERO;
        if (liabilities != null) {
            for (Map.Entry<String, Object> entry : liabilities.entrySet()) {
                if (entry.getKey().toLowerCase().contains("home") && entry.getValue() instanceof Map<?, ?> loan) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> loanMap = (Map<String, Object>) loan;
                    BigDecimal emi = getBigDecimal(loanMap, "emiAmount");
                    if (emi == null) emi = getBigDecimal(loanMap, "emi");
                    if (emi != null) housingEMI = housingEMI.add(emi);
                }
            }
        }
        BigDecimal housingRatio = cf.grossMonthlyIncome().signum() > 0
                ? housingEMI.divide(cf.grossMonthlyIncome(), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;
        String housingStatus = housingRatio.compareTo(new BigDecimal("40")) > 0 ? "high"
                : housingRatio.compareTo(BigDecimal.ZERO) == 0 ? "none" : "manageable";
        dims.add(new DashboardResponse.DimensionStatus(9, "Housing Commitment",
                housingStatus,
                "Housing EMI: ₹" + housingEMI.toPlainString() + " (" + housingRatio.setScale(1, RoundingMode.HALF_UP) + "% of income)",
                housingStatus.equals("high") ? List.of("Housing cost exceeds 40% of income — review options") : List.of()));

        // 10. Key Warnings
        List<String> warnings = new ArrayList<>();
        if (ec.isBelowThreshold()) warnings.add("Emergency fund below safe threshold");
        if (debtStatus.equals("high")) warnings.add("High debt-to-asset ratio");
        if (cf.isDeficit()) warnings.add("Monthly cash flow deficit — spending exceeds income");
        goals.stream().filter(g -> g.fundingPercent().compareTo(new BigDecimal("25")) < 0)
                .forEach(g -> warnings.add("Goal '" + g.goalName() + "' severely underfunded (" + g.fundingPercent() + "%)"));

        dims.add(new DashboardResponse.DimensionStatus(10, "Key Warnings & Actions",
                warnings.isEmpty() ? "clear" : "attention-needed",
                warnings.isEmpty() ? "No critical issues detected" : warnings.size() + " issue(s) require attention",
                warnings));

        return dims;
    }

    private CompositeRiskResult assessRisk(Map<String, Object> data,
                                            NetWorthResult nw, CashFlowResult cf,
                                            EmergencyCoverageResult ec, List<GoalAssessment> goals) {
        Map<String, Object> riskProfile = getSection(data, "riskProfile");
        String tolerance = getString(riskProfile, "behaviouralTolerance");
        Integer age = getInteger(riskProfile, "age");
        Integer retirementAge = getInteger(riskProfile, "retirementAge");

        int shortestHorizon = goals.stream()
                .mapToInt(GoalAssessment::yearsRemaining)
                .min().orElse(20);

        BigDecimal accessible = nw.accessibleAssets();
        RiskContext ctx = new RiskContext(
                tolerance, cf.monthlySurplus(), nw.totalLiabilities(),
                accessible, cf.grossMonthlyIncome(),
                ec.coverageMonths(), null,
                shortestHorizon, age != null ? age : 35,
                retirementAge, accessible, nw.totalLiabilities()
        );

        return riskAssessor.assess(ctx);
    }

    /* ── Insurance adequacy assessment ──────────────────────────────── */

    private record InsuranceAssessment(String status, String description, List<String> actions) {}

    @SuppressWarnings("unchecked")
    private InsuranceAssessment assessInsurance(Map<String, Object> insurance,
                                                BigDecimal grossMonthlyIncome) {
        if (insurance == null || insurance.isEmpty()) {
            return new InsuranceAssessment("critical", "No insurance data on record",
                    List.of("Add term life and health insurance details"));
        }

        BigDecimal annualIncome = grossMonthlyIncome.multiply(BigDecimal.valueOf(12));
        BigDecimal totalLifeCover = BigDecimal.ZERO;
        BigDecimal totalHealthCover = BigDecimal.ZERO;
        boolean hasTermLife = false;
        boolean hasHealth = false;
        int policyCount = 0;

        for (Map.Entry<String, Object> entry : insurance.entrySet()) {
            policyCount++;
            String name = entry.getKey().toLowerCase();
            if (entry.getValue() instanceof Map<?, ?> pol) {
                Map<String, Object> policy = (Map<String, Object>) pol;
                BigDecimal cover = getBigDecimal(policy, "cover");
                if (cover == null) cover = getBigDecimal(policy, "sumAssured");
                if (cover == null) cover = BigDecimal.ZERO;

                if (name.contains("term") || name.contains("life")) {
                    hasTermLife = true;
                    totalLifeCover = totalLifeCover.add(cover);
                } else if (name.contains("health") || name.contains("medical")) {
                    hasHealth = true;
                    totalHealthCover = totalHealthCover.add(cover);
                }
            }
        }

        List<String> actions = new ArrayList<>();
        List<String> details = new ArrayList<>();
        details.add(policyCount + " policies");

        if (!hasTermLife) {
            actions.add("Add term life insurance (recommended: 10-15× annual income)");
        } else {
            BigDecimal lifeMultiple = annualIncome.signum() > 0
                    ? totalLifeCover.divide(annualIncome, 1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            details.add("Life cover: ₹" + totalLifeCover.toPlainString()
                    + " (" + lifeMultiple + "× annual income)");
            if (lifeMultiple.compareTo(BigDecimal.TEN) < 0) {
                actions.add("Life cover is " + lifeMultiple
                        + "× income — consider increasing to 10-15×");
            }
        }

        if (!hasHealth) {
            actions.add("Add health insurance for family");
        } else {
            details.add("Health cover: ₹" + totalHealthCover.toPlainString());
            if (totalHealthCover.compareTo(BigDecimal.valueOf(500_000)) < 0) {
                actions.add("Health cover below ₹5L — consider increasing");
            }
        }

        String status;
        if (!hasTermLife || !hasHealth) {
            status = "critical";
        } else if (actions.isEmpty()) {
            status = "healthy";
        } else {
            status = "review";
        }

        return new InsuranceAssessment(status, String.join(" | ", details), actions);
    }
}
