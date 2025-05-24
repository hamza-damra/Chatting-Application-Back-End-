package com.chatapp.service;

import com.chatapp.dto.UserResponse;
import com.chatapp.dto.UserUpdateRequest;
import com.chatapp.exception.ResourceNotFoundException;
import com.chatapp.model.User;
import com.chatapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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
}
