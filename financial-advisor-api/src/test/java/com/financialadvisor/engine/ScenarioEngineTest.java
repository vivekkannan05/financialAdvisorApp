package com.financialadvisor.engine;

import com.financialadvisor.config.AppProperties;
import com.financialadvisor.engine.model.ScenarioComparison;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UT-15: Scenario comparison — SIP increase vs mortgage prepayment.
 */
class ScenarioEngineTest {

    private ScenarioEngine scenarioEngine;

    @BeforeEach
    void setUp() {
        var jwt = new AppProperties.Security.Jwt("test-secret-must-be-at-least-32-bytes-long!", 60);
        var cors = new AppProperties.Security.Cors("http://localhost");
        var pwd = new AppProperties.Security.Password("bcrypt", 4);
        var enc = new AppProperties.Security.Encryption("test-key-32b");
        var security = new AppProperties.Security(jwt, cors, pwd, enc);
        var financial = new AppProperties.Financial(RoundingMode.HALF_UP, 2, 7, 6);
        var props = new AppProperties(security, financial);

        var healthEngine = new HealthAssessmentEngine(props);
        var goalEngine = new GoalPlanningEngine();
        scenarioEngine = new ScenarioEngine(healthEngine, goalEngine);
    }

    @Test
    @DisplayName("UT-15: SIP vs Prepay comparison shows both outcomes with tradeoffs")
    void compareSipVsPrepay_returnsBothOutcomes() {
        Map<String, Object> profile = buildProfileWithGoals();

        ScenarioComparison result = scenarioEngine.compareSipVsPrepayment(
                profile,
                new BigDecimal("10000"),
                new BigDecimal("500000"),
                new BigDecimal("0.085"),
                20
        );

        assertNotNull(result.scenarioA());
        assertNotNull(result.scenarioB());
        assertEquals("Increase SIP", result.scenarioAName());
        assertEquals("Mortgage Prepayment", result.scenarioBName());
    }

    @Test
    @DisplayName("UT-15: SIP scenario shows projected corpus growth")
    void compareSipVsPrepay_sipShowsCorpusGrowth() {
        Map<String, Object> profile = buildProfileWithGoals();

        ScenarioComparison result = scenarioEngine.compareSipVsPrepayment(
                profile, new BigDecimal("10000"), new BigDecimal("500000"),
                new BigDecimal("0.085"), 20
        );

        assertTrue(result.scenarioA().projectedCorpus().compareTo(BigDecimal.ZERO) > 0,
                "SIP scenario should project corpus growth");
        assertFalse(result.scenarioA().advantages().isEmpty());
    }

    @Test
    @DisplayName("UT-15: Prepay scenario shows interest saved")
    void compareSipVsPrepay_prepayShowsInterestSaved() {
        Map<String, Object> profile = buildProfileWithGoals();

        ScenarioComparison result = scenarioEngine.compareSipVsPrepayment(
                profile, new BigDecimal("10000"), new BigDecimal("500000"),
                new BigDecimal("0.085"), 20
        );

        assertTrue(result.scenarioB().totalInterestSaved().compareTo(BigDecimal.ZERO) > 0,
                "Prepay scenario should show interest saved");
        assertFalse(result.scenarioB().advantages().isEmpty());
    }

    @Test
    @DisplayName("Comparison includes tradeoffs and recommendation")
    void compareSipVsPrepay_includesTradeoffsAndRecommendation() {
        Map<String, Object> profile = buildProfileWithGoals();

        ScenarioComparison result = scenarioEngine.compareSipVsPrepayment(
                profile, new BigDecimal("10000"), new BigDecimal("500000"),
                new BigDecimal("0.085"), 20
        );

        assertFalse(result.tradeoffs().isEmpty(), "Should include tradeoff analysis");
        assertNotNull(result.recommendation(), "Should include a recommendation");
        assertFalse(result.recommendation().isBlank());
    }

    @Test
    @DisplayName("High loan rate recommendation favours prepayment")
    void compareSipVsPrepay_highLoanRate_favoursPrepayment() {
        Map<String, Object> profile = buildProfileWithGoals();

        ScenarioComparison result = scenarioEngine.compareSipVsPrepayment(
                profile, new BigDecimal("10000"), new BigDecimal("500000"),
                new BigDecimal("0.10"), 20
        );

        assertTrue(result.recommendation().toLowerCase().contains("prepay"),
                "High loan rate should favour prepayment");
    }

    private Map<String, Object> buildProfileWithGoals() {
        Map<String, Object> profile = new LinkedHashMap<>();
        Map<String, Object> assets = new LinkedHashMap<>();
        assets.put("mutualFunds", Map.of("value", 344698));
        assets.put("indianStocks", Map.of("value", 159611));
        assets.put("emergencyFD", Map.of("value", 900000, "goalTag", "emergency", "locked", true));
        profile.put("assets", assets);
        profile.put("expenses", Map.of("monthlyHousehold", 88000));
        profile.put("liabilities", Map.of());
        profile.put("insurance", Map.of());
        profile.put("goals", List.of(
                Map.of("id", "retirement", "name", "Retirement", "targetAmountToday", 5000000, "targetYear", 2046)
        ));
        return profile;
    }
}
