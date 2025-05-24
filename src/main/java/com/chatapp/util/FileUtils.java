package com.chatapp.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for file operations
 */
@Slf4j
@Component
public class FileUtils {

    private static final Map<String, String> MIME_TYPE_EXTENSIONS = new HashMap<>();
    
    static {
        // Image files
        MIME_TYPE_EXTENSIONS.put("image/jpeg", ".jpg");
        MIME_TYPE_EXTENSIONS.put("image/png", ".png");
        MIME_TYPE_EXTENSIONS.put("image/gif", ".gif");
        MIME_TYPE_EXTENSIONS.put("image/webp", ".webp");
        MIME_TYPE_EXTENSIONS.put("image/svg+xml", ".svg");
        MIME_TYPE_EXTENSIONS.put("image/bmp", ".bmp");
        MIME_TYPE_EXTENSIONS.put("image/tiff", ".tiff");
        
        // Document files
        MIME_TYPE_EXTENSIONS.put("application/pdf", ".pdf");
        MIME_TYPE_EXTENSIONS.put("application/msword", ".doc");
        MIME_TYPE_EXTENSIONS.put("application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx");
        MIME_TYPE_EXTENSIONS.put("application/vnd.ms-excel", ".xls");
        MIME_TYPE_EXTENSIONS.put("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx");
        MIME_TYPE_EXTENSIONS.put("application/vnd.ms-powerpoint", ".ppt");
        MIME_TYPE_EXTENSIONS.put("application/vnd.openxmlformats-officedocument.presentationml.presentation", ".pptx");
        MIME_TYPE_EXTENSIONS.put("text/plain", ".txt");
        MIME_TYPE_EXTENSIONS.put("text/html", ".html");
        MIME_TYPE_EXTENSIONS.put("text/css", ".css");
        MIME_TYPE_EXTENSIONS.put("text/javascript", ".js");
        MIME_TYPE_EXTENSIONS.put("application/json", ".json");
        MIME_TYPE_EXTENSIONS.put("application/xml", ".xml");
        
        // Audio files
        MIME_TYPE_EXTENSIONS.put("audio/mpeg", ".mp3");
        MIME_TYPE_EXTENSIONS.put("audio/wav", ".wav");
        MIME_TYPE_EXTENSIONS.put("audio/ogg", ".ogg");
        MIME_TYPE_EXTENSIONS.put("audio/aac", ".aac");
        MIME_TYPE_EXTENSIONS.put("audio/flac", ".flac");
        
        // Video files
        MIME_TYPE_EXTENSIONS.put("video/mp4", ".mp4");
        MIME_TYPE_EXTENSIONS.put("video/mpeg", ".mpeg");
        MIME_TYPE_EXTENSIONS.put("video/webm", ".webm");
        MIME_TYPE_EXTENSIONS.put("video/quicktime", ".mov");
        MIME_TYPE_EXTENSIONS.put("video/x-msvideo", ".avi");
        
        // Archive files
        MIME_TYPE_EXTENSIONS.put("application/zip", ".zip");
        MIME_TYPE_EXTENSIONS.put("application/x-rar-compressed", ".rar");
        MIME_TYPE_EXTENSIONS.put("application/x-tar", ".tar");
        MIME_TYPE_EXTENSIONS.put("application/gzip", ".gz");
    }

    /**
     * Get file extension for a MIME type
     * @param mimeType MIME type
     * @return File extension with dot (e.g., ".jpg") or empty string if not found
     */
    public static String getExtensionForMimeType(String mimeType) {
        if (mimeType == null || mimeType.isEmpty()) {
            return "";
        }
        
        // Check for exact match
        if (MIME_TYPE_EXTENSIONS.containsKey(mimeType)) {
            return MIME_TYPE_EXTENSIONS.get(mimeType);
        }
        
        // Check for prefix match (e.g., "image/")
        String prefix = mimeType.split("/")[0] + "/";
        for (Map.Entry<String, String> entry : MIME_TYPE_EXTENSIONS.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                return entry.getValue();
            }
        }
        
        return "";
    }
    
    /**
     * Calculate MD5 hash of a file
     * @param filePath Path to the file
     * @return MD5 hash as a hex string
     * @throws IOException If file cannot be read
     * @throws NoSuchAlgorithmException If MD5 algorithm is not available
     */
    public static String calculateMD5(Path filePath) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(Files.readAllBytes(filePath));
        byte[] digest = md.digest();
        
        // Convert to hex string
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    
    /**
     * Check if two files have the same content (are duplicates)
     * @param file1 First file
     * @param file2 Second file
     * @return true if files are duplicates, false otherwise
     */
    public static boolean areFilesDuplicate(File file1, File file2) {
        // Quick check: if sizes differ, files are definitely not duplicates
        if (file1.length() != file2.length()) {
            return false;
        }
        
        try {
            String hash1 = calculateMD5(file1.toPath());
            String hash2 = calculateMD5(file2.toPath());
            return hash1.equals(hash2);
        } catch (IOException | NoSuchAlgorithmException e) {
            log.error("Error comparing files: {} and {}", file1.getPath(), file2.getPath(), e);
            return false;
        }
    }
    
    /**
     * Detect MIME type from file content
     * @param filePath Path to the file
     * @return Detected MIME type or null if detection fails
     */
    public static String detectMimeType(Path filePath) {
        try {
            return Files.probeContentType(filePath);
        } catch (IOException e) {
            log.error("Error detecting MIME type for file: {}", filePath, e);
            return null;
        }
    }
}
