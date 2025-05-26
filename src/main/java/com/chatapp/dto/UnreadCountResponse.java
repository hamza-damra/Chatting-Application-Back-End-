package com.chatapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for real-time unread message count updates
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnreadCountResponse {
    
    /**
     * The chat room ID
     */
    private Long chatRoomId;
    
    /**
     * The chat room name
     */
    private String chatRoomName;
    
    /**
     * Current unread message count for this chat room
     */
    private int unreadCount;
    
    /**
     * Total unread messages across all chat rooms for the user
     */
    private int totalUnreadCount;
    
    /**
     * The ID of the latest unread message (if any)
     */
    private Long latestMessageId;
    
    /**
     * Content of the latest unread message (preview)
     */
    private String latestMessageContent;
    
    /**
     * Sender of the latest unread message
     */
    private String latestMessageSender;
    
    /**
     * Timestamp when this update was generated
     */
    private LocalDateTime timestamp;
    
    /**
     * Type of update (NEW_MESSAGE, MESSAGE_READ, BULK_READ)
     */
    private UpdateType updateType;
    
    /**
     * The user ID this update is for
     */
    private Long userId;
    
    public enum UpdateType {
        NEW_MESSAGE,    // A new message was received
        MESSAGE_READ,   // A message was marked as read
        BULK_READ,      // Multiple messages were marked as read
        ROOM_OPENED,    // User opened a chat room
        INITIAL_COUNT   // Initial unread count when user connects
    }
}
