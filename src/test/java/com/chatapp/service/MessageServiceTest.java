package com.chatapp.service;

import com.chatapp.dto.MessageRequest;
import com.chatapp.dto.MessageResponse;
import com.chatapp.dto.PagedResponse;
import com.chatapp.dto.UserResponse;
import com.chatapp.exception.ResourceNotFoundException;
import com.chatapp.model.ChatRoom;
import com.chatapp.model.Message;
import com.chatapp.model.MessageStatus;
import com.chatapp.model.User;
import com.chatapp.repository.MessageRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserService userService;

    @Mock
    private ChatRoomService chatRoomService;

    @Mock
    private MessageStatusService messageStatusService;

    @InjectMocks
    private MessageService messageService;

    private User currentUser;
    private User otherUser;
    private ChatRoom chatRoom;
    private Message message;
    private MessageRequest messageRequest;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        currentUser = User.builder()
                .id(1L)
                .username("currentuser")
                .email("current@example.com")
                .password("password")
                .fullName("Current User")
                .isOnline(true)
                .createdAt(LocalDateTime.now())
                .lastSeen(LocalDateTime.now())
                .roles(new HashSet<>(Set.of("ROLE_USER")))
                .build();

        otherUser = User.builder()
                .id(2L)
                .username("otheruser")
                .email("other@example.com")
                .password("password")
                .fullName("Other User")
                .isOnline(true)
                .createdAt(LocalDateTime.now())
                .lastSeen(LocalDateTime.now())
                .roles(new HashSet<>(Set.of("ROLE_USER")))
                .build();

        chatRoom = ChatRoom.builder()
                .id(1L)
                .name("Test Chat Room")
                .isPrivate(false)
                .creator(currentUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .participants(new HashSet<>(Set.of(currentUser, otherUser)))
                .build();

        message = Message.builder()
                .id(1L)
                .content("Hello, world!")
                .sender(currentUser)
                .chatRoom(chatRoom)
                .sentAt(LocalDateTime.now())
                .contentType("TEXT")
                .build();

        messageRequest = new MessageRequest();
        messageRequest.setContent("Hello, world!");
        messageRequest.setContentType("TEXT");
        
        userResponse = new UserResponse();
        userResponse.setId(1L);
        userResponse.setUsername("currentuser");
        userResponse.setFullName("Current User");
        userResponse.setEmail("current@example.com");
        userResponse.setOnline(true);
    }

    @Test
    void getMessageById_ShouldReturnMessage_WhenMessageExists() {
        // Arrange
        when(messageRepository.findById(1L)).thenReturn(Optional.of(message));

        // Act
        Message result = messageService.getMessageById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(message.getId(), result.getId());
        assertEquals(message.getContent(), result.getContent());
        verify(messageRepository).findById(1L);
    }

    @Test
    void getMessageById_ShouldThrowResourceNotFoundException_WhenMessageDoesNotExist() {
        // Arrange
        when(messageRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            messageService.getMessageById(999L);
        });
        verify(messageRepository).findById(999L);
    }

    @Test
    void getChatRoomMessages_ShouldReturnPagedMessages_WhenUserIsParticipant() {
        // Arrange
        int page = 0;
        int size = 10;
        List<Message> messages = List.of(message);
        Page<Message> messagePage = new PageImpl<>(messages);

        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(chatRoomService.getChatRoomById(1L)).thenReturn(chatRoom);
        when(messageRepository.findByChatRoomOrderBySentAtDesc(eq(chatRoom), any(Pageable.class))).thenReturn(messagePage);
        when(userService.convertToUserResponse(any(User.class))).thenReturn(userResponse);

        // Act
        PagedResponse<MessageResponse> result = messageService.getChatRoomMessages(1L, page, size);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        verify(userService).getCurrentUser();
        verify(chatRoomService).getChatRoomById(1L);
        verify(messageRepository).findByChatRoomOrderBySentAtDesc(eq(chatRoom), any(Pageable.class));
    }

    @Test
    void saveMessage_ShouldCreateAndReturnMessage() {
        // Arrange
        when(messageRepository.save(any(Message.class))).thenReturn(message);

        // Act
        Message result = messageService.saveMessage(currentUser, chatRoom, "Hello, world!", "TEXT");

        // Assert
        assertNotNull(result);
        assertEquals(message.getId(), result.getId());
        assertEquals(message.getContent(), result.getContent());
        verify(messageRepository).save(any(Message.class));
        verify(messageStatusService).createOrUpdateMessageStatus(any(Message.class), eq(currentUser), eq(MessageStatus.Status.SENT));
    }

    @Test
    void sendMessage_ShouldCreateAndReturnMessageResponse_WhenUserIsParticipant() {
        // Arrange
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(chatRoomService.getChatRoomById(1L)).thenReturn(chatRoom);
        when(messageRepository.save(any(Message.class))).thenReturn(message);
        when(userService.convertToUserResponse(any(User.class))).thenReturn(userResponse);

        // Act
        MessageResponse result = messageService.sendMessage(1L, messageRequest);

        // Assert
        assertNotNull(result);
        verify(userService).getCurrentUser();
        verify(chatRoomService).getChatRoomById(1L);
        verify(messageStatusService).createOrUpdateMessageStatus(any(Message.class), eq(currentUser), eq(MessageStatus.Status.SENT));
    }

    @Test
    void markMessageAsRead_ShouldUpdateMessageStatus_WhenUserIsParticipantAndNotSender() {
        // Arrange
        Message otherUserMessage = Message.builder()
                .id(2L)
                .content("Hello from other user!")
                .sender(otherUser)
                .chatRoom(chatRoom)
                .sentAt(LocalDateTime.now())
                .contentType("TEXT")
                .build();

        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(messageRepository.findById(2L)).thenReturn(Optional.of(otherUserMessage));

        // Act
        messageService.markMessageAsRead(2L);

        // Assert
        verify(userService).getCurrentUser();
        verify(messageRepository).findById(2L);
        verify(messageStatusService).notifyMessageStatusChange(otherUserMessage, currentUser, MessageStatus.Status.READ);
    }

    @Test
    void markAllMessagesAsRead_ShouldUpdateAllMessageStatuses_WhenUserIsParticipant() {
        // Arrange
        Message otherUserMessage = Message.builder()
                .id(2L)
                .content("Hello from other user!")
                .sender(otherUser)
                .chatRoom(chatRoom)
                .sentAt(LocalDateTime.now())
                .contentType("TEXT")
                .build();

        List<Message> messages = List.of(message, otherUserMessage);

        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(chatRoomService.getChatRoomById(1L)).thenReturn(chatRoom);
        when(messageRepository.findByChatRoomOrderBySentAtDesc(chatRoom)).thenReturn(messages);

        // Act
        messageService.markAllMessagesAsRead(1L);

        // Assert
        verify(userService).getCurrentUser();
        verify(chatRoomService).getChatRoomById(1L);
        verify(messageRepository).findByChatRoomOrderBySentAtDesc(chatRoom);
        // Should only mark other user's message as read, not current user's own message
        verify(messageStatusService).notifyMessageStatusChange(otherUserMessage, currentUser, MessageStatus.Status.READ);
        verify(messageStatusService, never()).notifyMessageStatusChange(message, currentUser, MessageStatus.Status.READ);
    }
}
