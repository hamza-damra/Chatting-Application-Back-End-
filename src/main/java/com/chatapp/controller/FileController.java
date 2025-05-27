package com.chatapp.controller;

import com.chatapp.config.FileStorageProperties;
import com.chatapp.exception.ResourceNotFoundException;
import com.chatapp.model.FileMetadata;
import com.chatapp.model.Message;
import com.chatapp.model.User;
import com.chatapp.repository.MessageRepository;
import com.chatapp.service.FileMetadataService;
import com.chatapp.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDateTime;

/**
 * Controller for serving uploaded files
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private final FileStorageProperties fileStorageProperties;
    private final MessageRepository messageRepository;
    private final FileMetadataService fileMetadataService;
    private final UserService userService;

    /**
     * Upload a file
     * @param file The file to upload
     * @param chatRoomId The chat room ID (optional)
     * @return File upload response with file URL
     */
    @PostMapping("/upload")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "chatRoomId", required = false) Long chatRoomId) {

        try {
            log.info("FILE UPLOAD: Received file upload request - filename: {}, size: {}, chatRoomId: {}",
                    file.getOriginalFilename(), file.getSize(), chatRoomId);

            // Get current user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.getUserByUsername(auth.getName());
            log.info("FILE UPLOAD: User: {}", currentUser.getUsername());

            // Validate file
            if (file.isEmpty()) {
                log.error("FILE UPLOAD: File is empty");
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "File is empty",
                    "message", "Please select a valid file to upload"
                ));
            }

            // Check file size (additional check beyond Spring configuration)
            if (file.getSize() > fileStorageProperties.getMaxFileSize()) {
                log.error("FILE UPLOAD: File too large - size: {}, max: {}",
                        file.getSize(), fileStorageProperties.getMaxFileSize());
                return ResponseEntity.status(413).body(Map.of(
                    "error", "File too large",
                    "message", "File size exceeds maximum allowed limit of " +
                              (fileStorageProperties.getMaxFileSize() / 1024 / 1024) + "MB"
                ));
            }

            // Check content type
            String contentType = file.getContentType();
            if (contentType == null || !fileStorageProperties.getAllowedContentTypes().contains(contentType)) {
                log.error("FILE UPLOAD: Unsupported content type: {}", contentType);
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Unsupported file type",
                    "message", "File type not supported. Allowed types: " +
                              String.join(", ", fileStorageProperties.getAllowedContentTypes())
                ));
            }

            // Save file using FileMetadataService
            FileMetadata savedFile = fileMetadataService.saveFile(file, currentUser);
            log.info("FILE UPLOAD: File saved successfully - ID: {}, path: {}",
                    savedFile.getId(), savedFile.getFilePath());

            // Create download URL
            String downloadUrl = "/api/files/download/" + savedFile.getFileName();
            String fullUrl = "http://abusaker.zapto.org:8080" + downloadUrl;

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("id", savedFile.getId());
            response.put("fileName", savedFile.getFileName());
            response.put("originalFileName", savedFile.getFileName());
            response.put("contentType", savedFile.getContentType());
            response.put("fileSize", savedFile.getFileSize());
            response.put("fileUrl", fullUrl);
            response.put("downloadUrl", downloadUrl);
            response.put("uploadedAt", savedFile.getUploadedAt());
            response.put("storageLocation", savedFile.getStorageLocation());

            log.info("FILE UPLOAD: Success response - fileUrl: {}", fullUrl);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("FILE UPLOAD: Error uploading file", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Upload failed",
                "message", "An error occurred while uploading the file: " + e.getMessage()
            ));
        }
    }

    /**
     * Download a file by filename
     * @param filename The filename to download
     * @return The file as a resource
     */
    @GetMapping("/download/{filename:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) {
        try {
            log.info("FILE DOWNLOAD: Request for file: {}", filename);

            // Find file metadata
            FileMetadata fileMetadata = fileMetadataService.findByFileName(filename);
            if (fileMetadata == null) {
                log.error("FILE DOWNLOAD: File metadata not found: {}", filename);
                return ResponseEntity.notFound().build();
            }

            // Get file path
            Path filePath = Paths.get(fileMetadata.getFilePath());
            log.info("FILE DOWNLOAD: Attempting to access file at: {}", filePath.toAbsolutePath());

            // Check if file exists
            if (!Files.exists(filePath)) {
                log.error("FILE DOWNLOAD: File does not exist: {}", filePath.toAbsolutePath());
                return ResponseEntity.notFound().build();
            }

            // Create resource
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                log.error("FILE DOWNLOAD: File not accessible: {}", filePath.toAbsolutePath());
                return ResponseEntity.notFound().build();
            }

            // Return file
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(fileMetadata.getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource);

        } catch (Exception e) {
            log.error("FILE DOWNLOAD: Error downloading file: {}", filename, e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Check the status of the file upload directory
     * @return Status information about the upload directory
     */
    @GetMapping("/status")
    public ResponseEntity<?> checkUploadDirectory() {
        try {
            Path uploadPath = Paths.get(fileStorageProperties.getUploadDir()).toAbsolutePath();
            boolean exists = Files.exists(uploadPath);
            boolean isDirectory = exists && Files.isDirectory(uploadPath);
            boolean isWritable = exists && Files.isWritable(uploadPath);

            // Create the directory if it doesn't exist
            if (!exists) {
                Files.createDirectories(uploadPath);
                log.info("Created upload directory: {}", uploadPath);
                exists = true;
                isDirectory = true;
                isWritable = Files.isWritable(uploadPath);
            }

            // Create subdirectories if they don't exist
            String[] subdirs = {"images", "documents", "audio", "video", "other", "temp"};
            Map<String, Integer> fileCounts = new java.util.HashMap<>();

            for (String subdir : subdirs) {
                Path subdirPath = uploadPath.resolve(subdir);
                if (!Files.exists(subdirPath)) {
                    Files.createDirectories(subdirPath);
                    log.info("Created subdirectory: {}", subdirPath);
                    fileCounts.put(subdir, 0);
                } else {
                    // Count files in the subdirectory
                    long count = Files.list(subdirPath)
                        .filter(Files::isRegularFile)
                        .count();
                    fileCounts.put(subdir, (int) count);
                }
            }

            // Count files in the root directory
            long rootCount = Files.list(uploadPath)
                .filter(Files::isRegularFile)
                .count();
            fileCounts.put("root", (int) rootCount);

            // Calculate total file count
            int totalFileCount = fileCounts.values().stream().mapToInt(Integer::intValue).sum();

            Map<String, Object> response = new HashMap<>();
            response.put("uploadDir", uploadPath.toString());
            response.put("exists", exists);
            response.put("isDirectory", isDirectory);
            response.put("isWritable", isWritable);
            response.put("fileCount", totalFileCount);
            response.put("fileCounts", fileCounts);
            response.put("allowedContentTypes", fileStorageProperties.getAllowedContentTypes());
            response.put("maxFileSize", fileStorageProperties.getMaxFileSize());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error checking upload directory", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error checking upload directory");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Test file creation
     * @return Status of the test file creation
     */
    @GetMapping("/test-write")
    public ResponseEntity<?> testFileWrite() {
        try {
            // Create temp directory if it doesn't exist
            Path uploadPath = Paths.get(fileStorageProperties.getUploadDir()).toAbsolutePath();
            Path tempPath = uploadPath.resolve("temp");
            if (!Files.exists(tempPath)) {
                Files.createDirectories(tempPath);
                log.info("Created temp directory: {}", tempPath);
            }

            // Create a test file
            String testFileName = "test-file-" + System.currentTimeMillis() + ".txt";
            Path testFilePath = tempPath.resolve(testFileName);

            // Write some content to the file
            String content = "This is a test file created at " + LocalDateTime.now();
            Files.write(testFilePath, content.getBytes());

            // Verify the file was created
            boolean fileExists = Files.exists(testFilePath);
            long fileSize = Files.size(testFilePath);

            Map<String, Object> response = new HashMap<>();
            response.put("success", fileExists);
            response.put("filePath", testFilePath.toString());
            response.put("fileSize", fileSize);
            response.put("content", content);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error creating test file", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error creating test file");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Serve a file by message ID
     * @param messageId The ID of the message containing the file
     * @return The file as a resource
     */
    @GetMapping("/message/{messageId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Resource> getFileByMessageId(@PathVariable Long messageId) {
        try {
            log.info("FILE DOWNLOAD: Request for file by message ID: {}", messageId);

            // Get current user for access control
            User currentUser = userService.getCurrentUser();

            // Find the message
            Message message = messageRepository.findById(messageId)
                    .orElseThrow(() -> new ResourceNotFoundException("Message not found with id: " + messageId));

            // Verify user has access to this message (must be participant in the chat room)
            if (!message.getChatRoom().getParticipants().contains(currentUser)) {
                log.error("FILE DOWNLOAD: User {} attempted to access file from chat room {} without being a participant",
                    currentUser.getUsername(), message.getChatRoom().getName());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Get the file path - check attachmentUrl, fileMetadata, and content field
            String filePath = null;
            String fileName = null;

            // First, try to get file path from attachmentUrl (WebSocket uploads)
            String attachmentUrl = message.getAttachmentUrl();
            if (attachmentUrl != null && !attachmentUrl.isEmpty()) {
                filePath = attachmentUrl;
                fileName = Paths.get(attachmentUrl).getFileName().toString();
                log.info("FILE DOWNLOAD: Using attachmentUrl from message {}: {}", messageId, attachmentUrl);
            }
            // If no attachmentUrl, try to get file path from fileMetadata (REST API uploads)
            else if (message.getFileMetadata() != null) {
                FileMetadata fileMetadata = message.getFileMetadata();
                filePath = fileMetadata.getFilePath();
                fileName = fileMetadata.getFileName();
                log.info("FILE DOWNLOAD: Using fileMetadata from message {}: {} (fileName: {})",
                    messageId, filePath, fileName);
            }
            // If neither attachmentUrl nor fileMetadata, check if content field contains a file URL
            else if (message.getContent() != null && !message.getContent().isEmpty()) {
                String content = message.getContent();
                // Check if content looks like a file URL (contains file download endpoint)
                if (content.contains("/api/files/download/") || content.contains("http")) {
                    // Extract filename from URL if it's a full URL
                    if (content.startsWith("http")) {
                        // Extract the filename from the URL
                        String[] urlParts = content.split("/");
                        if (urlParts.length > 0) {
                            String lastPart = urlParts[urlParts.length - 1];
                            // Use the filename from URL to find the actual file
                            FileMetadata fileMetadata = fileMetadataService.findByFileName(lastPart);
                            if (fileMetadata != null) {
                                filePath = fileMetadata.getFilePath();
                                fileName = fileMetadata.getFileName();
                                log.info("FILE DOWNLOAD: Using content URL from message {} to find file: {} -> {}",
                                    messageId, content, filePath);
                            } else {
                                log.warn("FILE DOWNLOAD: Content URL found but no file metadata for filename: {}", lastPart);
                            }
                        }
                    } else {
                        // Content might be a relative path or filename
                        filePath = content;
                        fileName = Paths.get(content).getFileName().toString();
                        log.info("FILE DOWNLOAD: Using content as file path from message {}: {}", messageId, content);
                    }
                }
            }

            // If no file path found through any method, return 404
            if (filePath == null || filePath.isEmpty()) {
                log.error("FILE DOWNLOAD: Message {} does not have an attachment (no attachmentUrl, fileMetadata, or valid content URL)", messageId);
                return ResponseEntity.notFound().build();
            }

            log.info("FILE DOWNLOAD: Serving file from message {}: {}", messageId, filePath);

            // Create a resource from the file path
            Path filePathObj = Paths.get(filePath);
            log.info("FILE DOWNLOAD: Attempting to access file at path: {}", filePathObj.toAbsolutePath());

            // Check if the file exists on disk
            if (!Files.exists(filePathObj)) {
                log.error("FILE DOWNLOAD: File does not exist on disk: {}", filePathObj.toAbsolutePath());
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(filePathObj.toUri());

            // Check if the resource exists and is readable
            if (!resource.exists() || !resource.isReadable()) {
                log.error("FILE DOWNLOAD: File exists but is not accessible as a resource: {}", filePathObj.toAbsolutePath());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }

            // Determine content type
            String contentType = message.getContentType();
            if (contentType == null || contentType.isEmpty()) {
                contentType = Files.probeContentType(filePathObj);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }
            }

            // Get original filename for download
            String filename = fileName; // Use the fileName we determined earlier
            if (filename == null || filename.isEmpty()) {
                // Fallback to message content or file path filename
                filename = message.getContent();
                if (filename == null || filename.isEmpty()) {
                    filename = filePathObj.getFileName().toString();
                }
            }

            log.info("FILE DOWNLOAD: Successfully serving file {} with content type {}", filename, contentType);

            // Return the file
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (ResourceNotFoundException e) {
            log.error("FILE DOWNLOAD: Resource not found for message {}: {}", messageId, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            log.error("FILE DOWNLOAD: Error serving file for message {}", messageId, e);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("FILE DOWNLOAD: Unexpected error serving file for message {}", messageId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Serve a file directly from the organized directory structure
     * @param category The file category (images, documents, audio, video, other)
     * @param filename The filename
     * @return The file as a resource
     */
    @GetMapping("/category/{category}/{filename:.+}")
    public ResponseEntity<Resource> getFileByCategory(
            @PathVariable String category,
            @PathVariable String filename) {
        try {
            // Validate category
            if (!isValidCategory(category)) {
                log.error("Invalid file category: {}", category);
                return ResponseEntity.badRequest().build();
            }

            // Build the file path
            Path basePath = Paths.get(fileStorageProperties.getUploadDir()).toAbsolutePath();
            Path filePath = basePath.resolve(category).resolve(filename);

            log.info("Attempting to access file at path: {}", filePath.toAbsolutePath());

            // Check if the file exists on disk
            if (!Files.exists(filePath)) {
                log.error("File does not exist on disk: {}", filePath.toAbsolutePath());
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(filePath.toUri());

            // Check if the resource exists and is readable
            if (!resource.exists() || !resource.isReadable()) {
                log.error("File exists but is not accessible as a resource: {}", filePath.toAbsolutePath());
                return ResponseEntity.status(500).build();
            }

            // Determine content type
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            // Return the file
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (IOException e) {
            log.error("Error serving file", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Check if the category is valid
     * @param category The category to check
     * @return true if valid, false otherwise
     */
    private boolean isValidCategory(String category) {
        return category.equals("images") ||
               category.equals("documents") ||
               category.equals("audio") ||
               category.equals("video") ||
               category.equals("other");
    }
}
