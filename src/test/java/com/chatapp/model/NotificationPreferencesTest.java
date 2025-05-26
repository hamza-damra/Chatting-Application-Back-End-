package com.chatapp.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class NotificationPreferencesTest {

    private NotificationPreferences preferences;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .id(1L)
            .username("testuser")
            .email("test@example.com")
            .build();

        preferences = NotificationPreferences.builder()
            .user(testUser)
            .doNotDisturb(true)
            .build();
    }

    @Test
    void shouldSuppressNotifications_WhenDndDisabled_ShouldReturnFalse() {
        // Arrange
        preferences.setDoNotDisturb(false);

        // Act
        boolean result = preferences.shouldSuppressNotifications();

        // Assert
        assertFalse(result);
    }

    @Test
    void shouldSuppressNotifications_WhenDndEnabledWithoutTimes_ShouldReturnTrue() {
        // Arrange
        preferences.setDoNotDisturb(true);
        preferences.setDndStartTime(null);
        preferences.setDndEndTime(null);

        // Act
        boolean result = preferences.shouldSuppressNotifications();

        // Assert
        assertTrue(result);
    }

    @Test
    void shouldSuppressNotifications_WhenCurrentTimeInSameDayRange_ShouldReturnTrue() {
        // Arrange - Set DND from 09:00 to 17:00
        preferences.setDoNotDisturb(true);
        preferences.setDndStartTime("09:00");
        preferences.setDndEndTime("17:00");

        // This test assumes current time is between 09:00 and 17:00
        // In a real scenario, you might want to mock the current time
        LocalTime now = LocalTime.now();
        LocalTime start = LocalTime.parse("09:00");
        LocalTime end = LocalTime.parse("17:00");

        // Act
        boolean result = preferences.shouldSuppressNotifications();

        // Assert
        if (now.isAfter(start) && now.isBefore(end)) {
            assertTrue(result, "Should suppress notifications during DND hours");
        } else {
            assertFalse(result, "Should not suppress notifications outside DND hours");
        }
    }

    @Test
    void shouldSuppressNotifications_WhenCurrentTimeInOvernightRange_ShouldReturnTrue() {
        // Arrange - Set DND from 22:00 to 08:00 (overnight)
        preferences.setDoNotDisturb(true);
        preferences.setDndStartTime("22:00");
        preferences.setDndEndTime("08:00");

        // This test assumes current time is either after 22:00 or before 08:00
        LocalTime now = LocalTime.now();
        LocalTime start = LocalTime.parse("22:00");
        LocalTime end = LocalTime.parse("08:00");

        // Act
        boolean result = preferences.shouldSuppressNotifications();

        // Assert
        if (now.isAfter(start) || now.isBefore(end)) {
            assertTrue(result, "Should suppress notifications during overnight DND hours");
        } else {
            assertFalse(result, "Should not suppress notifications outside overnight DND hours");
        }
    }

    @Test
    void shouldSuppressNotifications_WhenStartAndEndTimesEqual_ShouldReturnTrue() {
        // Arrange - Set DND with same start and end time (all day)
        preferences.setDoNotDisturb(true);
        preferences.setDndStartTime("12:00");
        preferences.setDndEndTime("12:00");

        // Act
        boolean result = preferences.shouldSuppressNotifications();

        // Assert
        assertTrue(result, "Should suppress notifications all day when start and end times are equal");
    }

    @Test
    void shouldSuppressNotifications_WhenInvalidTimeFormat_ShouldFallbackToDndFlag() {
        // Arrange - Set invalid time format
        preferences.setDoNotDisturb(true);
        preferences.setDndStartTime("invalid");
        preferences.setDndEndTime("25:00");

        // Act
        boolean result = preferences.shouldSuppressNotifications();

        // Assert
        assertTrue(result, "Should fallback to DND flag when time format is invalid");
    }

    @Test
    void isNotificationTypeEnabled_WhenPushNotificationsDisabled_ShouldReturnFalse() {
        // Arrange
        preferences.setPushNotificationsEnabled(false);

        // Act
        boolean result = preferences.isNotificationTypeEnabled(Notification.NotificationType.NEW_MESSAGE);

        // Assert
        assertFalse(result);
    }

    @Test
    void isNotificationTypeEnabled_WhenDndActive_ShouldReturnFalse() {
        // Arrange
        preferences.setPushNotificationsEnabled(true);
        preferences.setNewMessageNotifications(true);
        preferences.setDoNotDisturb(true);
        preferences.setDndStartTime(null); // This will make DND always active

        // Act
        boolean result = preferences.isNotificationTypeEnabled(Notification.NotificationType.NEW_MESSAGE);

        // Assert
        assertFalse(result);
    }

    @Test
    void isNotificationTypeEnabled_WhenSpecificTypeDisabled_ShouldReturnFalse() {
        // Arrange
        preferences.setPushNotificationsEnabled(true);
        preferences.setDoNotDisturb(false);
        preferences.setNewMessageNotifications(false);

        // Act
        boolean result = preferences.isNotificationTypeEnabled(Notification.NotificationType.NEW_MESSAGE);

        // Assert
        assertFalse(result);
    }

    @Test
    void isNotificationTypeEnabled_WhenAllConditionsMet_ShouldReturnTrue() {
        // Arrange
        preferences.setPushNotificationsEnabled(true);
        preferences.setDoNotDisturb(false);
        preferences.setNewMessageNotifications(true);

        // Act
        boolean result = preferences.isNotificationTypeEnabled(Notification.NotificationType.NEW_MESSAGE);

        // Assert
        assertTrue(result);
    }

    @Test
    void isNotificationTypeEnabled_ShouldHandleAllNotificationTypes() {
        // Arrange
        preferences.setPushNotificationsEnabled(true);
        preferences.setDoNotDisturb(false);
        preferences.setNewMessageNotifications(true);
        preferences.setPrivateMessageNotifications(true);
        preferences.setGroupMessageNotifications(true);
        preferences.setMentionNotifications(true);
        preferences.setChatRoomInviteNotifications(true);
        preferences.setFileSharingNotifications(true);
        preferences.setSystemAnnouncementNotifications(true);

        // Act & Assert
        assertTrue(preferences.isNotificationTypeEnabled(Notification.NotificationType.NEW_MESSAGE));
        assertTrue(preferences.isNotificationTypeEnabled(Notification.NotificationType.PRIVATE_MESSAGE));
        assertTrue(preferences.isNotificationTypeEnabled(Notification.NotificationType.GROUP_MESSAGE));
        assertTrue(preferences.isNotificationTypeEnabled(Notification.NotificationType.MENTION));
        assertTrue(preferences.isNotificationTypeEnabled(Notification.NotificationType.CHAT_ROOM_INVITE));
        assertTrue(preferences.isNotificationTypeEnabled(Notification.NotificationType.CHAT_ROOM_ADDED));
        assertTrue(preferences.isNotificationTypeEnabled(Notification.NotificationType.CHAT_ROOM_REMOVED));
        assertTrue(preferences.isNotificationTypeEnabled(Notification.NotificationType.FILE_SHARED));
        assertTrue(preferences.isNotificationTypeEnabled(Notification.NotificationType.SYSTEM_ANNOUNCEMENT));
        
        // Test default behavior for new types
        assertTrue(preferences.isNotificationTypeEnabled(Notification.NotificationType.USER_JOINED));
        assertTrue(preferences.isNotificationTypeEnabled(Notification.NotificationType.USER_LEFT));
    }
}
