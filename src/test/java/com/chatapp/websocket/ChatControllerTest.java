package com.chatapp.websocket;

import com.chatapp.dto.MessageResponse;
import com.chatapp.exception.ChatRoomAccessDeniedException;
import com.chatapp.model.ChatRoom;
import com.chatapp.model.Message;
import com.chatapp.model.User;
import com.chatapp.service.ChatRoomService;
import com.chatapp.service.MessageService;
import com.chatapp.service.UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ChatControllerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private MessageService messageService;

    @Mock
    private UserService userService;

    @Mock
    private ChatRoomService chatRoomService;

    @InjectMocks
    private ChatController chatController;

    private User sender;
    private ChatRoom chatRoom;
    private Message message;
    private ChatMessageRequest chatMessageRequest;
    private Principal principal;
    private SimpMessageHeaderAccessor headerAccessor;
    private Map<String, Object> sessionAttributes;

    @BeforeEach
    void setUp() {
        sender = User.builder()
                .id(1L)
                .username("sender")
                .email("sender@example.com")
                .password("password")
                .fullName("Sender User")
                .isOnline(true)
                .createdAt(LocalDateTime.now())
                .lastSeen(LocalDateTime.now())
                .roles(new HashSet<>(Set.of("ROLE_USER")))
                .build();

        chatRoom = ChatRoom.builder()
                .id(1L)
                .name("Test Chat Room")
                .privateFlag(false)
                .creator(sender)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .participants(new HashSet<>(Set.of(sender)))
                .build();

        message = Message.builder()
                .id(1L)
                .content("Hello, world!")
                .sender(sender)
                .chatRoom(chatRoom)
                .sentAt(LocalDateTime.now())
                .contentType("TEXT")
                .build();

        chatMessageRequest = new ChatMessageRequest();
        chatMessageRequest.setContent("Hello, world!");
        chatMessageRequest.setContentType("TEXT");

        principal = mock(Principal.class);
        when(principal.getName()).thenReturn("sender");

        sessionAttributes = new HashMap<>();
        headerAccessor = mock(SimpMessageHeaderAccessor.class);
        when(headerAccessor.getSessionAttributes()).thenReturn(sessionAttributes);

        // Mock the MessageResponse for the messageService
        MessageResponse messageResponse = new MessageResponse();
        messageResponse.setId(1L);
        messageResponse.setContent("Hello, world!");
        messageResponse.setContentType("TEXT");
        when(messageService.sendMessage(any(), any())).thenReturn(messageResponse);

        // Ensure chatRoomService.isUserInChatRoom returns true
        when(chatRoomService.isUserInChatRoom(any(), any())).thenReturn(true);

        // Mock the addUserToChatRoom method to do nothing
        doNothing().when(chatRoomService).addUserToChatRoom(any(), any());
    }

    @Test
    void sendMessage_ShouldSaveMessageAndNotifyParticipants() {
        // Arrange
        Long roomId = 1L;
        when(userService.getUserByUsername("sender")).thenReturn(sender);
        when(chatRoomService.getChatRoomById(roomId)).thenReturn(chatRoom);
        when(messageService.saveMessage(any(User.class), any(ChatRoom.class), anyString(), anyString())).thenReturn(message);

        // Act
        chatController.sendMessage(roomId, chatMessageRequest, principal);

        // Assert
        verify(userService).getUserByUsername("sender");
        verify(chatRoomService).getChatRoomById(roomId);
        verify(messageService).saveMessage(sender, chatRoom, chatMessageRequest.getContent(), chatMessageRequest.getContentType());
        verify(messagingTemplate).convertAndSend(eq("/topic/chatrooms/" + roomId), any(Object.class));
    }

    @Test
    void joinRoom_ShouldAddUserToRoomAndNotifyParticipants() {
        // Arrange
        Long roomId = 1L;
        when(userService.getUserByUsername("sender")).thenReturn(sender);
        when(chatRoomService.getChatRoomById(roomId)).thenReturn(chatRoom);

        // Act
        chatController.joinRoom(roomId, headerAccessor, principal);

        // Assert
        verify(userService).getUserByUsername("sender");
        verify(chatRoomService).getChatRoomById(roomId);
        verify(headerAccessor.getSessionAttributes(), times(2)).put(any(), any());
        verify(messagingTemplate).convertAndSend(eq("/topic/chatrooms/" + roomId), any(Object.class));
    }

    @Test
    void sendMessage_ShouldThrowException_WhenUserNotInChatRoom() {
        // Arrange
        Long roomId = 1L;
        when(userService.getUserByUsername("sender")).thenReturn(sender);
        when(chatRoomService.getChatRoomById(roomId)).thenReturn(chatRoom);
        when(chatRoomService.isUserInChatRoom(sender, chatRoom)).thenReturn(false);

        // Act & Assert
        assertThrows(ChatRoomAccessDeniedException.class, () -> {
            chatController.sendMessage(roomId, chatMessageRequest, principal);
        });
    }
}
