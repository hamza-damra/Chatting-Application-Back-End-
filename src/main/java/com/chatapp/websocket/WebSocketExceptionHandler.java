package com.chatapp.websocket;

import com.chatapp.exception.ChatRoomAccessDeniedException;
import com.chatapp.exception.DuplicateChatRoomException;
import com.chatapp.exception.InvalidMessageStatusException;
import com.chatapp.exception.ResourceNotFoundException;
import com.chatapp.exception.WebSocketConnectionException;
import org.springframework.security.access.AccessDeniedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles exceptions that occur during WebSocket communication.
 */
@Controller
@Slf4j
@RequiredArgsConstructor
public class WebSocketExceptionHandler {

    private final SimpMessageSendingOperations messagingTemplate;

    @MessageExceptionHandler(ResourceNotFoundException.class)
    public void handleResourceNotFoundException(ResourceNotFoundException ex, Principal principal) {
        log.error("Resource not found in WebSocket communication: {}", ex.getMessage());
        if (principal != null) {
            sendErrorToUser(principal.getName(), "NOT_FOUND", ex.getMessage());
        } else {
            log.error("Cannot send error to user: principal is null");
        }
    }

    @MessageExceptionHandler(ChatRoomAccessDeniedException.class)
    public void handleChatRoomAccessDeniedException(ChatRoomAccessDeniedException ex, Principal principal) {
        log.error("Access denied to chat room in WebSocket communication: {}", ex.getMessage());
        log.error("Exception details: ", ex); // Log the full stack trace for debugging

        if (principal != null) {
            try {
                String username = principal.getName();
                log.info("Sending ACCESS_DENIED error to user: {}", username);

                // Create error details
                Map<String, Object> errorDetails = new HashMap<>();
                errorDetails.put("type", "ACCESS_DENIED");
                errorDetails.put("message", ex.getMessage());
                errorDetails.put("timestamp", System.currentTimeMillis());

                // Send to the user's error queue
                messagingTemplate.convertAndSendToUser(
                    username,
                    "/queue/errors",
                    errorDetails
                );
                log.info("Successfully sent ACCESS_DENIED error to user: {}", username);

                // Also send to the general error topic for debugging
                messagingTemplate.convertAndSend(
                    "/topic/errors",
                    errorDetails
                );
                log.info("Sent ACCESS_DENIED error to general topic");

            } catch (Exception e) {
                log.error("Failed to send error to user: {}", principal.getName(), e);
            }
        } else {
            // Also send to a general topic for anonymous users
            try {
                Map<String, Object> errorDetails = new HashMap<>();
                errorDetails.put("type", "ACCESS_DENIED");
                errorDetails.put("message", ex.getMessage());
                errorDetails.put("timestamp", System.currentTimeMillis());

                messagingTemplate.convertAndSend("/topic/errors", errorDetails);
                log.info("Sent ACCESS_DENIED error to general topic for anonymous user");
            } catch (Exception e) {
                log.error("Failed to send error to general topic", e);
            }

            log.error("Cannot send error to user: principal is null");
        }
    }

    @MessageExceptionHandler(DuplicateChatRoomException.class)
    public void handleDuplicateChatRoomException(DuplicateChatRoomException ex, Principal principal) {
        log.error("Duplicate chat room creation attempt in WebSocket communication: {}", ex.getMessage());
        if (principal != null) {
            sendErrorToUser(principal.getName(), "DUPLICATE_CHAT", ex.getMessage());
        } else {
            log.error("Cannot send error to user: principal is null");
        }
    }

    @MessageExceptionHandler(NullPointerException.class)
    public void handleNullPointerException(NullPointerException ex, Principal principal) {
        log.error("NullPointerException in WebSocket communication: ", ex);
        if (principal != null) {
            sendErrorToUser(principal.getName(), "INTERNAL_ERROR", "An internal error occurred. Please try again.");
        } else {
            log.error("Cannot send error to user: principal is null");
        }
    }

    @MessageExceptionHandler(Exception.class)
    public void handleGenericException(Exception ex, Principal principal) {
        log.error("Unexpected exception in WebSocket communication: ", ex);
        if (principal != null) {
            sendErrorToUser(principal.getName(), "INTERNAL_ERROR", "An unexpected error occurred. Please try again.");
        } else {
            log.error("Cannot send error to user: principal is null");
        }
    }

    @MessageExceptionHandler(InvalidMessageStatusException.class)
    public void handleInvalidMessageStatusException(InvalidMessageStatusException ex, Principal principal) {
        log.error("Invalid message status in WebSocket communication: {}", ex.getMessage());
        if (principal != null) {
            sendErrorToUser(principal.getName(), "INVALID_STATUS", ex.getMessage());
        } else {
            log.error("Cannot send error to user: principal is null");
        }
    }

