package com.chatapp.model;

import lombok.*;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing a push notification that can be persisted for offline users
 */
@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notification_recipient", columnList = "recipient_id"),
    @Index(name = "idx_notification_type", columnList = "notification_type"),
    @Index(name = "idx_notification_read", columnList = "is_read"),
    @Index(name = "idx_notification_created", columnList = "created_at"),
    @Index(name = "idx_notification_delivered", columnList = "is_delivered")
})
@Getter
@Setter
@ToString(exclude = {"recipient", "relatedMessage", "relatedChatRoom"})
@EqualsAndHashCode(of = {"id"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user who should receive this notification
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    /**
     * Type of notification
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType notificationType;

    /**
     * Title of the notification
     */
    @Column(nullable = false, length = 255)
    private String title;

    /**
     * Content/body of the notification
     */
    @Column(columnDefinition = "TEXT")
    private String content;

    /**
     * Additional data as JSON string for extensibility
     */
    @Column(name = "data", columnDefinition = "TEXT")
    private String data;

    /**
     * Priority level of the notification
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Priority priority = Priority.NORMAL;

    /**
     * Whether the notification has been read by the user
     */
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean isRead = false;

    /**
     * Whether the notification has been delivered via WebSocket
     */
    @Column(name = "is_delivered", nullable = false)
    @Builder.Default
    private boolean isDelivered = false;

    /**
     * When the notification was created
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * When the notification was delivered
     */
    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    /**
     * When the notification was read
     */
    @Column(name = "read_at")
    private LocalDateTime readAt;

    /**
     * When the notification expires (optional)
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /**
     * Related message (if this notification is about a message)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_message_id")
    private Message relatedMessage;

    /**
     * Related chat room (if this notification is about a chat room)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_chatroom_id")
    private ChatRoom relatedChatRoom;

    /**
     * User who triggered this notification (e.g., message sender)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "triggered_by_user_id")
    private User triggeredByUser;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /**
     * Mark notification as delivered
     */
    public void markAsDelivered() {
        this.isDelivered = true;
        this.deliveredAt = LocalDateTime.now();
    }

    /**
     * Mark notification as read
     */
    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }

    /**
     * Check if notification is expired
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Types of notifications
     */
    public enum NotificationType {
        NEW_MESSAGE,           // New message in a chat room
        PRIVATE_MESSAGE,       // New private message
        GROUP_MESSAGE,         // New group message
        MENTION,              // User was mentioned
        CHAT_ROOM_INVITE,     // Invited to a chat room
        CHAT_ROOM_ADDED,      // Added to a chat room
        CHAT_ROOM_REMOVED,    // Removed from a chat room
        USER_JOINED,          // User joined a chat room
        USER_LEFT,            // User left a chat room
        FILE_SHARED,          // File was shared
        SYSTEM_ANNOUNCEMENT,  // System-wide announcement
        FRIEND_REQUEST,       // Friend request (future feature)
        FRIEND_ACCEPTED       // Friend request accepted (future feature)
    }

    /**
     * Priority levels for notifications
     */
    public enum Priority {
        LOW,      // Non-urgent notifications
        NORMAL,   // Standard notifications
        HIGH,     // Important notifications
        URGENT    // Critical notifications that should be delivered immediately
    }
}
