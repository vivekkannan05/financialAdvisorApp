package com.financialadvisor.engine.risk;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Composite risk assessor (Design Doc §4.2.3).
 *
 * Evaluates all 5 risk dimensions and applies the core rule:
 * "When risk dimensions conflict, the safer constraint wins" (FR-4.3).
 */
@Component
public class RiskAssessor {

    private final List<RiskDimension> dimensions;

    public RiskAssessor() {
        this.dimensions = List.of(
                new ToleranceDimension(),
                new CapacityDimension(),
                new RequirementDimension(),
                new HorizonDimension(),
                new LifeStageDimension()
        );
    }

    /**
     * Evaluates all 5 dimensions and returns composite result.
     * The effective level = minimum (safest) across all dimensions.
     */
    public CompositeRiskResult assess(RiskContext context) {
        List<RiskScore> scores = dimensions.stream()
                .map(d -> d.evaluate(context))
                .toList();

        RiskScore constraining = scores.stream()
                .min(Comparator.comparingInt(s -> s.level().numericLevel()))
                .orElse(new RiskScore(RiskLevel.MODERATE, "Default", "No dimensions evaluated"));

        RiskLevel effective = constraining.level();

        String summary = "Effective risk: " + effective.label()
                + " (constrained by " + constraining.dimension() + ": " + constraining.reasoning() + ")";

        return new CompositeRiskResult(effective, scores, constraining.dimension(), summary);
    }
}
