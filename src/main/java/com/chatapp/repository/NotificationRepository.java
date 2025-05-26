package com.chatapp.repository;

import com.chatapp.model.Notification;
import com.chatapp.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Find all notifications for a specific user, ordered by creation date (newest first)
     */
    Page<Notification> findByRecipientOrderByCreatedAtDesc(User recipient, Pageable pageable);

    /**
     * Find unread notifications for a specific user
     */
    List<Notification> findByRecipientAndIsReadFalseOrderByCreatedAtDesc(User recipient);

    /**
     * Find undelivered notifications for a specific user
     */
    List<Notification> findByRecipientAndIsDeliveredFalseOrderByCreatedAtDesc(User recipient);

    /**
     * Find notifications by type for a specific user
     */
    Page<Notification> findByRecipientAndNotificationTypeOrderByCreatedAtDesc(
        User recipient,
        Notification.NotificationType notificationType,
        Pageable pageable
    );

    /**
     * Find notifications by priority for a specific user
     */
    List<Notification> findByRecipientAndPriorityOrderByCreatedAtDesc(
        User recipient,
        Notification.Priority priority
    );

    /**
     * Count unread notifications for a user
     */
    long countByRecipientAndIsReadFalse(User recipient);

    /**
     * Count undelivered notifications for a user
     */
    long countByRecipientAndIsDeliveredFalse(User recipient);

    /**
     * Count all notifications for a user
     */
    long countByRecipient(User recipient);

    /**
     * Find notifications that have expired
     */
    @Query("SELECT n FROM Notification n WHERE n.expiresAt IS NOT NULL AND n.expiresAt < :now")
    List<Notification> findExpiredNotifications(@Param("now") LocalDateTime now);

    /**
     * Mark all notifications as read for a user
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt WHERE n.recipient = :recipient AND n.isRead = false")
    int markAllAsReadForUser(@Param("recipient") User recipient, @Param("readAt") LocalDateTime readAt);

    /**
     * Mark all notifications as delivered for a user
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isDelivered = true, n.deliveredAt = :deliveredAt WHERE n.recipient = :recipient AND n.isDelivered = false")
    int markAllAsDeliveredForUser(@Param("recipient") User recipient, @Param("deliveredAt") LocalDateTime deliveredAt);

    /**
     * Mark specific notification as read
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt WHERE n.id = :notificationId AND n.recipient = :recipient")
    int markAsRead(@Param("notificationId") Long notificationId, @Param("recipient") User recipient, @Param("readAt") LocalDateTime readAt);

    /**
     * Mark specific notification as delivered
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isDelivered = true, n.deliveredAt = :deliveredAt WHERE n.id = :notificationId")
    int markAsDelivered(@Param("notificationId") Long notificationId, @Param("deliveredAt") LocalDateTime deliveredAt);

    /**
     * Find old notifications for a user (to be deleted by service)
     * This is safer than a complex DELETE query
     */
    @Query("SELECT n FROM Notification n WHERE n.recipient = :recipient ORDER BY n.createdAt ASC")
    List<Notification> findOldNotificationsForUser(@Param("recipient") User recipient, Pageable pageable);

    /**
     * Delete expired notifications
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.expiresAt IS NOT NULL AND n.expiresAt < :now")
    int deleteExpiredNotifications(@Param("now") LocalDateTime now);

    /**
     * Find notifications related to a specific message
     */
    List<Notification> findByRelatedMessage_Id(Long messageId);

    /**
     * Find notifications related to a specific chat room
     */
    List<Notification> findByRelatedChatRoom_Id(Long chatRoomId);

    /**
     * Find recent notifications for a user (within last N days)
     */
    @Query("SELECT n FROM Notification n WHERE n.recipient = :recipient AND n.createdAt >= :since ORDER BY n.createdAt DESC")
    List<Notification> findRecentNotifications(@Param("recipient") User recipient, @Param("since") LocalDateTime since);

    /**
     * Get notification statistics for a user
     */
    @Query("SELECT " +
           "COUNT(n) as total, " +
           "SUM(CASE WHEN n.isRead = false THEN 1 ELSE 0 END) as unread, " +
           "SUM(CASE WHEN n.isDelivered = false THEN 1 ELSE 0 END) as undelivered " +
           "FROM Notification n WHERE n.recipient = :recipient")
    Object[] getNotificationStats(@Param("recipient") User recipient);
}
