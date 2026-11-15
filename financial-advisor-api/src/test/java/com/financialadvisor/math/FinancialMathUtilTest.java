package com.financialadvisor.math;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UT-24: Financial rounding — HALF_UP with scale 2.
 * UT-05: Goal future value with inflation.
 * UT-06: Goal shortfall / required monthly contribution.
 */
class FinancialMathUtilTest {

    @Test
    @DisplayName("UT-24: roundInr uses HALF_UP with scale 2")
    void roundInr_halfUpScale2() {
        assertEquals(new BigDecimal("100.46"), FinancialMathUtil.roundInr(new BigDecimal("100.455")));
        assertEquals(new BigDecimal("100.45"), FinancialMathUtil.roundInr(new BigDecimal("100.454")));
        assertEquals(new BigDecimal("100.00"), FinancialMathUtil.roundInr(new BigDecimal("100")));
    }

    @Test
    @DisplayName("UT-05: Future value with 6% inflation over 10 years")
    void futureValue_withInflation() {
        BigDecimal pv = new BigDecimal("2000000");
        BigDecimal rate = new BigDecimal("0.06");
        BigDecimal fv = FinancialMathUtil.futureValue(pv, rate, 10);

        assertTrue(fv.compareTo(new BigDecimal("3581695")) > 0);
        assertTrue(fv.compareTo(new BigDecimal("3581697")) < 0);
    }

    @Test
    @DisplayName("Future value with 0% rate returns same amount")
    void futureValue_zeroRate_returnsSame() {
        BigDecimal pv = new BigDecimal("1000000");
        BigDecimal fv = FinancialMathUtil.futureValue(pv, BigDecimal.ZERO, 10);
        assertEquals(0, fv.compareTo(new BigDecimal("1000000.00")));
    }

    @Test
    @DisplayName("Present value is inverse of future value")
    void presentValue_inverseOfFutureValue() {
        BigDecimal fv = new BigDecimal("3581696.43");
        BigDecimal rate = new BigDecimal("0.06");
        BigDecimal pv = FinancialMathUtil.presentValue(fv, rate, 10);

        assertTrue(pv.subtract(new BigDecimal("2000000")).abs().compareTo(BigDecimal.ONE) < 0,
                "PV of the FV should be approximately the original PV");
    }

    @Test
    @DisplayName("Future value of annuity — 10K/month at 10% for 20 years")
    void futureValueOfAnnuity_sipProjection() {
        BigDecimal monthly = new BigDecimal("10000");
        BigDecimal rate = new BigDecimal("0.10");
        BigDecimal fva = FinancialMathUtil.futureValueOfAnnuity(monthly, rate, 20);

        assertTrue(fva.compareTo(new BigDecimal("7000000")) > 0,
                "₹10K SIP at 10% for 20 years should exceed ₹70 lakh");
    }

    @Test
    @DisplayName("UT-06: Required monthly contribution for a shortfall")
    void requiredMonthlyContribution_calculatesCorrectSIP() {
        BigDecimal target = new BigDecimal("5000000");
        BigDecimal current = new BigDecimal("500000");
        BigDecimal rate = new BigDecimal("0.10");
        BigDecimal sip = FinancialMathUtil.requiredMonthlyContribution(target, current, rate, 10);

        assertTrue(sip.compareTo(BigDecimal.ZERO) > 0);
        assertTrue(sip.compareTo(new BigDecimal("50000")) < 0,
                "SIP should be reasonable for the gap");
    }

    @Test
    @DisplayName("Required contribution is zero when already funded")
    void requiredMonthlyContribution_alreadyFunded_returnsZero() {
        BigDecimal target = new BigDecimal("1000000");
        BigDecimal current = new BigDecimal("2000000");
        BigDecimal rate = new BigDecimal("0.10");
        BigDecimal sip = FinancialMathUtil.requiredMonthlyContribution(target, current, rate, 10);

        assertEquals(0, sip.compareTo(BigDecimal.ZERO.setScale(2)));
    }

    @Test
    @DisplayName("Emergency coverage months calculation")
    void emergencyCoverageMonths_correctRatio() {
        BigDecimal emergency = new BigDecimal("900000");
        BigDecimal monthly = new BigDecimal("88000");
        BigDecimal months = FinancialMathUtil.emergencyCoverageMonths(emergency, monthly);

        assertTrue(months.compareTo(new BigDecimal("10")) > 0);
        assertTrue(months.compareTo(new BigDecimal("10.3")) < 0);
    }

    @Test
    @DisplayName("Emergency coverage with zero expense returns zero")
    void emergencyCoverageMonths_zeroExpense_returnsZero() {
        BigDecimal months = FinancialMathUtil.emergencyCoverageMonths(
                new BigDecimal("900000"), BigDecimal.ZERO);
        assertEquals(0, months.compareTo(BigDecimal.ZERO.setScale(2)));
    }

    @Test
    @DisplayName("Goal funding percent — 50% funded")
    void goalFundingPercent_halfFunded() {
        BigDecimal pct = FinancialMathUtil.goalFundingPercent(
                new BigDecimal("500000"),
                new BigDecimal("500000"),
                new BigDecimal("1000000"));
        assertEquals(0, pct.compareTo(new BigDecimal("50.00")));
    }

    @Test
    @DisplayName("toBigDecimal handles various types")
    void toBigDecimal_handlesTypes() {
        assertEquals(new BigDecimal("100.0"), FinancialMathUtil.toBigDecimal(100.0));
        assertEquals(new BigDecimal("200"), FinancialMathUtil.toBigDecimal("200"));
        assertNotNull(FinancialMathUtil.toBigDecimal(42));
        assertNull(FinancialMathUtil.toBigDecimal(null));
        assertNull(FinancialMathUtil.toBigDecimal("not-a-number"));
    }
}
