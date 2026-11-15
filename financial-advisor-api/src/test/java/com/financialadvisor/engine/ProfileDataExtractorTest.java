package com.financialadvisor.engine;

import com.financialadvisor.exception.AppException;
import com.financialadvisor.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UT-16: Missing field handling — engine requests data.
 * UT-23: Input validation — negative financial amount rejected.
 */
class ProfileDataExtractorTest {

    @Test
    @DisplayName("UT-16: requireBigDecimal throws INSUFFICIENT_DATA for missing field")
    void requireBigDecimal_missingField_throwsInsufficientData() {
        Map<String, Object> data = Map.of("name", "test");

        AppException ex = assertThrows(AppException.class,
                () -> ProfileDataExtractor.requireBigDecimal(data, "monthlySalary", "cash flow"));

        assertEquals(ErrorCode.INSUFFICIENT_DATA, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("monthlySalary"));
        assertTrue(ex.getMessage().contains("cash flow"));
    }

    @Test
    @DisplayName("UT-23: Negative financial amount is rejected with VALIDATION_ERROR")
    void requireBigDecimal_negativeAmount_throwsValidationError() {
        Map<String, Object> data = Map.of("value", -50000);

        AppException ex = assertThrows(AppException.class,
                () -> ProfileDataExtractor.requireBigDecimal(data, "value", "net worth"));

        assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
    }

    @Test
    @DisplayName("UT-16: requireFields throws with all missing fields listed")
    void requireFields_multipleMissing_listsAll() {
        Map<String, Object> data = Map.of("name", "test");

        AppException ex = assertThrows(AppException.class,
                () -> ProfileDataExtractor.requireFields(data, "emergency coverage",
                        "income.monthlySalary", "expenses.monthlyHousehold"));

        assertEquals(ErrorCode.INSUFFICIENT_DATA, ex.getErrorCode());
        assertEquals(2, ex.getDetails().size());
    }

    @Test
    @DisplayName("requireSection throws for missing section")
    void requireSection_missing_throwsInsufficientData() {
        Map<String, Object> data = Map.of();

        AppException ex = assertThrows(AppException.class,
                () -> ProfileDataExtractor.requireSection(data, "income", "cash flow"));

        assertEquals(ErrorCode.INSUFFICIENT_DATA, ex.getErrorCode());
    }

    @Test
    @DisplayName("getSection returns empty map for missing section")
    void getSection_missing_returnsEmptyMap() {
        Map<String, Object> section = ProfileDataExtractor.getSection(Map.of(), "assets");
        assertTrue(section.isEmpty());
    }

    @Test
    @DisplayName("getBigDecimal converts various number types")
    void getBigDecimal_handlesTypes() {
        Map<String, Object> data = Map.of(
                "int", 100, "double", 99.5, "string", "200", "bd", new BigDecimal("300")
        );

        assertNotNull(ProfileDataExtractor.getBigDecimal(data, "int"));
        assertNotNull(ProfileDataExtractor.getBigDecimal(data, "double"));
        assertNotNull(ProfileDataExtractor.getBigDecimal(data, "string"));
        assertNotNull(ProfileDataExtractor.getBigDecimal(data, "bd"));
        assertNull(ProfileDataExtractor.getBigDecimal(data, "nonexistent"));
    }

    @Test
    @DisplayName("getBoolean returns correct values")
    void getBoolean_handlesTypes() {
        Map<String, Object> data = Map.of("a", true, "b", "true", "c", false);

        assertTrue(ProfileDataExtractor.getBoolean(data, "a"));
        assertTrue(ProfileDataExtractor.getBoolean(data, "b"));
        assertFalse(ProfileDataExtractor.getBoolean(data, "c"));
        assertFalse(ProfileDataExtractor.getBoolean(data, "missing"));
    }
}
