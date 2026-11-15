package com.financialadvisor.engine.risk;

/**
 * Score from a single risk dimension evaluation (FR-4.2).
 */
public record RiskScore(
        RiskLevel level,
        String dimension,
        String reasoning
) {}
