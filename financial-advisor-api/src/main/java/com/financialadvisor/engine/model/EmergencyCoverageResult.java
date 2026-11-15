package com.financialadvisor.engine.model;

import java.math.BigDecimal;

/**
 * Result of emergency coverage calculation (FR-2.3).
 * Coverage months = emergency assets / mandatory monthly expenditure.
 */
public record EmergencyCoverageResult(
        BigDecimal emergencyAssets,
        BigDecimal mandatoryMonthlyExpense,
        BigDecimal coverageMonths,
        BigDecimal thresholdMonths,
        String status
) {
    public boolean isBelowThreshold() {
        return coverageMonths.compareTo(thresholdMonths) < 0;
    }
}
