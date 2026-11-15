package com.financialadvisor.engine;

import com.financialadvisor.config.AppProperties;
import com.financialadvisor.engine.model.EmergencyCoverageResult;
import com.financialadvisor.engine.model.NetWorthResult;
import com.financialadvisor.math.FinancialMathUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static com.financialadvisor.engine.ProfileDataExtractor.*;

/**
 * Health Assessment Engine (FR-2.x).
 *
 * Calculates: net worth, cash flow health, emergency coverage, concentration risk.
 * All calculations use BigDecimal with HALF_UP rounding (NFR-3.5).
 */
@Service
public class HealthAssessmentEngine {

    /* ── 4-tier liquidity classification keywords ─────────────────────── */

    /** Tier 1 — instantly accessible (savings, FD, cash, RD, post office). */
    private static final Set<String> LIQUID_KEYWORDS = Set.of(
            "saving", "fixed deposit", "fd", "cash", "recurring deposit", "rd", "post office");

    /** Tier 2 — redeemable at market value within days (MF, stocks, bonds, crypto). */
    private static final Set<String> MARKET_LINKED_KEYWORDS = Set.of(
            "mutual fund", "mutualfund", "stock", "share", "sovereign gold",
            "corporate bond", "digital gold", "ulip", "cryptocurrency", "crypto",
            "sip", "equity");

    /** Tier 3 — lock-in / withdrawal restrictions (EPF, NPS, PPF). */
    private static final Set<String> RESTRICTED_KEYWORDS = Set.of(
            "epf", "nps", "ppf", "provident fund");

    /** Tier 4 — hard to convert quickly (physical gold, land, property). */
    private static final Set<String> ILLIQUID_ASSET_TYPES = Set.of(
            "land", "gold", "apartment", "property", "real estate", "real_estate", "realestate");

    private final BigDecimal emergencyThresholdMonths;

    public HealthAssessmentEngine(AppProperties appProperties) {
        this.emergencyThresholdMonths = BigDecimal.valueOf(
                appProperties.financial().emergencyMonthsThreshold());
    }

    /**
     * Calculates net worth with 4-tier liquidity breakdown (FR-2.1).
     */
    public NetWorthResult calculateNetWorth(Map<String, Object> profileData) {
        Map<String, Object> assets = getSection(profileData, "assets");
        Map<String, Object> liabilities = getSection(profileData, "liabilities");

        BigDecimal liquidTotal = BigDecimal.ZERO;
        BigDecimal marketLinkedTotal = BigDecimal.ZERO;
        BigDecimal restrictedTotal = BigDecimal.ZERO;
        BigDecimal illiquidTotal = BigDecimal.ZERO;
        Map<String, BigDecimal> assetBreakdown = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : assets.entrySet()) {
            String assetName = entry.getKey();
            BigDecimal value = extractAssetValue(entry.getValue());
            if (value == null) continue;

            assetBreakdown.put(assetName, FinancialMathUtil.roundInr(value));

            switch (classifyAssetTier(assetName, entry.getValue())) {
                case LIQUID -> liquidTotal = liquidTotal.add(value);
                case MARKET_LINKED -> marketLinkedTotal = marketLinkedTotal.add(value);
                case RESTRICTED -> restrictedTotal = restrictedTotal.add(value);
                case ILLIQUID -> illiquidTotal = illiquidTotal.add(value);
            }
        }

