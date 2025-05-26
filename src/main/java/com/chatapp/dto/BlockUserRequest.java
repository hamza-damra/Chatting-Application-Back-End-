package com.chatapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * DTO for blocking a user request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockUserRequest {

    @NotNull(message = "User ID to block is required")
    private Long userId;

    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    private String reason;
}
