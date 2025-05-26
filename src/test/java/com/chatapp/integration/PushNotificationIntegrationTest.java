package com.chatapp.integration;

import com.chatapp.dto.NotificationResponse;
import com.chatapp.model.*;
import com.chatapp.repository.*;
import com.chatapp.service.NotificationService;
import com.chatapp.service.NotificationPreferencesService;
import com.chatapp.service.UserPresenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration test for the push notification system
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PushNotificationIntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationPreferencesService preferencesService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationPreferencesRepository preferencesRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private MessageRepository messageRepository;

    @MockBean
    private SimpMessagingTemplate messagingTemplate;

    @MockBean
    private UserPresenceService userPresenceService;

    private User sender;
    private User recipient;
    private ChatRoom chatRoom;
    private Message message;

    @BeforeEach
    void setUp() {
        // Create test users
        sender = User.builder()
            .username("sender")
            .email("sender@test.com")
            .fullName("Test Sender")
            .password("password")
            .build();
        sender = userRepository.save(sender);

        recipient = User.builder()
            .username("recipient")
            .email("recipient@test.com")
            .fullName("Test Recipient")
            .password("password")
            .build();
        recipient = userRepository.save(recipient);

        // Create test chat room
        chatRoom = ChatRoom.builder()
            .name("Test Room")
            .isPrivate(false)
            .participants(Set.of(sender, recipient))
            .build();
        chatRoom = chatRoomRepository.save(chatRoom);

        // Create test message
        message = Message.builder()
            .content("Test message for notification")
            .contentType("TEXT")
            .sender(sender)
            .chatRoom(chatRoom)
            .sentAt(LocalDateTime.now())
            .build();
        message = messageRepository.save(message);
    }

    @Test
    void testCreateAndSendNotification_Success() {
        // Arrange
        when(userPresenceService.isUserActiveInRoom(anyString(), anyLong())).thenReturn(false);

        // Act
        Notification notification = notificationService.createAndSendNotification(
            recipient,
            Notification.NotificationType.NEW_MESSAGE,
            "New Message",
            "You have a new message",
            Map.of("messageId", message.getId()),
            Notification.Priority.NORMAL,
            message,
            chatRoom,
            sender
        );

        // Assert
        assertNotNull(notification);
        assertEquals("New Message", notification.getTitle());
        assertEquals("You have a new message", notification.getContent());
        assertEquals(recipient.getId(), notification.getRecipient().getId());
        assertEquals(Notification.NotificationType.NEW_MESSAGE, notification.getNotificationType());
        assertFalse(notification.isRead());
        assertTrue(notification.isDelivered()); // Should be marked as delivered via WebSocket

        // Verify WebSocket message was sent
        verify(messagingTemplate).convertAndSendToUser(
            eq("recipient"),
            eq("/notifications"),
            any(NotificationResponse.class)
        );
    }

    @Test
    void testSendMessageNotification_ToInactiveUsers() {
        // Arrange
        when(userPresenceService.isUserActiveInRoom("recipient", chatRoom.getId())).thenReturn(false);
        when(userPresenceService.isUserActiveInRoom("sender", chatRoom.getId())).thenReturn(true);

        // Act
        notificationService.sendMessageNotification(message, chatRoom, sender);

        // Assert
        List<Notification> notifications = notificationRepository.findByRecipientAndIsReadFalseOrderByCreatedAtDesc(recipient);
        assertEquals(1, notifications.size());

        Notification notification = notifications.get(0);
        assertEquals(Notification.NotificationType.GROUP_MESSAGE, notification.getNotificationType());
        assertEquals("New message in " + chatRoom.getName(), notification.getTitle());
        assertEquals(recipient.getId(), notification.getRecipient().getId());
        assertEquals(sender.getId(), notification.getTriggeredByUser().getId());

        // Verify WebSocket message was sent
        verify(messagingTemplate).convertAndSendToUser(
            eq("recipient"),
            eq("/notifications"),
            any(NotificationResponse.class)
        );
    }

    @Test
    void testSendMessageNotification_DoesNotSendToActiveUsers() {
        // Arrange - both users are active in the room
        when(userPresenceService.isUserActiveInRoom("recipient", chatRoom.getId())).thenReturn(true);
        when(userPresenceService.isUserActiveInRoom("sender", chatRoom.getId())).thenReturn(true);

        // Act
        notificationService.sendMessageNotification(message, chatRoom, sender);

        // Assert
        List<Notification> notifications = notificationRepository.findByRecipientAndIsReadFalseOrderByCreatedAtDesc(recipient);
        assertEquals(0, notifications.size());

        // Verify no WebSocket message was sent
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    void testNotificationPreferences_DisableNotifications() {
        // Arrange - disable notifications for recipient
        NotificationPreferences preferences = NotificationPreferences.builder()
            .user(recipient)
            .pushNotificationsEnabled(false)
            .build();
        preferencesRepository.save(preferences);

        when(userPresenceService.isUserActiveInRoom("recipient", chatRoom.getId())).thenReturn(false);

        // Act
        notificationService.sendMessageNotification(message, chatRoom, sender);

        // Assert
        List<Notification> notifications = notificationRepository.findByRecipientAndIsReadFalseOrderByCreatedAtDesc(recipient);
        assertEquals(0, notifications.size());

        // Verify no WebSocket message was sent
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    void testNotificationPreferences_DoNotDisturbMode() {
        // Arrange - enable DND mode
        NotificationPreferences preferences = NotificationPreferences.builder()
            .user(recipient)
            .pushNotificationsEnabled(true)
            .doNotDisturb(true)
            .dndStartTime(null) // Always in DND mode
            .dndEndTime(null)
            .build();
        preferencesRepository.save(preferences);

        when(userPresenceService.isUserActiveInRoom("recipient", chatRoom.getId())).thenReturn(false);

        // Act
        notificationService.sendMessageNotification(message, chatRoom, sender);

        // Assert
        List<Notification> notifications = notificationRepository.findByRecipientAndIsReadFalseOrderByCreatedAtDesc(recipient);
        assertEquals(0, notifications.size());
    }

    @Test
    void testGetNotificationsForUser() {
        // Arrange - create some notifications
        for (int i = 0; i < 5; i++) {
            notificationService.createAndSendNotification(
                recipient,
                Notification.NotificationType.NEW_MESSAGE,
                "Message " + i,
                "Content " + i,
                null,
                Notification.Priority.NORMAL,
                null,
                null,
                sender
            );
        }

        // Act
        Page<NotificationResponse> notifications = notificationService.getNotificationsForUser(
            recipient,
            PageRequest.of(0, 10)
        );

        // Assert
        assertEquals(5, notifications.getTotalElements());
        assertEquals(5, notifications.getContent().size());

        // Verify they are ordered by creation date (newest first)
        List<NotificationResponse> content = notifications.getContent();
        for (int i = 0; i < content.size() - 1; i++) {
            assertTrue(content.get(i).getCreatedAt().isAfter(content.get(i + 1).getCreatedAt()) ||
                      content.get(i).getCreatedAt().isEqual(content.get(i + 1).getCreatedAt()));
        }
    }

    @Test
    void testMarkNotificationAsRead() {
        // Arrange
        Notification notification = notificationService.createAndSendNotification(
            recipient,
            Notification.NotificationType.NEW_MESSAGE,
            "Test Message",
            "Test Content",
            null,
            Notification.Priority.NORMAL,
            null,
            null,
            sender
        );

        // Act
        boolean result = notificationService.markNotificationAsRead(notification.getId(), recipient);

        // Assert
        assertTrue(result);

        Notification updatedNotification = notificationRepository.findById(notification.getId()).orElse(null);
        assertNotNull(updatedNotification);
        assertTrue(updatedNotification.isRead());
        assertNotNull(updatedNotification.getReadAt());
    }

    @Test
    void testGetUnreadNotificationCount() {
        // Arrange - create some notifications
        for (int i = 0; i < 3; i++) {
            notificationService.createAndSendNotification(
                recipient,
                Notification.NotificationType.NEW_MESSAGE,
                "Message " + i,
                "Content " + i,
                null,
                Notification.Priority.NORMAL,
                null,
                null,
                sender
            );
        }

        // Mark one as read
        List<Notification> notifications = notificationRepository.findByRecipientAndIsReadFalseOrderByCreatedAtDesc(recipient);
        notificationService.markNotificationAsRead(notifications.get(0).getId(), recipient);

        // Act
        long unreadCount = notificationService.getUnreadNotificationCount(recipient);

        // Assert
        assertEquals(2, unreadCount);
    }

    @Test
    void testPrivateMessageNotification() {
        // Arrange - create private chat room
        ChatRoom privateRoom = ChatRoom.builder()
            .name("Private Chat")
            .isPrivate(true)
            .participants(Set.of(sender, recipient))
            .build();
        privateRoom = chatRoomRepository.save(privateRoom);

        Message privateMessage = Message.builder()
            .content("Private message")
            .contentType("TEXT")
            .sender(sender)
            .chatRoom(privateRoom)
            .sentAt(LocalDateTime.now())
            .build();
        privateMessage = messageRepository.save(privateMessage);

        when(userPresenceService.isUserActiveInRoom("recipient", privateRoom.getId())).thenReturn(false);

        // Act
        notificationService.sendMessageNotification(privateMessage, privateRoom, sender);

        // Assert
        List<Notification> notifications = notificationRepository.findByRecipientAndIsReadFalseOrderByCreatedAtDesc(recipient);
        assertEquals(1, notifications.size());

        Notification notification = notifications.get(0);
        assertEquals(Notification.NotificationType.PRIVATE_MESSAGE, notification.getNotificationType());
        assertEquals("New message from " + sender.getUsername(), notification.getTitle());
    }
}
