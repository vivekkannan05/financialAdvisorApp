package com.financialadvisor.advisory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory conversation history per user for multi-turn chat context.
 *
 * Stores the last N message pairs (user + assistant) per user, auto-expired
 * after a configurable inactivity timeout. This gives the LLM conversational
 * context so follow-up questions work naturally.
 *
 * V1: in-memory only — history lost on backend restart.
 * V2: persist to DynamoDB for cross-session continuity using key pattern:
 *   PK = USER#<userId>, SK = CHAT#<timestamp>-<uuid>
 *   Store messages as a JSON list attribute, TTL via DynamoDB TTL column.
 *   This allows conversation history to survive restarts and enables
 *   "continue where I left off" across browser sessions.
 */
@Component
public class ConversationMemory {

    private static final Logger log = LoggerFactory.getLogger(ConversationMemory.class);

    private static final int MAX_PAIRS = 10;
    private static final long TTL_MILLIS = 30 * 60 * 1000L; // 30 minutes

    private final ConcurrentHashMap<String, UserConversation> conversations = new ConcurrentHashMap<>();

    public record Message(String role, String content) {}

    private static class UserConversation {
        final List<Message> messages = new ArrayList<>();
        volatile long lastAccessedAt = Instant.now().toEpochMilli();

        void touch() {
            this.lastAccessedAt = Instant.now().toEpochMilli();
        }

        boolean isExpired() {
            return (Instant.now().toEpochMilli() - lastAccessedAt) > TTL_MILLIS;
        }
    }

    /**
     * Returns the conversation history for a user (up to MAX_PAIRS exchanges).
     * Expired conversations are cleared automatically.
     */
    public List<Message> getHistory(String userId) {
        cleanExpired();
        UserConversation conv = conversations.get(userId);
        if (conv == null || conv.isExpired()) {
            conversations.remove(userId);
            return List.of();
        }
        conv.touch();
        synchronized (conv.messages) {
            return List.copyOf(conv.messages);
        }
    }

    /**
     * Adds a user message and assistant response to the conversation history.
     * Trims to MAX_PAIRS (oldest messages dropped first).
     */
    public void addExchange(String userId, String userMessage, String assistantResponse) {
        UserConversation conv = conversations.computeIfAbsent(userId, _ -> new UserConversation());
        conv.touch();

        synchronized (conv.messages) {
            conv.messages.add(new Message("user", userMessage));
            conv.messages.add(new Message("assistant", assistantResponse));

            while (conv.messages.size() > MAX_PAIRS * 2) {
                conv.messages.removeFirst();
                conv.messages.removeFirst();
            }
        }

        log.debug("Conversation memory for user {}: {} messages ({} pairs)",
                userId, conv.messages.size(), conv.messages.size() / 2);
    }

    /**
     * Clears conversation history for a user (e.g., on logout).
     */
    public void clear(String userId) {
        conversations.remove(userId);
    }

    /**
     * Removes expired conversations to prevent memory leaks.
     */
    private void cleanExpired() {
        conversations.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
}
