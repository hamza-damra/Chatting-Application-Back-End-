package com.chatapp.service;

import com.chatapp.dto.NotificationPreferencesRequest;
import com.chatapp.dto.NotificationPreferencesResponse;
import com.chatapp.model.NotificationPreferences;
import com.chatapp.model.User;
import com.chatapp.repository.NotificationPreferencesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service for managing user notification preferences
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationPreferencesService {

    private final NotificationPreferencesRepository preferencesRepository;

    /**
     * Get notification preferences for a user
     */
    public NotificationPreferencesResponse getPreferencesForUser(User user) {
        Optional<NotificationPreferences> prefsOpt = preferencesRepository.findByUser(user);

        if (prefsOpt.isEmpty()) {
            // Create default preferences if they don't exist
            NotificationPreferences defaultPrefs = createDefaultPreferences(user);
            return NotificationPreferencesResponse.fromEntity(defaultPrefs);
        }

        return NotificationPreferencesResponse.fromEntity(prefsOpt.get());
    }

    /**
     * Update notification preferences for a user
     */
    public NotificationPreferencesResponse updatePreferencesForUser(User user, NotificationPreferencesRequest request) {
        Optional<NotificationPreferences> prefsOpt = preferencesRepository.findByUser(user);

        NotificationPreferences preferences;
        if (prefsOpt.isEmpty()) {
            // Create new preferences
            preferences = createDefaultPreferences(user);
        } else {
            preferences = prefsOpt.get();
        }

        // Update only non-null fields from the request
        updatePreferencesFromRequest(preferences, request);

        preferences = preferencesRepository.save(preferences);
        log.info("NOTIFICATION_PREFS: Updated preferences for user {}", user.getUsername());

        return NotificationPreferencesResponse.fromEntity(preferences);
    }

    /**
     * Create default notification preferences for a user
     */
    public NotificationPreferences createDefaultPreferences(User user) {
        NotificationPreferences preferences = NotificationPreferences.builder()
            .user(user)
            .build(); // Uses @Builder.Default values

        preferences = preferencesRepository.save(preferences);
        log.info("NOTIFICATION_PREFS: Created default preferences for user {}", user.getUsername());

        return preferences;
    }

    /**
     * Check if push notifications are enabled for a user
     */
    @Transactional(readOnly = true)
    public boolean isPushNotificationsEnabled(User user) {
        return preferencesRepository.isPushNotificationsEnabled(user).orElse(true);
    }

    /**
     * Check if do not disturb mode is enabled for a user
     */
    @Transactional(readOnly = true)
    public boolean isDoNotDisturbEnabled(User user) {
        return preferencesRepository.isDoNotDisturbEnabled(user).orElse(false);
    }

    /**
     * Reset preferences to default for a user
     */
    public NotificationPreferencesResponse resetToDefault(User user) {
        Optional<NotificationPreferences> prefsOpt = preferencesRepository.findByUser(user);

        if (prefsOpt.isPresent()) {
            preferencesRepository.delete(prefsOpt.get());
        }

        NotificationPreferences defaultPrefs = createDefaultPreferences(user);
        return NotificationPreferencesResponse.fromEntity(defaultPrefs);
    }

    /**
     * Delete preferences for a user
     */
    public void deletePreferencesForUser(User user) {
        preferencesRepository.deleteByUser(user);
        log.info("NOTIFICATION_PREFS: Deleted preferences for user {}", user.getUsername());
    }

    /**
     * Update preferences entity from request DTO
     */
    private void updatePreferencesFromRequest(NotificationPreferences preferences, NotificationPreferencesRequest request) {
        if (request.getPushNotificationsEnabled() != null) {
            preferences.setPushNotificationsEnabled(request.getPushNotificationsEnabled());
        }
        if (request.getNewMessageNotifications() != null) {
            preferences.setNewMessageNotifications(request.getNewMessageNotifications());
        }
        if (request.getPrivateMessageNotifications() != null) {
            preferences.setPrivateMessageNotifications(request.getPrivateMessageNotifications());
        }
        if (request.getGroupMessageNotifications() != null) {
            preferences.setGroupMessageNotifications(request.getGroupMessageNotifications());
        }
        if (request.getMentionNotifications() != null) {
            preferences.setMentionNotifications(request.getMentionNotifications());
        }
        if (request.getChatRoomInviteNotifications() != null) {
            preferences.setChatRoomInviteNotifications(request.getChatRoomInviteNotifications());
        }
        if (request.getFileSharingNotifications() != null) {
            preferences.setFileSharingNotifications(request.getFileSharingNotifications());
        }
        if (request.getSystemAnnouncementNotifications() != null) {
            preferences.setSystemAnnouncementNotifications(request.getSystemAnnouncementNotifications());
        }
        if (request.getDoNotDisturb() != null) {
            preferences.setDoNotDisturb(request.getDoNotDisturb());
        }
        if (request.getDndStartTime() != null) {
            preferences.setDndStartTime(request.getDndStartTime());
        }
        if (request.getDndEndTime() != null) {
            preferences.setDndEndTime(request.getDndEndTime());
        }
        if (request.getSoundEnabled() != null) {
            preferences.setSoundEnabled(request.getSoundEnabled());
        }
        if (request.getVibrationEnabled() != null) {
            preferences.setVibrationEnabled(request.getVibrationEnabled());
        }
        if (request.getShowPreview() != null) {
            preferences.setShowPreview(request.getShowPreview());
        }
        if (request.getMaxOfflineNotifications() != null) {
            preferences.setMaxOfflineNotifications(request.getMaxOfflineNotifications());
        }
    }
}
