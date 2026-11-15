package com.financialadvisor.engine.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Side-by-side comparison of two alternative financial decisions (FR-8.1).
 */
public record ScenarioComparison(
        String scenarioAName,
        String scenarioBName,
        ScenarioOutcome scenarioA,
        ScenarioOutcome scenarioB,
        String recommendation,
        List<String> tradeoffs
) {
    public record ScenarioOutcome(
            String name,
            Map<String, BigDecimal> goalFundingPercents,
            BigDecimal netWorthImpact,
            BigDecimal totalInterestSaved,
            BigDecimal projectedCorpus,
            List<String> advantages,
            List<String> disadvantages
    ) {}
}
