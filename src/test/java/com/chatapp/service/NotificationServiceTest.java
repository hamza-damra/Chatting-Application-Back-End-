package com.chatapp.service;

import com.chatapp.dto.NotificationResponse;
import com.chatapp.model.*;
import com.chatapp.repository.NotificationRepository;
import com.chatapp.repository.NotificationPreferencesRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationPreferencesRepository preferencesRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private UserPresenceService userPresenceService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private NotificationService notificationService;

    private User testUser;
    private User senderUser;
    private ChatRoom testChatRoom;
    private Message testMessage;
    private NotificationPreferences testPreferences;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .id(1L)
            .username("testuser")
            .email("test@example.com")
            .fullName("Test User")
            .build();

        senderUser = User.builder()
            .id(2L)
            .username("sender")
            .email("sender@example.com")
            .fullName("Sender User")
            .build();

        testChatRoom = ChatRoom.builder()
            .id(1L)
            .name("Test Room")
            .isPrivate(false)
            .participants(Set.of(testUser, senderUser))
            .build();

        testMessage = Message.builder()
            .id(1L)
            .content("Test message")
            .contentType("TEXT")
            .sender(senderUser)
            .chatRoom(testChatRoom)
            .sentAt(LocalDateTime.now())
            .build();

        testPreferences = NotificationPreferences.builder()
            .id(1L)
            .user(testUser)
            .pushNotificationsEnabled(true)
            .newMessageNotifications(true)
            .groupMessageNotifications(true)
            .build();
    }

    @Test
    void createAndSendNotification_ShouldCreateAndSaveNotification() {
        // Arrange
        when(preferencesRepository.findByUser(testUser)).thenReturn(Optional.of(testPreferences));
        try {
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"test\":\"data\"}");
        } catch (Exception e) {
            // This won't happen in test, but needed for compilation
        }

        Notification savedNotification = Notification.builder()
            .id(1L)
            .recipient(testUser)
            .notificationType(Notification.NotificationType.NEW_MESSAGE)
            .title("Test Notification")
            .content("Test content")
            .priority(Notification.Priority.NORMAL)
            .build();

        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);

        // Act
        Notification result = notificationService.createAndSendNotification(
            testUser,
            Notification.NotificationType.NEW_MESSAGE,
            "Test Notification",
            "Test content",
            Map.of("test", "data"),
            Notification.Priority.NORMAL,
            testMessage,
            testChatRoom,
            senderUser
        );

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Notification", result.getTitle());
        verify(notificationRepository).save(any(Notification.class));
        verify(messagingTemplate).convertAndSendToUser(eq("testuser"), eq("/notifications"), any());
    }

    @Test
    void createAndSendNotification_ShouldReturnNullWhenNotificationDisabled() {
        // Arrange
        testPreferences.setNewMessageNotifications(false);
        when(preferencesRepository.findByUser(testUser)).thenReturn(Optional.of(testPreferences));

        // Act
        Notification result = notificationService.createAndSendNotification(
            testUser,
            Notification.NotificationType.NEW_MESSAGE,
            "Test Notification",
            "Test content",
            null,
            Notification.Priority.NORMAL,
            null,
            null,
            null
        );

        // Assert
        assertNull(result);
        verify(notificationRepository, never()).save(any());
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    void sendMessageNotification_ShouldSendToInactiveUsers() {
        // Arrange
        when(userPresenceService.isUserActiveInRoom("testuser", 1L)).thenReturn(false);
        when(userPresenceService.isUserActiveInRoom("sender", 1L)).thenReturn(true);
        when(preferencesRepository.findByUser(testUser)).thenReturn(Optional.of(testPreferences));
        try {
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"messageId\":1}");
        } catch (Exception e) {
            // This won't happen in test, but needed for compilation
        }

        Notification savedNotification = Notification.builder()
            .id(1L)
            .recipient(testUser)
            .build();
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);

        // Act
        notificationService.sendMessageNotification(testMessage, testChatRoom, senderUser);

        // Assert
        verify(notificationRepository).save(any(Notification.class));
        verify(messagingTemplate).convertAndSendToUser(eq("testuser"), eq("/notifications"), any());
    }

    @Test
    void sendMessageNotification_ShouldNotSendToActiveUsers() {
        // Arrange
        when(userPresenceService.isUserActiveInRoom("testuser", 1L)).thenReturn(true);
        when(userPresenceService.isUserActiveInRoom("sender", 1L)).thenReturn(true);

        // Act
        notificationService.sendMessageNotification(testMessage, testChatRoom, senderUser);

        // Assert
        verify(notificationRepository, never()).save(any());
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    void getNotificationsForUser_ShouldReturnPagedNotifications() {
        // Arrange
        Notification notification = Notification.builder()
            .id(1L)
            .recipient(testUser)
            .notificationType(Notification.NotificationType.NEW_MESSAGE)
            .title("Test")
            .content("Content")
            .priority(Notification.Priority.NORMAL)
            .createdAt(LocalDateTime.now())
            .build();

        Page<Notification> notificationPage = new PageImpl<>(List.of(notification));
        Pageable pageable = PageRequest.of(0, 20);

        when(notificationRepository.findByRecipientOrderByCreatedAtDesc(testUser, pageable))
            .thenReturn(notificationPage);

        // Act
        Page<NotificationResponse> result = notificationService.getNotificationsForUser(testUser, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Test", result.getContent().get(0).getTitle());
    }

    @Test
    void markNotificationAsRead_ShouldReturnTrueWhenSuccessful() {
        // Arrange
        when(notificationRepository.markAsRead(eq(1L), eq(testUser), any(LocalDateTime.class)))
            .thenReturn(1);

        // Act
        boolean result = notificationService.markNotificationAsRead(1L, testUser);

        // Assert
        assertTrue(result);
        verify(notificationRepository).markAsRead(eq(1L), eq(testUser), any(LocalDateTime.class));
    }

    @Test
    void markNotificationAsRead_ShouldReturnFalseWhenNotFound() {
        // Arrange
        when(notificationRepository.markAsRead(eq(1L), eq(testUser), any(LocalDateTime.class)))
            .thenReturn(0);

        // Act
        boolean result = notificationService.markNotificationAsRead(1L, testUser);

        // Assert
        assertFalse(result);
    }

    @Test
    void getUnreadNotificationCount_ShouldReturnCorrectCount() {
        // Arrange
        when(notificationRepository.countByRecipientAndIsReadFalse(testUser)).thenReturn(5L);

        // Act
        long result = notificationService.getUnreadNotificationCount(testUser);

        // Assert
        assertEquals(5L, result);
    }

    @Test
    void createDefaultPreferences_ShouldCreateAndSavePreferences() {
        // Arrange
        NotificationPreferences savedPreferences = NotificationPreferences.builder()
            .id(1L)
            .user(testUser)
            .pushNotificationsEnabled(true)
            .build();

        when(preferencesRepository.save(any(NotificationPreferences.class)))
            .thenReturn(savedPreferences);

        // Act
        NotificationPreferences result = notificationService.createDefaultPreferences(testUser);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(testUser, result.getUser());
        assertTrue(result.isPushNotificationsEnabled());
        verify(preferencesRepository).save(any(NotificationPreferences.class));
    }
}
