package com.chatapp.service;

import com.chatapp.dto.ChatRoomResponse;
import com.chatapp.dto.MessageResponse;
import com.chatapp.model.ChatRoom;
import com.chatapp.model.Message;
import com.chatapp.model.MessageStatus;
import com.chatapp.model.User;
import com.chatapp.repository.ChatRoomRepository;
import com.chatapp.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ChatRoomServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserService userService;

    @Mock
    private DtoConverterService dtoConverterService;

    @Mock
    private MessageStatusService messageStatusService;

    @InjectMocks
    private ChatRoomService chatRoomService;

    private User currentUser;
    private User otherUser;
    private ChatRoom chatRoom;
    private Message lastMessage;

    @BeforeEach
    void setUp() {
        // Set up test data
        currentUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .build();

        otherUser = User.builder()
                .id(2L)
                .username("otheruser")
                .email("other@example.com")
                .build();

        chatRoom = ChatRoom.builder()
                .id(1L)
                .name("Test Chat Room")
                .privateFlag(true)
                .creator(currentUser)
                .participants(new HashSet<>(Set.of(currentUser, otherUser)))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        lastMessage = Message.builder()
                .id(1L)
                .content("Hello, this is a test message")
                .contentType("TEXT")
                .sender(otherUser)
                .chatRoom(chatRoom)
                .sentAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testGetChatRoomResponseById_WithLastMessage() {
        // Mock the current user
        when(userService.getCurrentUser()).thenReturn(currentUser);

        // Mock the chat room repository
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(chatRoom));

        // Mock the message repository to return the last message
        when(messageRepository.findMostRecentMessagesInChatRoom(eq(chatRoom), any(PageRequest.class)))
                .thenReturn(List.of(lastMessage));

        // Mock the message repository to return a list of messages for unread count
        List<Message> messages = new ArrayList<>();
        messages.add(lastMessage);
        when(messageRepository.findByChatRoomOrderBySentAtDesc(chatRoom)).thenReturn(messages);

        // Mock the message status service to indicate the message is unread
        when(messageStatusService.getMessageStatusForUser(lastMessage, currentUser))
                .thenReturn(null); // null means unread

        // Also test with a delivered status
        Message deliveredMessage = Message.builder()
                .id(2L)
                .content("This message has been delivered")
                .contentType("TEXT")
                .sender(otherUser)
                .chatRoom(chatRoom)
                .sentAt(LocalDateTime.now().minusMinutes(5))
                .build();
        messages.add(deliveredMessage);
        when(messageStatusService.getMessageStatusForUser(deliveredMessage, currentUser))
                .thenReturn(MessageStatus.Status.DELIVERED);

        // Mock the message service to convert the message to a response
        MessageResponse messageResponse = MessageResponse.builder()
                .id(lastMessage.getId())
                .content(lastMessage.getContent())
                .contentType(lastMessage.getContentType())
                .sender(null) // Not important for this test
                .chatRoomId(chatRoom.getId())
                .sentAt(lastMessage.getSentAt())
                .build();
        when(dtoConverterService.convertToMessageResponse(lastMessage)).thenReturn(messageResponse);

        // Call the service method
        ChatRoomResponse response = chatRoomService.getChatRoomResponseById(1L);

        // Verify the response
        assertNotNull(response);
        assertEquals(chatRoom.getId(), response.getId());
        assertEquals(chatRoom.getName(), response.getName());
        assertEquals(chatRoom.isPrivate(), response.isPrivate());

        // Verify the last message is included
        assertNotNull(response.getLastMessage());
        assertEquals(lastMessage.getId(), response.getLastMessage().getId());
        assertEquals(lastMessage.getContent(), response.getLastMessage().getContent());

        // Verify the unread count (1 unread + 1 delivered = 2 total unread messages)
        assertEquals(2, response.getUnreadCount());
    }
}
