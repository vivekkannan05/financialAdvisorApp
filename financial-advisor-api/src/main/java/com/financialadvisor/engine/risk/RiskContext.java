package com.financialadvisor.engine.risk;

import java.math.BigDecimal;

/**
 * Context for risk evaluation, containing all inputs the 5 dimensions need (FR-4.2).
 */
public record RiskContext(
        String behaviouralTolerance,
        BigDecimal monthlySurplus,
        BigDecimal totalLiabilities,
        BigDecimal liquidAssets,
        BigDecimal totalIncome,
        BigDecimal emergencyCoverageMonths,
        BigDecimal requiredReturnRate,
        int goalHorizonYears,
        int age,
        Integer retirementAge,
        BigDecimal accumulatedCorpus,
        BigDecimal totalDebt
) {
    public int yearsToRetirement() {
        int retAge = (retirementAge != null) ? retirementAge : 60;
        return Math.max(0, retAge - age);
    }
}
