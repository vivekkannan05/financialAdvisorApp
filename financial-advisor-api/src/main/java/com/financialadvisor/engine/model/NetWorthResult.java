package com.financialadvisor.engine.model;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Result of net worth calculation (FR-2.1).
 * Separates liquid and illiquid assets per spec requirement.
 */
public record NetWorthResult(
        BigDecimal totalAssets,
        BigDecimal liquidAssets,
        BigDecimal illiquidAssets,
        BigDecimal totalLiabilities,
        BigDecimal netWorth,
        Map<String, BigDecimal> assetBreakdown,
        Map<String, BigDecimal> liabilityBreakdown
) {}
