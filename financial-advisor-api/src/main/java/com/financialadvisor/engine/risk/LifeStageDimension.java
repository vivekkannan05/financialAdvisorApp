package com.financialadvisor.engine.risk;

/**
 * Life stage: age, years to retirement, earning period, corpus (FR-4.2).
 */
public final class LifeStageDimension implements RiskDimension {

    @Override
    public RiskScore evaluate(RiskContext context) {
        int yearsToRetirement = context.yearsToRetirement();
        int age = context.age();

        RiskLevel level;
        String reasoning;
        if (yearsToRetirement > 25) {
            level = RiskLevel.VERY_HIGH;
            reasoning = "Age " + age + ", " + yearsToRetirement + " years to retirement → long earning horizon";
        } else if (yearsToRetirement > 15) {
            level = RiskLevel.HIGH;
            reasoning = "Age " + age + ", " + yearsToRetirement + " years to retirement → growth phase";
        } else if (yearsToRetirement > 7) {
            level = RiskLevel.MODERATE;
            reasoning = "Age " + age + ", " + yearsToRetirement + " years to retirement → start de-risking";
        } else if (yearsToRetirement > 3) {
            level = RiskLevel.LOW;
            reasoning = "Age " + age + ", " + yearsToRetirement + " years to retirement → preservation phase";
        } else {
            level = RiskLevel.VERY_LOW;
            reasoning = "Age " + age + ", " + yearsToRetirement + " years to retirement → capital preservation";
        }

        return new RiskScore(level, "LifeStage", reasoning);
    }
}
