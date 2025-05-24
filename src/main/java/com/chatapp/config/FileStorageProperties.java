package com.chatapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.file-storage")
public class FileStorageProperties {
    private String uploadDir = "uploads";
    private long maxFileSize = 10485760; // 10MB default
    private List<String> allowedContentTypes = java.util.Arrays.asList(
        "image/jpeg", "image/png", "image/gif", "application/pdf",
        "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "text/plain", "audio/mpeg", "audio/wav", "video/mp4", "video/mpeg"
    );

    // Map of content type prefixes to subdirectories
    private final Map<String, String> contentTypeDirectories = new HashMap<>();

    // Initialize the content type directories map
    {
        // Image files
        contentTypeDirectories.put("image/", "images");

        // Document files
        contentTypeDirectories.put("application/pdf", "documents");
        contentTypeDirectories.put("application/msword", "documents");
        contentTypeDirectories.put("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "documents");
        contentTypeDirectories.put("application/vnd.ms-excel", "documents");
        contentTypeDirectories.put("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "documents");
        contentTypeDirectories.put("text/plain", "documents");

        // Audio files
        contentTypeDirectories.put("audio/", "audio");

        // Video files - Adding specific entries for common video types
        contentTypeDirectories.put("video/", "video");
        contentTypeDirectories.put("video/mp4", "video");
        contentTypeDirectories.put("video/mpeg", "video");
        contentTypeDirectories.put("video/quicktime", "video");
        contentTypeDirectories.put("video/x-msvideo", "video");
        contentTypeDirectories.put("video/webm", "video");
        contentTypeDirectories.put("video/x-matroska", "video");
        contentTypeDirectories.put("video/x-ms-wmv", "video");
        contentTypeDirectories.put("video/3gpp", "video");
        contentTypeDirectories.put("video/3gpp2", "video");
    }

    /**
     * Validates that the upload directory exists and is writable
     * @return true if the directory is valid, false otherwise
     */
    public boolean validateUploadDirectory() {
        try {
            java.nio.file.Path path = java.nio.file.Paths.get(uploadDir);
            if (!java.nio.file.Files.exists(path)) {
                java.nio.file.Files.createDirectories(path);
            }
            return java.nio.file.Files.isWritable(path);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if the content type is allowed
     * @param contentType the content type to check
     * @return true if the content type is allowed, false otherwise
     */
    public boolean isContentTypeAllowed(String contentType) {
        return allowedContentTypes.contains(contentType);
    }

    /**
     * Gets the appropriate subdirectory for a content type
     * @param contentType the content type
     * @return the subdirectory path
     */
    public String getSubdirectoryForContentType(String contentType) {
        if (contentType == null) {
            return "other";
        }
        
        // Special handling for video files
        if (contentType.startsWith("video/")) {
            return "video";
        }
        
        // First check for exact matches
        if (contentTypeDirectories.containsKey(contentType)) {
            return contentTypeDirectories.get(contentType);
        }

        // Then check for prefix matches
        for (Map.Entry<String, String> entry : contentTypeDirectories.entrySet()) {
            if (entry.getKey().endsWith("/") && contentType.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }

        // Default to "other" if no match is found
        return "other";
    }

    /**
     * Gets the full path for storing a file with the given content type
     * @param contentType the content type
     * @return the full path
     */
    public Path getPathForContentType(String contentType) {
        // Special handling for video files
        if (contentType != null && contentType.startsWith("video/")) {
            return Paths.get(uploadDir).resolve("video");
        }
        
        String subdirectory = getSubdirectoryForContentType(contentType);
        return Paths.get(uploadDir).resolve(subdirectory);
    }
}
