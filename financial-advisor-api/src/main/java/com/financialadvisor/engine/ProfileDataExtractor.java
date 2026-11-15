package com.financialadvisor.engine;

import com.financialadvisor.exception.AppException;
import com.financialadvisor.exception.ErrorCode;
import com.financialadvisor.exception.ErrorResponse;
import com.financialadvisor.math.FinancialMathUtil;

import java.math.BigDecimal;
import java.util.*;

/**
 * Safely extracts typed values from the flexible profile data map.
 * Implements the strict data validation pattern (NFR-3.1, NFR-3.2):
 * if required fields are missing, throws INSUFFICIENT_DATA with details.
 */
public final class ProfileDataExtractor {

    private ProfileDataExtractor() {}

    /**
     * Gets a nested map from the profile data.
     * Returns empty map if the section doesn't exist.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getSection(Map<String, Object> data, String key) {
        Object val = data.get(key);
        if (val instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Collections.emptyMap();
    }

    /**
     * Gets a nested map, throwing INSUFFICIENT_DATA if missing.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> requireSection(Map<String, Object> data,
                                                      String key, String purpose) {
        Object val = data.get(key);
        if (val instanceof Map<?, ?> map && !map.isEmpty()) {
            return (Map<String, Object>) map;
        }
        throw insufficientData(key, purpose);
    }

    /**
     * Gets a list from the profile data.
     */
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> getListSection(Map<String, Object> data, String key) {
        Object val = data.get(key);
        if (val instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return Collections.emptyList();
    }

    /**
     * Extracts a BigDecimal value from a map.
     * Returns null if the field is missing or null.
     */
    public static BigDecimal getBigDecimal(Map<String, Object> map, String field) {
        return FinancialMathUtil.toBigDecimal(map.get(field));
    }

    /**
     * Extracts a required BigDecimal value, throwing INSUFFICIENT_DATA if missing.
     */
    public static BigDecimal requireBigDecimal(Map<String, Object> map,
                                                String field, String purpose) {
        BigDecimal val = getBigDecimal(map, field);
        if (val == null) {
            throw insufficientData(field, purpose);
        }
        if (val.compareTo(BigDecimal.ZERO) < 0) {
            throw new AppException(ErrorCode.VALIDATION_ERROR,
                    "Financial amount cannot be negative: " + field,
                    List.of(new ErrorResponse.FieldDetail(field, "must be non-negative")));
        }
        return val;
    }

    /**
     * Gets a String value from a map, or null.
     */
    public static String getString(Map<String, Object> map, String field) {
        Object val = map.get(field);
        return val != null ? val.toString() : null;
    }

    /**
     * Gets an integer value from a map, or null.
     */
    public static Integer getInteger(Map<String, Object> map, String field) {
        Object val = map.get(field);
        if (val instanceof Number num) return num.intValue();
        if (val instanceof String str) {
            try { return Integer.parseInt(str); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    /**
     * Gets a boolean value from a map, defaulting to false.
     */
    public static boolean getBoolean(Map<String, Object> map, String field) {
        Object val = map.get(field);
        if (val instanceof Boolean b) return b;
        if (val instanceof String s) return Boolean.parseBoolean(s);
        return false;
    }

    /**
     * Validates that all required fields are present, throwing a single
     * INSUFFICIENT_DATA error listing all missing fields.
     */
    public static void requireFields(Map<String, Object> data,
                                      String purpose, String... fields) {
        List<ErrorResponse.FieldDetail> missing = new ArrayList<>();
        for (String field : fields) {
            if (!hasValue(data, field)) {
                missing.add(new ErrorResponse.FieldDetail(field, "required for " + purpose));
            }
        }
        if (!missing.isEmpty()) {
            throw new AppException(ErrorCode.INSUFFICIENT_DATA,
                    "Cannot perform " + purpose + ": " + missing.size() + " required field(s) missing",
                    missing);
        }
    }

    private static boolean hasValue(Map<String, Object> data, String dotPath) {
        String[] parts = dotPath.split("\\.");
        Object current = data;
        for (String part : parts) {
            if (current instanceof Map<?, ?> map) {
                current = map.get(part);
            } else {
                return false;
            }
        }
        return current != null;
    }

    private static AppException insufficientData(String field, String purpose) {
        return new AppException(ErrorCode.INSUFFICIENT_DATA,
                "Cannot calculate " + purpose + ": " + field + " is not provided",
                List.of(new ErrorResponse.FieldDetail(field, "required for " + purpose)));
    }
}
