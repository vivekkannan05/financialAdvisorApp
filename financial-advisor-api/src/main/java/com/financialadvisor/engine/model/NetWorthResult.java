package com.financialadvisor.engine.model;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Result of net worth calculation (FR-2.1).
 * Uses 4-tier liquidity classification:
 *   Liquid (Savings, FD, Cash, RD, Post Office)
 *   Market-Linked (MF, Stocks, SGB, Corporate Bonds, Digital Gold, ULIP, Crypto)
 *   Restricted (EPF, NPS, PPF — lock-in periods)
 *   Illiquid (Gold, Land, Real Estate, Property)
 */
public record NetWorthResult(
        BigDecimal totalAssets,
        BigDecimal liquidAssets,
        BigDecimal marketLinkedAssets,
        BigDecimal restrictedAssets,
        BigDecimal illiquidAssets,
        BigDecimal totalLiabilities,
        BigDecimal netWorth,
        Map<String, BigDecimal> assetBreakdown,
        Map<String, BigDecimal> liabilityBreakdown
) {
    /** Liquid + Market-Linked: assets accessible within days (for risk/stress calculations). */
    public BigDecimal accessibleAssets() {
        return liquidAssets.add(marketLinkedAssets);
    }
}
