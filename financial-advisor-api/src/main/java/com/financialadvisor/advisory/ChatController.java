package com.financialadvisor.advisory;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for conversational advisory (§4.2: POST /api/v1/chat).
 * Implements FR-12.5 (conversational chat interface).
 */
@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(
            Authentication auth,
            @Valid @RequestBody ChatRequest request) {
        String userId = auth.getName();
        log.info("Chat request from user {}: {}...", userId,
                request.message().substring(0, Math.min(request.message().length(), 50)));

        ChatResponse response = chatService.chat(userId, request.message());
        return ResponseEntity.ok(response);
    }
}
