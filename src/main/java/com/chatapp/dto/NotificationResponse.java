package com.chatapp.dto;

import com.chatapp.model.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for notification responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private Long id;
    private String title;
    private String content;
    private String data;
    private Notification.NotificationType notificationType;
    private Notification.Priority priority;
    private boolean isRead;
    private boolean isDelivered;
    private LocalDateTime createdAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime readAt;
    private LocalDateTime expiresAt;

    // Related entities (simplified)
    private Long relatedMessageId;
    private Long relatedChatRoomId;
    private String relatedChatRoomName;
    private Long triggeredByUserId;
    private String triggeredByUsername;
    private String triggeredByFullName;

    /**
     * Create a NotificationResponse from a Notification entity
     */
    public static NotificationResponse fromEntity(Notification notification) {
        NotificationResponseBuilder builder = NotificationResponse.builder()
            .id(notification.getId())
            .title(notification.getTitle())
            .content(notification.getContent())
            .data(notification.getData())
            .notificationType(notification.getNotificationType())
            .priority(notification.getPriority())
            .isRead(notification.isRead())
            .isDelivered(notification.isDelivered())
            .createdAt(notification.getCreatedAt())
            .deliveredAt(notification.getDeliveredAt())
            .readAt(notification.getReadAt())
            .expiresAt(notification.getExpiresAt());

        // Add related message info if present
        if (notification.getRelatedMessage() != null) {
            builder.relatedMessageId(notification.getRelatedMessage().getId());
        }

        // Add related chat room info if present
        if (notification.getRelatedChatRoom() != null) {
            builder.relatedChatRoomId(notification.getRelatedChatRoom().getId())
                   .relatedChatRoomName(notification.getRelatedChatRoom().getName());
        }

        // Add triggered by user info if present
        if (notification.getTriggeredByUser() != null) {
            builder.triggeredByUserId(notification.getTriggeredByUser().getId())
                   .triggeredByUsername(notification.getTriggeredByUser().getUsername())
                   .triggeredByFullName(notification.getTriggeredByUser().getFullName());
        }

        return builder.build();
    }
}
