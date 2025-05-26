package com.chatapp.controller;

import com.chatapp.dto.DeleteUserRequest;
import com.chatapp.service.UserManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

/**
 * REST controller for user management operations
 */
@RestController
@RequestMapping("/api/users/management")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Management", description = "User account management operations")
public class UserManagementController {

    private final UserManagementService userManagementService;

    @Operation(summary = "Delete current user account", description = "Delete the current user's account permanently")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Account deleted successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request - invalid password or request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @DeleteMapping("/delete-account")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> deleteCurrentUserAccount(
            @Parameter(description = "Delete account request") @Valid @RequestBody DeleteUserRequest request) {
        
        userManagementService.deleteCurrentUserAccount(request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete user account (Admin)", description = "Delete another user's account (admin only)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "User account deleted successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - admin role required"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping("/delete-user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUserAccount(
            @Parameter(description = "ID of the user to delete") @PathVariable Long userId,
            @Parameter(description = "Reason for deletion") @RequestParam(required = false) String reason,
            @Parameter(description = "Whether to delete all data or just deactivate") @RequestParam(defaultValue = "true") boolean deleteData) {
        
        userManagementService.deleteUserAccount(userId, reason, deleteData);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Deactivate user account (Admin)", description = "Deactivate a user account (admin only)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "User account deactivated successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - admin role required"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PutMapping("/deactivate/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateUserAccount(
            @Parameter(description = "ID of the user to deactivate") @PathVariable Long userId,
            @Parameter(description = "Reason for deactivation") @RequestParam(required = false) String reason) {
        
        userManagementService.deactivateUserAccount(userId, reason);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Reactivate user account (Admin)", description = "Reactivate a deactivated user account (admin only)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "User account reactivated successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - admin role required"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PutMapping("/reactivate/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> reactivateUserAccount(
            @Parameter(description = "ID of the user to reactivate") @PathVariable Long userId) {
        
        userManagementService.reactivateUserAccount(userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Check account status", description = "Check if a user account is active or deactivated")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Account status retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/status/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getUserAccountStatus(
            @Parameter(description = "ID of the user to check") @PathVariable Long userId) {
        
        // This would require getting the user and checking their status
        // Implementation depends on how we want to expose this information
        return ResponseEntity.ok(Map.of(
            "message", "Account status check endpoint",
            "userId", userId
        ));
    }
}
