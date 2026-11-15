package com.financialadvisor.advisory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Prompt injection detection service (NFR-1.6).
 *
 * Screens user chat input for common prompt injection patterns before
 * passing it to the LLM. The system prompt persona is protected from
 * user-prompt overrides.
 */
@Service
public class PromptGuardService {

    private static final Logger log = LoggerFactory.getLogger(PromptGuardService.class);

    private static final List<Pattern> INJECTION_PATTERNS = List.of(
            Pattern.compile("(?i)ignore\\s+(all\\s+)?previous\\s+(instructions|prompts|rules)"),
            Pattern.compile("(?i)forget\\s+(all\\s+)?(previous|your|above)\\s+(instructions|prompts|rules|context)"),
            Pattern.compile("(?i)you\\s+are\\s+now\\s+(a|an|the)\\s+"),
            Pattern.compile("(?i)act\\s+as\\s+(a|an|if)\\s+"),
            Pattern.compile("(?i)pretend\\s+(you\\s+are|to\\s+be)\\s+"),
            Pattern.compile("(?i)new\\s+(instructions|rules|persona|role)\\s*:"),
            Pattern.compile("(?i)system\\s*:\\s*"),
            Pattern.compile("(?i)\\[SYSTEM\\]"),
            Pattern.compile("(?i)override\\s+(system|default|previous)"),
            Pattern.compile("(?i)disregard\\s+(all|any|your)"),
            Pattern.compile("(?i)jailbreak"),
            Pattern.compile("(?i)DAN\\s+mode"),
            Pattern.compile("(?i)do\\s+anything\\s+now")
    );

    public record GuardResult(boolean passed, String reason) {
        public static GuardResult ok() {
            return new GuardResult(true, null);
        }

        public static GuardResult blocked(String reason) {
            return new GuardResult(false, reason);
        }
    }

    /**
     * Screens user input for prompt injection attempts.
     *
     * @param userMessage the raw user chat message
     * @return GuardResult indicating whether the message is safe
     */
    public GuardResult screen(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return GuardResult.blocked("Empty message");
        }

        if (userMessage.length() > 10_000) {
            log.warn("Prompt guard: message exceeds 10,000 characters — potential injection");
            return GuardResult.blocked("Message too long");
        }

        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(userMessage).find()) {
                log.warn("Prompt guard: injection pattern detected — '{}'", pattern.pattern());
                return GuardResult.blocked("I'm your financial adviser. " +
                        "I can only help with financial planning questions.");
            }
        }

        return GuardResult.ok();
    }
}
