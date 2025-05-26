package com.chatapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for real-time unread message notifications sent to users who are not actively in a chat room
 * This is different from UnreadCountResponse which is for count updates
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnreadMessageNotification {
    
    /**
     * The message ID
     */
    private Long messageId;
    
    /**
     * The chat room ID where the message was sent
     */
    private Long chatRoomId;
    
    /**
     * The chat room name
     */
    private String chatRoomName;
    
    /**
     * The sender's user ID
     */
    private Long senderId;
    
    /**
     * The sender's username
     */
    private String senderUsername;
    
    /**
     * The sender's full name (if available)
     */
    private String senderFullName;
    
    /**
     * Content preview of the message (truncated)
     */
    private String contentPreview;
    
    /**
     * Type of content (TEXT, IMAGE, FILE, etc.)
     */
    private String contentType;
    
    /**
     * Timestamp when the message was sent
     */
    private LocalDateTime sentAt;
    
    /**
     * Timestamp when this notification was generated
     */
    private LocalDateTime notificationTimestamp;
    
    /**
     * Current unread count for this chat room
     */
    private int unreadCount;
    
    /**
     * Total unread messages across all chat rooms for the recipient
     */
    private int totalUnreadCount;
    
    /**
     * The recipient user ID (who should receive this notification)
     */
    private Long recipientUserId;
    
    /**
     * Whether this is a private chat or group chat
     */
    private boolean isPrivateChat;
    
    /**
     * Number of participants in the chat room
     */
    private int participantCount;
    
    /**
     * Attachment URL if the message contains a file
     */
    private String attachmentUrl;
    
    /**
     * Type of notification
     */
    private NotificationType notificationType;
    
    public enum NotificationType {
        NEW_MESSAGE,        // New message received while user is away from room
        MENTION,           // User was mentioned in a message (future enhancement)
        PRIVATE_MESSAGE,   // New message in a private chat
        GROUP_MESSAGE      // New message in a group chat
    }
}