        BigDecimal liabilityTotal = BigDecimal.ZERO;
        Map<String, BigDecimal> liabilityBreakdown = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : liabilities.entrySet()) {
            String liabilityName = entry.getKey();
            BigDecimal value = extractLiabilityValue(entry.getValue());
            if (value == null) continue;

            liabilityBreakdown.put(liabilityName, FinancialMathUtil.roundInr(value));
            liabilityTotal = liabilityTotal.add(value);
        }

        BigDecimal totalAssets = liquidTotal.add(marketLinkedTotal).add(restrictedTotal).add(illiquidTotal);
        BigDecimal netWorth = totalAssets.subtract(liabilityTotal);

        return new NetWorthResult(
                FinancialMathUtil.roundInr(totalAssets),
                FinancialMathUtil.roundInr(liquidTotal),
                FinancialMathUtil.roundInr(marketLinkedTotal),
                FinancialMathUtil.roundInr(restrictedTotal),
                FinancialMathUtil.roundInr(illiquidTotal),
                FinancialMathUtil.roundInr(liabilityTotal),
                FinancialMathUtil.roundInr(netWorth),
                assetBreakdown,
                liabilityBreakdown
        );
    }

    /**
     * Calculates emergency coverage in months (FR-2.3).
     * Emergency assets = assets tagged as "emergency" with locked=true.
     * Mandatory monthly expense = expenses + EMIs + insurance premiums.
     */
    public EmergencyCoverageResult calculateEmergencyCoverage(Map<String, Object> profileData) {
        BigDecimal emergencyAssets = calculateEmergencyAssets(profileData);
        BigDecimal mandatoryMonthly = calculateMandatoryMonthlyExpense(profileData);

        BigDecimal coverageMonths = FinancialMathUtil.emergencyCoverageMonths(
                emergencyAssets, mandatoryMonthly);

        String status;
        if (coverageMonths.compareTo(emergencyThresholdMonths) >= 0) {
            status = "healthy";
        } else if (coverageMonths.compareTo(new BigDecimal("3")) >= 0) {
            status = "warning";
        } else {
            status = "critical";
        }

        return new EmergencyCoverageResult(
                emergencyAssets, mandatoryMonthly, coverageMonths,
                emergencyThresholdMonths, status);
    }

    /**
     * Calculates total investable capital (excluding locked/emergency assets per FR-6.4).
     */
    public BigDecimal calculateInvestableCapital(Map<String, Object> profileData) {
        Map<String, Object> assets = getSection(profileData, "assets");
        BigDecimal investable = BigDecimal.ZERO;

        for (Map.Entry<String, Object> entry : assets.entrySet()) {
            if (isLocked(entry.getValue())) continue;
            if (isIlliquid(entry.getKey(), entry.getValue())) continue;

            BigDecimal value = extractAssetValue(entry.getValue());
            if (value != null) {
                investable = investable.add(value);
            }
        }

        return FinancialMathUtil.roundInr(investable);
    }

    /**
     * Sums emergency-designated assets (locked FDs, emergency-tagged assets).
     */
    BigDecimal calculateEmergencyAssets(Map<String, Object> profileData) {
        Map<String, Object> assets = getSection(profileData, "assets");
        BigDecimal total = BigDecimal.ZERO;

        for (Map.Entry<String, Object> entry : assets.entrySet()) {
            if (isEmergencyTagged(entry.getValue())) {
                BigDecimal value = extractAssetValue(entry.getValue());
                if (value != null) total = total.add(value);
            }
        }
        return total;
    }

    /**
     * Calculates mandatory monthly expenditure (expenses + EMIs + insurance).
     */
    BigDecimal calculateMandatoryMonthlyExpense(Map<String, Object> profileData) {
        Map<String, Object> expenses = getSection(profileData, "expenses");
        BigDecimal monthlyExpense = getBigDecimal(expenses, "monthlyHousehold");

        if (monthlyExpense == null) {
            monthlyExpense = BigDecimal.ZERO;
            for (Map.Entry<String, Object> entry : expenses.entrySet()) {
                BigDecimal value = FinancialMathUtil.toBigDecimal(entry.getValue());
                if (value != null) monthlyExpense = monthlyExpense.add(value);
            }
        }

        BigDecimal emiTotal = calculateMonthlyEMIs(profileData);
        BigDecimal insuranceTotal = calculateMonthlyInsurance(profileData);

        return monthlyExpense.add(emiTotal).add(insuranceTotal);
    }

    private BigDecimal calculateMonthlyEMIs(Map<String, Object> profileData) {
        Map<String, Object> liabilities = getSection(profileData, "liabilities");
        BigDecimal total = BigDecimal.ZERO;

        for (Map.Entry<String, Object> entry : liabilities.entrySet()) {
            if (entry.getValue() instanceof Map<?, ?> liab) {
                @SuppressWarnings("unchecked")
                Map<String, Object> liabMap = (Map<String, Object>) liab;
                BigDecimal emi = getBigDecimal(liabMap, "emiAmount");
                if (emi == null) emi = getBigDecimal(liabMap, "emi");
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
                if ("quarterly".equalsIgnoreCase(freq)) {
                    premium = premium.divide(BigDecimal.valueOf(3), 2, FinancialMathUtil.ROUNDING_MODE);
                } else if ("half-yearly".equalsIgnoreCase(freq)) {
                    premium = premium.divide(BigDecimal.valueOf(6), 2, FinancialMathUtil.ROUNDING_MODE);
                } else if ("yearly".equalsIgnoreCase(freq)) {
                    premium = premium.divide(BigDecimal.valueOf(12), 2, FinancialMathUtil.ROUNDING_MODE);
                }
                total = total.add(premium);
            } else if (entry.getKey().toLowerCase().endsWith("premium")) {
                BigDecimal premium = FinancialMathUtil.toBigDecimal(entry.getValue());
                if (premium != null) total = total.add(premium);
            }
        }
        return total;
    }

    @SuppressWarnings("unchecked")
    private BigDecimal extractAssetValue(Object asset) {
        if (asset instanceof Map<?, ?> map) {
            return getBigDecimal((Map<String, Object>) map, "value");
        }
        return FinancialMathUtil.toBigDecimal(asset);
    }

    @SuppressWarnings("unchecked")
    private BigDecimal extractLiabilityValue(Object liability) {
        if (liability instanceof Map<?, ?> map) {
            Map<String, Object> liabMap = (Map<String, Object>) map;
            BigDecimal principal = getBigDecimal(liabMap, "principal");
            if (principal != null) return principal;
            BigDecimal outstanding = getBigDecimal(liabMap, "outstanding");
            if (outstanding != null) return outstanding;
            return getBigDecimal(liabMap, "value");
        }
        return FinancialMathUtil.toBigDecimal(liability);
    }

    /* ── 4-tier asset classification ──────────────────────────────────── */

    enum AssetTier { LIQUID, MARKET_LINKED, RESTRICTED, ILLIQUID }

    @SuppressWarnings("unchecked")
    AssetTier classifyAssetTier(String assetName, Object asset) {
        String lower = assetName.toLowerCase().replaceAll("[\\s_/\\u2014\\-]+", " ").trim();

        if (matchesAny(lower, RESTRICTED_KEYWORDS)) return AssetTier.RESTRICTED;
        if (matchesAny(lower, MARKET_LINKED_KEYWORDS)) return AssetTier.MARKET_LINKED;

        if (matchesAny(lower, ILLIQUID_ASSET_TYPES)) return AssetTier.ILLIQUID;
        if (asset instanceof Map<?, ?> map) {
            Object liquidFlag = ((Map<String, Object>) map).get("liquid");
            if (Boolean.FALSE.equals(liquidFlag)) return AssetTier.ILLIQUID;
        }

        return AssetTier.LIQUID;
    }

    private static boolean matchesAny(String normalised, Set<String> keywords) {
        for (String kw : keywords) {
            if (normalised.contains(kw)) return true;
        }
        return false;
    }

    /** Backward-compatible check used by {@link #calculateInvestableCapital}. */
    @SuppressWarnings("unchecked")
    private boolean isIlliquid(String assetName, Object asset) {
        AssetTier tier = classifyAssetTier(assetName, asset);
        return tier == AssetTier.ILLIQUID;
    }

    @SuppressWarnings("unchecked")
    private boolean isLocked(Object asset) {
        if (asset instanceof Map<?, ?> map) {
            return getBoolean((Map<String, Object>) map, "locked");
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private boolean isEmergencyTagged(Object asset) {
        if (asset instanceof Map<?, ?> map) {
            String tag = getString((Map<String, Object>) map, "goalTag");
            if (tag == null) return false;
            String lower = tag.toLowerCase();
            return lower.contains("emergency");
        }
        return false;
    }
}
