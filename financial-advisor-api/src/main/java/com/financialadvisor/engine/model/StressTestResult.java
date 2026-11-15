package com.financialadvisor.engine.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Result of a stress test scenario (FR-7.5).
 * Shows impact on portfolio, goals, and key metrics.
 */
public record StressTestResult(
        StressScenario scenario,
        BigDecimal portfolioValueBefore,
        BigDecimal portfolioValueAfter,
        BigDecimal portfolioImpact,
        BigDecimal emergencyCoverageBefore,
        BigDecimal emergencyCoverageAfter,
        List<GoalImpact> goalImpacts,
        List<String> warnings
) {
    public record GoalImpact(
            String goalId,
            String goalName,
            BigDecimal fundingPercentBefore,
            BigDecimal fundingPercentAfter,
            boolean wasViable,
            boolean stillViable,
            String status
    ) {}
}
