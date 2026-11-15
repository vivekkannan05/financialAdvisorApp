package com.financialadvisor.advisory;

import com.financialadvisor.exception.AppException;
import com.financialadvisor.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Orchestrates the chat advisory flow (FR-12.3, FR-12.5).
 *
 * Flow:
 * 1. Prompt guard screens input (NFR-1.6)
 * 2. Check Ollama availability (NFR-3.8)
 * 3. If available: invoke ChatClient with tools → validate output (FR-12.4)
 * 4. If unavailable: return rule-engine-only fallback (NFR-3.3)
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatClient chatClient;
    private final PromptGuardService promptGuard;
    private final OllamaAvailabilityService ollamaAvailability;
    private final LlmOutputValidator validator;
    private final FinancialAdvisoryTools tools;

    public ChatService(ChatClient.Builder chatClientBuilder,
                       FinancialAdvisoryTools advisoryTools,
                       PromptGuardService promptGuard,
                       OllamaAvailabilityService ollamaAvailability,
                       LlmOutputValidator validator) {
        this.chatClient = chatClientBuilder
                .defaultSystem(AdvisorySystemPrompt.PERSONA)
                .defaultTools(advisoryTools)
                .build();
        this.promptGuard = promptGuard;
        this.ollamaAvailability = ollamaAvailability;
        this.validator = validator;
        this.tools = advisoryTools;
    }

    /**
     * Processes a user chat message through the full advisory pipeline.
     *
     * @param userId  the authenticated user ID
     * @param message the user's chat message
     * @return ChatResponse with AI or fallback response
     */
    public ChatResponse chat(String userId, String message) {
        PromptGuardService.GuardResult guard = promptGuard.screen(message);
        if (!guard.passed()) {
            log.warn("Prompt guard blocked message from user {}: {}", userId, guard.reason());
            return ChatResponse.guardBlocked(guard.reason());
        }

        ollamaAvailability.refresh();

        if (!ollamaAvailability.isAvailable()) {
            log.info("Ollama unavailable — returning rule-engine fallback for user {}", userId);
            return buildFallbackResponse(userId, message);
        }

        try {
            String contextualMessage = "User ID: " + userId + "\nUser question: " + message;

            String aiResponse = chatClient.prompt()
                    .user(contextualMessage)
                    .call()
                    .content();

            Map<String, Object> engineData = tools.getFinancialHealth(userId);

            LlmOutputValidator.ValidationResult validation = validator.validate(aiResponse, engineData);

            log.info("Chat completed for user {} — {} validation warnings",
                    userId, validation.warnings().size());

            return ChatResponse.aiResponse(
                    validation.validatedResponse(),
                    engineData,
                    validation.warnings()
            );

        } catch (Exception e) {
            log.error("LLM call failed for user {} — falling back to rule engine: {}",
                    userId, e.getMessage());
            return buildFallbackResponse(userId, message);
        }
    }

    /**
     * Builds a rule-engine-only response when the LLM is unavailable (NFR-3.3).
     */
    private ChatResponse buildFallbackResponse(String userId, String message) {
        try {
            Map<String, Object> engineData = new LinkedHashMap<>();
            engineData.put("financialHealth", tools.getFinancialHealth(userId));
            engineData.put("goalFunding", tools.calculateGoalFunding(userId));
            engineData.put("riskAssessment", tools.assessRisk(userId));

            String fallbackMessage = AdvisorySystemPrompt.FALLBACK_NOTICE +
                    "\n\nYour question: \"" + message + "\"\n\n" +
                    "Your current financial data is attached below. " +
                    "Please review the computed results while AI interpretation is being restored.";

            return ChatResponse.fallbackResponse(fallbackMessage, engineData);

        } catch (AppException e) {
            return ChatResponse.fallbackResponse(
                    AdvisorySystemPrompt.FALLBACK_NOTICE +
                    "\n\nCould not retrieve profile data: " + e.getMessage(),
                    null
            );
        }
    }
}
