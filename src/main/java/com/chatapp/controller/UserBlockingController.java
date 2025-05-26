package com.chatapp.controller;

import com.chatapp.dto.BlockUserRequest;
import com.chatapp.dto.BlockedUserResponse;
import com.chatapp.service.UserBlockingService;
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
import java.util.List;
import java.util.Map;

/**
 * REST controller for user blocking functionality
 */
@RestController
@RequestMapping("/api/users/blocking")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Blocking", description = "User blocking and unblocking operations")
public class UserBlockingController {

    private final UserBlockingService userBlockingService;

    @Operation(summary = "Block a user", description = "Block another user to prevent communication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User blocked successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request - user already blocked or invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping("/block")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<BlockedUserResponse> blockUser(
            @Parameter(description = "Block user request") @Valid @RequestBody BlockUserRequest request) {
        
        BlockedUserResponse response = userBlockingService.blockUser(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Unblock a user", description = "Remove block on a previously blocked user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "User unblocked successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request - user not blocked"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping("/unblock/{userId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> unblockUser(
            @Parameter(description = "ID of the user to unblock") @PathVariable Long userId) {
        
        userBlockingService.unblockUser(userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get blocked users", description = "Get list of all users blocked by the current user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Blocked users retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/blocked")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<BlockedUserResponse>> getBlockedUsers() {
        List<BlockedUserResponse> blockedUsers = userBlockingService.getBlockedUsers();
        return ResponseEntity.ok(blockedUsers);
    }

    @Operation(summary = "Check if user is blocked", description = "Check if a specific user is blocked by the current user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Block status retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/is-blocked/{userId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Boolean>> isUserBlocked(
            @Parameter(description = "ID of the user to check") @PathVariable Long userId) {
        
        boolean isBlocked = userBlockingService.isUserBlocked(userId);
        return ResponseEntity.ok(Map.of("isBlocked", isBlocked));
    }

    @Operation(summary = "Get blocked users count", description = "Get count of users blocked by the current user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Count retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/count")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Long>> getBlockedUsersCount() {
        long count = userBlockingService.getBlockedUsersCount();
        return ResponseEntity.ok(Map.of("blockedUsersCount", count));
    }
}
