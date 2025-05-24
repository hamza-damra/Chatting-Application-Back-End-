package com.chatapp.service;

import com.chatapp.config.FileStorageProperties;
import com.chatapp.model.FileMetadata;
import com.chatapp.model.User;
import com.chatapp.repository.FileMetadataRepository;
import com.chatapp.repository.UserRepository;
import com.chatapp.websocket.model.FileChunk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enhanced service for handling chunked file uploads via WebSocket
 * with improved file type handling and duplicate detection
 */
@Service
@Slf4j
public class FileChunkService {

    private final String uploadDir;
    private final FileStorageProperties fileStorageProperties;
    private final FileMetadataService fileMetadataService;
    private final UserRepository userRepository;

    // In-memory storage for chunks being assembled
    // Key: unique upload ID, Value: map of chunk index to chunk data
    private final Map<String, Map<Integer, byte[]>> chunkStorage = new ConcurrentHashMap<>();

    // Metadata for uploads in progress
    private final Map<String, FileUploadMetadata> uploadMetadata = new ConcurrentHashMap<>();

    // Track when uploads were started to allow for cleanup of abandoned uploads
    private final Map<String, LocalDateTime> uploadStartTimes = new ConcurrentHashMap<>();

    public FileChunkService(FileStorageProperties fileStorageProperties, 
                           FileMetadataService fileMetadataService,
                           UserRepository userRepository,
                           FileMetadataRepository fileMetadataRepository) {
        this.fileStorageProperties = fileStorageProperties;
        this.fileMetadataService = fileMetadataService;
        this.userRepository = userRepository;
        this.uploadDir = fileStorageProperties.getUploadDir();
        
        log.info("=== FILE UPLOAD SYSTEM INITIALIZATION ===");
        log.info("Upload base directory: {}", this.uploadDir);

        // Validate and create upload directory
        try {
            Path path = Paths.get(this.uploadDir);
            log.info("Checking if upload directory exists: {}", path.toAbsolutePath());
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("Created upload directory: {}", path.toAbsolutePath());
            } else {
                log.info("Upload directory already exists: {}", path.toAbsolutePath());
            }

            if (!Files.isWritable(path)) {
                log.error("CRITICAL ERROR: Upload directory is not writable: {}", path.toAbsolutePath());
                throw new IllegalStateException("Upload directory is not writable: " + path.toAbsolutePath());
            } else {
                log.info("Upload directory is writable: {}", path.toAbsolutePath());
            }

            // Create subdirectories if they don't exist
            createSubdirectories();

            log.info("Upload directory validated: {}", this.uploadDir);
            log.info("Allowed content types: {}", fileStorageProperties.getAllowedContentTypes());
            log.info("Maximum file size: {} bytes", fileStorageProperties.getMaxFileSize());
            log.info("=== FILE UPLOAD SYSTEM READY ===");
        } catch (IOException e) {
            log.error("CRITICAL ERROR: Failed to create upload directory: {}", this.uploadDir, e);
            throw new IllegalStateException("Failed to create upload directory: " + this.uploadDir, e);
        }
    }

    /**
     * Create standard subdirectories for different file types
     */
    private void createSubdirectories() throws IOException {
        String[] subdirs = {"images", "documents", "audio", "video", "other", "temp"};
        for (String subdir : subdirs) {
            Path subdirPath = Paths.get(uploadDir, subdir);
            log.info("Checking subdirectory: {}", subdirPath.toAbsolutePath());
            if (!Files.exists(subdirPath)) {
                Files.createDirectories(subdirPath);
                log.info("Created subdirectory: {}", subdirPath.toAbsolutePath());
            } else {
                log.info("Subdirectory already exists: {}", subdirPath.toAbsolutePath());
            }
            
            // Verify the subdirectory is writable
            if (!Files.isWritable(subdirPath)) {
                log.error("CRITICAL ERROR: Subdirectory is not writable: {}", subdirPath.toAbsolutePath());
                throw new IllegalStateException("Subdirectory is not writable: " + subdirPath.toAbsolutePath());
            } else {
                log.info("Subdirectory is writable: {}", subdirPath.toAbsolutePath());
            }
        }
    }

    /**
     * Cleanup task that runs every hour to remove abandoned uploads
     * An upload is considered abandoned if it hasn't been updated in 1 hour
     */
    @Scheduled(fixedRate = 3600000) // Run every hour
    public void cleanupAbandonedUploads() {
        log.info("Running cleanup of abandoned uploads");
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);

        Iterator<Map.Entry<String, LocalDateTime>> iterator = uploadStartTimes.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, LocalDateTime> entry = iterator.next();
            if (entry.getValue().isBefore(oneHourAgo)) {
                String uploadId = entry.getKey();
                log.info("Removing abandoned upload: {}", uploadId);

                // Remove from all maps
                chunkStorage.remove(uploadId);
                uploadMetadata.remove(uploadId);
                iterator.remove();
            }
        }
    }

    /**
     * Process a file chunk and return a file path if the file is complete
     * @param chunk The file chunk to process
     * @param userId The ID of the user uploading the file
     * @return The file path if the file is complete, null otherwise
     * @throws IllegalArgumentException if the chunk is invalid
     * @throws IllegalStateException if the upload is not found or the file type is not allowed
     */
    @Transactional
    public String processChunk(FileChunk chunk, Long userId) {
        log.info("[processChunk] START: userId={}, fileName={}, contentType={}, totalChunks={}, chunkIndex={}, uploadId={}",
            userId, chunk.getFileName(), chunk.getContentType(), chunk.getTotalChunks(), chunk.getChunkIndex(), chunk.getUploadId());
        try {
        // Validate chunk
            log.debug("[processChunk] Validating chunk...");
        validateChunk(chunk);
            log.debug("[processChunk] Chunk validated");

        // Get or generate a unique ID for this upload
        String uploadId;

        // Check if the client provided an uploadId
        if (chunk.getUploadId() != null && !chunk.getUploadId().isEmpty()) {
            uploadId = chunk.getUploadId();
                log.info("[processChunk] Using client-provided upload ID: {}", uploadId);

            // If this is the first chunk, initialize the storage
            if (chunk.getChunkIndex() == 1) {
                    log.debug("[processChunk] First chunk for client-provided uploadId");
                // Validate content type with detailed logging
                String contentType = chunk.getContentType();
                    log.info("[processChunk] Validating content type: {}", contentType);
                    log.debug("[processChunk] Allowed content types: {}", fileStorageProperties.getAllowedContentTypes());
                
                if (!fileStorageProperties.isContentTypeAllowed(contentType)) {
                        log.error("[processChunk] Content type not allowed: {}. Allowed types are: {}", 
                        contentType, fileStorageProperties.getAllowedContentTypes());
                    throw new IllegalArgumentException("Content type not allowed: " + contentType + 
                        ". Please use one of the following types: " + 
                        String.join(", ", fileStorageProperties.getAllowedContentTypes()));
                }
                    log.info("[processChunk] Content type {} is allowed", contentType);

                // Validate file size
                if (chunk.getFileSize() > fileStorageProperties.getMaxFileSize()) {
                        log.error("[processChunk] File size exceeds maximum allowed: {} > {}",
                        chunk.getFileSize(), fileStorageProperties.getMaxFileSize());
                    throw new IllegalArgumentException("File size exceeds maximum allowed: " +
                        chunk.getFileSize() + " > " + fileStorageProperties.getMaxFileSize());
                }

                // Initialize storage for this upload
                chunkStorage.put(uploadId, new ConcurrentHashMap<>());
                    log.debug("[processChunk] Initialized chunk storage for uploadId: {}", uploadId);

                // Store metadata about this upload
                uploadMetadata.put(uploadId, FileUploadMetadata.builder()
                    .fileName(chunk.getFileName())
                    .contentType(chunk.getContentType())
                    .totalChunks(chunk.getTotalChunks())
                    .fileSize(chunk.getFileSize())
                    .chatRoomId(chunk.getChatRoomId())
                    .userId(userId)
                    .messageId(chunk.getMessageId())
                    .build());
                    log.debug("[processChunk] Stored upload metadata for uploadId: {}", uploadId);

                // Record start time for cleanup
                uploadStartTimes.put(uploadId, LocalDateTime.now());
                    log.debug("[processChunk] Recorded start time for uploadId: {}", uploadId);

                    log.info("[processChunk] Started new chunked upload with client ID: {} for file: {}, total chunks: {}",
                    uploadId, chunk.getFileName(), chunk.getTotalChunks());
            } else {
                // For subsequent chunks, verify the upload exists
                if (!uploadMetadata.containsKey(uploadId)) {
                        log.error("[processChunk] Upload ID not found: {}", uploadId);
                    throw new IllegalStateException("Upload not found. Please restart the upload.");
                }
                    log.debug("[processChunk] Continuing upload for uploadId: {}", uploadId);

                // Update the last activity time
                uploadStartTimes.put(uploadId, LocalDateTime.now());
            }
        } else {
            // Client didn't provide an uploadId, use the old approach
            if (chunk.getChunkIndex() == 1) {
                    log.debug("[processChunk] First chunk for server-generated uploadId");
                // Validate content type
                if (!fileStorageProperties.isContentTypeAllowed(chunk.getContentType())) {
                        log.error("[processChunk] Content type not allowed: {}", chunk.getContentType());
                    throw new IllegalArgumentException("Content type not allowed: " + chunk.getContentType());
                }

                // Validate file size
                if (chunk.getFileSize() > fileStorageProperties.getMaxFileSize()) {
                        log.error("[processChunk] File size exceeds maximum allowed: {} > {}",
                        chunk.getFileSize(), fileStorageProperties.getMaxFileSize());
                    throw new IllegalArgumentException("File size exceeds maximum allowed: " +
                        chunk.getFileSize() + " > " + fileStorageProperties.getMaxFileSize());
                }

                uploadId = UUID.randomUUID().toString();
                chunkStorage.put(uploadId, new ConcurrentHashMap<>());
                    log.debug("[processChunk] Initialized chunk storage for uploadId: {}", uploadId);

                // Store metadata about this upload
                uploadMetadata.put(uploadId, FileUploadMetadata.builder()
                    .fileName(chunk.getFileName())
                    .contentType(chunk.getContentType())
                    .totalChunks(chunk.getTotalChunks())
                    .fileSize(chunk.getFileSize())
                    .chatRoomId(chunk.getChatRoomId())
                    .userId(userId)
                    .messageId(chunk.getMessageId())
                    .build());
                    log.debug("[processChunk] Stored upload metadata for uploadId: {}", uploadId);

                // Record start time for cleanup
                uploadStartTimes.put(uploadId, LocalDateTime.now());
                    log.debug("[processChunk] Recorded start time for uploadId: {}", uploadId);

                    log.info("[processChunk] Started new chunked upload with server ID: {} for file: {}, total chunks: {}",
                    uploadId, chunk.getFileName(), chunk.getTotalChunks());
            } else {
                // For subsequent chunks, find the upload ID by matching metadata
                uploadId = findUploadId(chunk, userId);
                if (uploadId == null) {
                        log.error("[processChunk] Could not find upload ID for chunk: {}", chunk.getChunkIndex());
                    throw new IllegalStateException("Upload not found. Please restart the upload.");
                }
                    log.debug("[processChunk] Continuing upload for uploadId: {}", uploadId);

                // Update the last activity time
                uploadStartTimes.put(uploadId, LocalDateTime.now());
            }
        }

        // Store the chunk
        try {
                log.debug("[processChunk] Decoding and storing chunk...");
            // Log the first 20 characters of the base64 data for debugging
            String dataPreview = chunk.getData().length() > 20
                ? chunk.getData().substring(0, 20) + "..."
                : chunk.getData();
                log.info("[processChunk] Decoding base64 data (preview): {}", dataPreview);

            byte[] decodedData = Base64.getDecoder().decode(chunk.getData());
                log.info("[processChunk] Decoded chunk size: {} bytes", decodedData.length);

            // Calculate and store important metrics
            int totalChunks = uploadMetadata.get(uploadId).getTotalChunks();
            int currentChunk = chunk.getChunkIndex();
            
            // Add the chunk to storage
            chunkStorage.get(uploadId).put(chunk.getChunkIndex(), decodedData);
            int receivedChunks = chunkStorage.get(uploadId).size();
            long receivedBytes = calculateReceivedBytes(uploadId);
            double progress = (receivedChunks * 100.0) / totalChunks;
            double bytesProgress = 0;
            
            // Calculate bytes progress if we know the total file size
            long fileSize = uploadMetadata.get(uploadId).getFileSize();
            if (fileSize > 0) {
                bytesProgress = (receivedBytes * 100.0) / fileSize;
            }
            
            // Calculate transfer rate
            LocalDateTime startTime = uploadStartTimes.get(uploadId);
            double elapsedSeconds = startTime != null ? 
                    ChronoUnit.MILLIS.between(startTime, LocalDateTime.now()) / 1000.0 : 0.1;
            double transferRate = receivedBytes / elapsedSeconds; // bytes per second
            
            // Estimate time remaining
            double remainingBytes = fileSize - receivedBytes;
            double estimatedRemainingSeconds = (transferRate > 0 && remainingBytes > 0) ? 
                    remainingBytes / transferRate : 0;
            
            // Create progress bar visual [=====>    ]
            int progressBarLength = 20;
            int completedBars = (int) (progressBarLength * progress / 100);
            StringBuilder progressBar = new StringBuilder("[");
            for (int i = 0; i < progressBarLength; i++) {
                if (i < completedBars) {
                    progressBar.append("=");
                } else if (i == completedBars) {
                    progressBar.append(">");
                } else {
                    progressBar.append(" ");
                }
            }
            progressBar.append("]");
            
            // Format transfer rate nicely
            String transferRateStr;
            if (transferRate >= 1_000_000) { // MB/s
                transferRateStr = String.format("%.2f MB/s", transferRate / 1_000_000);
            } else if (transferRate >= 1_000) { // KB/s
                transferRateStr = String.format("%.2f KB/s", transferRate / 1_000);
            } else { // B/s
                transferRateStr = String.format("%.0f B/s", transferRate);
            }
            
            // Format time remaining nicely
            String timeRemainingStr = formatTimeRemaining(estimatedRemainingSeconds);
            
            // Log detailed progress information
            log.info("[processChunk] UPLOAD PROGRESS: {} {}% | File: {} | Chunks: {}/{} | Bytes: {}/{} ({}%) | Rate: {} | ETA: {} | ID: {}",
                progressBar.toString(),
                String.format("%.2f", progress),
                uploadMetadata.get(uploadId).getFileName(),
                receivedChunks, totalChunks,
                formatFileSize(receivedBytes), formatFileSize(fileSize), String.format("%.2f", bytesProgress),
                transferRateStr,
                timeRemainingStr,
                uploadId);
                
            // For first chunk, last chunk, and every 10% progress, log more detailed message
            if (currentChunk == 1 || currentChunk == totalChunks || receivedChunks % Math.max(1, totalChunks / 10) == 0) {
                log.info("[processChunk] Milestone reached for upload {}: {}% complete, {} of {} transferred at {}",
                    uploadId, String.format("%.2f", progress), formatFileSize(receivedBytes), formatFileSize(fileSize), transferRateStr);
            }
        } catch (IllegalArgumentException e) {
                log.error("[processChunk] Failed to decode base64 data: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid base64 data: " + e.getMessage());
        }

        // Check if we have all chunks
        FileUploadMetadata metadata = uploadMetadata.get(uploadId);
        if (chunkStorage.get(uploadId).size() == metadata.getTotalChunks()) {
                log.info("[processChunk] All chunks received for uploadId: {}. Proceeding to save file.", uploadId);
            try {
                // Verify all chunks are present
                for (int i = 1; i <= metadata.getTotalChunks(); i++) {
                    if (!chunkStorage.get(uploadId).containsKey(i)) {
                            log.error("[processChunk] Missing chunk {} for upload: {}", i, uploadId);
                        throw new IllegalStateException("Missing chunk: " + i);
                    }
                }

                // All chunks received, assemble the file
                    log.info("[processChunk] Calling saveCompleteFile for uploadId: {}", uploadId);
                String filePath = saveCompleteFile(uploadId);
                    log.info("[processChunk] File upload complete: {}", filePath);

                // Register the file in the metadata database
                try {
                        log.info("[processChunk] Registering file in metadata database: {}", filePath);
                    User user = userRepository.findById(metadata.getUserId())
                        .orElseThrow(() -> new IllegalStateException("User not found with ID: " + metadata.getUserId()));
                    FileMetadata fileMetadata = fileMetadataService.registerFile(
                        filePath, 
                        metadata.getFileName(), 
                        metadata.getContentType(), 
                        user
                    );
                        log.info("[processChunk] Registered file in metadata database: {}", fileMetadata);
                } catch (Exception e) {
                        log.error("[processChunk] Error registering file in metadata database", e);
                    // Continue with the upload process even if metadata registration fails
                }

                // Clean up
                    log.debug("[processChunk] Cleaning up uploadId: {}", uploadId);
                chunkStorage.remove(uploadId);
                uploadMetadata.remove(uploadId);
                uploadStartTimes.remove(uploadId);
                    log.debug("[processChunk] Cleanup complete for uploadId: {}", uploadId);

                    log.info("[processChunk] END: Returning file path: {}", filePath);
                return filePath;
            } catch (IOException e) {
                    log.error("[processChunk] Error saving file for uploadId: {}", uploadId, e);
                throw new RuntimeException("Error saving file: " + e.getMessage());
                } catch (Exception e) {
                    log.error("[processChunk] Unexpected error during file save for uploadId: {}", uploadId, e);
                    throw e;
                }
            }

        // Not all chunks received yet
            log.info("[processChunk] Not all chunks received yet for uploadId: {}. Returning null.", uploadId);
            log.info("[processChunk] END: Returning null");
            return null;
        } catch (Exception ex) {
            log.error("[processChunk] Exception in processChunk: userId={}, fileName={}, chunkIndex={}, uploadId={}, error={}",
                userId, chunk.getFileName(), chunk.getChunkIndex(), chunk.getUploadId(), ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Find the upload ID for a first chunk based on file metadata
     * @param chunk The first chunk of the file
     * @param userId The ID of the user uploading the file
     * @return The upload ID if found, null otherwise
     */
    public String findUploadIdForFirstChunk(FileChunk chunk, Long userId) {
        if (chunk == null || chunk.getChunkIndex() != 1) {
            log.error("Invalid chunk for finding upload ID: {}", chunk);
            return null;
        }

        // Search through all uploads to find a match
        for (Map.Entry<String, FileUploadMetadata> entry : uploadMetadata.entrySet()) {
            String uploadId = entry.getKey();
            FileUploadMetadata metadata = entry.getValue();

            // Check if this upload matches the chunk metadata
            if (metadata.getFileName().equals(chunk.getFileName()) &&
                metadata.getContentType().equals(chunk.getContentType()) &&
                metadata.getTotalChunks() == chunk.getTotalChunks() &&
                metadata.getFileSize() == chunk.getFileSize() &&
                metadata.getChatRoomId().equals(chunk.getChatRoomId()) &&
                metadata.getUserId().equals(userId)) {
                
                log.info("Found matching upload ID: {} for first chunk", uploadId);
                return uploadId;
            }
        }

        log.warn("Could not find matching upload ID for first chunk: {}", chunk.getFileName());
        return null;
    }

    /**
     * Validate a file chunk
     * @param chunk The chunk to validate
     * @throws IllegalArgumentException if the chunk is invalid
     */
    private void validateChunk(FileChunk chunk) {
        if (chunk == null) {
            throw new IllegalArgumentException("Chunk cannot be null");
        }
        if (chunk.getChatRoomId() == null) {
            throw new IllegalArgumentException("Chat room ID cannot be null");
        }
        if (chunk.getChunkIndex() == null || chunk.getChunkIndex() < 1) {
            throw new IllegalArgumentException("Chunk index must be positive");
        }
        if (chunk.getTotalChunks() == null || chunk.getTotalChunks() < 1) {
            throw new IllegalArgumentException("Total chunks must be positive");
        }
        if (chunk.getChunkIndex() > chunk.getTotalChunks()) {
            throw new IllegalArgumentException("Chunk index cannot be greater than total chunks");
        }
        if (chunk.getFileName() == null || chunk.getFileName().isEmpty()) {
            throw new IllegalArgumentException("File name cannot be empty");
        }
        if (chunk.getContentType() == null || chunk.getContentType().isEmpty()) {
            throw new IllegalArgumentException("Content type cannot be empty");
        }
        if (chunk.getData() == null || chunk.getData().isEmpty()) {
            throw new IllegalArgumentException("Chunk data cannot be empty");
        }
        if (chunk.getFileSize() == null || chunk.getFileSize() < 1) {
            throw new IllegalArgumentException("File size must be positive");
        }

        // If uploadId is provided, it must not be empty
        if (chunk.getUploadId() != null && chunk.getUploadId().isEmpty()) {
            throw new IllegalArgumentException("Upload ID cannot be empty");
        }
    }

    /**
     * Find the upload ID for a chunk by matching metadata
     * @param chunk The chunk to find the upload ID for
     * @param userId The ID of the user uploading the file
     * @return The upload ID if found, null otherwise
     */
    private String findUploadId(FileChunk chunk, Long userId) {
        // Search through all uploads to find a match
        for (Map.Entry<String, FileUploadMetadata> entry : uploadMetadata.entrySet()) {
            String uploadId = entry.getKey();
            FileUploadMetadata metadata = entry.getValue();

            // Check if this upload matches the chunk metadata
            if (metadata.getFileName().equals(chunk.getFileName()) &&
                metadata.getContentType().equals(chunk.getContentType()) &&
                metadata.getTotalChunks() == chunk.getTotalChunks() &&
                metadata.getFileSize() == chunk.getFileSize() &&
                metadata.getChatRoomId().equals(chunk.getChatRoomId()) &&
                metadata.getUserId().equals(userId)) {
                
                log.info("Found matching upload ID: {} for chunk: {}", uploadId, chunk.getChunkIndex());
                return uploadId;
            }
        }

        log.warn("Could not find matching upload ID for chunk: {}", chunk.getChunkIndex());
        return null;
    }

    /**
     * Save a complete file from chunks
     * @param uploadId The upload ID
     * @return The path to the saved file
     * @throws IOException if an error occurs while saving the file
     */
    private String saveCompleteFile(String uploadId) throws IOException {
        log.info("[saveCompleteFile] START: uploadId={}", uploadId);
        try {
        Map<Integer, byte[]> chunks = chunkStorage.get(uploadId);
        FileUploadMetadata metadata = uploadMetadata.get(uploadId);

        if (chunks == null || metadata == null) {
                log.error("[saveCompleteFile] Upload not found: {}. Chunks: {}, Metadata: {}", uploadId, chunks, metadata);
            throw new IllegalStateException("Upload not found: " + uploadId);
        }

        // Determine the target directory based on content type
        Path targetDirectory = fileStorageProperties.getPathForContentType(metadata.getContentType());
            log.info("[saveCompleteFile] Target directory for content type {}: {}", metadata.getContentType(), targetDirectory);
            
        // Special handling for video files
        if (metadata.getContentType().startsWith("video/")) {
            log.info("[saveCompleteFile] VIDEO FILE DETECTED: contentType={}, fileName={}, fileSize={}, totalChunks={}",
                metadata.getContentType(), metadata.getFileName(), metadata.getFileSize(), metadata.getTotalChunks());
            
            // Double-check the video directory
            Path videoDir = Paths.get(uploadDir, "video");
            log.info("[saveCompleteFile] VIDEO directory path: {}", videoDir.toAbsolutePath());
            if (!Files.exists(videoDir)) {
                log.warn("[saveCompleteFile] VIDEO directory doesn't exist, creating it now: {}", videoDir.toAbsolutePath());
                Files.createDirectories(videoDir);
            }
            
            if (!Files.isWritable(videoDir)) {
                log.error("[saveCompleteFile] VIDEO directory is not writable: {}", videoDir.toAbsolutePath());
                // Try to set writable permissions
                try {
                    videoDir.toFile().setWritable(true, false);
                    log.info("[saveCompleteFile] Attempted to make VIDEO directory writable: {}", videoDir.toAbsolutePath());
                } catch (Exception e) {
                    log.error("[saveCompleteFile] Failed to set write permissions on VIDEO directory: {}", e.getMessage());
                }
            } else {
                log.info("[saveCompleteFile] VIDEO directory is writable: {}", videoDir.toAbsolutePath());
            }
            
            // Force the target directory to be the video directory
            targetDirectory = videoDir;
            log.info("[saveCompleteFile] Forced target directory for video to: {}", targetDirectory.toAbsolutePath());
        }

        // Create the directory if it doesn't exist
        if (!Files.exists(targetDirectory)) {
            try {
                    log.info("[saveCompleteFile] Target directory does not exist. Creating: {}", targetDirectory);
                Files.createDirectories(targetDirectory);
                    log.info("[saveCompleteFile] Created target directory: {}", targetDirectory);
            } catch (IOException e) {
                    log.error("[saveCompleteFile] Failed to create target directory: {}", targetDirectory, e);
                throw new IOException("Failed to create target directory: " + targetDirectory, e);
            }
            } else {
                log.info("[saveCompleteFile] Target directory already exists: {}", targetDirectory);
        }

        // Verify directory is writable
        if (!Files.isWritable(targetDirectory)) {
                log.error("[saveCompleteFile] Target directory is not writable: {}", targetDirectory);
            throw new IOException("Target directory is not writable: " + targetDirectory);
            } else {
                log.info("[saveCompleteFile] Target directory is writable: {}", targetDirectory);
        }

        // Create a unique filename with proper extension handling
        String originalFileName = metadata.getFileName();
        String fileExtension = "";
        if (originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
                log.info("[saveCompleteFile] Extracted file extension: {} from filename: {}", fileExtension, originalFileName);
        } else {
            // If no extension in filename, try to derive from content type
                fileExtension = com.chatapp.util.FileUtils.getExtensionForMimeType(metadata.getContentType());
                log.info("[saveCompleteFile] Using default extension {} for content type {}", fileExtension, metadata.getContentType());
        }

        // Create a more descriptive filename with original name and timestamp
            String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            String originalNamePart = originalFileName.replaceAll("[^a-zA-Z0-9.-]", "_");
        if (originalNamePart.length() > 20) {
            originalNamePart = originalNamePart.substring(0, 20); // Limit length
        }
            String uniqueFileName = timestamp + "-" + originalNamePart + "-" + java.util.UUID.randomUUID().toString().substring(0, 8) + fileExtension;
        Path filePath = targetDirectory.resolve(uniqueFileName);

            log.info("[saveCompleteFile] Final file path: {}", filePath);

        // Create parent directories if they don't exist
        if (!Files.exists(filePath.getParent())) {
                log.info("[saveCompleteFile] Parent directories do not exist. Creating: {}", filePath.getParent());
            Files.createDirectories(filePath.getParent());
                log.info("[saveCompleteFile] Created parent directories: {}", filePath.getParent());
            } else {
                log.info("[saveCompleteFile] Parent directories already exist: {}", filePath.getParent());
        }

        // Write all chunks to file in order
        long totalBytesWritten = 0;
        long startTimeMs = System.currentTimeMillis();
        log.info("[saveCompleteFile] Starting to write {} chunks to file: {}", metadata.getTotalChunks(), filePath);
        
        // For video files, use buffered output and larger buffer
        boolean isVideo = metadata.getContentType().startsWith("video/");
        if (isVideo) {
            log.info("[saveCompleteFile] Using buffered output for video file");
        }
        
        // Log expected file size
        log.info("[saveCompleteFile] Expected file size: {} ({})", metadata.getFileSize(), formatFileSize(metadata.getFileSize()));
        
        // Record start time for performance measurement
        log.info("[saveCompleteFile] Beginning file assembly at: {}", LocalDateTime.now());
        
        // Use try-with-resources to ensure streams are closed properly
        try (FileOutputStream fos = new FileOutputStream(filePath.toFile());
             java.io.BufferedOutputStream bos = new java.io.BufferedOutputStream(fos, isVideo ? 8192*4 : 8192)) {
                
            log.info("[saveCompleteFile] Created output streams for path: {}", filePath);
            
            // Create a timer for periodic progress updates
            final long[] lastUpdateTime = {System.currentTimeMillis()};
            
            for (int i = 1; i <= metadata.getTotalChunks(); i++) {
                byte[] chunk = chunks.get(i);
                if (chunk != null) {
                    try {
                        bos.write(chunk);
                        totalBytesWritten += chunk.length;
                        
                        // Calculate progress percentage
                        double progressPercent = (i * 100.0) / metadata.getTotalChunks();
                        double bytesProgressPercent = (totalBytesWritten * 100.0) / metadata.getFileSize();
                        
                        // Calculate elapsed time and transfer rate
                        long currentTime = System.currentTimeMillis();
                        double elapsedSec = (currentTime - startTimeMs) / 1000.0;
                        double transferRate = elapsedSec > 0 ? totalBytesWritten / elapsedSec : 0;
                        
                        // Determine if we should log progress
                        boolean shouldLog = false;
                        
                        // For video files or large files, use reduced logging
                        if (isVideo || metadata.getTotalChunks() > 100) {
                            // Log first chunk, last chunk, every 10% progress, or every 2 seconds
                            shouldLog = (i == 1) || (i == metadata.getTotalChunks()) || 
                                 (i % Math.max(1, metadata.getTotalChunks() / 10) == 0) ||
                                 (currentTime - lastUpdateTime[0] >= 2000);  // 2 seconds
                        } else {
                            // For smaller files, log every chunk
                            shouldLog = true;
                        }
                        
                        if (shouldLog) {
                            // Create progress bar
                            StringBuilder progressBar = createProgressBar(progressPercent);
                            
                            // Format transfer rate
                            String transferRateStr = formatTransferRate(transferRate);
                            
                            // Format time
                            String elapsedTimeStr = formatElapsedTime(elapsedSec);
                            
                            // Estimate remaining time
                            double remainingBytes = metadata.getFileSize() - totalBytesWritten;
                            double estimatedRemainingSeconds = transferRate > 0 ? remainingBytes / transferRate : 0;
                            String remainingTimeStr = formatTimeRemaining(estimatedRemainingSeconds);
                            
                            log.info("[saveCompleteFile] ASSEMBLY PROGRESS: {} {}% | Chunk: {}/{} | Written: {} of {} ({}%) | Rate: {} | Time: {} | ETA: {}", 
                                progressBar.toString(), 
                                String.format("%.2f", progressPercent), 
                                i, metadata.getTotalChunks(), 
                                formatFileSize(totalBytesWritten), formatFileSize(metadata.getFileSize()), String.format("%.2f", bytesProgressPercent),
                                transferRateStr, elapsedTimeStr, remainingTimeStr);
                                
                            // Update last update time
                            lastUpdateTime[0] = currentTime;
                        }
                    } catch (IOException e) {
                        log.error("[saveCompleteFile] Failed to write chunk {}: {}", i, e.getMessage(), e);
                        throw new IOException("Failed to write chunk " + i + ": " + e.getMessage(), e);
                    }
                } else {
                    log.error("[saveCompleteFile] Missing chunk {} of {}. Chunks map: {}", i, metadata.getTotalChunks(), chunks);
                    throw new IOException("Missing chunk: " + i);
                }
            }
            
            // Explicit flush to ensure all data is written
            bos.flush();
            log.info("[saveCompleteFile] Flushed file output streams");
            
            // Final performance stats
            long completionTimeMs = System.currentTimeMillis();
            double totalElapsedSec = (completionTimeMs - startTimeMs) / 1000.0;
            double avgTransferRate = totalElapsedSec > 0 ? totalBytesWritten / totalElapsedSec : 0;
            
            log.info("[saveCompleteFile] File assembly completed in {} at avg rate of {}", 
                formatElapsedTime(totalElapsedSec), formatTransferRate(avgTransferRate));
        } catch (IOException e) {
            log.error("[saveCompleteFile] Exception during file writing: {}", e.getMessage(), e);
            throw e;
        }

        // Verify the file was created
        if (!Files.exists(filePath)) {
            log.error("[saveCompleteFile] File was not created: {}", filePath);
            throw new IOException("File was not created: " + filePath);
        } else {
            log.info("[saveCompleteFile] File exists after writing: {}", filePath);
        }

        // Get file size
        long fileSize = Files.size(filePath);
        log.info("[saveCompleteFile] File saved successfully: {}, size: {} bytes (expected: {} bytes, written: {} bytes)",
            filePath, fileSize, metadata.getFileSize(), totalBytesWritten);
            
        // Additional validation for the saved file
        try {
            // Check if file is readable
            if (!Files.isReadable(filePath)) {
                log.error("[saveCompleteFile] WARNING: Saved file is not readable: {}", filePath);
            } else {
                log.info("[saveCompleteFile] Saved file is readable: {}", filePath);
            }
            
            // Check actual file size vs expected
            if (fileSize == 0) {
                log.error("[saveCompleteFile] WARNING: Saved file has zero size: {}", filePath);
            } else if (Math.abs(fileSize - metadata.getFileSize()) > 1024) { // Allow small difference
                log.warn("[saveCompleteFile] File size mismatch: saved={}, expected={}, difference={} bytes", 
                    fileSize, metadata.getFileSize(), Math.abs(fileSize - metadata.getFileSize()));
            } else {
                log.info("[saveCompleteFile] File size matches expected size within tolerance");
            }
            
            // For video files, try to open and verify the file
            if (metadata.getContentType().startsWith("video/")) {
                log.info("[saveCompleteFile] Performing extra validation for video file: {}", filePath);
                try {
                    // Try to read the first few bytes to validate file is accessible
                    byte[] testBytes = Files.readAllBytes(filePath);
                    log.info("[saveCompleteFile] Successfully read {} bytes from video file", testBytes.length);
                    
                    // Check if path exists in video folder
                    Path videoDir = Paths.get(uploadDir, "video");
                    boolean isInVideoDir = filePath.startsWith(videoDir);
                    log.info("[saveCompleteFile] Video file path check - Is in video directory: {}", isInVideoDir);
                    
                    // List video directory contents
                    if (Files.exists(videoDir)) {
                        log.info("[saveCompleteFile] Video directory contains: {}", 
                            Files.list(videoDir).map(p -> p.getFileName().toString()).collect(java.util.stream.Collectors.toList()));
                    }
                } catch (Exception e) {
                    log.error("[saveCompleteFile] Error during video file validation: {}", e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            // Just log validation errors, don't throw
            log.error("[saveCompleteFile] Error during file validation: {}", e.getMessage(), e);
        }

        // Return the absolute path to the file
        // Convert backslashes to forward slashes for consistent path format
        String finalPath = filePath.toString().replace('\\', '/');
        log.info("[saveCompleteFile] END: Returning final file path: {}", finalPath);
        return finalPath;
        } catch (Exception ex) {
            log.error("[saveCompleteFile] Exception: uploadId={}, error={}", uploadId, ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Calculate the total number of bytes received for an upload
     * @param uploadId The upload ID
     * @return The total bytes received
     */
    private long calculateReceivedBytes(String uploadId) {
        Map<Integer, byte[]> chunks = chunkStorage.get(uploadId);
        if (chunks == null) {
            return 0;
        }
        
        long totalBytes = 0;
        for (byte[] chunk : chunks.values()) {
            if (chunk != null) {
                totalBytes += chunk.length;
            }
        }
        return totalBytes;
    }
    
    /**
     * Format a file size in bytes to a human-readable string (B, KB, MB, GB)
     * @param bytes The size in bytes
     * @return A formatted string representing the file size
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1_000) {
            return bytes + " B";
        } else if (bytes < 1_000_000) {
            return String.format("%.2f KB", bytes / 1_000.0);
        } else if (bytes < 1_000_000_000) {
            return String.format("%.2f MB", bytes / 1_000_000.0);
        } else {
            return String.format("%.2f GB", bytes / 1_000_000_000.0);
        }
    }
    
    /**
     * Format seconds to a human-readable time string (e.g., "2m 30s")
     * @param seconds The time in seconds
     * @return A formatted string representing the time
     */
    private String formatTimeRemaining(double seconds) {
        if (seconds <= 0 || Double.isInfinite(seconds) || Double.isNaN(seconds)) {
            return "calculating...";
        }
        
        long totalSeconds = (long) seconds;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long remainingSeconds = totalSeconds % 60;
        
        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, remainingSeconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, remainingSeconds);
        } else {
            return String.format("%ds", remainingSeconds);
        }
    }

    /**
     * Creates a visual progress bar
     * 
     * @param progressPercent progress percentage (0-100)
     * @return a string representing the progress bar
     */
    private StringBuilder createProgressBar(double progressPercent) {
        int progressBarLength = 20;
        int completedBars = (int) (progressBarLength * progressPercent / 100);
        StringBuilder progressBar = new StringBuilder("[");
        for (int i = 0; i < progressBarLength; i++) {
            if (i < completedBars) {
                progressBar.append("=");
            } else if (i == completedBars) {
                progressBar.append(">");
            } else {
                progressBar.append(" ");
            }
        }
        progressBar.append("]");
        return progressBar;
    }
    
    /**
     * Format transfer rate to a human-readable string
     * 
     * @param bytesPerSecond transfer rate in bytes/second
     * @return formatted transfer rate string
     */
    private String formatTransferRate(double bytesPerSecond) {
        if (bytesPerSecond >= 1_000_000) { // MB/s
            return String.format("%.2f MB/s", bytesPerSecond / 1_000_000);
        } else if (bytesPerSecond >= 1_000) { // KB/s
            return String.format("%.2f KB/s", bytesPerSecond / 1_000);
        } else { // B/s
            return String.format("%.0f B/s", bytesPerSecond);
        }
    }
    
    /**
     * Format elapsed time to a human-readable string
     * 
     * @param seconds elapsed time in seconds
     * @return formatted elapsed time string
     */
    private String formatElapsedTime(double seconds) {
        long totalSeconds = (long) seconds;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long remainingSeconds = totalSeconds % 60;
        
        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, remainingSeconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, remainingSeconds);
        } else {
            return String.format("%.1fs", seconds);
        }
    }
}
