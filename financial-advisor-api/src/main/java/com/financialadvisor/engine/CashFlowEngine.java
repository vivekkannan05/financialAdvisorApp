package com.financialadvisor.engine;

import com.financialadvisor.engine.model.CashFlowResult;
import com.financialadvisor.math.FinancialMathUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

import static com.financialadvisor.engine.ProfileDataExtractor.*;

/**
 * Cash Flow Engine (FR-2.2, FR-5.x).
 *
 * Analyses income, mandatory expenses, EMIs, insurance premiums,
 * and investment commitments to determine available monthly surplus.
 */
@Service
public class CashFlowEngine {

    /**
     * Calculates monthly cash-flow surplus (FR-2.2).
     * Surplus = income - (expenses + EMIs + insurance + investments).
     */
    public CashFlowResult calculateCashFlow(Map<String, Object> profileData) {
        BigDecimal income = calculateGrossMonthlyIncome(profileData);
        BigDecimal expenses = calculateMonthlyExpenses(profileData);
        BigDecimal emis = calculateMonthlyEMIs(profileData);
        BigDecimal insurance = calculateMonthlyInsurance(profileData);
        BigDecimal investments = calculateMonthlyInvestments(profileData);

        BigDecimal surplus = income
                .subtract(expenses)
                .subtract(emis)
                .subtract(insurance)
                .subtract(investments);

        return new CashFlowResult(
                FinancialMathUtil.roundInr(income),
                FinancialMathUtil.roundInr(expenses),
                FinancialMathUtil.roundInr(emis),
                FinancialMathUtil.roundInr(insurance),
                FinancialMathUtil.roundInr(investments),
                FinancialMathUtil.roundInr(surplus)
        );
    }

    /**
     * Classifies credit card usage (FR-5.3, DL-012).
     * Paid in full = spending mechanism (not debt).
     * Revolving balance = liability/debt.
     */
    public boolean isCreditCardDebt(Map<String, Object> creditCardData) {
        Boolean paidInFull = null;
        Object val = creditCardData.get("paidInFull");
        if (val instanceof Boolean b) paidInFull = b;
        if (val instanceof String s) paidInFull = Boolean.parseBoolean(s);

        if (Boolean.TRUE.equals(paidInFull)) {
            return false;
        }

        Boolean isDebt = null;
        Object debtVal = creditCardData.get("isDebt");
        if (debtVal instanceof Boolean b) isDebt = b;
        if (debtVal instanceof String s) isDebt = Boolean.parseBoolean(s);

        return !Boolean.FALSE.equals(isDebt);
    }

    private BigDecimal calculateGrossMonthlyIncome(Map<String, Object> profileData) {
        Map<String, Object> income = getSection(profileData, "income");
        BigDecimal salary = getBigDecimal(income, "monthlySalary");
        if (salary == null) salary = BigDecimal.ZERO;

        BigDecimal other = getBigDecimal(income, "otherMonthlyIncome");
        if (other == null) other = BigDecimal.ZERO;

        return salary.add(other);
    }

    private BigDecimal calculateMonthlyExpenses(Map<String, Object> profileData) {
        Map<String, Object> expenses = getSection(profileData, "expenses");
        BigDecimal monthly = getBigDecimal(expenses, "monthlyHousehold");
        return (monthly != null) ? monthly : BigDecimal.ZERO;
    }

    private BigDecimal calculateMonthlyEMIs(Map<String, Object> profileData) {
        Map<String, Object> liabilities = getSection(profileData, "liabilities");
        BigDecimal total = BigDecimal.ZERO;

        for (Map.Entry<String, Object> entry : liabilities.entrySet()) {
            if (entry.getValue() instanceof Map<?, ?> liab) {
                @SuppressWarnings("unchecked")
                Map<String, Object> liabMap = (Map<String, Object>) liab;

                if ("creditCard".equals(entry.getKey())) {
                    if (!isCreditCardDebt(liabMap)) continue;
                }

                BigDecimal emi = getBigDecimal(liabMap, "emiAmount");
                if (emi != null) total = total.add(emi);
            }
        }
        return total;
    }

    private BigDecimal calculateMonthlyInsurance(Map<String, Object> profileData) {
        Map<String, Object> insurance = getSection(profileData, "insurance");
        BigDecimal total = BigDecimal.ZERO;

        for (Map.Entry<String, Object> entry : insurance.entrySet()) {
            if (entry.getValue() instanceof Map<?, ?> ins) {
                @SuppressWarnings("unchecked")
                Map<String, Object> insMap = (Map<String, Object>) ins;
                BigDecimal premium = getBigDecimal(insMap, "premium");
                if (premium == null) continue;

                String freq = getString(insMap, "premiumFrequency");
                if ("yearly".equalsIgnoreCase(freq)) {
                    premium = premium.divide(BigDecimal.valueOf(12), 2, FinancialMathUtil.ROUNDING_MODE);
                }
                total = total.add(premium);
            }
        }
        return total;
    }

    private BigDecimal calculateMonthlyInvestments(Map<String, Object> profileData) {
        Map<String, Object> investments = getSection(profileData, "investments");
        BigDecimal total = BigDecimal.ZERO;

        BigDecimal monthlySIP = getBigDecimal(investments, "monthlySIP");
        if (monthlySIP != null) total = total.add(monthlySIP);

        BigDecimal otherMonthly = getBigDecimal(investments, "otherMonthlyInvestment");
        if (otherMonthly != null) total = total.add(otherMonthly);

        return total;
    }
}
