package com.chatapp.service;

import com.chatapp.dto.MessageResponse;
import com.chatapp.dto.UserResponse;
import com.chatapp.dto.MessageStatusResponse;
import com.chatapp.model.Message;
import com.chatapp.model.MessageStatus;
import com.chatapp.model.User;
import com.chatapp.repository.MessageStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for converting entities to DTOs
 * This service helps break circular dependencies between other services
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DtoConverterService {

    private final UserService userService;
    private final MessageStatusRepository messageStatusRepository;

    /**
     * Converts a Message entity to a MessageResponse DTO
     * @param message The message entity to convert
     * @return The converted MessageResponse DTO
     */
    public MessageResponse convertToMessageResponse(Message message) {
        return convertToMessageResponse(message, null);
    }

    /**
     * Converts a Message entity to a MessageResponse DTO with optional current user context
     * @param message The message entity to convert
     * @param currentUser The current user (for status context), can be null
     * @return The converted MessageResponse DTO
     */
    public MessageResponse convertToMessageResponse(Message message, User currentUser) {
        // Get message status for current user if provided
        MessageStatus.Status userStatus = null;
        if (currentUser != null) {
            userStatus = messageStatusRepository.findByMessageAndUser(message, currentUser)
                .map(MessageStatus::getStatus)
                .orElse(null);
        }

        // Get all message statuses for this message (for sender's view)
        List<MessageStatusResponse> messageStatuses = null;
        if (currentUser != null && message.getSender().getId().equals(currentUser.getId())) {
            // Only include detailed statuses if current user is the sender
            messageStatuses = messageStatusRepository.findByMessage(message).stream()
                .map(this::convertToMessageStatusResponse)
                .collect(Collectors.toList());
        }

        // Generate proper download URL for files (only if message has an attachment)
        String downloadUrl = null;
        if (message.getAttachmentUrl() != null && !message.getAttachmentUrl().isEmpty() &&
            !message.getAttachmentUrl().trim().isEmpty()) {
            downloadUrl = "/api/files/message/" + message.getId();
        }

        return MessageResponse.builder()
            .id(message.getId())
            .content(message.getContent())
            .contentType(message.getContentType())
            .attachmentUrl(message.getAttachmentUrl())
            .downloadUrl(downloadUrl)
            .sender(userService.convertToUserResponse(message.getSender()))
            .chatRoomId(message.getChatRoom().getId())
            .sentAt(message.getSentAt())
            .status(userStatus)
            .messageStatuses(messageStatuses)
            .build();
    }

    /**
     * Converts a MessageStatus entity to a MessageStatusResponse DTO
     * @param messageStatus The message status entity to convert
     * @return The converted MessageStatusResponse DTO
     */
    private MessageStatusResponse convertToMessageStatusResponse(MessageStatus messageStatus) {
        return MessageStatusResponse.builder()
            .messageId(messageStatus.getMessage().getId())
            .userId(messageStatus.getUser().getId())
            .username(messageStatus.getUser().getUsername())
            .status(messageStatus.getStatus())
            .build();
    }

    /**
     * Batch converts multiple Message entities to MessageResponse DTOs
     * This method optimizes database queries by fetching all message statuses in batches
     * @param messages The list of message entities to convert
     * @param currentUser The current user (for status context), can be null
     * @return The list of converted MessageResponse DTOs
     */
    public List<MessageResponse> convertToMessageResponses(List<Message> messages, User currentUser) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }

        // Batch fetch message statuses to avoid N+1 queries
        Map<Long, MessageStatus.Status> userStatuses = new HashMap<>();
        Map<Long, List<MessageStatusResponse>> messageStatuses = new HashMap<>();

        if (currentUser != null) {
            // Fetch user's statuses for all messages in one query
            List<MessageStatus> userMessageStatuses = messageStatusRepository.findByMessagesAndUser(messages, currentUser);
            userStatuses = userMessageStatuses.stream()
                .collect(Collectors.toMap(
                    ms -> ms.getMessage().getId(),
                    MessageStatus::getStatus
                ));

            // Fetch all message statuses for messages sent by current user
            List<Message> userSentMessages = messages.stream()
                .filter(m -> m.getSender().getId().equals(currentUser.getId()))
                .collect(Collectors.toList());

            if (!userSentMessages.isEmpty()) {
                List<MessageStatus> allStatuses = messageStatusRepository.findByMessages(userSentMessages);
                messageStatuses = allStatuses.stream()
                    .collect(Collectors.groupingBy(
                        ms -> ms.getMessage().getId(),
                        Collectors.mapping(this::convertToMessageStatusResponse, Collectors.toList())
                    ));
            }
        }

        // Convert messages to responses using the pre-fetched data
        final Map<Long, MessageStatus.Status> finalUserStatuses = userStatuses;
        final Map<Long, List<MessageStatusResponse>> finalMessageStatuses = messageStatuses;

        return messages.stream()
            .map(message -> {
                // Generate proper download URL for files (only if message has an attachment)
                String downloadUrl = null;
                if (message.getAttachmentUrl() != null && !message.getAttachmentUrl().isEmpty() &&
                    !message.getAttachmentUrl().trim().isEmpty()) {
                    downloadUrl = "/api/files/message/" + message.getId();
                }

                return MessageResponse.builder()
                    .id(message.getId())
                    .content(message.getContent())
                    .contentType(message.getContentType())
                    .attachmentUrl(message.getAttachmentUrl())
                    .downloadUrl(downloadUrl)
                    .sender(userService.convertToUserResponse(message.getSender()))
                    .chatRoomId(message.getChatRoom().getId())
                    .sentAt(message.getSentAt())
                    .status(finalUserStatuses.get(message.getId()))
                    .messageStatuses(finalMessageStatuses.get(message.getId()))
                    .build();
            })
            .collect(Collectors.toList());
    }

    /**
     * Converts a User entity to a UserResponse DTO
     * @param user The user entity to convert
     * @return The converted UserResponse DTO
     */
    public UserResponse convertToUserResponse(User user) {
        return userService.convertToUserResponse(user);
    }
}
