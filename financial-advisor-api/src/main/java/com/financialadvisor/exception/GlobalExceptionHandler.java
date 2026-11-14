package com.financialadvisor.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * Global exception handler ensuring all errors follow the standard JSON format (NFR-5.3).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException ex) {
        log.warn("Application error: {} - {}", ex.getErrorCode(), ex.getMessage());
        HttpStatus status = mapErrorCodeToStatus(ex.getErrorCode());
        return ResponseEntity.status(status)
                .body(new ErrorResponse(ex.getErrorCode(), ex.getMessage(), ex.getDetails()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        List<ErrorResponse.FieldDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ErrorResponse.FieldDetail(error.getField(), error.getDefaultMessage()))
                .toList();

        log.warn("Validation error: {} field(s) invalid", details.size());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(ErrorCode.VALIDATION_ERROR, "Input validation failed", details));
    }

    @ExceptionHandler({BadCredentialsException.class, AuthenticationException.class})
    public ResponseEntity<ErrorResponse> handleAuthException(Exception ex) {
        log.warn("Authentication failure: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(ErrorCode.UNAUTHORIZED, "Invalid credentials"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ErrorCode.NOT_FOUND, "Resource not found: " + ex.getResourcePath()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(ErrorCode.INTERNAL_ERROR, "An unexpected error occurred"));
    }

    private HttpStatus mapErrorCodeToStatus(ErrorCode code) {
        return switch (code) {
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case VALIDATION_ERROR, BAD_REQUEST -> HttpStatus.BAD_REQUEST;
            case INSUFFICIENT_DATA, STALE_DATA -> HttpStatus.UNPROCESSABLE_ENTITY;
            case DOUBLE_COUNTING, EMERGENCY_FUND_BREACH -> HttpStatus.CONFLICT;
            case LLM_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            case CALCULATION_ERROR, INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
