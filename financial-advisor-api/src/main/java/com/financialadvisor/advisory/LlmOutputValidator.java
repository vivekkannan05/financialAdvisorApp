package com.financialadvisor.advisory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates LLM financial assertions against rule engine outputs (FR-12.4, EC-4).
 *
 * Scans LLM response text for numerical financial claims and verifies them
 * against the known-good data from tool function results. Unverifiable
 * claims are flagged with warnings.
 */
@Service
public class LlmOutputValidator {

    private static final Logger log = LoggerFactory.getLogger(LlmOutputValidator.class);

    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
            "₹\\s*([\\d,]+(?:\\.\\d{1,2})?)"
    );

    private static final Pattern PERCENTAGE_PATTERN = Pattern.compile(
            "(\\d+(?:\\.\\d{1,2})?)\\s*%"
    );

    private static final Pattern MONTHS_PATTERN = Pattern.compile(
            "(\\d+(?:\\.\\d{1,2})?)\\s+months?"
    );

    public record ValidationResult(
            String validatedResponse,
            List<String> warnings,
            boolean hasUnverifiedClaims
    ) {}

    /**
     * Validates an LLM response against known financial data.
     *
     * @param llmResponse     the raw LLM text output
     * @param verifiedData    map of verified values from tool calls (key=description, value=BigDecimal or String)
     * @return validation result with original or annotated response and any warnings
     */
    public ValidationResult validate(String llmResponse, Map<String, Object> verifiedData) {
        if (llmResponse == null || llmResponse.isBlank()) {
            return new ValidationResult("", List.of(), false);
        }

        if (verifiedData == null || verifiedData.isEmpty()) {
            return new ValidationResult(llmResponse, List.of(
                    "No verified data available to validate LLM assertions"
            ), true);
        }

        List<String> warnings = new ArrayList<>();
        Set<String> verifiedAmounts = extractVerifiedAmounts(verifiedData);

        List<String> claimedAmounts = extractAmounts(llmResponse);
        for (String claimed : claimedAmounts) {
            if (!verifiedAmounts.contains(normaliseAmount(claimed))) {
                String warning = "Unverified amount ₹" + claimed + " in LLM response — " +
                        "not found in rule engine outputs";
                warnings.add(warning);
                log.warn("LLM validation: {}", warning);
            }
        }

        boolean hasUnverified = !warnings.isEmpty();

        return new ValidationResult(llmResponse, warnings, hasUnverified);
    }

    private List<String> extractAmounts(String text) {
        List<String> amounts = new ArrayList<>();
        Matcher matcher = AMOUNT_PATTERN.matcher(text);
        while (matcher.find()) {
            amounts.add(matcher.group(1));
        }
        return amounts;
    }

    @SuppressWarnings("unchecked")
    private Set<String> extractVerifiedAmounts(Map<String, Object> data) {
        Set<String> amounts = new HashSet<>();
        flattenValues(data, amounts);
        return amounts;
    }

    @SuppressWarnings("unchecked")
    private void flattenValues(Object obj, Set<String> amounts) {
        if (obj instanceof Map<?, ?> map) {
            for (Object value : map.values()) {
                flattenValues(value, amounts);
            }
        } else if (obj instanceof List<?> list) {
            for (Object item : list) {
                flattenValues(item, amounts);
            }
        } else if (obj instanceof String str) {
            try {
                BigDecimal bd = new BigDecimal(str);
                amounts.add(normaliseAmount(bd.toPlainString()));
                amounts.add(normaliseAmount(str));
            } catch (NumberFormatException ignored) {
            }
        } else if (obj instanceof Number num) {
            amounts.add(normaliseAmount(BigDecimal.valueOf(num.doubleValue()).toPlainString()));
        }
    }

    private String normaliseAmount(String amount) {
        return amount.replace(",", "").replace(" ", "").trim();
    }
}
