package com.financialadvisor.engine.risk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UT-08: Near-term goal gets low risk despite high tolerance (FR-3.6, FR-4.3).
 * UT-09: Safer constraint wins when dimensions conflict (FR-4.3).
 */
class RiskAssessorTest {

    private RiskAssessor assessor;

    @BeforeEach
    void setUp() {
        assessor = new RiskAssessor();
    }

    @Test
    @DisplayName("UT-08: Near-term goal (2 years) gets VERY_LOW risk despite growth-oriented tolerance")
    void assess_nearTermGoal_overridesTolerance() {
        RiskContext context = new RiskContext(
                "growth-oriented",
                new BigDecimal("50000"),
                BigDecimal.ZERO,
                new BigDecimal("1000000"),
                new BigDecimal("200000"),
                new BigDecimal("10"),
                null,
                2,
                40, 60,
                new BigDecimal("1000000"),
                BigDecimal.ZERO
        );

        CompositeRiskResult result = assessor.assess(context);

        assertEquals(RiskLevel.VERY_LOW, result.effectiveLevel(),
                "2-year goal should force VERY_LOW despite growth tolerance");
        assertEquals("Horizon", result.constrainingDimension());
    }

    @Test
    @DisplayName("UT-09: Safer constraint wins — tolerance HIGH but capacity LOW")
    void assess_saferConstraintWins() {
        RiskContext context = new RiskContext(
                "aggressive",
                BigDecimal.ZERO,
                new BigDecimal("5000000"),
                new BigDecimal("100000"),
                new BigDecimal("50000"),
                new BigDecimal("1"),
                null,
                15,
                40, 60,
                new BigDecimal("100000"),
                new BigDecimal("5000000")
        );

        CompositeRiskResult result = assessor.assess(context);

        assertTrue(result.effectiveLevel().numericLevel() <= RiskLevel.LOW.numericLevel(),
                "High debt, low liquidity, zero surplus should force LOW or VERY_LOW despite aggressive tolerance");

        RiskScore toleranceScore = result.dimensionScores().stream()
                .filter(s -> "Tolerance".equals(s.dimension()))
                .findFirst().orElseThrow();
        assertTrue(toleranceScore.level().numericLevel() > result.effectiveLevel().numericLevel(),
                "Tolerance should suggest higher risk than the effective composite level");
    }

    @Test
    @DisplayName("Long horizon + good capacity + growth tolerance = HIGH risk")
    void assess_longHorizon_goodCapacity_highRisk() {
        RiskContext context = new RiskContext(
                "growth-oriented",
                new BigDecimal("80000"),
                BigDecimal.ZERO,
                new BigDecimal("2000000"),
                new BigDecimal("200000"),
                new BigDecimal("12"),
                null,
                20,
                35, 60,
                new BigDecimal("2000000"),
                BigDecimal.ZERO
        );

        CompositeRiskResult result = assessor.assess(context);
        assertTrue(result.effectiveLevel().numericLevel() >= RiskLevel.HIGH.numericLevel(),
                "Long horizon, good financials, growth tolerance should allow HIGH risk");
    }

    @Test
    @DisplayName("Composite result contains all 5 dimension scores")
    void assess_containsAllFiveDimensions() {
        RiskContext context = new RiskContext(
                "moderate", new BigDecimal("50000"),
                BigDecimal.ZERO, new BigDecimal("1000000"),
                new BigDecimal("200000"), new BigDecimal("8"),
                null, 10, 40, 60,
                new BigDecimal("1000000"), BigDecimal.ZERO
        );

        CompositeRiskResult result = assessor.assess(context);
        assertEquals(5, result.dimensionScores().size());
    }

    @Test
    @DisplayName("Near-retirement life stage forces lower risk")
    void assess_nearRetirement_lowersRisk() {
        RiskContext context = new RiskContext(
                "growth-oriented",
                new BigDecimal("50000"),
                BigDecimal.ZERO,
                new BigDecimal("5000000"),
                new BigDecimal("200000"),
                new BigDecimal("10"),
                null,
                10,
                57, 60,
                new BigDecimal("5000000"),
                BigDecimal.ZERO
        );

        CompositeRiskResult result = assessor.assess(context);

        RiskScore lifeStage = result.dimensionScores().stream()
                .filter(s -> "LifeStage".equals(s.dimension()))
                .findFirst().orElseThrow();
        assertTrue(lifeStage.level().numericLevel() <= RiskLevel.LOW.numericLevel(),
                "3 years to retirement should map to LOW or VERY_LOW");
    }
}
