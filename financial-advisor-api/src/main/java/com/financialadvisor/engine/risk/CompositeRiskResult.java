package com.financialadvisor.engine.risk;

import java.util.List;

/**
 * Result of composite risk assessment across all 5 dimensions (FR-4.2).
 */
public record CompositeRiskResult(
        RiskLevel effectiveLevel,
        List<RiskScore> dimensionScores,
        String constrainingDimension,
        String summary
) {}
