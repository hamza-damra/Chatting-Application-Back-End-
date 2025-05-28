package com.chatapp.service;

import com.chatapp.dto.ChatRoomRequest;
import com.chatapp.dto.ChatRoomResponse;
import com.chatapp.dto.UserResponse;
import com.chatapp.exception.BadRequestException;
import com.chatapp.exception.DuplicateChatRoomException;
import com.chatapp.exception.ResourceNotFoundException;
import com.chatapp.exception.UnauthorizedException;
import com.chatapp.model.ChatRoom;
import com.chatapp.model.Message;
import com.chatapp.model.MessageStatus;
import com.chatapp.model.User;
import com.chatapp.repository.ChatRoomRepository;
import com.chatapp.repository.MessageRepository;
import com.chatapp.repository.MessageStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;
    private final MessageStatusRepository messageStatusRepository;
    private final UserService userService;
    private final DtoConverterService dtoConverterService;

    /**
     * Check if a user is a participant in a chat room
     * This method is provided for backward compatibility with tests
     * @param user The user to check
     * @param chatRoom The chat room to check
     * @return true if the user is a participant, false otherwise
     */
    public boolean isUserInChatRoom(User user, ChatRoom chatRoom) {
        return chatRoom.getParticipants().contains(user);
    }

    /**
     * Add a user to a chat room
     * This method is provided for backward compatibility with tests
     * @param user The user to add
     * @param chatRoom The chat room to add the user to
     */
    public void addUserToChatRoom(User user, ChatRoom chatRoom) {
        chatRoom.addParticipant(user);
        chatRoomRepository.save(chatRoom);
    }

    /**
     * Get chat room by ID without access control - INTERNAL USE ONLY
     * This method should only be used internally where access control is handled separately
     * For external access, use getChatRoomByIdSecure() instead
     */
    @Transactional(readOnly = true)
    public ChatRoom getChatRoomById(Long id) {
        return chatRoomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chat room not found with id: " + id));
    }

    /**
     * Get chat room by ID with access control verification
     * This method ensures the current user is a participant in the chat room
     */
    @Transactional(readOnly = true)
    public ChatRoom getChatRoomByIdSecure(Long id) {
        ChatRoom chatRoom = getChatRoomById(id);
        User currentUser = userService.getCurrentUser();

        // Verify user is a participant in the chat room
        if (!chatRoom.getParticipants().contains(currentUser)) {
            throw new UnauthorizedException("You are not a participant in this chat room");
        }

        return chatRoom;
    }

    @Transactional(readOnly = true)
    public ChatRoomResponse getChatRoomResponseById(Long id) {
        // Use the secure version that already includes access control
        ChatRoom chatRoom = getChatRoomByIdSecure(id);
        return convertToChatRoomResponse(chatRoom);
    }

    @Transactional(readOnly = true)
    public List<ChatRoom> getUserChatRooms(User user) {
        return chatRoomRepository.findByParticipantsContaining(user);
    }

    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getCurrentUserChatRooms() {
        User currentUser = userService.getCurrentUser();
        List<ChatRoom> chatRooms = getUserChatRooms(currentUser);

        return chatRooms.stream()
                .map(this::convertToChatRoomResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ChatRoomResponse createChatRoom(ChatRoomRequest request) {
        User currentUser = userService.getCurrentUser();

        log.info("SERVICE: Creating chat room - name: {}, isPrivate: {}, participantIds: {}",
                request.getName(), request.isPrivate(), request.getParticipantIds());

        // Check for duplicate private chat rooms
        if (request.isPrivate() && request.getParticipantIds() != null && request.getParticipantIds().size() == 1) {
            // This is a private chat with exactly one other user
            Long otherUserId = request.getParticipantIds().get(0);
            User otherUser = userService.getUserById(otherUserId);

            // Check if a private chat already exists between these users
            Optional<ChatRoom> existingChatRoom = chatRoomRepository.findPrivateChatBetween(currentUser, otherUser);

            if (existingChatRoom.isPresent()) {
                log.warn("Attempt to create duplicate private chat between {} and {}",
                        currentUser.getUsername(), otherUser.getUsername());
                throw new DuplicateChatRoomException(
                        "A private chat already exists between you and " + otherUser.getUsername());
            }
        }

        log.debug("Creating chat room with isPrivate: {}", request.isPrivate());

        ChatRoom chatRoom = ChatRoom.builder()
                .name(request.getName())
                .privateFlag(request.isPrivate())
                .creator(currentUser)
                .participants(new HashSet<>())
                .build();

        log.debug("ChatRoom built with isPrivate: {}", chatRoom.isPrivate());

        // Additional debug: manually set the field to ensure it's set correctly
        chatRoom.setPrivate(request.isPrivate());
        log.debug("ChatRoom after manual setPrivate with isPrivate: {}", chatRoom.isPrivate());

        chatRoom.addParticipant(currentUser);

        // Add other participants if provided
        if (request.getParticipantIds() != null && !request.getParticipantIds().isEmpty()) {
            for (Long participantId : request.getParticipantIds()) {
                User participant = userService.getUserById(participantId);
                chatRoom.addParticipant(participant);
            }
        }

        log.debug("ChatRoom before save with isPrivate: {}", chatRoom.isPrivate());
        ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);
        log.debug("ChatRoom saved with isPrivate: {}", savedChatRoom.isPrivate());
        log.info("Chat room created: {} by user: {}", savedChatRoom.getName(), currentUser.getUsername());
        log.info("SERVICE: Saved ChatRoom entity - id: {}, name: {}, isPrivate: {}",
                savedChatRoom.getId(), savedChatRoom.getName(), savedChatRoom.isPrivate());

        // Additional debug: check if the saved entity has the correct value
        ChatRoom reloadedChatRoom = chatRoomRepository.findById(savedChatRoom.getId()).orElse(null);
        if (reloadedChatRoom != null) {
            log.debug("ChatRoom reloaded from DB with isPrivate: {}", reloadedChatRoom.isPrivate());
        }

        return convertToChatRoomResponse(savedChatRoom);
    }

    @Transactional
    public ChatRoomResponse createOrGetPrivateChat(Long userId) {
        User currentUser = userService.getCurrentUser();
        User otherUser = userService.getUserById(userId);

        // Check if private chat already exists between these users
        Optional<ChatRoom> existingChatRoom = chatRoomRepository.findPrivateChatBetween(currentUser, otherUser);

        if (existingChatRoom.isPresent()) {
            log.info("Found existing private chat between {} and {}",
                    currentUser.getUsername(), otherUser.getUsername());
            return convertToChatRoomResponse(existingChatRoom.get());
        }

        // Create new private chat
        ChatRoom chatRoom = ChatRoom.builder()
                .name(currentUser.getUsername() + " & " + otherUser.getUsername())
                .privateFlag(true)
                .creator(currentUser)
                .participants(new HashSet<>())
                .build();

        chatRoom.addParticipant(currentUser);
        chatRoom.addParticipant(otherUser);

        ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);
        log.info("Private chat created between: {} and {}", currentUser.getUsername(), otherUser.getUsername());

        return convertToChatRoomResponse(savedChatRoom);
    }

    @Transactional
    public ChatRoomResponse updateChatRoom(Long id, ChatRoomRequest request) {
        User currentUser = userService.getCurrentUser();
        ChatRoom chatRoom = getChatRoomById(id);

        // Verify user is the creator of the chat room
        if (!chatRoom.getCreator().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("Only the creator can update this chat room");
        }

        // Update chat room properties
        chatRoom.setName(request.getName());
        chatRoom.setPrivate(request.isPrivate());

        ChatRoom updatedChatRoom = chatRoomRepository.save(chatRoom);
        log.info("Chat room updated: {}", updatedChatRoom.getName());

        return convertToChatRoomResponse(updatedChatRoom);
    }

    @Transactional
    public void deleteChatRoom(Long id) {
        User currentUser = userService.getCurrentUser();
        ChatRoom chatRoom = getChatRoomById(id);

        // Verify user is the creator of the chat room
        if (!chatRoom.getCreator().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("Only the creator can delete this chat room");
        }

        chatRoomRepository.delete(chatRoom);
        log.info("Chat room deleted: {}", chatRoom.getName());
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getChatRoomParticipants(Long id) {
        User currentUser = userService.getCurrentUser();
        ChatRoom chatRoom = getChatRoomById(id);

        // Verify user is a participant in the chat room
        if (!chatRoom.getParticipants().contains(currentUser)) {
            throw new UnauthorizedException("You are not a participant in this chat room");
        }

        return chatRoom.getParticipants().stream()
                .map(userService::convertToUserResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void addParticipant(Long chatRoomId, Long userId) {
        User currentUser = userService.getCurrentUser();
        ChatRoom chatRoom = getChatRoomById(chatRoomId);
        User userToAdd = userService.getUserById(userId);

        // Verify user is the creator of the chat room
        if (!chatRoom.getCreator().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("Only the creator can add participants");
        }

        // Check if user is already a participant
        if (chatRoom.getParticipants().contains(userToAdd)) {
            throw new BadRequestException("User is already a participant in this chat room");
        }

        chatRoom.addParticipant(userToAdd);
        chatRoomRepository.save(chatRoom);
        log.info("User {} added to chat room: {}", userToAdd.getUsername(), chatRoom.getName());
    }

    @Transactional
    public void removeParticipant(Long chatRoomId, Long userId) {
        User currentUser = userService.getCurrentUser();
        ChatRoom chatRoom = getChatRoomById(chatRoomId);
        User userToRemove = userService.getUserById(userId);

        // Verify user is the creator of the chat room or removing themselves
        if (!chatRoom.getCreator().getId().equals(currentUser.getId()) &&
            !currentUser.getId().equals(userId)) {
            throw new UnauthorizedException("You don't have permission to remove this participant");
        }

        // Check if user is a participant
        if (!chatRoom.getParticipants().contains(userToRemove)) {
            throw new BadRequestException("User is not a participant in this chat room");
        }

        // Don't allow removing the creator unless they're removing themselves
        if (chatRoom.getCreator().getId().equals(userToRemove.getId()) &&
            !currentUser.getId().equals(userId)) {
            throw new BadRequestException("Cannot remove the creator from the chat room");
        }

        chatRoom.removeParticipant(userToRemove);
        chatRoomRepository.save(chatRoom);
        log.info("User {} removed from chat room: {}", userToRemove.getUsername(), chatRoom.getName());
    }

    private ChatRoomResponse convertToChatRoomResponse(ChatRoom chatRoom) {
        // Get the current user for unread count
        User currentUser = userService.getCurrentUser();

        // Find the most recent message in the chat room
        List<Message> recentMessages = messageRepository.findMostRecentMessagesInChatRoom(
                chatRoom, PageRequest.of(0, 1));

        // Convert the message to a response if it exists (with current user context)
        com.chatapp.dto.MessageResponse lastMessage = null;
        String lastMessageContent = null;
        String lastMessageSender = null;
        LocalDateTime lastMessageTime = null;

        if (!recentMessages.isEmpty()) {
            Message recentMessage = recentMessages.get(0);
            lastMessage = dtoConverterService.convertToMessageResponse(recentMessage, currentUser);

            // Extract flat fields for Flutter compatibility
            lastMessageContent = recentMessage.getContent();
            lastMessageSender = recentMessage.getSender().getFullName();
            lastMessageTime = recentMessage.getSentAt();
        }

        // Count unread messages for the current user using optimized batch query
        int unreadCount = 0;
        List<Message> messages = messageRepository.findByChatRoomOrderBySentAtDesc(chatRoom);

        // Filter out messages sent by current user
        List<Message> messagesFromOthers = messages.stream()
            .filter(message -> !message.getSender().getId().equals(currentUser.getId()))
            .collect(Collectors.toList());

        if (!messagesFromOthers.isEmpty()) {
            // Batch fetch message statuses to avoid N+1 queries
            List<MessageStatus> userStatuses = messageStatusRepository.findByMessagesAndUser(messagesFromOthers, currentUser);
            Set<Long> readMessageIds = userStatuses.stream()
                .filter(ms -> ms.getStatus() == MessageStatus.Status.READ)
                .map(ms -> ms.getMessage().getId())
                .collect(Collectors.toSet());

            // Count messages that are not read
            unreadCount = (int) messagesFromOthers.stream()
                .filter(message -> !readMessageIds.contains(message.getId()))
                .count();
        }

        log.debug("Converting ChatRoom to response - ChatRoom.isPrivate: {}", chatRoom.isPrivate());

        ChatRoomResponse response = ChatRoomResponse.builder()
                .id(chatRoom.getId())
                .name(chatRoom.getName())
                .isPrivate(chatRoom.isPrivate())
                .createdAt(chatRoom.getCreatedAt())
                .updatedAt(chatRoom.getUpdatedAt())
                .creator(userService.convertToUserResponse(chatRoom.getCreator()))
                .participants(chatRoom.getParticipants().stream()
                        .map(userService::convertToUserResponse)
                        .collect(Collectors.toList()))
                .lastMessage(lastMessage)
                .unreadCount(unreadCount)
                // Add flat fields for Flutter compatibility
                .lastMessageContent(lastMessageContent)
                .lastMessageSender(lastMessageSender)
                .lastMessageTime(lastMessageTime)
                .build();

        log.debug("ChatRoomResponse built with isPrivate: {}", response.isPrivate());
        return response;
    }
}