    @MessageExceptionHandler(MessageDeliveryException.class)
    public void handleMessageDeliveryException(MessageDeliveryException ex, Principal principal) {
        log.error("Message delivery failed in WebSocket communication: {}", ex.getMessage());
        if (principal != null) {
            sendErrorToUser(principal.getName(), "DELIVERY_FAILED", "Failed to deliver message");
        } else {
            log.error("Cannot send error to user: principal is null");
        }
    }

    @MessageExceptionHandler(WebSocketConnectionException.class)
    public void handleWebSocketConnectionException(WebSocketConnectionException ex, Principal principal) {
        log.error("WebSocket connection error: {}", ex.getMessage());
        if (principal != null) {
            sendErrorToUser(principal.getName(), "CONNECTION_ERROR", ex.getMessage());
        } else {
            log.error("Cannot send error to user: principal is null");
        }
    }

    @MessageExceptionHandler(AccessDeniedException.class)
    public void handleAccessDeniedException(AccessDeniedException ex, Principal principal) {
        log.error("Access denied in WebSocket communication: {}", ex.getMessage());
        log.error("Exception details: ", ex); // Log the full stack trace for debugging

        if (principal != null) {
            try {
                log.info("Sending AUTHENTICATION_REQUIRED error to user: {}", principal.getName());
                sendErrorToUser(principal.getName(), "AUTHENTICATION_REQUIRED", "Authentication is required for this operation");
                log.info("Successfully sent AUTHENTICATION_REQUIRED error to user: {}", principal.getName());
            } catch (Exception e) {
                log.error("Failed to send error to user: {}", principal.getName(), e);
            }
        } else {
            // For authentication errors, also send to a general topic
            try {
                Map<String, Object> errorDetails = new HashMap<>();
                errorDetails.put("type", "AUTHENTICATION_REQUIRED");
                errorDetails.put("message", "Authentication is required for this operation");
                errorDetails.put("timestamp", System.currentTimeMillis());

                messagingTemplate.convertAndSend("/topic/errors", errorDetails);
                log.info("Sent AUTHENTICATION_REQUIRED error to general topic");
            } catch (Exception e) {
                log.error("Failed to send error to general topic", e);
            }

            log.error("Cannot send error to user: principal is null");
        }
    }

    @MessageExceptionHandler(IllegalArgumentException.class)
    public void handleIllegalArgumentException(IllegalArgumentException ex, Principal principal) {
        log.error("Invalid argument in WebSocket communication: {}", ex.getMessage());
        log.error("Exception details: ", ex); // Log the full stack trace for debugging

        if (principal != null) {
            try {
                log.info("Sending BAD_REQUEST error to user: {}", principal.getName());
                sendErrorToUser(principal.getName(), "BAD_REQUEST", ex.getMessage());
                log.info("Successfully sent BAD_REQUEST error to user: {}", principal.getName());
            } catch (Exception e) {
                log.error("Failed to send error to user: {}", principal.getName(), e);
            }
        } else {
            // Also send to a general topic
            try {
                Map<String, Object> errorDetails = new HashMap<>();
                errorDetails.put("type", "BAD_REQUEST");
                errorDetails.put("message", ex.getMessage());
                errorDetails.put("timestamp", System.currentTimeMillis());

                messagingTemplate.convertAndSend("/topic/errors", errorDetails);
                log.info("Sent BAD_REQUEST error to general topic");
            } catch (Exception e) {
                log.error("Failed to send error to general topic", e);
            }

            log.error("Cannot send error to user: principal is null");
        }
    }



    private void sendErrorToUser(String username, String errorType, String errorMessage) {
        if (username == null || username.isEmpty()) {
            log.error("Cannot send error to user: username is null or empty");
            return;
        }

        try {
            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("type", errorType);
            errorDetails.put("message", errorMessage);
            errorDetails.put("timestamp", System.currentTimeMillis());

            // Also send to a general error topic for debugging
            messagingTemplate.convertAndSend(
                "/topic/errors",
                errorDetails
            );
            log.debug("Sent error to general topic: /topic/errors");

            // Send to the user-specific queue
            messagingTemplate.convertAndSendToUser(
                username,
                "/queue/errors",
                errorDetails
            );
            log.debug("Sent error to user queue: /user/{}/queue/errors", username);

        } catch (Exception e) {
            log.error("Failed to send error message to user: {}", username, e);
        }
    }
}
