package com.chatapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;

/**
 * DTO for updating notification preferences
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferencesRequest {

    private Boolean pushNotificationsEnabled;
    private Boolean newMessageNotifications;
    private Boolean privateMessageNotifications;
    private Boolean groupMessageNotifications;
    private Boolean mentionNotifications;
    private Boolean chatRoomInviteNotifications;
    private Boolean fileSharingNotifications;
    private Boolean systemAnnouncementNotifications;
    private Boolean doNotDisturb;

    @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "Time must be in HH:MM format (24-hour)")
    private String dndStartTime;

    @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "Time must be in HH:MM format (24-hour)")
    private String dndEndTime;

    private Boolean soundEnabled;
    private Boolean vibrationEnabled;
    private Boolean showPreview;

    @Min(value = 10, message = "Maximum offline notifications must be at least 10")
    @Max(value = 1000, message = "Maximum offline notifications cannot exceed 1000")
    private Integer maxOfflineNotifications;
}
