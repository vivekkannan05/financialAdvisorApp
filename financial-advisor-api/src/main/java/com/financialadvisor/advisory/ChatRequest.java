package com.financialadvisor.advisory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Chat request body for POST /api/v1/chat.
 */
public record ChatRequest(
        @NotBlank(message = "Message is required")
        @Size(max = 5000, message = "Message must not exceed 5000 characters")
        String message
) {}
