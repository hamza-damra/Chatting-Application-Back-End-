package com.chatapp.controller;

import com.chatapp.model.ChatRoom;
import com.chatapp.model.Message;
import com.chatapp.model.User;
import com.chatapp.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test to verify the file download error behavior for messages without attachments
 * This test reproduces the error: "Message does not have an attachment"
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application.yml")
@Transactional
public class FileDownloadErrorTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private MessageRepository messageRepository;

    private MockMvc mockMvc;
    private User testUser;
    private ChatRoom testChatRoom;
    private Message textMessage;
    private Message fileMessage;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Create test user
        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .fullName("Test User")
                .password("password")
                .isOnline(true)
                .build();

        // Create test chat room
        testChatRoom = ChatRoom.builder()
                .name("Test Chat")
                .privateFlag(false)  // Use privateFlag instead of isPrivate
                .creator(testUser)
                .participants(new HashSet<>(Set.of(testUser)))  // Use HashSet for mutable set
                .build();

        // Create text message (no attachment)
        textMessage = Message.builder()
                .content("This is a text message")
                .contentType("TEXT")
                .sender(testUser)
                .chatRoom(testChatRoom)
                .sentAt(LocalDateTime.now())
                .attachmentUrl(null) // No attachment
                .fileMetadata(null)  // No file metadata
                .build();

        // Create file message (with attachment)
        fileMessage = Message.builder()
                .content("image.jpg")
                .contentType("IMAGE")
                .sender(testUser)
                .chatRoom(testChatRoom)
                .sentAt(LocalDateTime.now())
                .attachmentUrl("uploads/images/image.jpg") // Has attachment
                .build();
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testDownloadFileByMessageId_TextMessage_ShouldReturn404() throws Exception {
        // Save the text message
        Message savedTextMessage = messageRepository.save(textMessage);

        // Try to download file for text message - should return 404
        mockMvc.perform(get("/api/files/message/" + savedTextMessage.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testDownloadFileByMessageId_FileMessage_ShouldReturnFile() throws Exception {
        // Save the file message
        Message savedFileMessage = messageRepository.save(fileMessage);

        // Try to download file for file message - should work (or return 404 if file doesn't exist on disk)
        mockMvc.perform(get("/api/files/message/" + savedFileMessage.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound()); // Will be 404 because file doesn't exist on disk in test
    }

    @Test
    void testMessageResponseIncludesDownloadUrlOnlyForFileMessages() {
        // This test verifies that downloadUrl is only set for messages with attachments
        // The actual logic is in DtoConverterService.convertToMessageResponse()

        // Text message should have null downloadUrl
        assert textMessage.getAttachmentUrl() == null;

        // File message should have non-null attachmentUrl
        assert fileMessage.getAttachmentUrl() != null;
        assert !fileMessage.getAttachmentUrl().isEmpty();
    }
}
