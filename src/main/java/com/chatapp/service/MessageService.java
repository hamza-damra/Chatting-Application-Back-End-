package com.chatapp.service;

import com.chatapp.dto.MessageRequest;
import com.chatapp.dto.MessageResponse;
import com.chatapp.dto.PagedResponse;
import com.chatapp.exception.ChatRoomAccessDeniedException;
import com.chatapp.exception.InvalidMessageStatusException;
import com.chatapp.exception.MessageTooLongException;
import com.chatapp.exception.ResourceNotFoundException;
import com.chatapp.model.ChatRoom;
import com.chatapp.model.Message;
import com.chatapp.model.MessageStatus;
import com.chatapp.model.User;
import com.chatapp.repository.ChatRoomRepository;
import com.chatapp.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserService userService;
    private final ChatRoomRepository chatRoomRepository;
    private final MessageStatusService messageStatusService;
    private final DtoConverterService dtoConverterService;
    private final UserBlockingService userBlockingService;

    /**
     * Get message by ID without access control - INTERNAL USE ONLY
     * This method should only be used internally where access control is handled separately
     * For external access, use getMessageByIdSecure() instead
     */
    @Transactional(readOnly = true)
    public Message getMessageById(Long id) {
        return messageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found with id: " + id));
    }

    /**
     * Get message by ID with access control verification
     * This method ensures the current user is a participant in the chat room containing the message
     */
    @Transactional(readOnly = true)
    public Message getMessageByIdSecure(Long id) {
        Message message = getMessageById(id);
        User currentUser = userService.getCurrentUser();

        // Verify user is a participant in the chat room containing this message
        if (!message.getChatRoom().getParticipants().contains(currentUser)) {
            throw new ChatRoomAccessDeniedException("You are not a participant in the chat room containing this message");
        }

        return message;
    }

    @Transactional(readOnly = true)
    public MessageResponse getMessageResponseById(Long id) {
        // Use the secure version that already includes access control
        Message message = getMessageByIdSecure(id);
        return convertToMessageResponse(message);
    }

    @Transactional(readOnly = true)
    public PagedResponse<MessageResponse> getChatRoomMessages(Long chatRoomId, int page, int size) {
        User currentUser = userService.getCurrentUser();
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
            .orElseThrow(() -> new ResourceNotFoundException("Chat room not found with id: " + chatRoomId));

        // Verify user is a participant in the chat room
        if (!chatRoom.getParticipants().contains(currentUser)) {
            throw new ChatRoomAccessDeniedException("User is not a participant in this chat room");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("sentAt").descending());
        Page<Message> messages = messageRepository.findByChatRoomOrderBySentAtDesc(chatRoom, pageable);

        List<MessageResponse> content = messages.getContent().stream()
                .map(this::convertToMessageResponse)
                .collect(Collectors.toList());

        return new PagedResponse<>(
            content,
            messages.getNumber(),
            messages.getSize(),
            messages.getTotalElements(),
            messages.getTotalPages(),
            messages.isLast()
        );
    }

    @Transactional
    public Message saveMessage(User sender, ChatRoom chatRoom, String content, String contentType) {
        Message message = Message.builder()
                .content(content)
                .sender(sender)
                .chatRoom(chatRoom)
                .contentType(contentType)
                .build();

        Message savedMessage = messageRepository.save(message);

        // Create SENT status for the sender
        messageStatusService.createOrUpdateMessageStatus(savedMessage, sender, MessageStatus.Status.SENT);

        return savedMessage;
    }

    @Transactional
    public MessageResponse sendMessage(Long chatRoomId, MessageRequest request) {
        User currentUser = userService.getCurrentUser();
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
            .orElseThrow(() -> new ResourceNotFoundException("Chat room not found with id: " + chatRoomId));

        // Verify user is a participant in the chat room
        if (!chatRoom.getParticipants().contains(currentUser)) {
            throw new ChatRoomAccessDeniedException("User is not a participant in this chat room");
        }

        // Check if user is blocked from sending messages to any participant
        for (User participant : chatRoom.getParticipants()) {
            if (!participant.getId().equals(currentUser.getId()) &&
                !userBlockingService.canSendMessageTo(participant)) {
                throw new ChatRoomAccessDeniedException("Cannot send message due to blocking restrictions");
            }
        }

        // Check message length (assuming a reasonable limit of 10000 characters)
        if (request.getContent() != null && request.getContent().length() > 10000) {
            throw new MessageTooLongException("Message content exceeds the maximum allowed length of 10000 characters");
        }

        Message message = saveMessage(
            currentUser,
            chatRoom,
            request.getContent(),
            request.getContentType()
        );

        return convertToMessageResponse(message);
    }

    @Transactional
    public void deleteMessage(Long id) {
        User currentUser = userService.getCurrentUser();
        // Use secure version to ensure user has access to the message
        Message message = getMessageByIdSecure(id);

        // Verify user is the sender of the message
        if (!message.getSender().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("User is not the sender of this message");
        }

        messageRepository.delete(message);
    }

    @Transactional
    public void markMessageAsRead(Long id) {
        User currentUser = userService.getCurrentUser();
        // Use secure version - it already includes participant verification
        Message message = getMessageByIdSecure(id);

        // Don't mark your own messages as read
        if (!message.getSender().getId().equals(currentUser.getId())) {
            messageStatusService.notifyMessageStatusChange(message, currentUser, MessageStatus.Status.READ);
        } else {
            throw new InvalidMessageStatusException("Cannot mark your own messages as read");
        }
    }

    @Transactional
    public void markAllMessagesAsRead(Long chatRoomId) {
        User currentUser = userService.getCurrentUser();
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
            .orElseThrow(() -> new ResourceNotFoundException("Chat room not found with id: " + chatRoomId));

        // Verify user is a participant in the chat room
        if (!chatRoom.getParticipants().contains(currentUser)) {
            throw new ChatRoomAccessDeniedException("User is not a participant in this chat room");
        }

        // Get all messages in the chat room not sent by the current user
        List<Message> messages = messageRepository.findByChatRoomOrderBySentAtDesc(chatRoom);

        boolean messagesMarked = false;
        for (Message message : messages) {
            // Don't mark your own messages as read
            if (!message.getSender().getId().equals(currentUser.getId())) {
                messageStatusService.notifyMessageStatusChange(message, currentUser, MessageStatus.Status.READ);
                messagesMarked = true;
            }
        }

        if (!messagesMarked) {
            log.info("No messages to mark as read for user {} in chat room {}", currentUser.getUsername(), chatRoomId);
        }
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getUnreadMessages() {
        User currentUser = userService.getCurrentUser();
        List<ChatRoom> chatRooms = chatRoomRepository.findByParticipantsContaining(currentUser);

        return chatRooms.stream()
            .flatMap(chatRoom -> messageRepository.findByChatRoomOrderBySentAtDesc(chatRoom).stream())
            .filter(message -> !message.getSender().getId().equals(currentUser.getId()))
            .filter(message -> {
                MessageStatus.Status status = messageStatusService.getMessageStatusForUser(message, currentUser);
                return status == null || status != MessageStatus.Status.READ;
            })
            .map(this::convertToMessageResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public void updateMessageStatus(Message message, User user, MessageStatus.Status status) {
        messageStatusService.notifyMessageStatusChange(message, user, status);
    }

    /**
     * Get the status of a message for a specific user
     * @param message The message to check
     * @param user The user to check the status for
     * @return The status of the message for the user, or null if no status exists
     */
    @Transactional(readOnly = true)
    public MessageStatus.Status getMessageStatusForUser(Message message, User user) {
        return messageStatusService.getMessageStatusForUser(message, user);
    }

    @Transactional
    public void updateMessageStatus(Long messageId, MessageStatus.Status status) {
        User currentUser = userService.getCurrentUser();
        // Use secure version - it already includes participant verification
        Message message = getMessageByIdSecure(messageId);

        // Don't update status for your own messages
        if (message.getSender().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Cannot update status for your own messages");
        }

        // Validate status
        if (status == null) {
            throw new InvalidMessageStatusException("Message status cannot be null");
        }

        messageStatusService.notifyMessageStatusChange(message, currentUser, status);
    }

    /**
     * Converts a Message entity to a MessageResponse DTO
     * @param message The message entity to convert
     * @return The converted MessageResponse DTO
     */
    public MessageResponse convertToMessageResponse(Message message) {
        return dtoConverterService.convertToMessageResponse(message);
    }
}
