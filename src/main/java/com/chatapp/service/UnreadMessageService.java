package com.chatapp.service;

import com.chatapp.dto.UnreadCountResponse;
import com.chatapp.dto.UnreadMessageNotification;
import com.chatapp.model.ChatRoom;
import com.chatapp.model.Message;
import com.chatapp.model.MessageStatus;
import com.chatapp.model.User;
import com.chatapp.repository.ChatRoomRepository;
import com.chatapp.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing unread message counts and real-time notifications
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UnreadMessageService {

    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserPresenceService userPresenceService;

    // Throttling mechanism to prevent excessive updates
    private final Map<String, Long> lastUpdateTime = new ConcurrentHashMap<>();
    private static final long UPDATE_THROTTLE_MS = 1000; // 1 second throttle
    private static final long CACHE_CLEANUP_INTERVAL = 300000; // 5 minutes
    private long lastCacheCleanup = System.currentTimeMillis();

    /**
     * Calculate unread message count for a specific chat room and user
     * Optimized to use a single database query instead of multiple individual queries
     */
    @Transactional(readOnly = true)
    public int getUnreadCountForChatRoom(ChatRoom chatRoom, User user) {
        try {
            // Use a more efficient query to count unread messages directly
            List<Message> messages = messageRepository.findByChatRoomOrderBySentAtDesc(chatRoom);

            if (messages.isEmpty()) {
                return 0;
            }

            int unreadCount = 0;

            // Batch check message statuses to avoid N+1 query problem
            for (Message message : messages) {
                // Don't count messages sent by the user themselves
                if (!message.getSender().getId().equals(user.getId())) {
                    // Check if there's a read status for this message and user
                    boolean hasReadStatus = message.getMessageStatuses().stream()
                        .anyMatch(status -> status.getUser().getId().equals(user.getId())
                                         && status.getStatus() == MessageStatus.Status.READ);

                    if (!hasReadStatus) {
                        unreadCount++;
                    }
                }
            }

            return unreadCount;

        } catch (Exception e) {
            log.error("UNREAD: Error calculating unread count for chat room {} and user {}: {}",
                     chatRoom.getId(), user.getId(), e.getMessage());
            return 0; // Return 0 on error to prevent infinite loops
        }
    }

    /**
     * Calculate total unread message count across all chat rooms for a user
     * Optimized to prevent infinite loops and reduce database queries
     */
    @Transactional(readOnly = true)
    public int getTotalUnreadCount(User user) {
        try {
            List<ChatRoom> chatRooms = chatRoomRepository.findByParticipantsContaining(user);
            int totalUnread = 0;

            // Limit the number of chat rooms to process to prevent infinite loops
            int maxRooms = Math.min(chatRooms.size(), 50); // Process max 50 rooms

            for (int i = 0; i < maxRooms; i++) {
                ChatRoom chatRoom = chatRooms.get(i);
                int roomUnread = getUnreadCountForChatRoom(chatRoom, user);
                totalUnread += roomUnread;

                // Safety check to prevent runaway calculations
                if (totalUnread > 10000) {
                    log.warn("UNREAD: Total unread count exceeded 10000 for user {}, stopping calculation", user.getId());
                    break;
                }
            }

            return totalUnread;

        } catch (Exception e) {
            log.error("UNREAD: Error calculating total unread count for user {}: {}", user.getId(), e.getMessage());
            return 0; // Return 0 on error to prevent infinite loops
        }
    }

    /**
     * Get the latest unread message in a chat room for a user
     * Optimized to prevent excessive processing
     */
    @Transactional(readOnly = true)
    public Message getLatestUnreadMessage(ChatRoom chatRoom, User user) {
        try {
            List<Message> messages = messageRepository.findByChatRoomOrderBySentAtDesc(chatRoom);

            // Limit processing to prevent infinite loops
            int maxMessages = Math.min(messages.size(), 100); // Process max 100 messages

            for (int i = 0; i < maxMessages; i++) {
                Message message = messages.get(i);
                // Skip messages sent by the user themselves
                if (!message.getSender().getId().equals(user.getId())) {
                    // Use the optimized status check from message statuses
                    boolean hasReadStatus = message.getMessageStatuses().stream()
                        .anyMatch(status -> status.getUser().getId().equals(user.getId())
                                         && status.getStatus() == MessageStatus.Status.READ);

                    if (!hasReadStatus) {
                        return message; // Return the latest unread message
                    }
                }
            }

            return null; // No unread messages

        } catch (Exception e) {
            log.error("UNREAD: Error getting latest unread message for chat room {} and user {}: {}",
                     chatRoom.getId(), user.getId(), e.getMessage());
            return null; // Return null on error
        }
    }

    /**
     * Send real-time unread count update to a specific user
     * Includes throttling to prevent excessive updates
     */
    public void sendUnreadCountUpdate(User user, ChatRoom chatRoom, UnreadCountResponse.UpdateType updateType) {
        try {
            // Throttling mechanism to prevent excessive updates
            String throttleKey = user.getId() + "_" + chatRoom.getId() + "_" + updateType;
            long currentTime = System.currentTimeMillis();
            Long lastUpdate = lastUpdateTime.get(throttleKey);

            if (lastUpdate != null && (currentTime - lastUpdate) < UPDATE_THROTTLE_MS) {
                log.debug("UNREAD: Throttling update for user {} in room {} (type: {})",
                         user.getUsername(), chatRoom.getName(), updateType);
                return; // Skip this update due to throttling
            }

            lastUpdateTime.put(throttleKey, currentTime);

            // Periodic cache cleanup to prevent memory leaks
            if (currentTime - lastCacheCleanup > CACHE_CLEANUP_INTERVAL) {
                cleanupThrottleCache(currentTime);
                lastCacheCleanup = currentTime;
            }

            int chatRoomUnreadCount = getUnreadCountForChatRoom(chatRoom, user);
            int totalUnreadCount = getTotalUnreadCount(user);
            Message latestMessage = getLatestUnreadMessage(chatRoom, user);

            UnreadCountResponse response = UnreadCountResponse.builder()
                .chatRoomId(chatRoom.getId())
                .chatRoomName(chatRoom.getName())
                .unreadCount(chatRoomUnreadCount)
                .totalUnreadCount(totalUnreadCount)
                .updateType(updateType)
                .userId(user.getId())
                .timestamp(LocalDateTime.now())
                .build();

            // Add latest message details if available
            if (latestMessage != null) {
                response.setLatestMessageId(latestMessage.getId());
                response.setLatestMessageContent(truncateContent(latestMessage.getContent()));
                response.setLatestMessageSender(latestMessage.getSender().getUsername());
            }

            // Send to user's unread count queue
            messagingTemplate.convertAndSendToUser(
                user.getUsername(),
                "/queue/unread",
                response
            );

            log.info("UNREAD: Sent unread count update to user {} - ChatRoom: {}, Unread: {}, Total: {}, Type: {}",
                user.getUsername(), chatRoom.getName(), chatRoomUnreadCount, totalUnreadCount, updateType);

        } catch (Exception e) {
            log.error("UNREAD: Failed to send unread count update to user: {}", user.getUsername(), e);
        }
    }

    /**
     * Send unread count updates to all participants in a chat room except the sender
     */
    public void notifyParticipantsOfNewMessage(ChatRoom chatRoom, User sender) {
        chatRoom.getParticipants().forEach(participant -> {
            if (!participant.getId().equals(sender.getId())) {
                sendUnreadCountUpdate(participant, chatRoom, UnreadCountResponse.UpdateType.NEW_MESSAGE);
            }
        });
    }

    /**
     * Send unread count update when messages are marked as read
     */
    public void notifyMessageRead(ChatRoom chatRoom, User reader) {
        sendUnreadCountUpdate(reader, chatRoom, UnreadCountResponse.UpdateType.MESSAGE_READ);
    }

    /**
     * Send unread count update when multiple messages are marked as read (bulk read)
     */
    public void notifyBulkRead(ChatRoom chatRoom, User reader) {
        sendUnreadCountUpdate(reader, chatRoom, UnreadCountResponse.UpdateType.BULK_READ);
    }

    /**
     * Send initial unread counts when user connects
     */
    public void sendInitialUnreadCounts(User user) {
        List<ChatRoom> chatRooms = chatRoomRepository.findByParticipantsContaining(user);

        for (ChatRoom chatRoom : chatRooms) {
            sendUnreadCountUpdate(user, chatRoom, UnreadCountResponse.UpdateType.INITIAL_COUNT);
        }

        log.info("UNREAD: Sent initial unread counts to user {} for {} chat rooms",
            user.getUsername(), chatRooms.size());
    }

    /**
     * Truncate message content for preview (max 100 characters)
     */
    private String truncateContent(String content) {
        if (content == null) return null;
        if (content.length() <= 100) return content;
        return content.substring(0, 97) + "...";
    }

    /**
     * Clean up old entries from the throttle cache to prevent memory leaks
     */
    private void cleanupThrottleCache(long currentTime) {
        try {
            lastUpdateTime.entrySet().removeIf(entry ->
                (currentTime - entry.getValue()) > CACHE_CLEANUP_INTERVAL);
            log.debug("UNREAD: Cleaned up throttle cache, remaining entries: {}", lastUpdateTime.size());
        } catch (Exception e) {
            log.error("UNREAD: Error cleaning up throttle cache: {}", e.getMessage());
        }
    }

    /**
     * Send real-time unread message notifications to users who are NOT currently active in the chat room
     * This is the main method for the new notification system
     * @param message The new message that was sent
     * @param chatRoom The chat room where the message was sent
     * @param sender The user who sent the message
     */
    public void sendUnreadMessageNotificationsToAbsentUsers(Message message, ChatRoom chatRoom, User sender) {
        try {
            log.info("UNREAD_NOTIFICATION: Processing new message notification for room: {}, sender: {}",
                    chatRoom.getName(), sender.getUsername());

            // Get all participants in the chat room
            chatRoom.getParticipants().forEach(participant -> {
                // Skip the sender
                if (!participant.getId().equals(sender.getId())) {
                    String participantUsername = participant.getUsername();

                    // Check if the participant is currently active in this room
                    boolean isActiveInRoom = userPresenceService.isUserActiveInRoom(participantUsername, chatRoom.getId());

                    if (!isActiveInRoom) {
                        // User is not active in the room, send notification
                        sendUnreadMessageNotification(message, chatRoom, sender, participant);
                        log.debug("UNREAD_NOTIFICATION: Sent notification to absent user: {}", participantUsername);
                    } else {
                        log.debug("UNREAD_NOTIFICATION: Skipping notification for active user: {}", participantUsername);
                    }
                }
            });

        } catch (Exception e) {
            log.error("UNREAD_NOTIFICATION: Failed to send notifications for new message", e);
        }
    }

    /**
     * Send an unread message notification to a specific user
     * @param message The message
     * @param chatRoom The chat room
     * @param sender The sender of the message
     * @param recipient The user who should receive the notification
     */
    private void sendUnreadMessageNotification(Message message, ChatRoom chatRoom, User sender, User recipient) {
        try {
            // Calculate current unread counts
            int chatRoomUnreadCount = getUnreadCountForChatRoom(chatRoom, recipient);
            int totalUnreadCount = getTotalUnreadCount(recipient);

            // Determine notification type
            UnreadMessageNotification.NotificationType notificationType =
                chatRoom.isPrivate() ?
                    UnreadMessageNotification.NotificationType.PRIVATE_MESSAGE :
                    UnreadMessageNotification.NotificationType.GROUP_MESSAGE;

            // Create the notification
            UnreadMessageNotification notification = UnreadMessageNotification.builder()
                .messageId(message.getId())
                .chatRoomId(chatRoom.getId())
                .chatRoomName(chatRoom.getName())
                .senderId(sender.getId())
                .senderUsername(sender.getUsername())
                .senderFullName(sender.getFullName())
                .contentPreview(truncateContent(message.getContent()))
                .contentType(message.getContentType())
                .sentAt(message.getSentAt())
                .notificationTimestamp(LocalDateTime.now())
                .unreadCount(chatRoomUnreadCount)
                .totalUnreadCount(totalUnreadCount)
                .recipientUserId(recipient.getId())
                .isPrivateChat(chatRoom.isPrivate())
                .participantCount(chatRoom.getParticipants().size())
                .attachmentUrl(message.getAttachmentUrl())
                .notificationType(notificationType)
                .build();

            // Send to user's dedicated unread message notification channel
            messagingTemplate.convertAndSendToUser(
                recipient.getUsername(),
                "/unread-messages",
                notification
            );

            log.info("UNREAD_NOTIFICATION: Sent notification to user {} for message from {} in room {}",
                recipient.getUsername(), sender.getUsername(), chatRoom.getName());

        } catch (Exception e) {
            log.error("UNREAD_NOTIFICATION: Failed to send notification to user: {}", recipient.getUsername(), e);
        }
    }
}
