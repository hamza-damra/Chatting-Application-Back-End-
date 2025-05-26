package com.chatapp.dto;

import com.chatapp.model.NotificationPreferences;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for notification preferences responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferencesResponse {

    private Long id;
    private boolean pushNotificationsEnabled;
    private boolean newMessageNotifications;
    private boolean privateMessageNotifications;
    private boolean groupMessageNotifications;
    private boolean mentionNotifications;
    private boolean chatRoomInviteNotifications;
    private boolean fileSharingNotifications;
    private boolean systemAnnouncementNotifications;
    private boolean doNotDisturb;
    private String dndStartTime;
    private String dndEndTime;
    private boolean soundEnabled;
    private boolean vibrationEnabled;
    private boolean showPreview;
    private int maxOfflineNotifications;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Create a NotificationPreferencesResponse from a NotificationPreferences entity
     */
    public static NotificationPreferencesResponse fromEntity(NotificationPreferences preferences) {
        return NotificationPreferencesResponse.builder()
            .id(preferences.getId())
            .pushNotificationsEnabled(preferences.isPushNotificationsEnabled())
            .newMessageNotifications(preferences.isNewMessageNotifications())
            .privateMessageNotifications(preferences.isPrivateMessageNotifications())
            .groupMessageNotifications(preferences.isGroupMessageNotifications())
            .mentionNotifications(preferences.isMentionNotifications())
            .chatRoomInviteNotifications(preferences.isChatRoomInviteNotifications())
            .fileSharingNotifications(preferences.isFileSharingNotifications())
            .systemAnnouncementNotifications(preferences.isSystemAnnouncementNotifications())
            .doNotDisturb(preferences.isDoNotDisturb())
            .dndStartTime(preferences.getDndStartTime())
            .dndEndTime(preferences.getDndEndTime())
            .soundEnabled(preferences.isSoundEnabled())
            .vibrationEnabled(preferences.isVibrationEnabled())
            .showPreview(preferences.isShowPreview())
            .maxOfflineNotifications(preferences.getMaxOfflineNotifications())
            .createdAt(preferences.getCreatedAt())
            .updatedAt(preferences.getUpdatedAt())
            .build();
    }
}
