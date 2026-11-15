package com.financialadvisor.engine.risk;

/**
 * Goal horizon: time until money is needed (FR-4.2).
 * Near-term goals (< 3 years) get low risk regardless of tolerance (FR-3.6, DL-009).
 */
public final class HorizonDimension implements RiskDimension {

    @Override
    public RiskScore evaluate(RiskContext context) {
        int years = context.goalHorizonYears();

        RiskLevel level;
        String reasoning;
        if (years < 3) {
            level = RiskLevel.VERY_LOW;
            reasoning = "Goal horizon < 3 years → very conservative (FR-3.6)";
        } else if (years < 5) {
            level = RiskLevel.LOW;
            reasoning = "Goal horizon 3-5 years → conservative";
        } else if (years < 10) {
            level = RiskLevel.MODERATE;
            reasoning = "Goal horizon 5-10 years → moderate";
        } else if (years < 20) {
            level = RiskLevel.HIGH;
            reasoning = "Goal horizon 10-20 years → growth-oriented";
        } else {
            level = RiskLevel.VERY_HIGH;
            reasoning = "Goal horizon 20+ years → aggressive growth permitted";
        }

        return new RiskScore(level, "Horizon", reasoning);
    }
}
