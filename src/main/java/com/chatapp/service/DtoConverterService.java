package com.chatapp.service;

import com.chatapp.dto.MessageResponse;
import com.chatapp.dto.UserResponse;
import com.chatapp.model.Message;
import com.chatapp.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Service for converting entities to DTOs
 * This service helps break circular dependencies between other services
 */
@Service
@RequiredArgsConstructor
public class DtoConverterService {

    private final UserService userService;

    /**
     * Converts a Message entity to a MessageResponse DTO
     * @param message The message entity to convert
     * @return The converted MessageResponse DTO
     */
    public MessageResponse convertToMessageResponse(Message message) {
        return MessageResponse.builder()
            .id(message.getId())
            .content(message.getContent())
            .contentType(message.getContentType())
            .attachmentUrl(message.getAttachmentUrl())
            .sender(userService.convertToUserResponse(message.getSender()))
            .chatRoomId(message.getChatRoom().getId())
            .sentAt(message.getSentAt())
            .build();
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
