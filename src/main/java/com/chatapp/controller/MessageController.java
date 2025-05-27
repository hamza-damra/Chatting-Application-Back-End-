package com.chatapp.controller;

import com.chatapp.dto.MessageRequest;
import com.chatapp.dto.MessageResponse;
import com.chatapp.dto.MessageStatusUpdateRequest;
import com.chatapp.dto.PagedResponse;
import com.chatapp.exception.InvalidMessageStatusException;
import com.chatapp.service.MessageService;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    /**
     * Handle POST requests to the base endpoint
     */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Object> handleBasePostEndpoint() {
        return ResponseEntity
            .badRequest()
            .body(Map.of(
                "error", "Invalid endpoint",
                "message", "Messages must be sent to a specific chat room using /api/messages/chatroom/{chatRoomId}",
                "timestamp", LocalDateTime.now()
            ));
    }

    /**
     * Handle GET requests to the base endpoint
     */
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Object> handleBaseGetEndpoint() {
        return ResponseEntity
            .ok()
            .body(Map.of(
                "message", "To get messages, use one of the following endpoints:",
                "endpoints", List.of(
                    "/api/messages/chatroom/{chatRoomId} - Get messages for a specific chat room",
                    "/api/messages/{id} - Get a specific message by ID"
                ),
                "timestamp", LocalDateTime.now()
            ));
    }

    @GetMapping("/chatroom/{chatRoomId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PagedResponse<MessageResponse>> getChatRoomMessages(
            @PathVariable Long chatRoomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Validate pagination parameters
        if (page < 0) {
            page = 0;
        }
        if (size <= 0 || size > 100) {
            size = 20; // Default to reasonable size
        }

        PagedResponse<MessageResponse> messages = messageService.getChatRoomMessages(chatRoomId, page, size);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<MessageResponse> getMessageById(@PathVariable Long id) {
        MessageResponse message = messageService.getMessageResponseById(id);
        return ResponseEntity.ok(message);
    }

    @PostMapping("/chatroom/{chatRoomId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable Long chatRoomId,
            @Valid @RequestBody MessageRequest request) {
        MessageResponse message = messageService.sendMessage(chatRoomId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long id) {
        messageService.deleteMessage(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/unread")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<MessageResponse>> getUnreadMessages() {
        List<MessageResponse> messages = messageService.getUnreadMessages();
        return ResponseEntity.ok(messages);
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> markMessageAsRead(@PathVariable Long id) {
        messageService.markMessageAsRead(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> updateMessageStatus(
            @PathVariable Long id,
            @Valid @RequestBody MessageStatusUpdateRequest request) {
        try {
            messageService.updateMessageStatus(id, request.getStatus());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            throw new InvalidMessageStatusException(e.getMessage());
        }
    }

    @PutMapping("/chatroom/{chatRoomId}/read")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> markAllMessagesAsRead(@PathVariable Long chatRoomId) {
        messageService.markAllMessagesAsRead(chatRoomId);
        return ResponseEntity.noContent().build();
    }
}
