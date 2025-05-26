package com.chatapp.model;

import lombok.*;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Entity representing user notification preferences
 */
@Entity
@Table(name = "notification_preferences", indexes = {
    @Index(name = "idx_notification_prefs_user", columnList = "user_id", unique = true)
})
@Getter
@Setter
@ToString(exclude = {"user"})
@EqualsAndHashCode(of = {"id"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user these preferences belong to
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /**
     * Enable/disable all push notifications
     */
    @Column(name = "push_notifications_enabled", nullable = false)
    @Builder.Default
    private boolean pushNotificationsEnabled = true;

    /**
     * Enable/disable new message notifications
     */
    @Column(name = "new_message_notifications", nullable = false)
    @Builder.Default
    private boolean newMessageNotifications = true;

    /**
     * Enable/disable private message notifications
     */
    @Column(name = "private_message_notifications", nullable = false)
    @Builder.Default
    private boolean privateMessageNotifications = true;

    /**
     * Enable/disable group message notifications
     */
    @Column(name = "group_message_notifications", nullable = false)
    @Builder.Default
    private boolean groupMessageNotifications = true;

    /**
     * Enable/disable mention notifications
     */
    @Column(name = "mention_notifications", nullable = false)
    @Builder.Default
    private boolean mentionNotifications = true;

    /**
     * Enable/disable chat room invite notifications
     */
    @Column(name = "chat_room_invite_notifications", nullable = false)
    @Builder.Default
    private boolean chatRoomInviteNotifications = true;

    /**
     * Enable/disable file sharing notifications
     */
    @Column(name = "file_sharing_notifications", nullable = false)
    @Builder.Default
    private boolean fileSharingNotifications = true;

    /**
     * Enable/disable system announcement notifications
     */
    @Column(name = "system_announcement_notifications", nullable = false)
    @Builder.Default
    private boolean systemAnnouncementNotifications = true;

    /**
     * Do not disturb mode - suppress all notifications
     */
    @Column(name = "do_not_disturb", nullable = false)
    @Builder.Default
    private boolean doNotDisturb = false;

    /**
     * Do not disturb start time (24-hour format, e.g., "22:00")
     */
    @Column(name = "dnd_start_time")
    private String dndStartTime;

    /**
     * Do not disturb end time (24-hour format, e.g., "08:00")
     */
    @Column(name = "dnd_end_time")
    private String dndEndTime;

    /**
     * Sound notifications enabled
     */
    @Column(name = "sound_enabled", nullable = false)
    @Builder.Default
    private boolean soundEnabled = true;

    /**
     * Vibration notifications enabled
     */
    @Column(name = "vibration_enabled", nullable = false)
    @Builder.Default
    private boolean vibrationEnabled = true;

    /**
     * Show notification preview in lock screen
     */
    @Column(name = "show_preview", nullable = false)
    @Builder.Default
    private boolean showPreview = true;

    /**
     * Maximum number of notifications to store for offline delivery
     */
    @Column(name = "max_offline_notifications", nullable = false)
    @Builder.Default
    private int maxOfflineNotifications = 100;

    /**
     * When these preferences were created
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * When these preferences were last updated
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Check if notifications should be suppressed due to do not disturb mode
     */
    public boolean shouldSuppressNotifications() {
        if (!doNotDisturb) {
            return false;
        }

        if (dndStartTime == null || dndEndTime == null) {
            return doNotDisturb;
        }

        return isCurrentTimeInDndRange();
    }

    /**
     * Check if the current time falls within the DND time range
     */
    private boolean isCurrentTimeInDndRange() {
        // Additional null check for safety
        if (dndStartTime == null || dndEndTime == null ||
            dndStartTime.trim().isEmpty() || dndEndTime.trim().isEmpty()) {
            return doNotDisturb;
        }

        try {
            LocalTime now = LocalTime.now();
            LocalTime startTime = LocalTime.parse(dndStartTime.trim());
            LocalTime endTime = LocalTime.parse(dndEndTime.trim());

            // Handle same-day range (e.g., 09:00 to 17:00)
            if (startTime.isBefore(endTime)) {
                return !now.isBefore(startTime) && !now.isAfter(endTime);
            }
            // Handle overnight range (e.g., 22:00 to 08:00)
            else if (startTime.isAfter(endTime)) {
                return !now.isBefore(startTime) || !now.isAfter(endTime);
            }
            // Handle edge case where start and end times are the same
            else {
                return true; // Suppress all day if start == end
            }
        } catch (Exception e) {
            // If there's any parsing error, fall back to the DND flag
            // Note: Could add logging here if needed, but keeping model classes simple
            return doNotDisturb;
        }
    }

    /**
     * Check if a specific notification type is enabled
     */
    public boolean isNotificationTypeEnabled(Notification.NotificationType type) {
        if (!pushNotificationsEnabled || shouldSuppressNotifications()) {
            return false;
        }

        return switch (type) {
            case NEW_MESSAGE -> newMessageNotifications;
            case PRIVATE_MESSAGE -> privateMessageNotifications;
            case GROUP_MESSAGE -> groupMessageNotifications;
            case MENTION -> mentionNotifications;
            case CHAT_ROOM_INVITE, CHAT_ROOM_ADDED, CHAT_ROOM_REMOVED -> chatRoomInviteNotifications;
            case FILE_SHARED -> fileSharingNotifications;
            case SYSTEM_ANNOUNCEMENT -> systemAnnouncementNotifications;
            default -> true; // Enable by default for new types
        };
    }
}
