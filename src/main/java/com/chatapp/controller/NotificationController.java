package com.chatapp.controller;

import com.chatapp.dto.NotificationPreferencesRequest;
import com.chatapp.dto.NotificationPreferencesResponse;
import com.chatapp.dto.NotificationResponse;
import com.chatapp.model.User;
import com.chatapp.service.NotificationPreferencesService;
import com.chatapp.service.NotificationService;
import com.chatapp.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * REST controller for managing notifications and notification preferences
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notifications", description = "Notification management endpoints")
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationPreferencesService preferencesService;
    private final UserService userService;

    @Operation(summary = "Get user notifications", description = "Retrieve paginated notifications for the authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Notifications retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            Principal principal) {

        // Validate pagination parameters
        if (page < 0) {
            page = 0;
        }
        if (size <= 0 || size > 100) {
            size = 20; // Default to reasonable size
        }

        User user = userService.getUserByUsername(principal.getName());
        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationResponse> notifications = notificationService.getNotificationsForUser(user, pageable);

        return ResponseEntity.ok(notifications);
    }

    @Operation(summary = "Get unread notifications", description = "Retrieve all unread notifications for the authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Unread notifications retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/unread")
    public ResponseEntity<List<NotificationResponse>> getUnreadNotifications(Principal principal) {
        User user = userService.getUserByUsername(principal.getName());
        List<NotificationResponse> notifications = notificationService.getUnreadNotificationsForUser(user);

        return ResponseEntity.ok(notifications);
    }

    @Operation(summary = "Get unread notification count", description = "Get the count of unread notifications for the authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Unread count retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/unread/count")
    public ResponseEntity<Map<String, Long>> getUnreadNotificationCount(Principal principal) {
        User user = userService.getUserByUsername(principal.getName());
        long count = notificationService.getUnreadNotificationCount(user);

        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @Operation(summary = "Mark notification as read", description = "Mark a specific notification as read")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Notification marked as read successfully"),
        @ApiResponse(responseCode = "404", description = "Notification not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Map<String, String>> markNotificationAsRead(
            @Parameter(description = "Notification ID") @PathVariable Long notificationId,
            Principal principal) {

        User user = userService.getUserByUsername(principal.getName());
        boolean success = notificationService.markNotificationAsRead(notificationId, user);

        if (success) {
            return ResponseEntity.ok(Map.of("message", "Notification marked as read"));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Mark all notifications as read", description = "Mark all notifications as read for the authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "All notifications marked as read successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PutMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllNotificationsAsRead(Principal principal) {
        User user = userService.getUserByUsername(principal.getName());
        int updatedCount = notificationService.markAllNotificationsAsRead(user);

        return ResponseEntity.ok(Map.of(
            "message", "All notifications marked as read",
            "updatedCount", updatedCount
        ));
    }

    @Operation(summary = "Get notification preferences", description = "Retrieve notification preferences for the authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Preferences retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/preferences")
    public ResponseEntity<NotificationPreferencesResponse> getNotificationPreferences(Principal principal) {
        User user = userService.getUserByUsername(principal.getName());
        NotificationPreferencesResponse preferences = preferencesService.getPreferencesForUser(user);

        return ResponseEntity.ok(preferences);
    }

    @Operation(summary = "Update notification preferences", description = "Update notification preferences for the authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Preferences updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PutMapping("/preferences")
    public ResponseEntity<NotificationPreferencesResponse> updateNotificationPreferences(
            @Parameter(description = "Notification preferences update request") @Valid @RequestBody NotificationPreferencesRequest request,
            Principal principal) {

        User user = userService.getUserByUsername(principal.getName());
        NotificationPreferencesResponse preferences = preferencesService.updatePreferencesForUser(user, request);

        return ResponseEntity.ok(preferences);
    }

    @Operation(summary = "Reset notification preferences", description = "Reset notification preferences to default values")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Preferences reset successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/preferences/reset")
    public ResponseEntity<NotificationPreferencesResponse> resetNotificationPreferences(Principal principal) {
        User user = userService.getUserByUsername(principal.getName());
        NotificationPreferencesResponse preferences = preferencesService.resetToDefault(user);

        return ResponseEntity.ok(preferences);
    }
}
