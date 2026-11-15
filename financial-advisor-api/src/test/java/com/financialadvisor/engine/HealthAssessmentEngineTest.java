package com.financialadvisor.engine;

import com.financialadvisor.config.AppProperties;
import com.financialadvisor.engine.model.EmergencyCoverageResult;
import com.financialadvisor.engine.model.NetWorthResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UT-01: Net worth with mixed liquid/illiquid assets.
 * UT-03: Emergency coverage ratio.
 * UT-04: Emergency coverage recalculation after EMI starts.
 * UT-12: Emergency fund exclusion from investable capital.
 */
class HealthAssessmentEngineTest {

    private HealthAssessmentEngine engine;

    @BeforeEach
    void setUp() {
        var jwt = new AppProperties.Security.Jwt("test-secret-must-be-at-least-32-bytes-long!", 60);
        var cors = new AppProperties.Security.Cors("http://localhost");
        var pwd = new AppProperties.Security.Password("bcrypt", 4);
        var enc = new AppProperties.Security.Encryption("test-key-32b");
        var security = new AppProperties.Security(jwt, cors, pwd, enc);
        var financial = new AppProperties.Financial(RoundingMode.HALF_UP, 2, 7, 6);
        engine = new HealthAssessmentEngine(new AppProperties(security, financial));
    }

    @Test
    @DisplayName("UT-01: Net worth separates liquid and illiquid assets")
    void calculateNetWorth_separatesLiquidAndIlliquid() {
        Map<String, Object> profile = buildBaselineProfile();
        NetWorthResult result = engine.calculateNetWorth(profile);

        assertTrue(result.liquidAssets().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(result.illiquidAssets().compareTo(BigDecimal.ZERO) > 0);
        assertEquals(0, result.totalAssets().compareTo(
                result.liquidAssets().add(result.illiquidAssets())));
        assertEquals(0, result.netWorth().compareTo(
                result.totalAssets().subtract(result.totalLiabilities())));
    }

    @Test
    @DisplayName("UT-01: Net worth includes FD, EPF, MFs, stocks as liquid")
    void calculateNetWorth_fdEpfMfStocksAreLiquid() {
        Map<String, Object> profile = buildBaselineProfile();
        NetWorthResult result = engine.calculateNetWorth(profile);

        BigDecimal expectedLiquid = new BigDecimal("900000")
                .add(new BigDecimal("500000"))
                .add(new BigDecimal("344698"))
                .add(new BigDecimal("159611"));

        assertEquals(0, result.liquidAssets().compareTo(expectedLiquid.setScale(2, RoundingMode.HALF_UP)));
    }

    @Test
    @DisplayName("UT-01: Net worth includes land as illiquid")
    void calculateNetWorth_landIsIlliquid() {
        Map<String, Object> profile = buildBaselineProfile();
        NetWorthResult result = engine.calculateNetWorth(profile);

        assertTrue(result.illiquidAssets().compareTo(new BigDecimal("500000")) >= 0,
                "Illiquid should include land value");
    }

    @Test
    @DisplayName("UT-03: Emergency coverage for baseline profile (AC-2.3)")
    void calculateEmergencyCoverage_baselineProfile() {
        Map<String, Object> profile = buildBaselineProfile();
        EmergencyCoverageResult result = engine.calculateEmergencyCoverage(profile);

        // Mandatory monthly = ₹88K expenses + insurance premiums (~₹18.3K/mo)
        // ₹9L / ~₹106.3K ≈ 8.46 months
        assertTrue(result.coverageMonths().compareTo(new BigDecimal("8")) > 0,
                "Coverage should exceed 8 months");
        assertTrue(result.coverageMonths().compareTo(new BigDecimal("9")) < 0,
                "Coverage should be below 9 months (expenses + insurance included)");
        assertEquals("healthy", result.status());
    }

    @Test
    @DisplayName("UT-04: Emergency coverage decreases when EMI starts (AC-2.4)")
    void calculateEmergencyCoverage_withEMI_decreases() {
        Map<String, Object> profile = buildBaselineProfile();
        EmergencyCoverageResult before = engine.calculateEmergencyCoverage(profile);

        @SuppressWarnings("unchecked")
        Map<String, Object> liabilities = (Map<String, Object>) profile.get("liabilities");
        Map<String, Object> homeLoan = new LinkedHashMap<>();
        homeLoan.put("principal", 18000000);
        homeLoan.put("emiAmount", 150000);
        homeLoan.put("status", "active");
        liabilities.put("homeLoan", homeLoan);

        EmergencyCoverageResult after = engine.calculateEmergencyCoverage(profile);

        assertTrue(after.coverageMonths().compareTo(before.coverageMonths()) < 0,
                "Coverage should decrease when EMI starts");
    }

    @Test
    @DisplayName("UT-12: Emergency fund (locked=true) excluded from investable capital")
    void calculateInvestableCapital_excludesLockedEmergencyFund() {
        Map<String, Object> profile = buildBaselineProfile();
        BigDecimal investable = engine.calculateInvestableCapital(profile);

        BigDecimal emergencyFDValue = new BigDecimal("900000");
        BigDecimal totalLiquid = new BigDecimal("900000")
                .add(new BigDecimal("500000"))
                .add(new BigDecimal("344698"))
                .add(new BigDecimal("159611"));

        BigDecimal expectedInvestable = totalLiquid.subtract(emergencyFDValue);
        assertEquals(0, investable.compareTo(expectedInvestable.setScale(2, RoundingMode.HALF_UP)),
                "Investable capital should exclude the locked emergency FD");
    }

    @Test
    @DisplayName("Empty profile returns zero net worth")
    void calculateNetWorth_emptyProfile_returnsZero() {
        NetWorthResult result = engine.calculateNetWorth(Map.of());
        assertEquals(0, result.netWorth().compareTo(BigDecimal.ZERO.setScale(2)));
    }

    private Map<String, Object> buildBaselineProfile() {
        Map<String, Object> profile = new LinkedHashMap<>();

        Map<String, Object> assets = new LinkedHashMap<>();
        assets.put("emergencyFD", Map.of("value", 900000, "goalTag", "emergency", "locked", true));
        assets.put("epf", Map.of("value", 500000, "goalTag", "retirement"));
        assets.put("mutualFunds", Map.of("value", 344698));
        assets.put("indianStocks", Map.of("value", 159611));
        assets.put("land", Map.of("value", 500000, "liquid", false));
        profile.put("assets", assets);

        Map<String, Object> liabilities = new LinkedHashMap<>();
        liabilities.put("creditCard", Map.of("monthlySpend", 25000, "paidInFull", true, "isDebt", false));
        profile.put("liabilities", liabilities);

        Map<String, Object> expenses = new LinkedHashMap<>();
        expenses.put("monthlyHousehold", 88000);
        profile.put("expenses", expenses);

        Map<String, Object> insurance = new LinkedHashMap<>();
        insurance.put("termLife", Map.of("cover", 35000000, "premium", 11000, "premiumFrequency", "monthly"));
        insurance.put("termLifeSpouse", Map.of("cover", 10000000, "premium", 3000, "premiumFrequency", "monthly"));
        insurance.put("healthFamily", Map.of("cover", 1000000, "premium", 12000, "premiumFrequency", "yearly"));
        insurance.put("healthParents", Map.of("cover", 1000000, "premium", 40000, "premiumFrequency", "yearly"));
        profile.put("insurance", insurance);

        return profile;
    }
}
