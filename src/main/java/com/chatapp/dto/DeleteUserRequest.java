package com.chatapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * DTO for user account deletion request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeleteUserRequest {

    @NotBlank(message = "Password is required for account deletion")
    private String password;

    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    private String reason;

    @Builder.Default
    private boolean deleteData = true; // Whether to delete all user data or just deactivate
}
