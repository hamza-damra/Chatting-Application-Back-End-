package com.chatapp.websocket;

import com.chatapp.dto.MessageResponse;
import com.chatapp.model.ChatRoom;
import com.chatapp.model.Message;
import com.chatapp.model.User;
import com.chatapp.repository.ChatRoomRepository;
import com.chatapp.repository.MessageRepository;
import com.chatapp.service.DtoConverterService;
import com.chatapp.service.FileChunkService;
import com.chatapp.service.UserService;
import com.chatapp.websocket.model.FileChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class BinaryFileControllerTest {

    @Mock
    private FileChunkService fileChunkService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private DtoConverterService dtoConverterService;

    @Mock
    private UserService userService;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private MessageRepository messageRepository;

    @InjectMocks
    private BinaryFileController binaryFileController;

    @Mock
    private Principal principal;

    @Mock
    private SimpMessageHeaderAccessor headerAccessor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(principal.getName()).thenReturn("testuser");
    }

    @Test
    void handleFileChunk_shouldThrowExceptionWhenPrincipalIsNull() {
        // Arrange
        FileChunk chunk = createTestChunk();

        // Act & Assert
        assertThrows(AccessDeniedException.class, () -> {
            binaryFileController.handleFileChunk(chunk, headerAccessor, null);
        });
    }

    @Test
    void handleFileChunk_shouldSendProgressUpdateWhenFileIsNotComplete() {
        // Arrange
        FileChunk chunk = createTestChunk();
        User user = createTestUser();

        when(userService.getUserByUsername("testuser")).thenReturn(user);
        when(fileChunkService.processChunk(any(FileChunk.class), anyLong())).thenReturn(null);
        // Mock the findUploadIdForFirstChunk method for the first chunk
        when(fileChunkService.findUploadIdForFirstChunk(any(FileChunk.class), anyLong())).thenReturn("test-upload-id");

        // Act
        binaryFileController.handleFileChunk(chunk, headerAccessor, principal);

        // Assert
        verify(fileChunkService).processChunk(chunk, user.getId());
        verify(messagingTemplate).convertAndSendToUser(
            eq("testuser"),
            eq("/queue/files.progress"),
            any(BinaryFileController.FileUploadProgress.class)
        );
        // No need to verify this as it's not called in this test case
    }

    @Test
    void handleFileChunk_shouldCreateMessageAndSendNotificationsWhenFileIsComplete() {
        // Arrange
        FileChunk chunk = createTestChunk();
        User user = createTestUser();
        ChatRoom chatRoom = createTestChatRoom();
        Message message = createTestMessage(user, chatRoom);
        MessageResponse messageResponse = new MessageResponse();

        when(userService.getUserByUsername("testuser")).thenReturn(user);
        when(fileChunkService.processChunk(any(FileChunk.class), anyLong())).thenReturn("/path/to/file.txt");
        when(chatRoomRepository.findById(anyLong())).thenReturn(Optional.of(chatRoom));
        when(messageRepository.save(any(Message.class))).thenReturn(message);
        when(dtoConverterService.convertToMessageResponse(any(Message.class))).thenReturn(messageResponse);
        // Mock the findUploadIdForFirstChunk method for the first chunk
        when(fileChunkService.findUploadIdForFirstChunk(any(FileChunk.class), anyLong())).thenReturn("test-upload-id");

        // Act
        binaryFileController.handleFileChunk(chunk, headerAccessor, principal);

        // Assert
        verify(fileChunkService).processChunk(chunk, user.getId());
        verify(chatRoomRepository).findById(chunk.getChatRoomId());
        verify(messageRepository).save(any(Message.class));
        verify(dtoConverterService).convertToMessageResponse(message);

        // Verify messages sent
        verify(messagingTemplate).convertAndSend(
            eq("/topic/chatrooms/" + chunk.getChatRoomId()),
            eq(messageResponse),
            eq(java.util.Collections.emptyMap())
        );
        verify(messagingTemplate).convertAndSendToUser(
            eq("testuser"),
            eq("/queue/files"),
            eq(messageResponse)
        );
    }

    @Test
    void handleFileChunk_shouldSendErrorMessageWhenExceptionOccurs() {
        // Arrange
        FileChunk chunk = createTestChunk();

        when(userService.getUserByUsername("testuser")).thenThrow(new RuntimeException("Test exception"));

        // Act
        binaryFileController.handleFileChunk(chunk, headerAccessor, principal);

        // Assert
        verify(messagingTemplate).convertAndSendToUser(
            eq("testuser"),
            eq("/queue/errors"),
            any(BinaryFileController.FileUploadError.class)
        );
    }

    private FileChunk createTestChunk() {
        return FileChunk.builder()
            .chatRoomId(1L)
            .chunkIndex(1)
            .totalChunks(2)
            .fileName("test.txt")
            .contentType("text/plain")
            .data("SGVsbG8gV29ybGQ=") // "Hello World" in Base64
            .fileSize(11L)
            .build();
    }

    private User createTestUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        return user;
    }

    private ChatRoom createTestChatRoom() {
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setId(1L);
        chatRoom.setName("Test Room");
        return chatRoom;
    }

    private Message createTestMessage(User user, ChatRoom chatRoom) {
        Message message = new Message();
        message.setId(1L);
        message.setSender(user);
        message.setChatRoom(chatRoom);
        message.setContent("test.txt");
        message.setContentType("text/plain");
        message.setAttachmentUrl("/path/to/file.txt");
        message.setSentAt(LocalDateTime.now());
        return message;
    }
}
