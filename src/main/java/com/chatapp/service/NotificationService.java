package com.chatapp.service;

import com.chatapp.dto.NotificationResponse;
import com.chatapp.model.*;
import com.chatapp.repository.NotificationRepository;
import com.chatapp.repository.NotificationPreferencesRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing push notifications
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferencesRepository preferencesRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserPresenceService userPresenceService;
    private final ObjectMapper objectMapper;

    /**
     * Create and send a notification
     */
    public Notification createAndSendNotification(
            User recipient,
            Notification.NotificationType type,
            String title,
            String content,
            Map<String, Object> data,
            Notification.Priority priority,
            Message relatedMessage,
            ChatRoom relatedChatRoom,
            User triggeredByUser) {

        // Check if user has this type of notification enabled
        if (!isNotificationEnabled(recipient, type)) {
            log.debug("NOTIFICATION: Notification type {} disabled for user {}", type, recipient.getUsername());
            return null;
        }

        // Create notification entity
        Notification notification = Notification.builder()
            .recipient(recipient)
            .notificationType(type)
            .title(title)
            .content(content)
            .data(serializeData(data))
            .priority(priority)
            .relatedMessage(relatedMessage)
            .relatedChatRoom(relatedChatRoom)
            .triggeredByUser(triggeredByUser)
            .build();

        // Save to database
        notification = notificationRepository.save(notification);
        log.info("NOTIFICATION: Created notification {} for user {}", notification.getId(), recipient.getUsername());

        // Try to deliver immediately via WebSocket
        boolean delivered = deliverNotificationViaWebSocket(notification);

        if (delivered) {
            notification.markAsDelivered();
            notificationRepository.save(notification);
        }

        return notification;
    }

    /**
     * Send a message notification (integrates with existing UnreadMessageService)
     */
    public void sendMessageNotification(Message message, ChatRoom chatRoom, User sender) {
        chatRoom.getParticipants().forEach(participant -> {
            if (!participant.getId().equals(sender.getId())) {
                // Check if user is active in the room
                boolean isActiveInRoom = userPresenceService.isUserActiveInRoom(
                    participant.getUsername(),
                    chatRoom.getId()
                );

                if (!isActiveInRoom) {
                    // Determine notification type
                    Notification.NotificationType type = chatRoom.isPrivate()
                        ? Notification.NotificationType.PRIVATE_MESSAGE
                        : Notification.NotificationType.GROUP_MESSAGE;

                    // Create title and content
                    String title = chatRoom.isPrivate()
                        ? "New message from " + sender.getUsername()
                        : "New message in " + chatRoom.getName();

                    String content = truncateContent(message.getContent());

                    // Create data payload
                    Map<String, Object> data = Map.of(
                        "messageId", message.getId(),
                        "chatRoomId", chatRoom.getId(),
                        "senderId", sender.getId(),
                        "contentType", message.getContentType(),
                        "hasAttachment", message.getAttachmentUrl() != null
                    );

                    createAndSendNotification(
                        participant,
                        type,
                        title,
                        content,
                        data,
                        Notification.Priority.NORMAL,
                        message,
                        chatRoom,
                        sender
                    );
                }
            }
        });
    }

    /**
     * Deliver notification via WebSocket
     */
    private boolean deliverNotificationViaWebSocket(Notification notification) {
        try {
            NotificationResponse response = NotificationResponse.fromEntity(notification);

            messagingTemplate.convertAndSendToUser(
                notification.getRecipient().getUsername(),
                "/notifications",
                response
            );

            log.debug("NOTIFICATION: Delivered notification {} via WebSocket to user {}",
                notification.getId(), notification.getRecipient().getUsername());
            return true;
        } catch (Exception e) {
            log.error("NOTIFICATION: Failed to deliver notification {} via WebSocket",
                notification.getId(), e);
            return false;
        }
    }

    /**
     * Get notifications for a user
     */
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotificationsForUser(User user, Pageable pageable) {
        Page<Notification> notifications = notificationRepository.findByRecipientOrderByCreatedAtDesc(user, pageable);
        return notifications.map(NotificationResponse::fromEntity);
    }

    /**
     * Get unread notifications for a user
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnreadNotificationsForUser(User user) {
        List<Notification> notifications = notificationRepository.findByRecipientAndIsReadFalseOrderByCreatedAtDesc(user);
        return notifications.stream()
            .map(NotificationResponse::fromEntity)
            .toList();
    }

    /**
     * Mark notification as read
     */
    public boolean markNotificationAsRead(Long notificationId, User user) {
        int updated = notificationRepository.markAsRead(notificationId, user, LocalDateTime.now());
        if (updated > 0) {
            log.debug("NOTIFICATION: Marked notification {} as read for user {}", notificationId, user.getUsername());
            return true;
        }
        return false;
    }

    /**
     * Mark all notifications as read for a user
     */
    public int markAllNotificationsAsRead(User user) {
        int updated = notificationRepository.markAllAsReadForUser(user, LocalDateTime.now());
        log.info("NOTIFICATION: Marked {} notifications as read for user {}", updated, user.getUsername());
        return updated;
    }

    /**
     * Get unread notification count for a user
     */
    @Transactional(readOnly = true)
    public long getUnreadNotificationCount(User user) {
        return notificationRepository.countByRecipientAndIsReadFalse(user);
    }

    /**
     * Check if a notification type is enabled for a user
     */
    private boolean isNotificationEnabled(User user, Notification.NotificationType type) {
        Optional<NotificationPreferences> prefsOpt = preferencesRepository.findByUser(user);

        NotificationPreferences preferences;
        if (prefsOpt.isEmpty()) {
            // Create default preferences if they don't exist
            preferences = createDefaultPreferences(user);
        } else {
            preferences = prefsOpt.get();
        }

        return preferences.isNotificationTypeEnabled(type);
    }

    /**
     * Create default notification preferences for a user
     */
    public NotificationPreferences createDefaultPreferences(User user) {
        NotificationPreferences preferences = NotificationPreferences.builder()
            .user(user)
            .build(); // Uses @Builder.Default values

        return preferencesRepository.save(preferences);
    }

    /**
     * Serialize data map to JSON string
     */
    private String serializeData(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.error("NOTIFICATION: Failed to serialize notification data", e);
            return null;
        }
    }

    /**
     * Clean up old notifications for a user (keep only the most recent ones)
     */
    public int cleanupOldNotifications(User user, int maxNotifications) {
        try {
            long totalNotifications = notificationRepository.countByRecipient(user);

            if (totalNotifications <= maxNotifications) {
                return 0; // No cleanup needed
            }

            int notificationsToDelete = (int) (totalNotifications - maxNotifications);
            Pageable pageable = PageRequest.of(0, notificationsToDelete);
            List<Notification> oldNotifications = notificationRepository.findOldNotificationsForUser(user, pageable);

            notificationRepository.deleteAll(oldNotifications);

            log.info("NOTIFICATION: Cleaned up {} old notifications for user {}",
                oldNotifications.size(), user.getUsername());

            return oldNotifications.size();
        } catch (Exception e) {
            log.error("NOTIFICATION: Failed to cleanup old notifications for user {}", user.getUsername(), e);
            return 0;
        }
    }

    /**
     * Clean up expired notifications
     */
    public int cleanupExpiredNotifications() {
        try {
            List<Notification> expiredNotifications = notificationRepository.findExpiredNotifications(LocalDateTime.now());
            notificationRepository.deleteAll(expiredNotifications);

            log.info("NOTIFICATION: Cleaned up {} expired notifications", expiredNotifications.size());
            return expiredNotifications.size();
        } catch (Exception e) {
            log.error("NOTIFICATION: Failed to cleanup expired notifications", e);
            return 0;
        }
    }

    /**
     * Truncate content for notification preview
     */
    private String truncateContent(String content) {
        if (content == null) {
            return null;
        }

        final int maxLength = 100;
        if (content.length() <= maxLength) {
            return content;
        }

        return content.substring(0, maxLength) + "...";
    }
}
