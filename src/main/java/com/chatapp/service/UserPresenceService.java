package com.chatapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Service for tracking user presence in specific chat rooms
 * Determines which users are currently active/viewing specific chat rooms
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserPresenceService {

    // Map: roomId -> Set of usernames currently active in that room
    private final Map<Long, Set<String>> roomPresence = new ConcurrentHashMap<>();
    
    // Map: username -> Set of roomIds the user is currently active in
    private final Map<String, Set<Long>> userPresence = new ConcurrentHashMap<>();

    /**
     * Mark a user as active in a specific chat room
     * @param username The username of the user
     * @param roomId The chat room ID
     */
    public void markUserActiveInRoom(String username, Long roomId) {
        if (username == null || roomId == null) {
            log.warn("PRESENCE: Cannot mark user active - username or roomId is null");
            return;
        }

        // Add user to room's active users
        roomPresence.computeIfAbsent(roomId, k -> new CopyOnWriteArraySet<>()).add(username);
        
        // Add room to user's active rooms
        userPresence.computeIfAbsent(username, k -> new CopyOnWriteArraySet<>()).add(roomId);
        
        log.debug("PRESENCE: User {} is now active in room {}", username, roomId);
    }

    /**
     * Mark a user as no longer active in a specific chat room
     * @param username The username of the user
     * @param roomId The chat room ID
     */
    public void markUserInactiveInRoom(String username, Long roomId) {
        if (username == null || roomId == null) {
            log.warn("PRESENCE: Cannot mark user inactive - username or roomId is null");
            return;
        }

        // Remove user from room's active users
        Set<String> roomUsers = roomPresence.get(roomId);
        if (roomUsers != null) {
            roomUsers.remove(username);
            if (roomUsers.isEmpty()) {
                roomPresence.remove(roomId);
            }
        }
        
        // Remove room from user's active rooms
        Set<Long> userRooms = userPresence.get(username);
        if (userRooms != null) {
            userRooms.remove(roomId);
            if (userRooms.isEmpty()) {
                userPresence.remove(username);
            }
        }
        
        log.debug("PRESENCE: User {} is no longer active in room {}", username, roomId);
    }

    /**
     * Remove user from all rooms (called when user disconnects)
     * @param username The username of the user
     */
    public void markUserOffline(String username) {
        if (username == null) {
            log.warn("PRESENCE: Cannot mark user offline - username is null");
            return;
        }

        Set<Long> userRooms = userPresence.remove(username);
        if (userRooms != null) {
            for (Long roomId : userRooms) {
                Set<String> roomUsers = roomPresence.get(roomId);
                if (roomUsers != null) {
                    roomUsers.remove(username);
                    if (roomUsers.isEmpty()) {
                        roomPresence.remove(roomId);
                    }
                }
            }
        }
        
        log.debug("PRESENCE: User {} marked as offline and removed from all rooms", username);
    }

    /**
     * Check if a user is currently active in a specific chat room
     * @param username The username to check
     * @param roomId The chat room ID to check
     * @return true if the user is active in the room, false otherwise
     */
    public boolean isUserActiveInRoom(String username, Long roomId) {
        if (username == null || roomId == null) {
            return false;
        }

        Set<String> roomUsers = roomPresence.get(roomId);
        return roomUsers != null && roomUsers.contains(username);
    }

    /**
     * Get all users currently active in a specific chat room
     * @param roomId The chat room ID
     * @return Set of usernames active in the room
     */
    public Set<String> getActiveUsersInRoom(Long roomId) {
        if (roomId == null) {
            return Set.of();
        }

        Set<String> roomUsers = roomPresence.get(roomId);
        return roomUsers != null ? Set.copyOf(roomUsers) : Set.of();
    }

    /**
     * Get all rooms where a user is currently active
     * @param username The username
     * @return Set of room IDs where the user is active
     */
    public Set<Long> getActiveRoomsForUser(String username) {
        if (username == null) {
            return Set.of();
        }

        Set<Long> userRooms = userPresence.get(username);
        return userRooms != null ? Set.copyOf(userRooms) : Set.of();
    }

    /**
     * Get current presence statistics for monitoring
     * @return Map with presence statistics
     */
    public Map<String, Object> getPresenceStats() {
        return Map.of(
            "totalActiveRooms", roomPresence.size(),
            "totalActiveUsers", userPresence.size(),
            "roomPresenceDetails", Map.copyOf(roomPresence),
            "userPresenceDetails", Map.copyOf(userPresence)
        );
    }
}
