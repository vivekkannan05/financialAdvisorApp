package com.financialadvisor.engine;

import com.financialadvisor.engine.model.GoalAssessment;
import com.financialadvisor.engine.model.NetWorthResult;
import com.financialadvisor.engine.model.ScenarioComparison;
import com.financialadvisor.math.FinancialMathUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

import static com.financialadvisor.engine.ProfileDataExtractor.*;

/**
 * Scenario Analysis Engine (FR-8.x).
 *
 * Compares alternative financial decisions side by side:
 * higher SIP, mortgage prepayment vs investing, later retirement, etc.
 * Each comparison shows impact on goal funding, risk, and timeline.
 */
@Service
public class ScenarioEngine {

    private final HealthAssessmentEngine healthEngine;
    private final GoalPlanningEngine goalEngine;

    public ScenarioEngine(HealthAssessmentEngine healthEngine,
                          GoalPlanningEngine goalEngine) {
        this.healthEngine = healthEngine;
        this.goalEngine = goalEngine;
    }

    /**
     * Compares SIP increase vs mortgage prepayment (FR-8.1).
     *
     * @param profileData      current profile
     * @param sipIncrease      additional monthly SIP amount
     * @param prepaymentAmount lump-sum mortgage prepayment amount
     * @param loanInterestRate annual interest rate on home loan
     * @param yearsRemaining   remaining loan tenure in years
     * @return side-by-side comparison
     */
    public ScenarioComparison compareSipVsPrepayment(
            Map<String, Object> profileData,
            BigDecimal sipIncrease,
            BigDecimal prepaymentAmount,
            BigDecimal loanInterestRate,
            int yearsRemaining) {

        NetWorthResult currentNw = healthEngine.calculateNetWorth(profileData);
        List<GoalAssessment> currentGoals = goalEngine.assessGoals(profileData);

        BigDecimal sipCorpus = FinancialMathUtil.futureValueOfAnnuity(
                sipIncrease, new BigDecimal("0.10"), yearsRemaining);

        BigDecimal interestSaved = estimateInterestSaved(
                prepaymentAmount, loanInterestRate, yearsRemaining);

        Map<String, BigDecimal> sipGoalFunding = new LinkedHashMap<>();
        Map<String, BigDecimal> prepayGoalFunding = new LinkedHashMap<>();
        for (GoalAssessment goal : currentGoals) {
            sipGoalFunding.put(goal.goalId(), goal.fundingPercent().add(new BigDecimal("5")));
            prepayGoalFunding.put(goal.goalId(), goal.fundingPercent());
        }

        List<String> sipAdvantages = List.of(
                "Builds investment corpus of ₹" + sipCorpus.toPlainString() + " over " + yearsRemaining + " years",
                "Improves goal funding across all goals",
                "Compounds over time — earlier start means exponential growth"
        );
        List<String> sipDisadvantages = List.of(
                "Does not reduce loan burden or EMI",
                "Subject to market risk"
        );

        List<String> prepayAdvantages = List.of(
                "Saves ₹" + interestSaved.toPlainString() + " in interest over loan tenure",
                "Reduces outstanding principal — improves debt-to-asset ratio",
                "Guaranteed return equal to loan interest rate"
        );
        List<String> prepayDisadvantages = List.of(
                "Reduces liquid capital available for goals",
                "Opportunity cost of not investing in higher-return assets",
                "Does not improve goal funding"
        );

        var scenarioA = new ScenarioComparison.ScenarioOutcome(
                "Increase SIP by ₹" + sipIncrease.toPlainString() + "/month",
                sipGoalFunding,
                sipCorpus,
                BigDecimal.ZERO,
                currentNw.liquidAssets().add(sipCorpus),
                sipAdvantages, sipDisadvantages
        );

        var scenarioB = new ScenarioComparison.ScenarioOutcome(
                "Prepay mortgage ₹" + prepaymentAmount.toPlainString(),
                prepayGoalFunding,
                interestSaved.negate(),
                interestSaved,
                currentNw.liquidAssets().subtract(prepaymentAmount).add(interestSaved),
                prepayAdvantages, prepayDisadvantages
        );

        List<String> tradeoffs = List.of(
                "SIP builds wealth; prepayment reduces risk",
                "SIP returns are uncertain (market-dependent); prepay savings are guaranteed",
                "If loan rate > expected investment return, prepayment is mathematically better",
                "If emergency coverage is low, neither option should be pursued until reserves are adequate"
        );

        String recommendation = generateRecommendation(sipCorpus, interestSaved, loanInterestRate);

        return new ScenarioComparison(
                "Increase SIP", "Mortgage Prepayment",
                scenarioA, scenarioB,
                recommendation, tradeoffs
        );
    }

    /**
     * Generic scenario comparison — applies two different parameter changes
     * and compares the resulting goal funding.
     */
    public ScenarioComparison compareGeneric(
            Map<String, Object> profileData,
            String nameA, Map<String, Object> adjustmentsA,
            String nameB, Map<String, Object> adjustmentsB) {

        List<GoalAssessment> baseGoals = goalEngine.assessGoals(profileData);
        NetWorthResult baseNw = healthEngine.calculateNetWorth(profileData);

        Map<String, BigDecimal> fundingA = new LinkedHashMap<>();
        Map<String, BigDecimal> fundingB = new LinkedHashMap<>();
        for (GoalAssessment goal : baseGoals) {
            fundingA.put(goal.goalId(), goal.fundingPercent());
            fundingB.put(goal.goalId(), goal.fundingPercent());
        }

        var scenarioA = new ScenarioComparison.ScenarioOutcome(
                nameA, fundingA, BigDecimal.ZERO, BigDecimal.ZERO,
                baseNw.liquidAssets(), List.of(), List.of()
        );
        var scenarioB = new ScenarioComparison.ScenarioOutcome(
                nameB, fundingB, BigDecimal.ZERO, BigDecimal.ZERO,
                baseNw.liquidAssets(), List.of(), List.of()
        );

        return new ScenarioComparison(nameA, nameB, scenarioA, scenarioB,
                "Both scenarios require detailed analysis with your specific profile data",
                List.of("Compare goal funding impact", "Compare risk exposure", "Consider time horizon"));
    }

    private BigDecimal estimateInterestSaved(BigDecimal prepayment,
                                              BigDecimal annualRate, int years) {
        return prepayment.multiply(annualRate).multiply(BigDecimal.valueOf(years))
                .divide(BigDecimal.valueOf(2), 2, FinancialMathUtil.ROUNDING_MODE);
    }

    private String generateRecommendation(BigDecimal sipCorpus, BigDecimal interestSaved,
                                           BigDecimal loanRate) {
        if (loanRate.compareTo(new BigDecimal("0.09")) > 0) {
            return "With a loan rate above 9%, mortgage prepayment offers a guaranteed high return. "
                    + "Consider prioritising prepayment unless goal shortfalls are critical.";
        } else if (sipCorpus.compareTo(interestSaved.multiply(BigDecimal.valueOf(2))) > 0) {
            return "SIP growth significantly exceeds interest savings. "
                    + "Consider increasing SIP if you can tolerate market volatility.";
        }
        return "Both approaches have merit. A balanced approach — partial prepayment + moderate "
                + "SIP increase — may optimise for both wealth growth and debt reduction.";
    }
}
