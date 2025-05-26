package com.chatapp.websocket;

import com.chatapp.dto.NotificationResponse;
import com.chatapp.model.User;
import com.chatapp.service.NotificationService;
import com.chatapp.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * WebSocket controller for real-time notification management
 */
@Controller("webSocketNotificationController")
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Handle request to get unread notifications via WebSocket
     */
    @MessageMapping("/notifications.getUnread")
    public void getUnreadNotifications(Principal principal) {
        if (principal == null) {
            log.error("WEBSOCKET_NOTIFICATION: Unauthorized access - authentication required");
            return;
        }

        try {
            User user = userService.getUserByUsername(principal.getName());
            List<NotificationResponse> unreadNotifications = notificationService.getUnreadNotificationsForUser(user);

            // Send unread notifications to the user
            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/notifications/unread",
                unreadNotifications
            );

            log.debug("WEBSOCKET_NOTIFICATION: Sent {} unread notifications to user {}",
                unreadNotifications.size(), principal.getName());

        } catch (Exception e) {
            log.error("WEBSOCKET_NOTIFICATION: Failed to get unread notifications for user {}",
                principal.getName(), e);

            sendErrorToUser(principal.getName(), "Failed to retrieve unread notifications");
        }
    }

    /**
     * Handle request to mark notification as read via WebSocket
     */
    @MessageMapping("/notifications.markAsRead")
    public void markNotificationAsRead(@Payload NotificationMarkAsReadRequest request, Principal principal) {
        if (principal == null) {
            log.error("WEBSOCKET_NOTIFICATION: Unauthorized access - authentication required");
            return;
        }

        if (request.getNotificationId() == null) {
            log.error("WEBSOCKET_NOTIFICATION: Notification ID is required");
            sendErrorToUser(principal.getName(), "Notification ID is required");
            return;
        }

        try {
            User user = userService.getUserByUsername(principal.getName());
            boolean success = notificationService.markNotificationAsRead(request.getNotificationId(), user);

            if (success) {
                // Send confirmation to the user
                messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/notifications/read-confirmation",
                    Map.of(
                        "notificationId", request.getNotificationId(),
                        "status", "read"
                    )
                );

                // Send updated unread count
                long unreadCount = notificationService.getUnreadNotificationCount(user);
                messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/notifications/unread-count",
                    Map.of("unreadCount", unreadCount)
                );

                log.debug("WEBSOCKET_NOTIFICATION: Marked notification {} as read for user {}",
                    request.getNotificationId(), principal.getName());
            } else {
                sendErrorToUser(principal.getName(), "Notification not found or already read");
            }

        } catch (Exception e) {
            log.error("WEBSOCKET_NOTIFICATION: Failed to mark notification as read for user {}",
                principal.getName(), e);

            sendErrorToUser(principal.getName(), "Failed to mark notification as read");
        }
    }

    /**
     * Handle request to mark all notifications as read via WebSocket
     */
    @MessageMapping("/notifications.markAllAsRead")
    public void markAllNotificationsAsRead(Principal principal) {
        if (principal == null) {
            log.error("WEBSOCKET_NOTIFICATION: Unauthorized access - authentication required");
            return;
        }

        try {
            User user = userService.getUserByUsername(principal.getName());
            int updatedCount = notificationService.markAllNotificationsAsRead(user);

            // Send confirmation to the user
            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/notifications/read-all-confirmation",
                Map.of(
                    "updatedCount", updatedCount,
                    "status", "all_read"
                )
            );

            // Send updated unread count (should be 0)
            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/notifications/unread-count",
                Map.of("unreadCount", 0L)
            );

            log.info("WEBSOCKET_NOTIFICATION: Marked {} notifications as read for user {}",
                updatedCount, principal.getName());

        } catch (Exception e) {
            log.error("WEBSOCKET_NOTIFICATION: Failed to mark all notifications as read for user {}",
                principal.getName(), e);

            sendErrorToUser(principal.getName(), "Failed to mark all notifications as read");
        }
    }

    /**
     * Handle request to get unread notification count via WebSocket
     */
    @MessageMapping("/notifications.getUnreadCount")
    public void getUnreadNotificationCount(Principal principal) {
        if (principal == null) {
            log.error("WEBSOCKET_NOTIFICATION: Unauthorized access - authentication required");
            return;
        }

        try {
            User user = userService.getUserByUsername(principal.getName());
            long unreadCount = notificationService.getUnreadNotificationCount(user);

            // Send unread count to the user
            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/notifications/unread-count",
                Map.of("unreadCount", unreadCount)
            );

            log.debug("WEBSOCKET_NOTIFICATION: Sent unread count {} to user {}",
                unreadCount, principal.getName());

        } catch (Exception e) {
            log.error("WEBSOCKET_NOTIFICATION: Failed to get unread count for user {}",
                principal.getName(), e);

            sendErrorToUser(principal.getName(), "Failed to retrieve unread count");
        }
    }

    /**
     * Send error message to user
     */
    private void sendErrorToUser(String username, String errorMessage) {
        try {
            messagingTemplate.convertAndSendToUser(
                username,
                "/notifications/error",
                Map.of(
                    "error", errorMessage,
                    "timestamp", System.currentTimeMillis()
                )
            );
        } catch (Exception e) {
            log.error("WEBSOCKET_NOTIFICATION: Failed to send error message to user {}", username, e);
        }
    }

    /**
     * Request DTO for marking notification as read
     */
    public static class NotificationMarkAsReadRequest {
        private Long notificationId;

        public Long getNotificationId() {
            return notificationId;
        }

        public void setNotificationId(Long notificationId) {
            this.notificationId = notificationId;
        }
    }
}
