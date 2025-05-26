package com.chatapp.controller;

import com.chatapp.model.*;
import com.chatapp.service.NotificationService;
import com.chatapp.service.UserService;
import com.chatapp.service.WebSocketMonitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Test controller for manually testing push notifications
 * This should be removed in production
 */
@RestController
@RequestMapping("/api/test/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notification Testing", description = "Endpoints for testing push notifications (Development only)")
public class NotificationTestController {

    private final NotificationService notificationService;
    private final UserService userService;
    private final WebSocketMonitorService monitorService;

    @Operation(summary = "Send test notification", description = "Send a test notification to the authenticated user")
    @PostMapping("/send-test")
    public ResponseEntity<Map<String, Object>> sendTestNotification(
            @Parameter(description = "Notification type") @RequestParam(defaultValue = "NEW_MESSAGE") String type,
            @Parameter(description = "Notification title") @RequestParam(defaultValue = "Test Notification") String title,
            @Parameter(description = "Notification content") @RequestParam(defaultValue = "This is a test notification") String content,
            @Parameter(description = "Priority level") @RequestParam(defaultValue = "NORMAL") String priority,
            Principal principal) {

        try {
            User user = userService.getUserByUsername(principal.getName());

            Notification.NotificationType notificationType;
            try {
                notificationType = Notification.NotificationType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                notificationType = Notification.NotificationType.NEW_MESSAGE;
            }

            Notification.Priority priorityLevel;
            try {
                priorityLevel = Notification.Priority.valueOf(priority.toUpperCase());
            } catch (IllegalArgumentException e) {
                priorityLevel = Notification.Priority.NORMAL;
            }

            Notification notification = notificationService.createAndSendNotification(
                user,
                notificationType,
                title,
                content,
                Map.of(
                    "testData", "This is test data",
                    "timestamp", System.currentTimeMillis()
                ),
                priorityLevel,
                null, // no related message
                null, // no related chat room
                user  // triggered by self for testing
            );

            if (notification != null) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Test notification sent successfully",
                    "notificationId", notification.getId(),
                    "type", notification.getNotificationType().toString(),
                    "priority", notification.getPriority().toString(),
                    "delivered", notification.isDelivered()
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Notification was not sent (likely disabled in user preferences)"
                ));
            }

        } catch (Exception e) {
            log.error("TEST: Failed to send test notification", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Failed to send test notification: " + e.getMessage()
            ));
        }
    }

    @Operation(summary = "Send test notification to another user", description = "Send a test notification to a specific user")
    @PostMapping("/send-to-user")
    public ResponseEntity<Map<String, Object>> sendTestNotificationToUser(
            @Parameter(description = "Target username") @RequestParam String targetUsername,
            @Parameter(description = "Notification type") @RequestParam(defaultValue = "NEW_MESSAGE") String type,
            @Parameter(description = "Notification title") @RequestParam(defaultValue = "Test Notification") String title,
            @Parameter(description = "Notification content") @RequestParam(defaultValue = "This is a test notification") String content,
            Principal principal) {

        try {
            User sender = userService.getUserByUsername(principal.getName());
            User recipient = userService.getUserByUsername(targetUsername);

            Notification.NotificationType notificationType;
            try {
                notificationType = Notification.NotificationType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                notificationType = Notification.NotificationType.NEW_MESSAGE;
            }

            Notification notification = notificationService.createAndSendNotification(
                recipient,
                notificationType,
                title,
                content,
                Map.of(
                    "senderUsername", sender.getUsername(),
                    "testData", "Cross-user test notification"
                ),
                Notification.Priority.NORMAL,
                null, // no related message
                null, // no related chat room
                sender
            );

            if (notification != null) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Test notification sent to " + targetUsername,
                    "notificationId", notification.getId(),
                    "recipient", targetUsername,
                    "delivered", notification.isDelivered()
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Notification was not sent to " + targetUsername + " (likely disabled in user preferences)"
                ));
            }

        } catch (Exception e) {
            log.error("TEST: Failed to send test notification to user {}", targetUsername, e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to send test notification: " + e.getMessage()
            ));
        }
    }

    @Operation(summary = "Test notification types", description = "Send multiple test notifications of different types")
    @PostMapping("/test-all-types")
    public ResponseEntity<Map<String, Object>> testAllNotificationTypes(Principal principal) {
        try {
            User user = userService.getUserByUsername(principal.getName());
            int successCount = 0;
            int totalCount = 0;

            // Test different notification types
            Notification.NotificationType[] types = {
                Notification.NotificationType.NEW_MESSAGE,
                Notification.NotificationType.PRIVATE_MESSAGE,
                Notification.NotificationType.GROUP_MESSAGE,
                Notification.NotificationType.SYSTEM_ANNOUNCEMENT,
                Notification.NotificationType.FILE_SHARED
            };

            for (Notification.NotificationType type : types) {
                totalCount++;
                try {
                    Notification notification = notificationService.createAndSendNotification(
                        user,
                        type,
                        "Test " + type.toString(),
                        "Testing notification type: " + type.toString(),
                        Map.of("type", type.toString()),
                        Notification.Priority.NORMAL,
                        null,
                        null,
                        user
                    );

                    if (notification != null) {
                        successCount++;
                    }
                } catch (Exception e) {
                    log.error("TEST: Failed to send notification of type {}", type, e);
                }
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Sent " + successCount + " out of " + totalCount + " test notifications",
                "successCount", successCount,
                "totalCount", totalCount
            ));

        } catch (Exception e) {
            log.error("TEST: Failed to test all notification types", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Failed to test notification types: " + e.getMessage()
            ));
        }
    }

    @Operation(summary = "Get notification stats", description = "Get notification statistics for the authenticated user")
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getNotificationStats(Principal principal) {
        try {
            User user = userService.getUserByUsername(principal.getName());

            long unreadCount = notificationService.getUnreadNotificationCount(user);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "username", user.getUsername(),
                "unreadCount", unreadCount,
                "message", "Notification stats retrieved successfully"
            ));

        } catch (Exception e) {
            log.error("TEST: Failed to get notification stats", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Failed to get notification stats: " + e.getMessage()
            ));
        }
    }

    @Operation(summary = "Backend diagnostic", description = "Run comprehensive backend notification system diagnostic")
    @GetMapping("/diagnostic")
    public ResponseEntity<Map<String, Object>> runBackendDiagnostic(Principal principal) {
        Map<String, Object> diagnostic = new LinkedHashMap<>();

        try {
            // Basic service availability
            diagnostic.put("notificationServiceAvailable", notificationService != null);
            diagnostic.put("userServiceAvailable", userService != null);

            // User authentication
            if (principal != null) {
                diagnostic.put("userAuthenticated", true);
                diagnostic.put("authenticatedUser", principal.getName());

                try {
                    User user = userService.getUserByUsername(principal.getName());
                    diagnostic.put("userFound", true);
                    diagnostic.put("userId", user.getId());

                    // Notification stats
                    long unreadCount = notificationService.getUnreadNotificationCount(user);
                    diagnostic.put("unreadNotificationCount", unreadCount);

                } catch (Exception e) {
                    diagnostic.put("userFound", false);
                    diagnostic.put("userError", e.getMessage());
                }
            } else {
                diagnostic.put("userAuthenticated", false);
            }

            // WebSocket endpoints
            diagnostic.put("webSocketEndpoint", "/ws");
            diagnostic.put("expectedSubscriptions", List.of(
                "/user/queue/unread",
                "/user/unread-messages",
                "/user/notifications",
                "/user/queue/notifications"
            ));

            // API endpoints
            diagnostic.put("restApiEndpoints", List.of(
                "GET /api/notifications",
                "GET /api/notifications/unread",
                "GET /api/notifications/unread/count",
                "PUT /api/notifications/{id}/read",
                "PUT /api/notifications/read-all",
                "GET /api/notifications/preferences",
                "PUT /api/notifications/preferences"
            ));

            // Test endpoints
            diagnostic.put("testEndpoints", List.of(
                "POST /api/test/notifications/send-test",
                "POST /api/test/notifications/send-to-user",
                "POST /api/test/notifications/test-all-types",
                "GET /api/test/notifications/stats",
                "GET /api/test/notifications/diagnostic"
            ));

            diagnostic.put("status", "SUCCESS");
            diagnostic.put("message", "Backend notification system is properly configured");

            return ResponseEntity.ok(diagnostic);

        } catch (Exception e) {
            diagnostic.put("status", "ERROR");
            diagnostic.put("message", "Diagnostic failed: " + e.getMessage());
            diagnostic.put("error", e.getClass().getSimpleName());

            log.error("TEST: Backend diagnostic failed", e);
            return ResponseEntity.internalServerError().body(diagnostic);
        }
    }

    @Operation(summary = "WebSocket statistics", description = "Get WebSocket connection and performance statistics")
    @GetMapping("/websocket-stats")
    public ResponseEntity<Map<String, Object>> getWebSocketStats() {
        try {
            Map<String, Object> stats = monitorService.getStatistics();
            stats.put("healthStatus", monitorService.getHealthStatus());
            stats.put("isHighLoad", monitorService.isHighLoad());
            stats.put("timestamp", java.time.LocalDateTime.now());

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("TEST: Failed to get WebSocket statistics", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Failed to get WebSocket statistics: " + e.getMessage()
            ));
        }
    }

    @Operation(summary = "Detailed WebSocket statistics", description = "Get detailed WebSocket statistics including user-specific data")
    @GetMapping("/websocket-stats/detailed")
    public ResponseEntity<Map<String, Object>> getDetailedWebSocketStats() {
        try {
            Map<String, Object> stats = monitorService.getDetailedStats();
            stats.put("healthStatus", monitorService.getHealthStatus());
            stats.put("isHighLoad", monitorService.isHighLoad());
            stats.put("timestamp", java.time.LocalDateTime.now());

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("TEST: Failed to get detailed WebSocket statistics", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Failed to get detailed WebSocket statistics: " + e.getMessage()
            ));
        }
    }
}
