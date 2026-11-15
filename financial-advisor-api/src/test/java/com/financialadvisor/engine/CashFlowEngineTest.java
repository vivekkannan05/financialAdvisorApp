package com.financialadvisor.engine;

import com.financialadvisor.engine.model.CashFlowResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UT-02: Cash-flow surplus calculation.
 * UT-10: Credit card paid-in-full not counted as debt.
 * UT-11: Credit card revolving balance counted as debt.
 */
class CashFlowEngineTest {

    private CashFlowEngine engine;

    @BeforeEach
    void setUp() {
        engine = new CashFlowEngine();
    }

    @Test
    @DisplayName("UT-02: Cash-flow surplus = income - (expenses + EMIs + insurance + investments)")
    void calculateCashFlow_correctSurplus() {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("income", Map.of("monthlySalary", 200000));
        profile.put("expenses", Map.of("monthlyHousehold", 88000));
        profile.put("liabilities", Map.of(
                "homeLoan", Map.of("principal", 18000000, "emiAmount", 50000)
        ));
        profile.put("insurance", Map.of(
                "termLife", Map.of("premium", 11000, "premiumFrequency", "monthly")
        ));
        profile.put("investments", Map.of("monthlySIP", 15000));

        CashFlowResult result = engine.calculateCashFlow(profile);

        assertEquals(0, result.grossMonthlyIncome().compareTo(new BigDecimal("200000.00")));
        assertEquals(0, result.monthlyExpenses().compareTo(new BigDecimal("88000.00")));
        assertEquals(0, result.monthlyEMIs().compareTo(new BigDecimal("50000.00")));
        assertEquals(0, result.monthlyInsurance().compareTo(new BigDecimal("11000.00")));
        assertEquals(0, result.monthlyInvestments().compareTo(new BigDecimal("15000.00")));

        BigDecimal expectedSurplus = new BigDecimal("200000")
                .subtract(new BigDecimal("88000"))
                .subtract(new BigDecimal("50000"))
                .subtract(new BigDecimal("11000"))
                .subtract(new BigDecimal("15000"));
        assertEquals(0, result.monthlySurplus().compareTo(expectedSurplus.setScale(2)));
        assertTrue(result.hasSurplus());
    }

    @Test
    @DisplayName("UT-10: Credit card paid-in-full is NOT debt (FR-5.3, DL-012)")
    void isCreditCardDebt_paidInFull_notDebt() {
        Map<String, Object> cc = Map.of("monthlySpend", 25000, "paidInFull", true);
        assertFalse(engine.isCreditCardDebt(cc));
    }

    @Test
    @DisplayName("UT-11: Credit card with revolving balance IS debt")
    void isCreditCardDebt_revolvingBalance_isDebt() {
        Map<String, Object> cc = Map.of("monthlySpend", 25000, "paidInFull", false, "isDebt", true);
        assertTrue(engine.isCreditCardDebt(cc));
    }

    @Test
    @DisplayName("UT-11: Credit card not paid in full defaults to debt")
    void isCreditCardDebt_notPaidInFull_defaultsToDebt() {
        Map<String, Object> cc = Map.of("monthlySpend", 25000, "paidInFull", false);
        assertTrue(engine.isCreditCardDebt(cc));
    }

    @Test
    @DisplayName("Cash flow with no income shows deficit")
    void calculateCashFlow_noIncome_showsDeficit() {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("expenses", Map.of("monthlyHousehold", 88000));

        CashFlowResult result = engine.calculateCashFlow(profile);
        assertTrue(result.isDeficit());
    }

    @Test
    @DisplayName("Yearly insurance premium is divided by 12")
    void calculateCashFlow_yearlyInsurance_dividedBy12() {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("income", Map.of("monthlySalary", 200000));
        profile.put("insurance", Map.of(
                "health", Map.of("premium", 12000, "premiumFrequency", "yearly")
        ));

        CashFlowResult result = engine.calculateCashFlow(profile);
        assertEquals(0, result.monthlyInsurance().compareTo(new BigDecimal("1000.00")));
    }
}
