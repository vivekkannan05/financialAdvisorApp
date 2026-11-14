package com.financialadvisor.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Standard error response format per NFR-5.3.
 * All API errors return this structure.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String error,
        String message,
        List<FieldDetail> details,
        String timestamp
) {
    public ErrorResponse(ErrorCode errorCode, String message) {
        this(errorCode.name(), message, null, Instant.now().toString());
    }

    public ErrorResponse(ErrorCode errorCode, String message, List<FieldDetail> details) {
        this(errorCode.name(), message, details, Instant.now().toString());
    }

    public record FieldDetail(String field, String reason) {}
}
