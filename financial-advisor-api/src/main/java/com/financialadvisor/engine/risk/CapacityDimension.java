package com.financialadvisor.engine.risk;

import java.math.BigDecimal;

/**
 * Risk capacity: financial ability to withstand loss (FR-4.2).
 * Evaluates liquidity, liabilities, income stability, and commitments.
 */
public final class CapacityDimension implements RiskDimension {

    private static final BigDecimal HIGH_DEBT_RATIO = new BigDecimal("0.50");
    private static final BigDecimal LOW_EMERGENCY = new BigDecimal("3");
    private static final BigDecimal ADEQUATE_EMERGENCY = new BigDecimal("6");

    @Override
    public RiskScore evaluate(RiskContext context) {
        int riskFactors = 0;
        StringBuilder reasoning = new StringBuilder("Capacity factors: ");

        BigDecimal debtToAssetRatio = BigDecimal.ZERO;
        if (context.liquidAssets().signum() > 0) {
            debtToAssetRatio = context.totalDebt()
                    .divide(context.liquidAssets(), 4, java.math.RoundingMode.HALF_UP);
        } else if (context.totalDebt().signum() > 0) {
            debtToAssetRatio = BigDecimal.TEN;
        }

        if (debtToAssetRatio.compareTo(HIGH_DEBT_RATIO) > 0) {
            riskFactors += 2;
            reasoning.append("high debt-to-liquid-asset ratio (").append(debtToAssetRatio).append("); ");
        }

        if (context.emergencyCoverageMonths().compareTo(LOW_EMERGENCY) < 0) {
            riskFactors += 2;
            reasoning.append("emergency coverage below 3 months; ");
        } else if (context.emergencyCoverageMonths().compareTo(ADEQUATE_EMERGENCY) < 0) {
            riskFactors++;
            reasoning.append("emergency coverage below 6 months; ");
        }

        if (context.monthlySurplus().signum() <= 0) {
            riskFactors += 2;
            reasoning.append("no monthly surplus; ");
        }

        RiskLevel level = switch (riskFactors) {
            case 0 -> RiskLevel.VERY_HIGH;
            case 1 -> RiskLevel.HIGH;
            case 2 -> RiskLevel.MODERATE;
            case 3 -> RiskLevel.LOW;
            default -> RiskLevel.VERY_LOW;
        };

        reasoning.append("→ ").append(level.label());
        return new RiskScore(level, "Capacity", reasoning.toString());
    }
}
