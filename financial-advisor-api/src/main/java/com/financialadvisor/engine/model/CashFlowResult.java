package com.financialadvisor.engine.model;

import java.math.BigDecimal;

/**
 * Result of cash-flow health calculation (FR-2.2, FR-5.1).
 * Monthly surplus = income - (expenses + EMIs + insurance + investments).
 */
public record CashFlowResult(
        BigDecimal grossMonthlyIncome,
        BigDecimal monthlyExpenses,
        BigDecimal monthlyEMIs,
        BigDecimal monthlyInsurance,
        BigDecimal monthlyInvestments,
        BigDecimal monthlySurplus
) {
    public boolean hasSurplus() {
        return monthlySurplus.signum() > 0;
    }

    public boolean isDeficit() {
        return monthlySurplus.signum() < 0;
    }
}
