package com.chatapp.service;

import com.chatapp.dto.BlockUserRequest;
import com.chatapp.dto.BlockedUserResponse;
import com.chatapp.exception.BadRequestException;
import com.chatapp.model.BlockedUser;
import com.chatapp.model.User;
import com.chatapp.repository.BlockedUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing user blocking functionality
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserBlockingService {

    private final BlockedUserRepository blockedUserRepository;
    private final UserService userService;

    /**
     * Block a user
     */
    public BlockedUserResponse blockUser(BlockUserRequest request) {
        User currentUser = userService.getCurrentUser();
        User userToBlock = userService.getUserById(request.getUserId());

        // Validate the request
        validateBlockRequest(currentUser, userToBlock);

        // Check if already blocked
        if (blockedUserRepository.isUserBlocked(currentUser, userToBlock)) {
            throw new BadRequestException("User is already blocked");
        }

        // Create block relationship
        BlockedUser blockedUser = BlockedUser.builder()
            .blocker(currentUser)
            .blocked(userToBlock)
            .reason(request.getReason())
            .build();

        blockedUser = blockedUserRepository.save(blockedUser);
        log.info("USER_BLOCKING: User {} blocked user {}",
                currentUser.getUsername(), userToBlock.getUsername());

        return BlockedUserResponse.fromEntity(blockedUser);
    }

    /**
     * Unblock a user
     */
    public void unblockUser(Long userId) {
        User currentUser = userService.getCurrentUser();
        User userToUnblock = userService.getUserById(userId);

        // Check if user is actually blocked
        if (!blockedUserRepository.isUserBlocked(currentUser, userToUnblock)) {
            throw new BadRequestException("User is not blocked");
        }

        // Remove block relationship
        blockedUserRepository.deleteByBlockerAndBlocked(currentUser, userToUnblock);
        log.info("USER_BLOCKING: User {} unblocked user {}",
                currentUser.getUsername(), userToUnblock.getUsername());
    }

    /**
     * Get all users blocked by the current user
     */
    @Transactional(readOnly = true)
    public List<BlockedUserResponse> getBlockedUsers() {
        User currentUser = userService.getCurrentUser();
        List<BlockedUser> blockedUsers = blockedUserRepository.findByBlocker(currentUser);

        return blockedUsers.stream()
            .map(BlockedUserResponse::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Check if current user has blocked a specific user
     */
    @Transactional(readOnly = true)
    public boolean isUserBlocked(Long userId) {
        User currentUser = userService.getCurrentUser();
        User otherUser = userService.getUserById(userId);

        return blockedUserRepository.isUserBlocked(currentUser, otherUser);
    }

    /**
     * Check if there's any block relationship between two users (either direction)
     */
    @Transactional(readOnly = true)
    public boolean isBlocked(User user1, User user2) {
        return blockedUserRepository.isBlocked(user1, user2);
    }

    /**
     * Check if current user can send message to another user
     */
    @Transactional(readOnly = true)
    public boolean canSendMessageTo(User recipient) {
        User currentUser = userService.getCurrentUser();
        return !blockedUserRepository.isBlocked(currentUser, recipient);
    }

    /**
     * Get count of blocked users for current user
     */
    @Transactional(readOnly = true)
    public long getBlockedUsersCount() {
        User currentUser = userService.getCurrentUser();
        return blockedUserRepository.countBlockedUsers(currentUser);
    }

    /**
     * Validate block request
     */
    private void validateBlockRequest(User blocker, User userToBlock) {
        // Cannot block yourself
        if (blocker.getId().equals(userToBlock.getId())) {
            throw new BadRequestException("Cannot block yourself");
        }

        // Additional validation can be added here
        // e.g., check if user exists, is active, etc.
    }

    /**
     * Clean up all block relationships for a user (used when deleting user)
     */
    public void cleanupBlockRelationships(User user) {
        blockedUserRepository.deleteAllByUser(user);
        log.info("USER_BLOCKING: Cleaned up all block relationships for user {}", user.getUsername());
    }
}
