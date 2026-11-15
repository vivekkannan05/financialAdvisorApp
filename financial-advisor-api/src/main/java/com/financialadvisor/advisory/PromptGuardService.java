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

    /* ── Prompt injection patterns ─────────────────────────────────────── */
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

    /* ── Off-topic detection ───────────────────────────────────────────
     * Matches common non-financial requests.  Each pattern is broad enough
     * to catch variations but narrow enough to avoid false positives on
     * legitimate financial queries (e.g. "write off" is financial).        */
    private static final List<Pattern> OFF_TOPIC_PATTERNS = List.of(
            Pattern.compile("(?i)\\b(write|compose|draft)\\s+.{0,30}\\b(poem|song|story|essay|code|script|email|letter)\\b"),
            Pattern.compile("(?i)\\b(help|assist)\\s+(me\\s+)?(with\\s+)?(my\\s+)?(homework|assignment|exam|test|interview)\\b"),
            Pattern.compile("(?i)\\b(what('s|\\s+is)\\s+the\\s+)(weather|time|date|news|score)\\b"),
            Pattern.compile("(?i)\\b(tell|say)\\s+(me\\s+)?(a\\s+)?(joke|riddle|fun\\s+fact|story)\\b"),
            Pattern.compile("(?i)\\b(translate|convert)\\s+.{0,30}(to|into)\\s+(hindi|tamil|french|spanish|german|japanese|chinese)\\b"),
            Pattern.compile("(?i)\\b(play|sing|draw|paint|design|cook|recipe|workout|exercise|diet\\s+plan)\\b"),
            Pattern.compile("(?i)\\b(who\\s+(is|was|are)|when\\s+(was|did|is)|where\\s+(is|was))\\s+(?!.*\\b(tax|loan|fund|invest|insur|bank|financ|money|rupee|lakh|crore|emi|sip|nps|epf|ppf|fd|rd|stock|mutual|debt|credit|debit|saving|budget|retire|goal|wealth|portfolio|income|expense|salary|bonus)\\b).{5,}"),
            Pattern.compile("(?i)\\b(generate|create|build|make)\\s+(a\\s+)?(website|app|program|game|image|picture|logo|video)\\b"),
            Pattern.compile("(?i)\\b(summarize|summarise|explain)\\s+(this\\s+)?(article|book|movie|show|series|paper)\\b"),
            Pattern.compile("(?i)\\b(relationship|dating|love|breakup|boyfriend|girlfriend|marriage\\s+advice)\\b"),
            Pattern.compile("(?i)\\b(medical|health|symptoms?|diseases?|medicines?|diagnos\\w*|prescription|doctor)\\b(?!.*\\b(insurance|cover|claim|premium|policy|floater)\\b)")
    );

    private static final String OFF_TOPIC_DECLINE =
            "😊 I appreciate the question, but I'm your **financial adviser** — " +
            "I can only help with money-related topics like savings, investments, loans, " +
            "insurance, taxes, goals, and budgeting.\n\n" +
            "Try asking me something like:\n" +
            "• \"Am I saving enough for retirement?\"\n" +
            "• \"Should I prepay my home loan or invest more?\"\n" +
            "• \"What happens if I lose my job?\"\n" +
            "• \"Is my emergency fund enough?\"";

    private static final String INJECTION_DECLINE =
            "I'm your financial adviser. I can only help with financial planning questions.";

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
                return GuardResult.blocked(INJECTION_DECLINE);
            }
        }

        for (Pattern pattern : OFF_TOPIC_PATTERNS) {
            if (pattern.matcher(userMessage).find()) {
                log.info("Prompt guard: off-topic message declined — '{}'", pattern.pattern());
                return GuardResult.blocked(OFF_TOPIC_DECLINE);
            }
        }

        return GuardResult.ok();
    }
}
