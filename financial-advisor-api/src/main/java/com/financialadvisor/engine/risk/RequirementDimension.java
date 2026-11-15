package com.financialadvisor.engine.risk;

import java.math.BigDecimal;

/**
 * Risk requirement: return needed to meet a specific goal (FR-4.2).
 * Higher required returns imply higher risk tolerance needed.
 */
public final class RequirementDimension implements RiskDimension {

    @Override
    public RiskScore evaluate(RiskContext context) {
        BigDecimal requiredReturn = context.requiredReturnRate();
        if (requiredReturn == null) {
            return new RiskScore(RiskLevel.VERY_HIGH, "Requirement",
                    "No specific return requirement → unconstrained");
        }

        RiskLevel level;
        if (requiredReturn.compareTo(new BigDecimal("0.06")) <= 0) {
            level = RiskLevel.VERY_LOW;
        } else if (requiredReturn.compareTo(new BigDecimal("0.09")) <= 0) {
            level = RiskLevel.LOW;
        } else if (requiredReturn.compareTo(new BigDecimal("0.12")) <= 0) {
            level = RiskLevel.MODERATE;
        } else if (requiredReturn.compareTo(new BigDecimal("0.15")) <= 0) {
            level = RiskLevel.HIGH;
        } else {
            level = RiskLevel.VERY_HIGH;
        }

        String pct = requiredReturn.multiply(BigDecimal.valueOf(100)).toPlainString();
        return new RiskScore(level, "Requirement",
                "Required return " + pct + "% → " + level.label());
    }
}
