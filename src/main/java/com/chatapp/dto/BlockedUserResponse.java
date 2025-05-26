package com.chatapp.dto;

import com.chatapp.model.BlockedUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for blocked user response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockedUserResponse {

    private Long id;
    private UserResponse blockedUser;
    private LocalDateTime blockedAt;
    private String reason;

    /**
     * Create a BlockedUserResponse from a BlockedUser entity
     */
    public static BlockedUserResponse fromEntity(BlockedUser blockedUser) {
        return BlockedUserResponse.builder()
            .id(blockedUser.getId())
            .blockedUser(UserResponse.builder()
                .id(blockedUser.getBlocked().getId())
                .username(blockedUser.getBlocked().getUsername())
                .email(blockedUser.getBlocked().getEmail())
                .fullName(blockedUser.getBlocked().getFullName())
                .profilePicture(blockedUser.getBlocked().getProfilePicture())
                .lastSeen(blockedUser.getBlocked().getLastSeen())
                .isOnline(blockedUser.getBlocked().isOnline())
                .build())
            .blockedAt(blockedUser.getCreatedAt())
            .reason(blockedUser.getReason())
            .build();
    }
}
