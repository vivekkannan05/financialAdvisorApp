package com.financialadvisor.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UT-27: Standard error response format — all error responses match the specified JSON schema.
 * Validates the ErrorResponse structure per NFR-5.3.
 */
class ErrorResponseTest {

    @Test
    @DisplayName("ErrorResponse with code and message produces correct structure")
    void simpleErrorResponse_hasRequiredFields() {
        var response = new ErrorResponse(ErrorCode.INSUFFICIENT_DATA,
                "Cannot calculate: monthly income not provided");

        assertEquals("INSUFFICIENT_DATA", response.error());
        assertEquals("Cannot calculate: monthly income not provided", response.message());
        assertNull(response.details());
        assertNotNull(response.timestamp());
    }

    @Test
    @DisplayName("ErrorResponse with field details includes all detail entries")
    void errorResponseWithDetails_includesFieldDetails() {
        var details = List.of(
                new ErrorResponse.FieldDetail("income.monthlySalary", "required for emergency coverage"),
                new ErrorResponse.FieldDetail("expenses.monthlyHousehold", "required for emergency coverage")
        );

        var response = new ErrorResponse(ErrorCode.INSUFFICIENT_DATA,
                "Missing required fields", details);

        assertEquals("INSUFFICIENT_DATA", response.error());
        assertEquals(2, response.details().size());
        assertEquals("income.monthlySalary", response.details().get(0).field());
        assertEquals("required for emergency coverage", response.details().get(0).reason());
    }

    @Test
    @DisplayName("ErrorResponse timestamp is ISO-8601 format")
    void errorResponse_timestampIsIso8601() {
        var response = new ErrorResponse(ErrorCode.UNAUTHORIZED, "Invalid credentials");
        assertNotNull(response.timestamp());
        assertTrue(response.timestamp().contains("T"), "Timestamp should be ISO-8601");
    }

    @Test
    @DisplayName("All error codes produce valid error strings")
    void allErrorCodes_produceValidStrings() {
        for (ErrorCode code : ErrorCode.values()) {
            var response = new ErrorResponse(code, "test");
            assertEquals(code.name(), response.error());
        }
    }

    @Test
    @DisplayName("AppException carries error code and details")
    void appException_carriesCodeAndDetails() {
        var details = List.of(new ErrorResponse.FieldDetail("field", "missing"));
        var ex = new AppException(ErrorCode.STALE_DATA, "Data is stale", details);

        assertEquals(ErrorCode.STALE_DATA, ex.getErrorCode());
        assertEquals("Data is stale", ex.getMessage());
        assertEquals(1, ex.getDetails().size());
    }
}
