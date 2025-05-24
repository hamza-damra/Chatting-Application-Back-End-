package com.chatapp.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Metadata for a file upload in progress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadMetadata {
    private String fileName;     // Original file name
    private String contentType;  // MIME type of the file
    private Integer totalChunks; // Total number of chunks for this file
    private Long fileSize;       // Total file size in bytes
    private Long chatRoomId;     // Chat room where the file is being sent
    private Long userId;         // User who is uploading the file
    private Long messageId;      // ID of the message being updated (null for new messages)
}
