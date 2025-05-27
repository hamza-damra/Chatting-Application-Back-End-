package com.chatapp.service;

import com.chatapp.dto.UserResponse;
import com.chatapp.dto.UserUpdateRequest;
import com.chatapp.exception.ResourceNotFoundException;
import com.chatapp.model.FileMetadata;
import com.chatapp.model.User;
import com.chatapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileMetadataService fileMetadataService;

    @Transactional(readOnly = true)
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return getUserByUsername(username);
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUserResponse() {
        return convertToUserResponse(getCurrentUser());
    }

    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public UserResponse getUserResponseById(Long id) {
        return convertToUserResponse(getUserById(id));
    }

    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserResponse> searchUsers(String query) {
        // Simple implementation - in a real app, you might use full-text search
        String lowerCaseQuery = query.toLowerCase();
        return userRepository.findAll().stream()
                .filter(user -> user.getUsername().toLowerCase().contains(lowerCaseQuery) ||
                        user.getFullName().toLowerCase().contains(lowerCaseQuery))
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserResponse updateCurrentUser(UserUpdateRequest request) {
        User currentUser = getCurrentUser();

        if (request.getFullName() != null && !request.getFullName().isEmpty()) {
            currentUser.setFullName(request.getFullName());
        }

        if (request.getProfilePicture() != null) {
            currentUser.setProfilePicture(request.getProfilePicture());
        }

        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            // Verify current password if provided
            if (request.getCurrentPassword() == null || !passwordEncoder.matches(
                    request.getCurrentPassword(), currentUser.getPassword())) {
                throw new IllegalArgumentException("Current password is incorrect");
            }
            currentUser.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        User updatedUser = userRepository.save(currentUser);
        log.info("User updated: {}", updatedUser.getUsername());

        return convertToUserResponse(updatedUser);
    }

    @Transactional
    public void updateUserStatus(Long userId, boolean isOnline) {
        User user = getUserById(userId);
        user.setOnline(isOnline);
        user.setLastSeen(LocalDateTime.now());
        userRepository.save(user);
        log.debug("User status updated: {} is {}", user.getUsername(), isOnline ? "online" : "offline");
    }

    public UserResponse convertToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .profilePicture(user.getProfilePicture())
                .lastSeen(user.getLastSeen())
                .isOnline(user.isOnline())
                .build();
    }

    /**
     * Add a profile image for the current user
     * @param file The image file to upload
     * @return Updated user response with new profile image URL
     */
    @Transactional
    public UserResponse addProfileImage(MultipartFile file) {
        log.info("PROFILE_IMAGE: Adding profile image for current user");

        // Validate file
        validateImageFile(file);

        User currentUser = getCurrentUser();

        // Save the file using FileMetadataService
        FileMetadata savedFile = fileMetadataService.saveFile(file, currentUser);
        log.info("PROFILE_IMAGE: File saved with ID: {}", savedFile.getId());

        // Create the profile image URL
        String profileImageUrl = "/api/files/download/" + savedFile.getFileName();

        // Update user's profile picture
        log.info("PROFILE_IMAGE: Setting profile picture URL: {} for user ID: {}", profileImageUrl, currentUser.getId());
        currentUser.setProfilePicture(profileImageUrl);

        // Save and flush to ensure immediate database update
        User updatedUser = userRepository.saveAndFlush(currentUser);

        log.info("PROFILE_IMAGE: Profile image added successfully for user: {} with URL: {}",
                updatedUser.getUsername(), updatedUser.getProfilePicture());

        // Verify the update was persisted
        User verifyUser = userRepository.findById(updatedUser.getId()).orElse(null);
        if (verifyUser != null) {
            log.info("PROFILE_IMAGE: Verification - User {} profile picture in DB: {}",
                    verifyUser.getUsername(), verifyUser.getProfilePicture());
        }

        return convertToUserResponse(updatedUser);
    }

    /**
     * Update existing profile image for the current user
     * @param file The new image file to upload
     * @return Updated user response with new profile image URL
     */
    @Transactional
    public UserResponse updateProfileImage(MultipartFile file) {
        log.info("PROFILE_IMAGE: Updating profile image for current user");

        // Validate file
        validateImageFile(file);

        User currentUser = getCurrentUser();

        // Save the new file using FileMetadataService
        FileMetadata savedFile = fileMetadataService.saveFile(file, currentUser);
        log.info("PROFILE_IMAGE: New file saved with ID: {}", savedFile.getId());

        // Create the new profile image URL
        String profileImageUrl = "/api/files/download/" + savedFile.getFileName();

        // Update user's profile picture
        String oldProfilePicture = currentUser.getProfilePicture();
        log.info("PROFILE_IMAGE: Setting profile picture URL: {} for user ID: {} (old: {})",
                profileImageUrl, currentUser.getId(), oldProfilePicture);
        currentUser.setProfilePicture(profileImageUrl);

        // Save and flush to ensure immediate database update
        User updatedUser = userRepository.saveAndFlush(currentUser);

        log.info("PROFILE_IMAGE: Profile image updated successfully for user: {} (old: {}, new: {})",
                updatedUser.getUsername(), oldProfilePicture, updatedUser.getProfilePicture());

        // Verify the update was persisted
        User verifyUser = userRepository.findById(updatedUser.getId()).orElse(null);
        if (verifyUser != null) {
            log.info("PROFILE_IMAGE: Verification - User {} profile picture in DB: {}",
                    verifyUser.getUsername(), verifyUser.getProfilePicture());
        }

        return convertToUserResponse(updatedUser);
    }

    /**
     * Validate that the uploaded file is a valid image
     * @param file The file to validate
     */
    private void validateImageFile(MultipartFile file) {
        log.debug("PROFILE_IMAGE: Starting file validation");

        if (file == null || file.isEmpty()) {
            log.error("PROFILE_IMAGE: File validation failed - file is null or empty");
            throw new IllegalArgumentException("File cannot be empty");
        }

        // Log file details for debugging
        String originalFilename = file.getOriginalFilename();
        String contentType = file.getContentType();
        long fileSize = file.getSize();

        log.info("PROFILE_IMAGE: Validating file - name: '{}', contentType: '{}', size: {} bytes",
                originalFilename, contentType, fileSize);

        // Check file size (max 1GB for profile images)
        long maxSize = 1024L * 1024L * 1024L; // 1GB
        if (fileSize > maxSize) {
            log.error("PROFILE_IMAGE: File validation failed - size {} exceeds maximum {}", fileSize, maxSize);
            throw new IllegalArgumentException("File size cannot exceed 1GB");
        }

        // Enhanced image type validation to support Android camera images
        List<String> allowedImageTypes = Arrays.asList(
            // Standard image types
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp",
            // Additional Android camera formats
            "image/heic", "image/heif", "image/bmp", "image/tiff", "image/tif",
            // Some Android devices use these variations
            "image/pjpeg", "image/x-png"
        );

        List<String> allowedImageExtensions = Arrays.asList(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".heic", ".heif", ".bmp", ".tiff", ".tif"
        );

        boolean isValidImage = false;
        String validationMethod = "";

        // Primary validation: Check content type
        if (contentType != null && !contentType.trim().isEmpty()) {
            String lowerContentType = contentType.toLowerCase().trim();
            if (allowedImageTypes.contains(lowerContentType)) {
                isValidImage = true;
                validationMethod = "content-type";
                log.info("PROFILE_IMAGE: File validated by content type: {}", contentType);
            } else {
                log.warn("PROFILE_IMAGE: Content type '{}' not in allowed list: {}", contentType, allowedImageTypes);
            }
        } else {
            log.warn("PROFILE_IMAGE: Content type is null or empty for file '{}'", originalFilename);
        }

        // Fallback validation: Check file extension (common for Android camera images)
        if (!isValidImage && originalFilename != null && !originalFilename.trim().isEmpty()) {
            String lowerFilename = originalFilename.toLowerCase().trim();
            for (String extension : allowedImageExtensions) {
                if (lowerFilename.endsWith(extension)) {
                    isValidImage = true;
                    validationMethod = "file-extension";
                    log.info("PROFILE_IMAGE: File validated by extension: {} (content-type was: '{}')",
                            extension, contentType);
                    break;
                }
            }
        }

        // Final validation check
        if (!isValidImage) {
            String errorMessage = String.format(
                "Only image files are allowed. Supported formats: JPEG, PNG, GIF, WebP, HEIC, BMP, TIFF. " +
                "Received content-type: '%s', filename: '%s'",
                contentType != null ? contentType : "null",
                originalFilename != null ? originalFilename : "null"
            );
            log.error("PROFILE_IMAGE: File validation failed - {}", errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }

        log.info("PROFILE_IMAGE: File validation passed - type: {}, size: {} bytes, validation method: {}",
                contentType, fileSize, validationMethod);
    }

    /**
     * Debug method to test profile picture update without file upload
     * @param profilePictureUrl The profile picture URL to set
     * @return Updated user response
     */
    @Transactional
    public UserResponse debugSetProfilePicture(String profilePictureUrl) {
        log.info("DEBUG: Setting profile picture URL: {}", profilePictureUrl);

        User currentUser = getCurrentUser();
        log.info("DEBUG: Current user ID: {}, username: {}", currentUser.getId(), currentUser.getUsername());
        log.info("DEBUG: Current profile picture: {}", currentUser.getProfilePicture());

        // Update user's profile picture
        currentUser.setProfilePicture(profilePictureUrl);

        // Save and flush to ensure immediate database update
        User updatedUser = userRepository.saveAndFlush(currentUser);

        log.info("DEBUG: Profile picture updated for user: {} with URL: {}",
                updatedUser.getUsername(), updatedUser.getProfilePicture());

        // Verify the update was persisted
        User verifyUser = userRepository.findById(updatedUser.getId()).orElse(null);
        if (verifyUser != null) {
            log.info("DEBUG: Verification - User {} profile picture in DB: {}",
                    verifyUser.getUsername(), verifyUser.getProfilePicture());
        } else {
            log.error("DEBUG: Could not find user after update!");
        }

        return convertToUserResponse(updatedUser);
    }

    /**
     * Get the current user's profile image as a Resource
     * @return Resource containing the profile image file, or null if no profile image exists
     */
    @Transactional(readOnly = true)
    public Resource getCurrentUserProfileImage() {
        User currentUser = getCurrentUser();
        return getUserProfileImageResource(currentUser);
    }

    /**
     * Get a specific user's profile image as a Resource
     * @param userId The user ID
     * @return Resource containing the profile image file, or null if no profile image exists
     */
    @Transactional(readOnly = true)
    public Resource getUserProfileImage(Long userId) {
        User user = getUserById(userId);
        return getUserProfileImageResource(user);
    }

    /**
     * Get the content type of the current user's profile image
     * @return Content type string, or null if no profile image exists
     */
    @Transactional(readOnly = true)
    public String getProfileImageContentType() {
        User currentUser = getCurrentUser();
        return getProfileImageContentTypeForUser(currentUser);
    }

    /**
     * Get the content type of a specific user's profile image
     * @param userId The user ID
     * @return Content type string, or null if no profile image exists
     */
    @Transactional(readOnly = true)
    public String getProfileImageContentType(Long userId) {
        User user = getUserById(userId);
        return getProfileImageContentTypeForUser(user);
    }

    /**
     * Helper method to get profile image resource for a user
     * @param user The user
     * @return Resource containing the profile image file, or null if no profile image exists
     */
    private Resource getUserProfileImageResource(User user) {
        if (user.getProfilePicture() == null || user.getProfilePicture().isEmpty()) {
            log.debug("PROFILE_IMAGE: User {} has no profile image", user.getUsername());
            return null;
        }

        try {
            // Extract filename from the profile picture URL
            // Profile picture URL format: "/api/files/download/{filename}"
            String profilePictureUrl = user.getProfilePicture();
            String filename = extractFilenameFromUrl(profilePictureUrl);

            if (filename == null) {
                log.error("PROFILE_IMAGE: Could not extract filename from URL: {}", profilePictureUrl);
                return null;
            }

            // Find the file metadata
            FileMetadata fileMetadata = fileMetadataService.findByFileName(filename);
            if (fileMetadata == null) {
                log.error("PROFILE_IMAGE: File metadata not found for filename: {}", filename);
                return null;
            }

            // Get the file path
            Path filePath = Paths.get(fileMetadata.getFilePath());
            log.debug("PROFILE_IMAGE: Attempting to access file at: {}", filePath.toAbsolutePath());

            // Check if file exists
            if (!Files.exists(filePath)) {
                log.error("PROFILE_IMAGE: File does not exist: {}", filePath.toAbsolutePath());
                return null;
            }

            // Create resource
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                log.error("PROFILE_IMAGE: File not accessible: {}", filePath.toAbsolutePath());
                return null;
            }

            log.debug("PROFILE_IMAGE: Successfully created resource for user {} profile image", user.getUsername());
            return resource;

        } catch (Exception e) {
            log.error("PROFILE_IMAGE: Error getting profile image for user {}: {}", user.getUsername(), e.getMessage());
            return null;
        }
    }

    /**
     * Helper method to get profile image content type for a user
     * @param user The user
     * @return Content type string, or null if no profile image exists
     */
    private String getProfileImageContentTypeForUser(User user) {
        if (user.getProfilePicture() == null || user.getProfilePicture().isEmpty()) {
            return null;
        }

        try {
            // Extract filename from the profile picture URL
            String profilePictureUrl = user.getProfilePicture();
            String filename = extractFilenameFromUrl(profilePictureUrl);

            if (filename == null) {
                log.error("PROFILE_IMAGE: Could not extract filename from URL: {}", profilePictureUrl);
                return null;
            }

            // Find the file metadata
            FileMetadata fileMetadata = fileMetadataService.findByFileName(filename);
            if (fileMetadata == null) {
                log.error("PROFILE_IMAGE: File metadata not found for filename: {}", filename);
                return null;
            }

            return fileMetadata.getContentType();

        } catch (Exception e) {
            log.error("PROFILE_IMAGE: Error getting content type for user {}: {}", user.getUsername(), e.getMessage());
            return null;
        }
    }

    /**
     * Extract filename from profile picture URL
     * @param url The profile picture URL (e.g., "/api/files/download/filename.jpg")
     * @return The filename, or null if extraction fails
     */
    private String extractFilenameFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }

        // Handle URLs like "/api/files/download/filename.jpg"
        if (url.startsWith("/api/files/download/")) {
            return url.substring("/api/files/download/".length());
        }

        // Handle full URLs like "http://domain.com/api/files/download/filename.jpg"
        if (url.contains("/api/files/download/")) {
            int index = url.indexOf("/api/files/download/");
            return url.substring(index + "/api/files/download/".length());
        }

        // If it's just a filename, return as is
        if (!url.contains("/")) {
            return url;
        }

        log.warn("PROFILE_IMAGE: Could not extract filename from URL: {}", url);
        return null;
    }
}
