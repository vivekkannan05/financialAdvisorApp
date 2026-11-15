package com.financialadvisor.advisory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UT-19: LLM output validation — rule engine strips/flags unverifiable financial assertions
 * (FR-12.4, EC-4).
 */
class LlmOutputValidatorTest {

    private LlmOutputValidator validator;

    @BeforeEach
    void setUp() {
        validator = new LlmOutputValidator();
    }

    @Test
    @DisplayName("UT-19: Verified amounts in LLM response pass validation")
    void verifiedAmounts_passValidation() {
        Map<String, Object> verifiedData = Map.of(
                "netWorth", "1500000.00",
                "monthlySurplus", "25000.00"
        );

        String llmResponse = "Your net worth is ₹1500000.00 with a monthly surplus of ₹25000.00.";
        var result = validator.validate(llmResponse, verifiedData);

        assertFalse(result.hasUnverifiedClaims());
        assertTrue(result.warnings().isEmpty());
    }

    @Test
    @DisplayName("UT-19: Unverified amounts are flagged with warning")
    void unverifiedAmounts_flaggedWithWarning() {
        Map<String, Object> verifiedData = Map.of(
                "netWorth", "1500000.00"
        );

        String llmResponse = "Your net worth is ₹1500000.00 but you could earn ₹500000 more.";
        var result = validator.validate(llmResponse, verifiedData);

        assertTrue(result.hasUnverifiedClaims());
        assertFalse(result.warnings().isEmpty());
        assertFalse(result.validatedResponse().contains("⚠️"),
                "Validator should not inject warnings into the response text — they go as separate data");
    }

    @Test
    @DisplayName("UT-19: Response without amounts passes validation")
    void noAmounts_passesValidation() {
        Map<String, Object> verifiedData = Map.of("netWorth", "100000");

        String llmResponse = "I recommend increasing your SIP allocation.";
        var result = validator.validate(llmResponse, verifiedData);

        assertFalse(result.hasUnverifiedClaims());
    }

    @Test
    @DisplayName("UT-19: Null verified data flags entire response as unverified")
    void nullVerifiedData_flagsAsUnverified() {
        String llmResponse = "Your net worth is ₹5000000.";
        var result = validator.validate(llmResponse, null);

        assertTrue(result.hasUnverifiedClaims());
    }

    @Test
    @DisplayName("Empty LLM response returns empty result")
    void emptyResponse_returnsEmptyResult() {
        var result = validator.validate("", Map.of("data", "value"));
        assertFalse(result.hasUnverifiedClaims());
        assertTrue(result.validatedResponse().isEmpty());
    }

    @Test
    @DisplayName("Nested verified data is correctly flattened for matching")
    void nestedVerifiedData_flattened() {
        Map<String, Object> verifiedData = new LinkedHashMap<>();
        verifiedData.put("financial", Map.of(
                "netWorth", "2500000.00",
                "surplus", "50000.00"
        ));

        String llmResponse = "Your net worth is ₹2500000.00.";
        var result = validator.validate(llmResponse, verifiedData);

        assertFalse(result.hasUnverifiedClaims());
    }

    @Test
    @DisplayName("Amounts with commas are normalised for matching")
    void amountsWithCommas_normalised() {
        Map<String, Object> verifiedData = Map.of("value", "1500000");

        String llmResponse = "The total is ₹15,00,000.";
        var result = validator.validate(llmResponse, verifiedData);

        assertFalse(result.hasUnverifiedClaims(),
                "₹15,00,000 should match verified value 1500000 after normalisation");
    }
}
