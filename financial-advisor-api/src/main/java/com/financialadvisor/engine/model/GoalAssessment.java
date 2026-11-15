package com.financialadvisor.engine.model;

import java.math.BigDecimal;

/**
 * Assessment of a single financial goal (FR-3.x).
 */
public record GoalAssessment(
        String goalId,
        String goalName,
        BigDecimal targetAmountToday,
        BigDecimal targetAmountFuture,
        BigDecimal currentFunding,
        BigDecimal shortfall,
        BigDecimal fundingPercent,
        BigDecimal requiredMonthlySIP,
        int yearsRemaining,
        String status
) {
    public boolean isFullyFunded() {
        return shortfall.signum() <= 0;
    }
}
