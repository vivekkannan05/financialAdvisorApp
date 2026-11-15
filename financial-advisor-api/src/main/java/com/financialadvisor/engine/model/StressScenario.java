package com.financialadvisor.engine.model;

import java.math.BigDecimal;

/**
 * Input definition for a stress test scenario (FR-7.x).
 */
public record StressScenario(
        StressType type,
        BigDecimal parameter,
        Integer durationMonths,
        String description
) {
    public enum StressType {
        /** FR-7.1: Partial or full income loss */
        INCOME_LOSS,
        /** FR-7.2: Equity market drawdown (e.g., -20%, -40%) */
        MARKET_FALL,
        /** FR-7.3: Interest rate change (e.g., +2%, -1%) */
        INTEREST_RATE_CHANGE,
        /** FR-7.4: Large unplanned expense */
        LARGE_EXPENSE
    }

    public static StressScenario incomeLoss(BigDecimal lossFraction, int months) {
        return new StressScenario(StressType.INCOME_LOSS, lossFraction, months,
                "Income loss of " + lossFraction.multiply(BigDecimal.valueOf(100)) + "% for " + months + " months");
    }

    public static StressScenario marketFall(BigDecimal drawdownFraction) {
        return new StressScenario(StressType.MARKET_FALL, drawdownFraction, null,
                "Market fall of " + drawdownFraction.multiply(BigDecimal.valueOf(100)) + "%");
    }

    public static StressScenario interestRateChange(BigDecimal rateChange) {
        return new StressScenario(StressType.INTEREST_RATE_CHANGE, rateChange, null,
                "Interest rate change of " + rateChange.multiply(BigDecimal.valueOf(100)) + "%");
    }

    public static StressScenario largeExpense(BigDecimal amount) {
        return new StressScenario(StressType.LARGE_EXPENSE, amount, null,
                "Unplanned expense of ₹" + amount.toPlainString());
    }
}
