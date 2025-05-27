package com.chatapp.controller;

import com.chatapp.dto.UserResponse;
import com.chatapp.dto.UserUpdateRequest;
import com.chatapp.service.UserService;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        UserResponse user = userService.getUserResponseById(id);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<UserResponse> getCurrentUser() {
        UserResponse user = userService.getCurrentUserResponse();
        return ResponseEntity.ok(user);
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<UserResponse> updateCurrentUser(@Valid @RequestBody UserUpdateRequest request) {
        UserResponse updatedUser = userService.updateCurrentUser(request);
        return ResponseEntity.ok(updatedUser);
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<UserResponse>> searchUsers(@RequestParam String query) {
        List<UserResponse> users = userService.searchUsers(query);
        return ResponseEntity.ok(users);
    }

    /**
     * Add or upload a new profile image for the current user
     * @param file The image file to upload
     * @return Updated user response with new profile image URL
     */
    @PostMapping("/me/profile-image")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<UserResponse> addProfileImage(@RequestParam("file") MultipartFile file) {
        log.info("PROFILE_IMAGE: Adding profile image for current user");

        try {
            UserResponse updatedUser = userService.addProfileImage(file);
            log.info("PROFILE_IMAGE: Profile image added successfully for user ID: {}", updatedUser.getId());
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            log.error("PROFILE_IMAGE: Error adding profile image: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Update existing profile image for the current user
     * @param file The new image file to upload
     * @return Updated user response with new profile image URL
     */
    @PutMapping("/me/profile-image")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<UserResponse> updateProfileImage(@RequestParam("file") MultipartFile file) {
        log.info("PROFILE_IMAGE: Updating profile image for current user");

        try {
            UserResponse updatedUser = userService.updateProfileImage(file);
            log.info("PROFILE_IMAGE: Profile image updated successfully for user ID: {}", updatedUser.getId());
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            log.error("PROFILE_IMAGE: Error updating profile image: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Debug endpoint to test profile picture update
     * @param profilePictureUrl The profile picture URL to set
     * @return Updated user response
     */
    @PostMapping("/me/profile-picture-debug")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<UserResponse> debugSetProfilePicture(@RequestParam("url") String profilePictureUrl) {
        log.info("DEBUG: Setting profile picture URL: {}", profilePictureUrl);

        try {
            UserResponse updatedUser = userService.debugSetProfilePicture(profilePictureUrl);
            log.info("DEBUG: Profile picture set successfully for user ID: {}", updatedUser.getId());
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            log.error("DEBUG: Error setting profile picture: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Get the current user's profile image
     * @return The profile image file as a resource, or 404 if no profile image exists
     */
    @GetMapping("/me/profile-image/view")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Resource> getCurrentUserProfileImage() {
        log.info("PROFILE_IMAGE: Getting profile image for current user");

        try {
            Resource profileImage = userService.getCurrentUserProfileImage();
            if (profileImage == null) {
                log.info("PROFILE_IMAGE: Current user has no profile image");
                return ResponseEntity.notFound().build();
            }

            // Determine content type
            String contentType = userService.getProfileImageContentType();
            if (contentType == null) {
                contentType = "image/jpeg"; // Default fallback
            }

            log.info("PROFILE_IMAGE: Successfully retrieved profile image for current user");
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                    .body(profileImage);
        } catch (Exception e) {
            log.error("PROFILE_IMAGE: Error getting current user profile image: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get a specific user's profile image by user ID
     * @param id The user ID
     * @return The profile image file as a resource, or 404 if no profile image exists
     */
    @GetMapping("/{id}/profile-image/view")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Resource> getUserProfileImage(@PathVariable Long id) {
        log.info("PROFILE_IMAGE: Getting profile image for user ID: {}", id);

        try {
            Resource profileImage = userService.getUserProfileImage(id);
            if (profileImage == null) {
                log.info("PROFILE_IMAGE: User {} has no profile image", id);
                return ResponseEntity.notFound().build();
            }

            // Determine content type
            String contentType = userService.getProfileImageContentType(id);
            if (contentType == null) {
                contentType = "image/jpeg"; // Default fallback
            }

            log.info("PROFILE_IMAGE: Successfully retrieved profile image for user ID: {}", id);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                    .body(profileImage);
        } catch (Exception e) {
            log.error("PROFILE_IMAGE: Error getting profile image for user {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}
