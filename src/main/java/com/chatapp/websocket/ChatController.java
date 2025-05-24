package com.chatapp.websocket;

import com.chatapp.exception.DuplicateChatRoomException;
import com.chatapp.exception.InvalidMessageStatusException;
import com.chatapp.exception.MessageTooLongException;
import com.chatapp.model.ChatRoom;
import com.chatapp.model.Message;
import com.chatapp.model.MessageStatus;
import com.chatapp.model.User;
import com.chatapp.repository.ChatRoomRepository;
import com.chatapp.repository.MessageRepository;
import com.chatapp.repository.MessageStatusRepository;
import com.chatapp.repository.UserRepository;
import com.chatapp.service.ChatRoomService;
import com.chatapp.service.MessageService;
import com.chatapp.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;
    private final UserService userService;
    private final ChatRoomService chatRoomService;
    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;
    private final MessageStatusRepository messageStatusRepository;



    /**
     * Handles messages sent to a chat room
     */
    @MessageMapping("/chat.sendMessage/{roomId}")
    @Transactional
    public void sendMessage(@DestinationVariable Long roomId,
                           @Payload ChatMessageRequest chatMessageRequest,
                           Principal principal) {
        log.info("WEBSOCKET: Received message for room {}: {}", roomId, chatMessageRequest);
        log.info("WEBSOCKET: Principal: {}", principal);

        // Require authentication for sending messages
        if (principal == null) {
            log.error("WEBSOCKET: Unauthorized access - authentication required for sending messages");
            throw new org.springframework.security.access.AccessDeniedException("Authentication required");
        }

        log.info("WEBSOCKET: Authenticated user: {}", principal.getName());
        User sender = userService.getUserByUsername(principal.getName());

        // Get the chat room
        log.info("WEBSOCKET: Finding chat room with ID: {}", roomId);
        ChatRoom chatRoom = chatRoomService.getChatRoomById(roomId);
        log.info("WEBSOCKET: Found chat room: {}", chatRoom.getName());

        // Verify that the user is a participant in the chat room
        if (!chatRoom.getParticipants().contains(sender)) {
            log.error("WEBSOCKET: User {} is not a participant in chat room {}",
                    sender.getUsername(), chatRoom.getName());
            throw new com.chatapp.exception.ChatRoomAccessDeniedException(
                    "You are not a participant in this chat room");
        }

        // Check message length (assuming a reasonable limit of 10000 characters)
        if (chatMessageRequest.getContent() != null && chatMessageRequest.getContent().length() > 10000) {
            throw new MessageTooLongException("Message content exceeds the maximum allowed length of 10000 characters");
        }

        // Save the message to the database
        log.info("WEBSOCKET: Saving message to database. Sender: {}, ChatRoom: {}, Content: {}",
            sender.getUsername(), chatRoom.getName(), chatMessageRequest.getContent());
        Message message;
        try {
            // Make sure the user is saved first
            sender = userRepository.save(sender);
            log.info("WEBSOCKET: Saved/updated user: {}", sender);

            // Make sure the chat room is saved
            chatRoom = chatRoomRepository.save(chatRoom);
            log.info("WEBSOCKET: Saved/updated chat room: {}", chatRoom);

            // Check if the message content looks like a file path that might not exist
            String content = chatMessageRequest.getContent();
            String attachmentUrl = null;
            if (content != null && (content.startsWith("uploads/") || content.contains("auto_generated"))) {
                log.error("WEBSOCKET: ❌ INVALID FILE MESSAGE - Client sent file path instead of proper file upload: {}", content);
                log.error("WEBSOCKET: ❌ The client should upload files via REST API (/api/files/upload) first, then send the file URL/metadata");
                log.error("WEBSOCKET: ❌ Current message will be rejected to prevent invalid file references");

                // Send error response back to the client
                ChatMessageResponse errorResponse = ChatMessageResponse.builder()
                    .id(-1L)
                    .senderId(sender.getId())
                    .senderName("System")
                    .content("❌ File upload error: Please use the proper file upload system. Upload files via the REST API first, then send the file URL.")
                    .contentType("error")
                    .timestamp(LocalDateTime.now())
                    .status(MessageStatus.Status.SENT)
                    .build();

                // Send error to the sender
                messagingTemplate.convertAndSendToUser(
                    sender.getUsername(),
                    "/queue/errors",
                    errorResponse
                );

                // Also send to the general topic so all participants see the error
                messagingTemplate.convertAndSend(
                    "/topic/chatrooms/" + roomId,
                    errorResponse
                );

                log.error("WEBSOCKET: ❌ Sent error message to client about improper file upload");
                return; // Stop processing this invalid message
            }

            // Create message directly instead of using the service
            message = Message.builder()
                .content(content)
                .contentType(chatMessageRequest.getContentType())
                .attachmentUrl(attachmentUrl)
                .sender(sender)
                .chatRoom(chatRoom)
                .sentAt(LocalDateTime.now())
                .build();

            // Save directly using repository
            message = messageRepository.save(message);
            log.info("WEBSOCKET: Message saved successfully with ID: {}", message.getId());

            // Create SENT status for the sender
            MessageStatus messageStatus = MessageStatus.builder()
                .message(message)
                .user(sender)
                .status(MessageStatus.Status.SENT)
                .build();

            // Save the message status
            messageStatus = messageStatusRepository.save(messageStatus);
            log.info("WEBSOCKET: Created message status: {}", messageStatus);
        } catch (Exception e) {
            log.error("WEBSOCKET: Failed to save message to database", e);
            throw e;
        }

        // Create response with message details
        ChatMessageResponse response = ChatMessageResponse.builder()
            .id(message.getId())
            .senderId(sender.getId())
            .senderName(sender.getUsername())
            .content(message.getContent())
            .contentType(message.getContentType())
            .timestamp(message.getSentAt())
            .status(MessageStatus.Status.SENT)
            .build();

        // Store the final sender reference for use in lambda
        final User finalSender = sender;

        // Log the message that will be sent
        log.info("WEBSOCKET: Preparing to send message with ID: {} to participants in room: {}",
                message.getId(), chatRoom.getName());
        log.info("WEBSOCKET: Message content: {}", response.getContent());
        log.info("WEBSOCKET: Total participants in room: {}", chatRoom.getParticipants().size());

        // First, try sending to the general topic for the chat room
        // This is for clients that might be subscribed to the general topic
        messagingTemplate.convertAndSend(
            "/topic/chatrooms/" + roomId,
            response
        );
        log.info("WEBSOCKET: Sent message to general topic: /topic/chatrooms/{}", roomId);

        // Then, send the message individually to each participant except the sender
        // This ensures delivery even if they're not subscribed to the general topic
        chatRoom.getParticipants().forEach(participant -> {
            if (participant != null && !participant.equals(finalSender) && participant.getUsername() != null) {
                String username = participant.getUsername();
                log.info("WEBSOCKET: Sending message to participant: {}", username);

                try {
                    // Send to the user's personal topic for this chat room
                    // Note: Spring will automatically prepend the user destination prefix (/user/)
                    messagingTemplate.convertAndSendToUser(
                        username,
                        "/topic/chatrooms/" + roomId,
                        response
                    );
                    log.info("WEBSOCKET: Sent message to user-specific topic: /user/{}/topic/chatrooms/{}",
                            username, roomId);

                    // Also send to user's queue for delivery receipt
                    messagingTemplate.convertAndSendToUser(
                        username,
                        "/queue/messages",
                        response
                    );
                    log.info("WEBSOCKET: Sent message to user queue: /user/{}/queue/messages", username);
                } catch (Exception e) {
                    log.error("WEBSOCKET: Failed to send message to user: {}", username, e);
                }
            }
        });
    }

    /**
     * Handles message status updates (delivered, read)
     */
    @MessageMapping("/chat.updateStatus")
    public void updateMessageStatus(@Payload MessageStatusRequest statusRequest,
                                   Principal principal) {
        log.debug("Updating message status: {}", statusRequest);

        if (statusRequest.getMessageId() == null) {
            throw new InvalidMessageStatusException("Message ID cannot be null");
        }

        if (statusRequest.getStatus() == null) {
            throw new InvalidMessageStatusException("Status cannot be null");
        }

        // Require authentication for updating message status
        if (principal == null) {
            log.error("WEBSOCKET: Unauthorized access - authentication required for updating message status");
            throw new org.springframework.security.access.AccessDeniedException("Authentication required");
        }

        User user = userService.getUserByUsername(principal.getName());
        log.debug("WEBSOCKET: Authenticated user: {}", user.getUsername());

        Message message;
        try {
            message = messageService.getMessageById(statusRequest.getMessageId());
        } catch (Exception e) {
            log.warn("WEBSOCKET: Message not found for status update: {}", statusRequest.getMessageId());
            return; // Skip processing if message doesn't exist
        }

        // Verify that the user is a participant in the chat room
        if (!message.getChatRoom().getParticipants().contains(user)) {
            log.error("WEBSOCKET: User {} is not a participant in chat room {}",
                    user.getUsername(), message.getChatRoom().getName());
            throw new com.chatapp.exception.ChatRoomAccessDeniedException(
                    "You are not a participant in this chat room");
        }

        // Verify that the user is not updating their own message status
        if (message.getSender().getId().equals(user.getId())) {
            log.error("WEBSOCKET: User {} attempted to update status of their own message",
                    user.getUsername());
            throw new InvalidMessageStatusException("Cannot update status for your own messages");
        }

        // Update message status in database
        messageService.updateMessageStatus(message, user, statusRequest.getStatus());

        // Notify the sender about the status update
        MessageStatusResponse response = MessageStatusResponse.builder()
            .messageId(message.getId())
            .userId(user.getId())
            .status(statusRequest.getStatus())
            .timestamp(java.time.LocalDateTime.now())
            .build();

        // Log the status update that will be sent
        log.info("WEBSOCKET: Preparing to send message status update. Message ID: {}, Status: {}",
                message.getId(), statusRequest.getStatus());

        try {
            if (message.getSender() != null && message.getSender().getUsername() != null) {
                String senderUsername = message.getSender().getUsername();
                log.info("WEBSOCKET: Sending status update to message sender: {}", senderUsername);

                // Send to the sender's status queue
                messagingTemplate.convertAndSendToUser(
                    senderUsername,
                    "/queue/status",
                    response
                );
                log.info("WEBSOCKET: Sent status update to user queue: /user/{}/queue/status", senderUsername);
            } else {
                log.error("WEBSOCKET: Cannot send status update - message sender is null or has no username");
            }
        } catch (Exception e) {
            log.error("WEBSOCKET: Failed to send message status update", e);
        }
    }

    /**
     * Handles user joining a chat room (legacy endpoint)
     */
    @MessageMapping("/chat.addUser")
    public void addUser(@Payload ChatEventRequest eventRequest,
                       SimpMessageHeaderAccessor headerAccessor,
                       Principal principal) {
        log.info("WEBSOCKET: Received request to add user to room {}", eventRequest.getRoomId());

        // Require authentication for joining chat rooms
        if (principal == null) {
            log.error("WEBSOCKET: Unauthorized access - authentication required for joining chat rooms");
            throw new org.springframework.security.access.AccessDeniedException("Authentication required");
        }

        // Check if roomId is null
        if (eventRequest.getRoomId() == null) {
            log.error("WEBSOCKET: Room ID is null in chat.addUser request");
            throw new IllegalArgumentException("Room ID cannot be null");
        } else {
            // Forward to the new endpoint implementation
            joinRoom(eventRequest.getRoomId(), headerAccessor, principal);
        }
    }

    /**
     * Handles user joining a chat room
     */
    @MessageMapping("/chat.join/{roomId}")
    public void joinRoom(@DestinationVariable Long roomId,
                        SimpMessageHeaderAccessor headerAccessor,
                        Principal principal) {
        // Require authentication for joining chat rooms
        if (principal == null) {
            log.error("WEBSOCKET: Unauthorized access - authentication required for joining chat rooms");
            throw new org.springframework.security.access.AccessDeniedException("Authentication required");
        }

        log.debug("WEBSOCKET: User {} joining room {}", principal.getName(), roomId);
        User user = userService.getUserByUsername(principal.getName());

        // Get the chat room
        ChatRoom chatRoom = chatRoomService.getChatRoomById(roomId);
        log.debug("WEBSOCKET: Found chat room: {}", chatRoom.getName());

        // Verify that the user is a participant in the chat room
        if (!chatRoom.getParticipants().contains(user)) {
            log.error("WEBSOCKET: User {} is not a participant in chat room {}",
                    user.getUsername(), chatRoom.getName());
            throw new com.chatapp.exception.ChatRoomAccessDeniedException(
                    "You are not a participant in this chat room");
        }

        // Add username to websocket session
        if (headerAccessor != null) {
            var sessionAttributes = headerAccessor.getSessionAttributes();
            if (sessionAttributes != null) {
                if (user != null && user.getUsername() != null) {
                    sessionAttributes.put("username", user.getUsername());
                }
                if (roomId != null) {
                    sessionAttributes.put("roomId", roomId);
                }
            }
        }

        // Check if user and chatRoom are not null before proceeding
        if (user != null && chatRoom != null) {
            // Notify others in the room
            ChatEventResponse response = ChatEventResponse.builder()
                .type(ChatEventResponse.EventType.JOIN)
                .userId(user.getId())
                .username(user.getUsername())
                .roomId(roomId)
                .timestamp(java.time.LocalDateTime.now())
                .build();

            // Log the join event that will be sent
            log.info("WEBSOCKET: Preparing to send join event for user: {} to room: {}",
                    user.getUsername(), chatRoom.getName());
            log.info("WEBSOCKET: Total participants in room: {}", chatRoom.getParticipants().size());

            // First, send to the general topic for the chat room
            // This is for clients that might be subscribed to the general topic
            messagingTemplate.convertAndSend(
                "/topic/chatrooms/" + roomId,
                response
            );
            log.info("WEBSOCKET: Sent join event to general topic: /topic/chatrooms/{}", roomId);

            // Then, send individually to each participant except the joining user
            final User joiningUser = user;
            chatRoom.getParticipants().forEach(participant -> {
                if (participant != null && !participant.equals(joiningUser) && participant.getUsername() != null) {
                    String username = participant.getUsername();
                    log.info("WEBSOCKET: Sending join event to participant: {}", username);

                    try {
                        // Send to the user's personal topic for this chat room
                        messagingTemplate.convertAndSendToUser(
                            username,
                            "/topic/chatrooms/" + roomId,
                            response
                        );
                        log.info("WEBSOCKET: Sent join event to user-specific topic: /user/{}/topic/chatrooms/{}",
                                username, roomId);
                    } catch (Exception e) {
                        log.error("WEBSOCKET: Failed to send join event to user: {}", username, e);
                    }
                }
            });
        } else {
            log.error("WEBSOCKET: Cannot notify participants - user or chatRoom is null. User: {}, ChatRoom: {}",
                     user != null ? user.getUsername() : "null",
                     chatRoom != null ? chatRoom.getId() : "null");
        }
    }

    /**
     * Handles chat room creation
     */
    @MessageMapping("/chat.createRoom")
    public void createChatRoom(@Payload ChatRoomRequest chatRoomRequest, Principal principal) {
        log.info("WEBSOCKET: Received request to create chat room: {}", chatRoomRequest);

        // Require authentication for creating chat rooms
        if (principal == null) {
            log.error("WEBSOCKET: Unauthorized access - authentication required for creating chat rooms");
            throw new org.springframework.security.access.AccessDeniedException("Authentication required");
        }

        log.info("WEBSOCKET: Authenticated user: {}", principal.getName());
        User user = userService.getUserByUsername(principal.getName());

        try {
            // Convert WebSocket request to service request
            com.chatapp.dto.ChatRoomRequest serviceRequest = com.chatapp.dto.ChatRoomRequest.builder()
                .name(chatRoomRequest.getName())
                .isPrivate(chatRoomRequest.isPrivate())
                .participantIds(chatRoomRequest.getParticipantIds())
                .build();

            // Use the service to create the chat room (this will check for duplicates)
            com.chatapp.dto.ChatRoomResponse chatRoomResponse = chatRoomService.createChatRoom(serviceRequest);
            log.info("WEBSOCKET: Chat room created successfully: {}", chatRoomResponse.getName());

            // Create response
            ChatEventResponse response = ChatEventResponse.builder()
                .type(ChatEventResponse.EventType.JOIN)
                .userId(user.getId())
                .username(user.getUsername())
                .roomId(chatRoomResponse.getId())
                .timestamp(java.time.LocalDateTime.now())
                .build();

            // Log the chat room creation event
            log.info("WEBSOCKET: Preparing to send chat room creation notification for room: {}",
                    chatRoomResponse.getName());
            log.info("WEBSOCKET: Total participants in room: {}", chatRoomResponse.getParticipants().size());

            try {
                // Send confirmation to the creator
                messagingTemplate.convertAndSendToUser(
                    user.getUsername(),
                    "/queue/chatrooms",
                    chatRoomResponse
                );
                log.info("WEBSOCKET: Sent chat room creation confirmation to creator: {}", user.getUsername());

                // Send to the general topic for the chat room
                messagingTemplate.convertAndSend(
                    "/topic/chatrooms/" + chatRoomResponse.getId(),
                    response
                );
                log.info("WEBSOCKET: Sent join event to general topic: /topic/chatrooms/{}", chatRoomResponse.getId());

                // Notify all participants about the new chat room
                chatRoomResponse.getParticipants().forEach(participant -> {
                    if (participant != null && participant.getUsername() != null &&
                        !participant.getId().equals(user.getId())) {

                        String username = participant.getUsername();
                        log.info("WEBSOCKET: Sending chat room creation notification to participant: {}", username);

                        try {
                            // Send chat room info to user's queue
                            messagingTemplate.convertAndSendToUser(
                                username,
                                "/queue/chatrooms",
                                chatRoomResponse
                            );
                            log.info("WEBSOCKET: Sent chat room info to user queue: /user/{}/queue/chatrooms", username);

                            // Also send a join event to the participant's topic
                            messagingTemplate.convertAndSendToUser(
                                username,
                                "/topic/chatrooms/" + chatRoomResponse.getId(),
                                response
                            );
                            log.info("WEBSOCKET: Sent join event to user-specific topic: /user/{}/topic/chatrooms/{}",
                                    username, chatRoomResponse.getId());
                        } catch (Exception e) {
                            log.error("WEBSOCKET: Failed to send chat room notification to user: {}", username, e);
                        }
                    }
                });
            } catch (Exception e) {
                log.error("WEBSOCKET: Error sending chat room creation notifications", e);
            }

        } catch (DuplicateChatRoomException ex) {
            log.warn("WEBSOCKET: Attempt to create duplicate chat room: {}", ex.getMessage());
            throw ex; // Let the exception handler deal with it
        } catch (Exception ex) {
            log.error("WEBSOCKET: Error creating chat room", ex);
            throw ex;
        }
    }
}
