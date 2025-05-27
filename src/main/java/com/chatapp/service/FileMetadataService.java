package com.chatapp.service;

import com.chatapp.config.FileStorageProperties;
import com.chatapp.model.FileMetadata;
import com.chatapp.model.User;
import com.chatapp.repository.FileMetadataRepository;
import com.chatapp.util.FileUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for file metadata operations and duplicate detection
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FileMetadataService {

    private final FileMetadataRepository fileMetadataRepository;
    private final FileStorageProperties fileStorageProperties;

    /**
     * Save file metadata to the database
     * This method is provided for backward compatibility with tests
     * @param fileMetadata The file metadata to save
     * @return The saved file metadata
     */
    @Transactional
    public FileMetadata saveFileMetadata(FileMetadata fileMetadata) {
        return fileMetadataRepository.save(fileMetadata);
    }

    /**
     * Register a new file in the metadata database
     * @param filePath Path to the file
     * @param fileName Original file name
     * @param contentType MIME type of the file
     * @param user User who uploaded the file
     * @return FileMetadata object for the registered file
     */
    @Transactional
    public FileMetadata registerFile(String filePath, String fileName, String contentType, User user) {
        log.info("[registerFile] START: filePath={}, fileName={}, contentType={}, userId={}",
            filePath, fileName, contentType, user != null ? user.getId() : "null");

        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                log.error("[registerFile] ERROR: File does not exist: {}", filePath);
                throw new IllegalArgumentException("File does not exist: " + filePath);
            }
            log.info("[registerFile] File exists at path: {}", path.toAbsolutePath());

            long fileSize = Files.size(path);
            log.info("[registerFile] File size: {} bytes", fileSize);

            // Calculate MD5 hash
            log.debug("[registerFile] Calculating file hash...");
            String fileHash = FileUtils.calculateMD5(path);
            log.info("[registerFile] File hash: {}", fileHash);

            String storageLocation = getStorageLocationFromPath(filePath);
            log.info("[registerFile] Storage location: {}", storageLocation);

            // Check for duplicates - use findAllByFileHash to handle multiple results
            log.debug("[registerFile] Checking for duplicates with hash: {}", fileHash);
            List<FileMetadata> existingFiles = fileMetadataRepository.findAllByFileHash(fileHash);

            FileMetadata metadata;
            if (!existingFiles.isEmpty()) {
                // Use the first existing file as the original
                FileMetadata originalFile = existingFiles.get(0);
                log.info("[registerFile] Duplicate file detected: {} (hash: {})", filePath, fileHash);

                // Create a new metadata entry for the duplicate
                metadata = FileMetadata.builder()
                    .fileName(fileName)
                    .contentType(contentType)
                    .filePath(filePath)
                    .fileSize(fileSize)
                    .fileHash(fileHash)
                    .uploadedAt(LocalDateTime.now())
                    .uploadedBy(user)
                    .storageLocation(storageLocation)
                    .isDuplicate(true)
                    .originalFileId(originalFile.getId())
                    .build();

                log.info("[registerFile] Created duplicate file metadata: fileName={}, filePath={}, storageLocation={}",
                    metadata.getFileName(), metadata.getFilePath(), metadata.getStorageLocation());
            } else {
                // Create a new metadata entry
                metadata = FileMetadata.builder()
                    .fileName(fileName)
                    .contentType(contentType)
                    .filePath(filePath)
                    .fileSize(fileSize)
                    .fileHash(fileHash)
                    .uploadedAt(LocalDateTime.now())
                    .uploadedBy(user)
                    .storageLocation(storageLocation)
                    .isDuplicate(false)
                    .build();

                log.info("[registerFile] Created new file metadata: fileName={}, filePath={}, storageLocation={}",
                    metadata.getFileName(), metadata.getFilePath(), metadata.getStorageLocation());
            }

            // Save the metadata to the database
            try {
                FileMetadata savedMetadata = fileMetadataRepository.save(metadata);
                log.info("[registerFile] END: Successfully saved file metadata with ID: {}",
                    savedMetadata.getId());
                return savedMetadata;
            } catch (Exception e) {
                log.error("[registerFile] ERROR: Failed to save file metadata: {}", e.getMessage(), e);
                throw new RuntimeException("Error saving file metadata: " + e.getMessage(), e);
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            log.error("[registerFile] ERROR: {}", e.getMessage(), e);
            throw new RuntimeException("Error registering file: " + e.getMessage(), e);
        }
    }

    /**
     * Extract storage location from file path
     * @param filePath Full path to the file
     * @return Storage location (folder name)
     */
    private String getStorageLocationFromPath(String filePath) {
        log.debug("[getStorageLocationFromPath] Analyzing file path: {}", filePath);

        // Normalize path to use forward slashes for consistency
        String normalizedPath = filePath.replace('\\', '/');
        log.debug("[getStorageLocationFromPath] Normalized path: {}", normalizedPath);

        Path path = Paths.get(normalizedPath);
        Path parent = path.getParent();

        if (parent == null) {
            log.warn("[getStorageLocationFromPath] No parent directory found for path: {}", normalizedPath);
            return "unknown";
        }

        String parentName = parent.getFileName().toString();
        log.debug("[getStorageLocationFromPath] Parent folder name: {}", parentName);

        // Explicitly check for video storage location
        if (normalizedPath.contains("/video/") || normalizedPath.contains("\\video\\")) {
            log.info("[getStorageLocationFromPath] Video file detected in video folder: {}", normalizedPath);
            return "video";
        }

        // Check if the parent folder is one of our storage locations
        if (parentName.equals("images") ||
            parentName.equals("documents") ||
            parentName.equals("audio") ||
            parentName.equals("video") ||
            parentName.equals("other")) {
            log.debug("[getStorageLocationFromPath] Found standard storage location: {}", parentName);
            return parentName;
        }

        // If not found by direct match, try to determine based on path segments
        String[] pathSegments = normalizedPath.split("[/\\\\]");
        for (String segment : pathSegments) {
            if (segment.equals("images") ||
                segment.equals("documents") ||
                segment.equals("audio") ||
                segment.equals("video") ||
                segment.equals("other")) {
                log.info("[getStorageLocationFromPath] Found storage location from path segments: {}", segment);
                return segment;
            }
        }

        // Last resort: check for "/uploads/xxx/" pattern
        if (normalizedPath.contains("/uploads/") || normalizedPath.contains("\\uploads\\")) {
            int uploadsIdx = Math.max(
                normalizedPath.indexOf("/uploads/"),
                normalizedPath.indexOf("\\uploads\\")
            );

            if (uploadsIdx >= 0) {
                String afterUploads = normalizedPath.substring(uploadsIdx + 9); // +9 for "/uploads/"
                int nextSlash = Math.min(
                    afterUploads.indexOf('/') != -1 ? afterUploads.indexOf('/') : Integer.MAX_VALUE,
                    afterUploads.indexOf('\\') != -1 ? afterUploads.indexOf('\\') : Integer.MAX_VALUE
                );

                if (nextSlash != Integer.MAX_VALUE) {
                    String folderName = afterUploads.substring(0, nextSlash);
                    log.info("[getStorageLocationFromPath] Found storage location after uploads: {}", folderName);
                    return folderName;
                }
            }
        }

        // If not, return the parent folder name anyway
        log.warn("[getStorageLocationFromPath] Using non-standard storage location: {}", parentName);
        return parentName;
    }

    /**
     * Find all duplicate files in the system
     * @return List of duplicate file metadata
     */
    public List<FileMetadata> findAllDuplicates() {
        return fileMetadataRepository.findAllByIsDuplicateTrue();
    }

    /**
     * Find all files in the system
     * @return List of all file metadata
     */
    public List<FileMetadata> findAllFiles() {
        return fileMetadataRepository.findAll();
    }

    /**
     * Remove duplicate files from the filesystem
     * @return Number of files removed
     */
    @Transactional
    public int removeDuplicateFiles() {
        List<FileMetadata> duplicates = findAllDuplicates();
        int removedCount = 0;

        for (FileMetadata duplicate : duplicates) {
            try {
                File file = new File(duplicate.getFilePath());
                if (file.exists()) {
                    boolean deleted = file.delete();
                    if (deleted) {
                        log.info("Deleted duplicate file: {}", duplicate.getFilePath());
                        removedCount++;
                    } else {
                        log.warn("Failed to delete duplicate file: {}", duplicate.getFilePath());
                    }
                } else {
                    log.warn("Duplicate file not found: {}", duplicate.getFilePath());
                }

                // Update the metadata to reflect that the file has been removed
                duplicate.setFilePath(duplicate.getFilePath() + ".removed");
                fileMetadataRepository.save(duplicate);
            } catch (Exception e) {
                log.error("Error removing duplicate file: {}", duplicate.getFilePath(), e);
            }
        }

        return removedCount;
    }

    /**
     * Scan all files in the upload directory for duplicates
     * @return Number of duplicates found
     */
    @Transactional
    public int scanForDuplicates() {
        int duplicatesFound = 0;

        try {
            // Get all files from the database
            List<FileMetadata> allFiles = fileMetadataRepository.findAll();

            // Group files by hash
            java.util.Map<String, List<FileMetadata>> filesByHash = new java.util.HashMap<>();

            for (FileMetadata file : allFiles) {
                filesByHash.computeIfAbsent(file.getFileHash(), k -> new java.util.ArrayList<>()).add(file);
            }

            // Find duplicates
            for (List<FileMetadata> filesWithSameHash : filesByHash.values()) {
                if (filesWithSameHash.size() > 1) {
                    // The first file is the original, the rest are duplicates
                    FileMetadata original = filesWithSameHash.get(0);

                    for (int i = 1; i < filesWithSameHash.size(); i++) {
                        FileMetadata duplicate = filesWithSameHash.get(i);

                        // Mark as duplicate if not already marked
                        if (!Boolean.TRUE.equals(duplicate.getIsDuplicate())) {
                            duplicate.setIsDuplicate(true);
                            duplicate.setOriginalFileId(original.getId());
                            fileMetadataRepository.save(duplicate);
                            duplicatesFound++;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error scanning for duplicates", e);
            throw new RuntimeException("Error scanning for duplicates: " + e.getMessage(), e);
        }

        return duplicatesFound;
    }

    /**
     * Scan the filesystem for files not in the database
     * @return Number of files added to the database
     */
    @Transactional
    public int scanFilesystem() {
        final int[] filesAdded = {0}; // Using array to make it effectively final for lambda

        try {
            // Get the upload directory
            Path uploadDir = Paths.get(fileStorageProperties.getUploadDir());

            // Scan all subdirectories
            String[] subdirs = {"images", "documents", "audio", "video", "other", "temp"};

            for (String subdir : subdirs) {
                Path subdirPath = uploadDir.resolve(subdir);

                if (Files.exists(subdirPath) && Files.isDirectory(subdirPath)) {
                    // Get all files in this subdirectory
                    Files.list(subdirPath).forEach(filePath -> {
                        if (Files.isRegularFile(filePath)) {
                            try {
                                // Check if this file is already in the database
                                String fileHash = FileUtils.calculateMD5(filePath);
                                List<FileMetadata> existingFiles = fileMetadataRepository.findAllByFileHash(fileHash);

                                if (existingFiles.isEmpty()) {
                                    // File not in database, add it
                                    String fileName = filePath.getFileName().toString();
                                    String contentType = Files.probeContentType(filePath);
                                    if (contentType == null) {
                                        contentType = "application/octet-stream";
                                    }

                                    // Create a system user for files found during scan
                                    User systemUser = new User();
                                    systemUser.setId(1L); // Assuming ID 1 is the system user

                                    FileMetadata metadata = FileMetadata.builder()
                                        .fileName(fileName)
                                        .contentType(contentType)
                                        .filePath(filePath.toString())
                                        .fileSize(Files.size(filePath))
                                        .fileHash(fileHash)
                                        .uploadedAt(LocalDateTime.now())
                                        .uploadedBy(systemUser)
                                        .storageLocation(subdir)
                                        .isDuplicate(false)
                                        .build();

                                    fileMetadataRepository.save(metadata);
                                    filesAdded[0]++; // Increment the counter in the array
                                }
                            } catch (Exception e) {
                                log.error("Error processing file: {}", filePath, e);
                            }
                        }
                    });
                }
            }
        } catch (Exception e) {
            log.error("Error scanning filesystem", e);
            throw new RuntimeException("Error scanning filesystem: " + e.getMessage(), e);
        }

        return filesAdded[0]; // Return the final count from the array
    }

    /**
     * Save a file from MultipartFile and register it in the database
     * @param file The multipart file to save
     * @param user The user uploading the file
     * @return The saved file metadata
     */
    @Transactional
    public FileMetadata saveFile(MultipartFile file, User user) {
        try {
            log.info("SAVE FILE: Starting file save process - filename: {}, size: {}, user: {}",
                    file.getOriginalFilename(), file.getSize(), user.getUsername());

            // Determine storage location based on content type
            String storageLocation = determineStorageLocation(file.getContentType());
            log.info("SAVE FILE: Determined storage location: {}", storageLocation);

            // Create upload directory if it doesn't exist
            Path uploadDir = Paths.get(fileStorageProperties.getUploadDir(), storageLocation);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
                log.info("SAVE FILE: Created directory: {}", uploadDir.toAbsolutePath());
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String timestamp = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
                    .format(java.time.LocalDateTime.now());
            String uniqueId = java.util.UUID.randomUUID().toString().substring(0, 8);
            String filename = timestamp + "-" + originalFilename + "-" + uniqueId + fileExtension;

            // Save file to disk using try-with-resources to ensure InputStream is closed
            Path filePath = uploadDir.resolve(filename);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
                log.info("SAVE FILE: File saved to: {}", filePath.toAbsolutePath());
            }

            // Register file in database
            FileMetadata metadata = registerFile(
                    filePath.toString(),
                    filename,
                    file.getContentType(),
                    user
            );

            log.info("SAVE FILE: File successfully saved and registered with ID: {}", metadata.getId());
            return metadata;

        } catch (IOException e) {
            log.error("SAVE FILE: Error saving file", e);
            throw new RuntimeException("Error saving file: " + e.getMessage(), e);
        }
    }

    /**
     * Find file metadata by filename
     * @param filename The filename to search for
     * @return The file metadata or null if not found
     */
    public FileMetadata findByFileName(String filename) {
        log.info("FIND BY FILENAME: Searching for file: {}", filename);

        Optional<FileMetadata> result = fileMetadataRepository.findByFileName(filename);
        if (result.isPresent()) {
            log.info("FIND BY FILENAME: Found file metadata for: {}", filename);
            return result.get();
        } else {
            log.warn("FIND BY FILENAME: File metadata not found for: {}", filename);
            return null;
        }
    }

    /**
     * Determine storage location based on content type
     * @param contentType The MIME content type
     * @return The storage location folder name
     */
    private String determineStorageLocation(String contentType) {
        if (contentType == null) {
            return "other";
        }

        if (contentType.startsWith("image/")) {
            return "images";
        } else if (contentType.startsWith("video/")) {
            return "video";
        } else if (contentType.startsWith("audio/")) {
            return "audio";
        } else if (contentType.equals("application/pdf") ||
                   contentType.contains("document") ||
                   contentType.contains("text") ||
                   contentType.contains("spreadsheet")) {
            return "documents";
        } else {
            return "other";
        }
    }
}
