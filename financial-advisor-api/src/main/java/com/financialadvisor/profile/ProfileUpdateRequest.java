package com.financialadvisor.profile;

import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

/**
 * Profile update request DTO (NFR-1.7).
 *
 * Uses a flexible Map structure to support the flexible profile schema (FR-1.2).
 * The map is validated at the controller level for required structure,
 * and individual field values are validated by the financial engines
 * when specific calculations are requested (NFR-3.1).
 */
public record ProfileUpdateRequest(
        @NotEmpty(message = "Profile data cannot be empty") Map<String, Object> data
) {}
