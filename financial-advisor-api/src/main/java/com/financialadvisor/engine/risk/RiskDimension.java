package com.financialadvisor.engine.risk;

/**
 * Sealed interface for risk dimensions (Design Doc §4.2.3).
 *
 * Each dimension evaluates one aspect of risk. The composite
 * RiskAssessor applies the "safer constraint wins" rule (FR-4.3).
 */
public sealed interface RiskDimension
        permits ToleranceDimension, CapacityDimension, RequirementDimension,
                HorizonDimension, LifeStageDimension {

    RiskScore evaluate(RiskContext context);
}
