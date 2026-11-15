package com.financialadvisor.engine.risk;

/**
 * Risk tolerance: psychological willingness to accept volatility (FR-4.2).
 */
public final class ToleranceDimension implements RiskDimension {

    @Override
    public RiskScore evaluate(RiskContext context) {
        String tolerance = context.behaviouralTolerance();
        if (tolerance == null) tolerance = "moderate";

        RiskLevel level = switch (tolerance.toLowerCase()) {
            case "very-conservative", "very_conservative" -> RiskLevel.VERY_LOW;
            case "conservative" -> RiskLevel.LOW;
            case "moderate", "balanced" -> RiskLevel.MODERATE;
            case "growth-oriented", "growth_oriented", "aggressive" -> RiskLevel.HIGH;
            case "very-aggressive", "very_aggressive" -> RiskLevel.VERY_HIGH;
            default -> RiskLevel.MODERATE;
        };

        return new RiskScore(level, "Tolerance",
                "Behavioural tolerance: " + tolerance + " → " + level.label());
    }
}
