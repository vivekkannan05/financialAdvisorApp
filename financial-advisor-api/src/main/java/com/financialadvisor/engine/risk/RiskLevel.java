package com.financialadvisor.engine.risk;

/**
 * Risk levels from most conservative to most aggressive.
 * Numeric value enables the "safer constraint wins" comparison (FR-4.3).
 */
public enum RiskLevel {
    VERY_LOW(1, "Very Conservative"),
    LOW(2, "Conservative"),
    MODERATE(3, "Moderate"),
    HIGH(4, "Aggressive"),
    VERY_HIGH(5, "Very Aggressive");

    private final int numericLevel;
    private final String label;

    RiskLevel(int numericLevel, String label) {
        this.numericLevel = numericLevel;
        this.label = label;
    }

    public int numericLevel() { return numericLevel; }
    public String label() { return label; }

    /**
     * Returns the safer (lower) of two risk levels.
     */
    public RiskLevel saferOf(RiskLevel other) {
        return this.numericLevel <= other.numericLevel ? this : other;
    }
}
