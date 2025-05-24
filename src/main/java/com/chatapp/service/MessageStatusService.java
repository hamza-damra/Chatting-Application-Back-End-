package com.chatapp.service;

import com.chatapp.model.Message;
import com.chatapp.model.MessageStatus;
import com.chatapp.model.User;
import com.chatapp.repository.MessageStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageStatusService {

    private final MessageStatusRepository messageStatusRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public MessageStatus createOrUpdateMessageStatus(Message message, User user, MessageStatus.Status status) {
        Optional<MessageStatus> existingStatus = messageStatusRepository.findByMessageAndUser(message, user);
        
        if (existingStatus.isPresent()) {
            MessageStatus messageStatus = existingStatus.get();
            // Only update if the new status is "higher" than the current one
            if (isStatusUpgrade(messageStatus.getStatus(), status)) {
                messageStatus.setStatus(status);
                messageStatus.setUpdatedAt(LocalDateTime.now());
                return messageStatusRepository.save(messageStatus);
            }
            return messageStatus;
        } else {
            MessageStatus messageStatus = MessageStatus.builder()
                .message(message)
                .user(user)
                .status(status)
                .build();
            return messageStatusRepository.save(messageStatus);
        }
    }

    @Transactional(readOnly = true)
    public List<MessageStatus> getMessageStatusesByMessage(Message message) {
        return messageStatusRepository.findByMessage(message);
    }

    @Transactional(readOnly = true)
    public MessageStatus.Status getMessageStatusForUser(Message message, User user) {
        Optional<MessageStatus> status = messageStatusRepository.findByMessageAndUser(message, user);
        return status.map(MessageStatus::getStatus).orElse(null);
    }

    @Transactional
    public void notifyMessageStatusChange(Message message, User user, MessageStatus.Status status) {
        // Create or update the status in the database
        MessageStatus messageStatus = createOrUpdateMessageStatus(message, user, status);
        
        // Send notification to the message sender
        com.chatapp.websocket.MessageStatusResponse response = com.chatapp.websocket.MessageStatusResponse.builder()
            .messageId(message.getId())
            .userId(user.getId())
            .status(status)
            .timestamp(messageStatus.getUpdatedAt())
            .build();
            
        messagingTemplate.convertAndSendToUser(
            message.getSender().getUsername(),
            "/queue/status",
            response
        );
        
        log.debug("Notified status change: Message {} is {} for user {}", 
                 message.getId(), status, user.getUsername());
    }
    
    private boolean isStatusUpgrade(MessageStatus.Status currentStatus, MessageStatus.Status newStatus) {
        // Define the hierarchy: SENT < DELIVERED < READ
        if (currentStatus == MessageStatus.Status.READ) {
            return false; // READ is the highest status, no upgrade possible
        }
        
        if (currentStatus == MessageStatus.Status.DELIVERED && newStatus == MessageStatus.Status.READ) {
            return true;
        }
        
        if (currentStatus == MessageStatus.Status.SENT && 
            (newStatus == MessageStatus.Status.DELIVERED || newStatus == MessageStatus.Status.READ)) {
            return true;
        }
        
        return false;
    }
}
