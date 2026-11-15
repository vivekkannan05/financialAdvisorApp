package com.financialadvisor.math;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Shared financial math utility (NFR-3.5, Design Doc §5.1).
 *
 * All methods use BigDecimal with RoundingMode.HALF_UP.
 * Intermediate calculations use higher precision (MathContext.DECIMAL128).
 * Final displayed values are rounded to scale 2 for INR amounts.
 *
 * This is the single source of truth for financial formulas —
 * all engines delegate to this class to prevent rounding inconsistencies.
 */
public final class FinancialMathUtil {

    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
    public static final int INR_SCALE = 2;
    private static final MathContext PRECISION = MathContext.DECIMAL128;

    private FinancialMathUtil() {}

    /**
     * Future Value: FV = PV × (1 + r)^n
     *
     * @param presentValue  current value (PV)
     * @param annualRate    annual rate as decimal (e.g. 0.07 for 7%)
     * @param years         number of years
     * @return future value rounded to INR scale
     */
    public static BigDecimal futureValue(BigDecimal presentValue, BigDecimal annualRate, int years) {
        BigDecimal factor = BigDecimal.ONE.add(annualRate).pow(years, PRECISION);
        return presentValue.multiply(factor, PRECISION).setScale(INR_SCALE, ROUNDING_MODE);
    }

    /**
     * Present Value: PV = FV / (1 + r)^n
     *
     * @param futureValue  target future value (FV)
     * @param annualRate   annual rate as decimal
     * @param years        number of years
     * @return present value rounded to INR scale
     */
    public static BigDecimal presentValue(BigDecimal futureValue, BigDecimal annualRate, int years) {
        BigDecimal factor = BigDecimal.ONE.add(annualRate).pow(years, PRECISION);
        return futureValue.divide(factor, PRECISION).setScale(INR_SCALE, ROUNDING_MODE);
    }

    /**
     * Future Value of Annuity: FVA = PMT × [(1+r)^n - 1] / r
     * where r = monthly rate, n = total months.
     *
     * @param monthlyPayment  regular monthly payment (PMT)
     * @param annualRate      annual rate as decimal
     * @param years           number of years
     * @return future value of annuity rounded to INR scale
     */
    public static BigDecimal futureValueOfAnnuity(BigDecimal monthlyPayment,
                                                   BigDecimal annualRate, int years) {
        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(12), PRECISION);
        int totalMonths = years * 12;

        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return monthlyPayment.multiply(BigDecimal.valueOf(totalMonths))
                    .setScale(INR_SCALE, ROUNDING_MODE);
        }

        BigDecimal factor = BigDecimal.ONE.add(monthlyRate).pow(totalMonths, PRECISION);
        BigDecimal numerator = factor.subtract(BigDecimal.ONE);
        BigDecimal result = monthlyPayment.multiply(numerator, PRECISION)
                .divide(monthlyRate, PRECISION);
        return result.setScale(INR_SCALE, ROUNDING_MODE);
    }

    /**
     * Required Monthly Contribution to reach a target:
     * PMT = (FV - PV×(1+r)^n) × r / [(1+r)^n - 1]
     * where r = monthly rate, n = total months.
     *
     * @param targetFutureValue  required future corpus
     * @param currentValue       current invested amount toward this goal
     * @param annualRate         expected annual return rate
     * @param years              time horizon in years
     * @return required monthly SIP amount rounded to INR scale
     */
    public static BigDecimal requiredMonthlyContribution(BigDecimal targetFutureValue,
                                                          BigDecimal currentValue,
                                                          BigDecimal annualRate, int years) {
        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(12), PRECISION);
        int totalMonths = years * 12;

        BigDecimal growthFactor = BigDecimal.ONE.add(monthlyRate).pow(totalMonths, PRECISION);
        BigDecimal futureOfCurrent = currentValue.multiply(growthFactor, PRECISION);
        BigDecimal gap = targetFutureValue.subtract(futureOfCurrent);

        if (gap.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(INR_SCALE, ROUNDING_MODE);
        }

        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return gap.divide(BigDecimal.valueOf(totalMonths), PRECISION)
                    .setScale(INR_SCALE, ROUNDING_MODE);
        }

        BigDecimal annuityFactor = growthFactor.subtract(BigDecimal.ONE)
                .divide(monthlyRate, PRECISION);
        return gap.divide(annuityFactor, PRECISION).setScale(INR_SCALE, ROUNDING_MODE);
    }

    /**
     * Goal Funding Percentage:
     * (current tagged assets + PV of future contributions) / required FV × 100
     *
     * @param currentAssets       current assets tagged to this goal
     * @param projectedFuture     projected future value of current assets + contributions
     * @param requiredFutureValue required corpus at goal date
     * @return funding percentage (e.g. 75.50 for 75.5%)
     */
    public static BigDecimal goalFundingPercent(BigDecimal currentAssets,
                                                BigDecimal projectedFuture,
                                                BigDecimal requiredFutureValue) {
        if (requiredFutureValue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.valueOf(100).setScale(INR_SCALE, ROUNDING_MODE);
        }
        return projectedFuture
                .divide(requiredFutureValue, PRECISION)
                .multiply(BigDecimal.valueOf(100), PRECISION)
                .setScale(INR_SCALE, ROUNDING_MODE);
    }

    /**
     * Emergency Coverage Ratio: emergency assets / mandatory monthly expenditure (FR-2.3).
     *
     * @param emergencyAssets         total emergency-designated assets
     * @param mandatoryMonthlyExpense total mandatory monthly outflow (expenses + EMIs + insurance)
     * @return months of emergency coverage
     */
    public static BigDecimal emergencyCoverageMonths(BigDecimal emergencyAssets,
                                                      BigDecimal mandatoryMonthlyExpense) {
        if (mandatoryMonthlyExpense.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(INR_SCALE, ROUNDING_MODE);
        }
        return emergencyAssets
                .divide(mandatoryMonthlyExpense, PRECISION)
                .setScale(INR_SCALE, ROUNDING_MODE);
    }

    /**
     * Rounds a BigDecimal to INR display scale (2 decimal places, HALF_UP).
     */
    public static BigDecimal roundInr(BigDecimal value) {
        return value.setScale(INR_SCALE, ROUNDING_MODE);
    }

    /**
     * Safely converts an Object (Integer, Long, Double, String, BigDecimal) to BigDecimal.
     * Returns null if the value cannot be converted.
     */
    public static BigDecimal toBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number num) return BigDecimal.valueOf(num.doubleValue());
        if (value instanceof String str) {
            try {
                return new BigDecimal(str);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
