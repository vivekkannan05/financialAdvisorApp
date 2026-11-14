package com.financialadvisor.exception;

import java.util.List;

/**
 * Base application exception that carries an ErrorCode and optional field details.
 * Used by business logic to signal domain-specific errors.
 */
public class AppException extends RuntimeException {

    private final ErrorCode errorCode;
    private final List<ErrorResponse.FieldDetail> details;

    public AppException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.details = null;
    }

    public AppException(ErrorCode errorCode, String message, List<ErrorResponse.FieldDetail> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public List<ErrorResponse.FieldDetail> getDetails() {
        return details;
    }
}
