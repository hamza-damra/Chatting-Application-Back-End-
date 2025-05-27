package com.chatapp.service;

import com.chatapp.dto.DeleteUserRequest;
import com.chatapp.exception.BadRequestException;
import com.chatapp.exception.UnauthorizedException;
import com.chatapp.model.User;
import com.chatapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for user management operations like deletion and deactivation
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserManagementService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final UserBlockingService userBlockingService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Delete current user's account
     */
    public void deleteCurrentUserAccount(DeleteUserRequest request) {
        User currentUser = userService.getCurrentUser();

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), currentUser.getPassword())) {
            throw new UnauthorizedException("Invalid password");
        }

        deleteUserAccount(currentUser, request.getReason(), request.isDeleteData());
    }

    /**
     * Delete user account (admin function)
     */
    public void deleteUserAccount(Long userId, String reason, boolean deleteData) {
        User currentUser = userService.getCurrentUser();

        // Check if current user is admin
        if (!currentUser.getRoles().contains("ADMIN")) {
            throw new UnauthorizedException("Only administrators can delete other users");
        }

        User userToDelete = userService.getUserById(userId);

        // Prevent admin from deleting themselves through this method
        if (currentUser.getId().equals(userToDelete.getId())) {
            throw new BadRequestException("Use the self-deletion endpoint to delete your own account");
        }

        deleteUserAccount(userToDelete, reason, deleteData);
    }

    /**
     * Deactivate user account (soft delete)
     */
    public void deactivateUserAccount(Long userId, String reason) {
        User currentUser = userService.getCurrentUser();

        // Check if current user is admin
        if (!currentUser.getRoles().contains("ADMIN")) {
            throw new UnauthorizedException("Only administrators can deactivate users");
        }

        User userToDeactivate = userService.getUserById(userId);

        // Add deactivated flag to user (we'll need to add this field to User entity)
        // For now, we'll use a role-based approach
        userToDeactivate.getRoles().add("DEACTIVATED");
        userToDeactivate.setOnline(false);
        userToDeactivate.setLastSeen(LocalDateTime.now());

        userRepository.save(userToDeactivate);
        log.info("USER_MANAGEMENT: User {} deactivated by admin {}. Reason: {}",
                userToDeactivate.getUsername(), currentUser.getUsername(), reason);
    }

    /**
     * Reactivate user account
     */
    public void reactivateUserAccount(Long userId) {
        User currentUser = userService.getCurrentUser();

        // Check if current user is admin
        if (!currentUser.getRoles().contains("ADMIN")) {
            throw new UnauthorizedException("Only administrators can reactivate users");
        }

        User userToReactivate = userService.getUserById(userId);

        // Remove deactivated flag
        userToReactivate.getRoles().remove("DEACTIVATED");

        userRepository.save(userToReactivate);
        log.info("USER_MANAGEMENT: User {} reactivated by admin {}",
                userToReactivate.getUsername(), currentUser.getUsername());
    }

    /**
     * Check if user is deactivated
     */
    public boolean isUserDeactivated(User user) {
        return user.getRoles().contains("DEACTIVATED");
    }

    /**
     * Internal method to delete user account
     */
    private void deleteUserAccount(User userToDelete, String reason, boolean deleteData) {
        log.info("USER_MANAGEMENT: Starting deletion process for user {}. Delete data: {}",
                userToDelete.getUsername(), deleteData);

        try {
            if (deleteData) {
                // Clean up all user data
                cleanupUserData(userToDelete);
            } else {
                // Just deactivate the account
                userToDelete.getRoles().add("DELETED");
                userToDelete.setOnline(false);
                userToDelete.setLastSeen(LocalDateTime.now());
                userRepository.save(userToDelete);
            }

            log.info("USER_MANAGEMENT: User {} deletion completed. Reason: {}",
                    userToDelete.getUsername(), reason);

        } catch (Exception e) {
            log.error("USER_MANAGEMENT: Failed to delete user {}", userToDelete.getUsername(), e);
            throw new RuntimeException("Failed to delete user account", e);
        }
    }

    /**
     * Clean up all user data
     */
    private void cleanupUserData(User user) {
        log.info("USER_MANAGEMENT: Cleaning up data for user {}", user.getUsername());

        // 1. Clean up block relationships
        userBlockingService.cleanupBlockRelationships(user);

        // 2. Remove user from all chat rooms
        // Note: We don't delete the chat rooms, just remove the user
        user.getChatRooms().forEach(chatRoom -> {
            chatRoom.removeParticipant(user);
        });

        // 3. Handle messages - we'll keep messages but anonymize them
        // This preserves chat history while removing personal data
        user.getSentMessages().forEach(message -> {
            // We could either delete messages or anonymize them
            // For chat history preservation, we'll anonymize
            message.setSender(null); // This will require handling in the frontend
        });

        // 4. Clean up file uploads
        user.getUploadedFiles().forEach(fileMetadata -> {
            fileMetadata.setUploadedBy(null);
        });

        // 5. Clean up message statuses
        user.getMessageStatuses().clear();

        // 6. Finally delete the user
        userRepository.delete(user);

        log.info("USER_MANAGEMENT: Data cleanup completed for user {}", user.getUsername());
    }
}
