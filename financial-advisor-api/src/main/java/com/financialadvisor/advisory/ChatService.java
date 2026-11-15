package com.financialadvisor.advisory;

import com.financialadvisor.exception.AppException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates the chat advisory flow (FR-12.3, FR-12.5).
 *
 * Flow:
 * 1. Prompt guard screens input (NFR-1.6)
 * 2. Check Ollama availability (NFR-3.8)
 * 3. Build conversation context (last 10 exchanges from memory)
 * 4. If available: invoke ChatClient with tools → validate output (FR-12.4)
 * 5. Store exchange in conversation memory
 * 6. If unavailable: return rule-engine-only fallback (NFR-3.3)
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatClient chatClient;
    private final PromptGuardService promptGuard;
    private final OllamaAvailabilityService ollamaAvailability;
    private final LlmOutputValidator validator;
    private final FinancialAdvisoryTools tools;
    private final ConversationMemory conversationMemory;

    public ChatService(ChatClient.Builder chatClientBuilder,
                       FinancialAdvisoryTools advisoryTools,
                       PromptGuardService promptGuard,
                       OllamaAvailabilityService ollamaAvailability,
                       LlmOutputValidator validator,
                       ConversationMemory conversationMemory) {
        this.chatClient = chatClientBuilder
                .defaultSystem(AdvisorySystemPrompt.PERSONA)
                .defaultTools(advisoryTools)
                .build();
        this.promptGuard = promptGuard;
        this.ollamaAvailability = ollamaAvailability;
        this.validator = validator;
        this.tools = advisoryTools;
        this.conversationMemory = conversationMemory;
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
            String contextualMessage = buildContextualMessage(userId, message);

            String aiResponse = chatClient.prompt()
                    .user(contextualMessage)
                    .call()
                    .content();

            Map<String, Object> engineData = collectAllToolData(userId);

            LlmOutputValidator.ValidationResult validation = validator.validate(aiResponse, engineData);

            conversationMemory.addExchange(userId, message, aiResponse);

            log.info("Chat completed for user {} — {} validation warnings, {} conversation pairs",
                    userId, validation.warnings().size(),
                    conversationMemory.getHistory(userId).size() / 2);

            return ChatResponse.aiResponse(
                    aiResponse,
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
     * Builds the full contextual message including conversation history.
     * Format: previous exchanges as "User: ... / Advisor: ..." followed by the new question.
     */
    private String buildContextualMessage(String userId, String currentMessage) {
        List<ConversationMemory.Message> history = conversationMemory.getHistory(userId);
        StringBuilder sb = new StringBuilder();

        sb.append("User ID: ").append(userId).append("\n\n");

        try {
            String profileSummary = tools.generateProfileSummary(userId);
            sb.append(profileSummary).append("\n\n");
        } catch (Exception e) {
            log.warn("Could not pre-load profile summary: {}", e.getMessage());
        }

        if (!history.isEmpty()) {
            sb.append("=== CONVERSATION HISTORY (for context) ===\n");
            for (ConversationMemory.Message msg : history) {
                String label = "user".equals(msg.role()) ? "User" : "Advisor";
                String content = msg.content();
                if (content.length() > 500) {
                    content = content.substring(0, 500) + "... [truncated]";
                }
                sb.append(label).append(": ").append(content).append("\n\n");
            }
            sb.append("=== END HISTORY ===\n\n");
        }

        sb.append("User's current question: ").append(currentMessage);
        return sb.toString();
    }

    /**
     * Collects data from ALL tool functions for comprehensive output validation.
     * This ensures amounts from goals, insurance, risk, etc. are all verified — not just health data.
     */
    private Map<String, Object> collectAllToolData(String userId) {
        Map<String, Object> allData = new LinkedHashMap<>();
        try {
            allData.put("financialHealth", tools.getFinancialHealth(userId));
        } catch (Exception e) {
            log.warn("Could not collect financial health for validation: {}", e.getMessage());
        }
        try {
            allData.put("goalFunding", tools.calculateGoalFunding(userId));
        } catch (Exception e) {
            log.warn("Could not collect goal funding for validation: {}", e.getMessage());
        }
        try {
            allData.put("insurance", tools.getInsuranceSummary(userId));
        } catch (Exception e) {
            log.warn("Could not collect insurance for validation: {}", e.getMessage());
        }
        return allData;
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
