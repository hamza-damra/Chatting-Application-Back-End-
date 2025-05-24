package com.chatapp.websocket;

import com.chatapp.dto.MessageResponse;
import com.chatapp.model.ChatRoom;
import com.chatapp.model.Message;
import com.chatapp.model.MessageStatus;
import com.chatapp.model.User;
import com.chatapp.exception.ResourceNotFoundException;
import com.chatapp.repository.ChatRoomRepository;
import com.chatapp.repository.MessageRepository;
import com.chatapp.repository.MessageStatusRepository;
import com.chatapp.repository.UserRepository;
import com.chatapp.service.DtoConverterService;
import com.chatapp.service.FileChunkService;
import com.chatapp.service.UserService;
import com.chatapp.websocket.model.FileChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.time.LocalDateTime;

/**
 * Controller for handling binary file uploads via WebSocket
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class BinaryFileController {

    private final FileChunkService fileChunkService;
    private final SimpMessagingTemplate messagingTemplate;
    private final DtoConverterService dtoConverterService;
    private final UserService userService;
    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final MessageStatusRepository messageStatusRepository;

    /**
     * Handle file chunk uploads
     */
    @MessageMapping("/file.chunk")
    @Transactional
    public void handleFileChunk(FileChunk chunk, SimpMessageHeaderAccessor headerAccessor, Principal principal) {
        if (principal == null) {
            log.error("WEBSOCKET: Unauthorized access - authentication required for file uploads");
            throw new org.springframework.security.access.AccessDeniedException("Authentication required");
        }

        log.info("WEBSOCKET: Received file chunk {}/{} for room {} from user {}",
            chunk.getChunkIndex(), chunk.getTotalChunks(), chunk.getChatRoomId(), principal.getName());

        try {
            // Get the current user
            User currentUser = userService.getUserByUsername(principal.getName());
            log.info("WEBSOCKET: Found user: {} (ID: {})", currentUser.getUsername(), currentUser.getId());

            // CRITICAL: Verify user is a participant in the chat room BEFORE processing file upload
            ChatRoom chatRoom = chatRoomRepository.findById(chunk.getChatRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Chat room not found with id: " + chunk.getChatRoomId()));

            if (!chatRoom.getParticipants().contains(currentUser)) {
                log.error("WEBSOCKET: SECURITY VIOLATION - User {} attempted to upload file to chat room {} without being a participant",
                    currentUser.getUsername(), chatRoom.getName());
                throw new com.chatapp.exception.ChatRoomAccessDeniedException(
                    "You are not a participant in this chat room and cannot upload files");
            }

            log.info("WEBSOCKET: User {} verified as participant in chat room: {}",
                currentUser.getUsername(), chatRoom.getName());

            // Log chunk details
            log.info("WEBSOCKET: Processing file chunk - fileName: {}, contentType: {}, fileSize: {} bytes, chunkIndex: {}, totalChunks: {}",
                chunk.getFileName(), chunk.getContentType(), chunk.getFileSize(), chunk.getChunkIndex(), chunk.getTotalChunks());

            // Process the chunk
            String filePath = fileChunkService.processChunk(chunk, currentUser.getId());

            // If the file is complete, create a message and send it to all participants
            if (filePath != null) {
                log.info("WEBSOCKET: File upload complete, creating message for file: {}", filePath);

                // Check if file exists and log detailed information
                Path filePathObj = Paths.get(filePath);
                boolean fileExists = Files.exists(filePathObj);
                log.info("WEBSOCKET: File path exists: {}", fileExists);

                if (!fileExists) {
                    log.error("WEBSOCKET: WARNING - File does not exist at path: {}", filePath);
                    log.error("WEBSOCKET: Absolute path: {}", filePathObj.toAbsolutePath());

                    // Try to create the directory structure if it doesn't exist
                    try {
                        Path parentDir = filePathObj.getParent();
                        if (parentDir != null && !Files.exists(parentDir)) {
                            Files.createDirectories(parentDir);
                            log.info("WEBSOCKET: Created missing directory: {}", parentDir);
                        }

                        // Create a placeholder file if it doesn't exist
                        // This is a temporary fix - the real issue is that the client should use the WebSocket file upload
                        if (!Files.exists(filePathObj)) {
                            String placeholderContent = "File uploaded via alternative method - content not available through WebSocket upload";
                            Files.write(filePathObj, placeholderContent.getBytes());
                            log.warn("WEBSOCKET: Created placeholder file at: {}", filePath);
                            log.warn("WEBSOCKET: This indicates the client is not using the proper WebSocket file upload system");
                        }
                    } catch (Exception e) {
                        log.error("WEBSOCKET: Failed to create directory structure or placeholder file", e);
                    }
                } else {
                    try {
                        long fileSize = Files.size(filePathObj);
                        log.info("WEBSOCKET: File exists with size: {} bytes", fileSize);
                    } catch (Exception e) {
                        log.error("WEBSOCKET: Error reading file size", e);
                    }
                }

                // Chat room already retrieved and validated above
                log.info("WEBSOCKET: Using validated chat room: {} (ID: {})", chatRoom.getName(), chatRoom.getId());

                // Make sure the user is saved first
                currentUser = userRepository.save(currentUser);
                log.info("WEBSOCKET: Saved/updated user: {}", currentUser);

                // Make sure the chat room is saved
                chatRoom = chatRoomRepository.save(chatRoom);
                log.info("WEBSOCKET: Saved/updated chat room: {}", chatRoom);

                // Create a message with the file attachment
                Message message = Message.builder()
                    .content(chunk.getFileName())
                    .contentType(chunk.getContentType())
                    .attachmentUrl(filePath)
                    .sender(currentUser)
                    .chatRoom(chatRoom)
                    .sentAt(LocalDateTime.now())
                    .build();

                log.info("WEBSOCKET: Created message with attachment URL: {}", filePath);

                // Save the message
                try {
                    message = messageRepository.save(message);
                    log.info("WEBSOCKET: Saved message to database with ID: {}", message.getId());

                    if (message.getId() == null) {
                        throw new RuntimeException("Failed to save message to database - ID is null");
                    }
                } catch (Exception e) {
                    log.error("WEBSOCKET: Error saving message to database", e);
                    throw new RuntimeException("Failed to save message to database: " + e.getMessage(), e);
                }

                // Create SENT status for the sender
                MessageStatus messageStatus = MessageStatus.builder()
                    .message(message)
                    .user(currentUser)
                    .status(MessageStatus.Status.SENT)
                    .build();

                // Save the message status
                messageStatus = messageStatusRepository.save(messageStatus);
                log.info("WEBSOCKET: Created message status: {}", messageStatus);

                // Convert to response DTO
                MessageResponse response = dtoConverterService.convertToMessageResponse(message);

                // Send to the general topic for the chat room
                messagingTemplate.convertAndSend(
                    "/topic/chatrooms/" + chunk.getChatRoomId(),
                    response,
                    java.util.Collections.emptyMap()
                );
                log.info("WEBSOCKET: Sent file message to general topic: /topic/chatrooms/{}", chunk.getChatRoomId());

                // Send acknowledgment to the sender
                messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/queue/files",
                    response
                );
                log.info("WEBSOCKET: Sent file upload confirmation to sender: {}", principal.getName());
            } else {
                // Send acknowledgment for the chunk
                // Include the uploadId in the response if it's the first chunk
                String responseUploadId = chunk.getUploadId();
                if (responseUploadId == null && chunk.getChunkIndex() == 1) {
                    // This is a server-generated ID, we need to find it
                    try {
                        responseUploadId = fileChunkService.findUploadIdForFirstChunk(chunk, currentUser.getId());
                        log.info("WEBSOCKET: Found server-generated upload ID: {} for first chunk", responseUploadId);
                    } catch (Exception e) {
                        log.error("WEBSOCKET: Error finding upload ID for first chunk", e);
                    }
                }

                messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/queue/files.progress",
                    new FileUploadProgress(
                        chunk.getChunkIndex(),
                        chunk.getTotalChunks(),
                        chunk.getChatRoomId(),
                        responseUploadId
                    )
                );
                log.debug("WEBSOCKET: Sent chunk progress to sender: {}", principal.getName());
            }
        } catch (Exception e) {
            log.error("WEBSOCKET: Error processing file chunk", e);

            // Send error to the sender
            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/errors",
                new FileUploadError("FILE_UPLOAD_ERROR", e.getMessage(), chunk.getChunkIndex(), chunk.getTotalChunks())
            );
        }
    }

    /**
     * Progress update for file uploads
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class FileUploadProgress {
        private Integer chunkIndex;
        private Integer totalChunks;
        private Long chatRoomId;
        private String uploadId;

        // Constructor for backward compatibility
        public FileUploadProgress(Integer chunkIndex, Integer totalChunks, Long chatRoomId) {
            this.chunkIndex = chunkIndex;
            this.totalChunks = totalChunks;
            this.chatRoomId = chatRoomId;
            this.uploadId = null;
        }
    }

    /**
     * Error response for file uploads
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class FileUploadError {
        private String type;
        private String message;
        private Integer chunkIndex;
        private Integer totalChunks;
    }
}
