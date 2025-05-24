package com.chatapp.websocket.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a chunk of a file being uploaded via WebSocket
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileChunk {
    private Long messageId;      // ID to associate chunks with a specific message (null for new messages)
    private Long chatRoomId;     // Chat room where the file is being sent
    private Integer chunkIndex;  // Current chunk index (1-based)
    private Integer totalChunks; // Total number of chunks for this file
    private String fileName;     // Original file name
    private String contentType;  // MIME type of the file
    private String data;         // Base64 encoded chunk data
    private Long fileSize;       // Total file size in bytes
    private String uploadId;     // Unique ID for this upload (generated on first chunk)

    /**
     * Recommended maximum size for a chunk in bytes before base64 encoding
     * This is to prevent WebSocket message size issues
     */
    public static final int RECOMMENDED_CHUNK_SIZE = 64 * 1024; // 64KB
}
