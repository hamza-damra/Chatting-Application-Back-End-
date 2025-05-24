package com.chatapp.controller;

import com.chatapp.model.ChatRoom;
import com.chatapp.model.Message;
import com.chatapp.model.MessageStatus;
import com.chatapp.model.User;
import com.chatapp.repository.ChatRoomRepository;
import com.chatapp.repository.MessageRepository;
import com.chatapp.repository.MessageStatusRepository;
import com.chatapp.repository.UserRepository;
import com.chatapp.websocket.ChatMessageRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class TestController {

    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;
    private final MessageStatusRepository messageStatusRepository;

    private static final String TEST_USERNAME = "websocket_test_user";

    @PostMapping("/create-message/{roomId}")
    @Transactional
    public ResponseEntity<Map<String, Object>> createTestMessage(
            @PathVariable Long roomId,
            @RequestBody ChatMessageRequest request) {

        log.info("Creating test message in room {}: {}", roomId, request);

        try {

        // Get or create test user
        User user = getOrCreateTestUser();
        // Make sure the user is saved
        user = userRepository.save(user);
        log.info("TEST-API: Saved/updated user: {}", user);

        // Get or create chat room
        ChatRoom chatRoom = getOrCreateChatRoom(roomId, user);
        // Make sure the chat room is saved
        chatRoom = chatRoomRepository.save(chatRoom);
        log.info("TEST-API: Saved/updated chat room: {}", chatRoom);

        // Create message
        Message message = Message.builder()
                .content(request.getContent())
                .contentType(request.getContentType())
                .sender(user)
                .chatRoom(chatRoom)
                .sentAt(LocalDateTime.now())
                .build();

        message = messageRepository.save(message);
        log.info("Created message: {}", message);

        // Create message status
        MessageStatus messageStatus = MessageStatus.builder()
                .message(message)
                .user(user)
                .status(MessageStatus.Status.SENT)
                .build();

        messageStatus = messageStatusRepository.save(messageStatus);
        log.info("Created message status: {}", messageStatus);

        Map<String, Object> response = new HashMap<>();
        response.put("messageId", message.getId());
        response.put("content", message.getContent());
        response.put("sender", user.getUsername());
        response.put("chatRoomId", chatRoom.getId());
        response.put("timestamp", message.getSentAt());

        return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error creating test message", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    private User getOrCreateTestUser() {
        try {
            // First try to find by username
            User user = userRepository.findByUsername(TEST_USERNAME).orElse(null);

            if (user != null) {
                log.debug("Found existing test user by username: {}", TEST_USERNAME);
                return user;
            }

            // If not found by username, try to find by email to avoid constraint violation
            user = userRepository.findByEmail("test@example.com").orElse(null);

            if (user != null) {
                log.debug("Found existing test user by email: test@example.com");
                return user;
            }

            log.debug("Creating new test user: {}", TEST_USERNAME);
            // Create a test user if it doesn't exist
            user = User.builder()
                    .username(TEST_USERNAME)
                    .email("test@example.com")
                    .password("password")
                    .isOnline(true)
                    .roles(new HashSet<>(Set.of("ROLE_USER")))
                    .build();

            // Save the user using the repository directly
            return userRepository.save(user);
        } catch (Exception e) {
            log.error("Error creating test user", e);
            // If we get here, something went wrong, but we still need a user
            // Try one more time to find the user
            return userRepository.findByUsername(TEST_USERNAME)
                .orElseThrow(() -> new RuntimeException("Failed to create or find test user", e));
        }
    }

    private ChatRoom getOrCreateChatRoom(Long roomId, User user) {
        return chatRoomRepository.findById(roomId)
                .orElseGet(() -> {
                    ChatRoom newChatRoom = ChatRoom.builder()
                            .name("Test Chat Room " + roomId)
                            .isPrivate(false)
                            .creator(user)
                            .build();
                    newChatRoom.addParticipant(user);
                    return chatRoomRepository.save(newChatRoom);
                });
    }
}
