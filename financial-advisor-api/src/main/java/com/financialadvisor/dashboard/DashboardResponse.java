package com.financialadvisor.dashboard;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 10-dimension financial health dashboard response (FR-10.1, FR-10.2).
 *
 * Dimensions: (1) Liquidity, (2) Insurance, (3) Debt, (4) Retirement,
 * (5) Education, (6) Portfolio Risk, (7) Income Concentration,
 * (8) Emergency Coverage, (9) Housing Commitment, (10) Key Warnings.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DashboardResponse(
        String userId,
        String snapshotId,
        String snapshotDate,
        List<DimensionStatus> dimensions,
        FinancialSummary summary,
        String timestamp
) {

    public record DimensionStatus(
            int index,
            String name,
            String status,
            String description,
            List<String> actions
    ) {}

    public record FinancialSummary(
            BigDecimal netWorth,
            BigDecimal liquidAssets,
            BigDecimal illiquidAssets,
            BigDecimal totalLiabilities,
            BigDecimal monthlySurplus,
            BigDecimal emergencyCoverageMonths,
            String riskLevel,
            int goalCount,
            int goalsOnTrack,
            int goalsAtRisk
    ) {}

    public static DashboardResponse create(String userId, String snapshotId, String snapshotDate,
                                            List<DimensionStatus> dimensions,
                                            FinancialSummary summary) {
        return new DashboardResponse(userId, snapshotId, snapshotDate, dimensions,
                summary, Instant.now().toString());
    }
}
