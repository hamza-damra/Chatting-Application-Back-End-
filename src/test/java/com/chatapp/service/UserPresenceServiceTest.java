package com.chatapp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class UserPresenceServiceTest {

    private UserPresenceService userPresenceService;

    @BeforeEach
    void setUp() {
        userPresenceService = new UserPresenceService();
    }

    @Test
    void testMarkUserActiveInRoom() {
        // Given
        String username = "testUser";
        Long roomId = 1L;

        // When
        userPresenceService.markUserActiveInRoom(username, roomId);

        // Then
        assertTrue(userPresenceService.isUserActiveInRoom(username, roomId));
        assertTrue(userPresenceService.getActiveUsersInRoom(roomId).contains(username));
        assertTrue(userPresenceService.getActiveRoomsForUser(username).contains(roomId));
    }

    @Test
    void testMarkUserInactiveInRoom() {
        // Given
        String username = "testUser";
        Long roomId = 1L;
        userPresenceService.markUserActiveInRoom(username, roomId);

        // When
        userPresenceService.markUserInactiveInRoom(username, roomId);

        // Then
        assertFalse(userPresenceService.isUserActiveInRoom(username, roomId));
        assertFalse(userPresenceService.getActiveUsersInRoom(roomId).contains(username));
        assertFalse(userPresenceService.getActiveRoomsForUser(username).contains(roomId));
    }

    @Test
    void testMarkUserOffline() {
        // Given
        String username = "testUser";
        Long roomId1 = 1L;
        Long roomId2 = 2L;
        userPresenceService.markUserActiveInRoom(username, roomId1);
        userPresenceService.markUserActiveInRoom(username, roomId2);

        // When
        userPresenceService.markUserOffline(username);

        // Then
        assertFalse(userPresenceService.isUserActiveInRoom(username, roomId1));
        assertFalse(userPresenceService.isUserActiveInRoom(username, roomId2));
        assertTrue(userPresenceService.getActiveRoomsForUser(username).isEmpty());
    }

    @Test
    void testMultipleUsersInSameRoom() {
        // Given
        String user1 = "user1";
        String user2 = "user2";
        Long roomId = 1L;

        // When
        userPresenceService.markUserActiveInRoom(user1, roomId);
        userPresenceService.markUserActiveInRoom(user2, roomId);

        // Then
        Set<String> activeUsers = userPresenceService.getActiveUsersInRoom(roomId);
        assertEquals(2, activeUsers.size());
        assertTrue(activeUsers.contains(user1));
        assertTrue(activeUsers.contains(user2));
    }

    @Test
    void testUserInMultipleRooms() {
        // Given
        String username = "testUser";
        Long roomId1 = 1L;
        Long roomId2 = 2L;

        // When
        userPresenceService.markUserActiveInRoom(username, roomId1);
        userPresenceService.markUserActiveInRoom(username, roomId2);

        // Then
        Set<Long> activeRooms = userPresenceService.getActiveRoomsForUser(username);
        assertEquals(2, activeRooms.size());
        assertTrue(activeRooms.contains(roomId1));
        assertTrue(activeRooms.contains(roomId2));
    }

    @Test
    void testNullHandling() {
        // Test null username
        userPresenceService.markUserActiveInRoom(null, 1L);
        assertFalse(userPresenceService.isUserActiveInRoom(null, 1L));

        // Test null roomId
        userPresenceService.markUserActiveInRoom("user", null);
        assertFalse(userPresenceService.isUserActiveInRoom("user", null));

        // Test null parameters in other methods
        userPresenceService.markUserInactiveInRoom(null, 1L);
        userPresenceService.markUserInactiveInRoom("user", null);
        userPresenceService.markUserOffline(null);

        // Should not throw exceptions
        assertTrue(userPresenceService.getActiveUsersInRoom(null).isEmpty());
        assertTrue(userPresenceService.getActiveRoomsForUser(null).isEmpty());
    }

    @Test
    void testPresenceStats() {
        // Given
        userPresenceService.markUserActiveInRoom("user1", 1L);
        userPresenceService.markUserActiveInRoom("user2", 1L);
        userPresenceService.markUserActiveInRoom("user1", 2L);

        // When
        var stats = userPresenceService.getPresenceStats();

        // Then
        assertEquals(2, stats.get("totalActiveRooms"));
        assertEquals(2, stats.get("totalActiveUsers"));
        assertNotNull(stats.get("roomPresenceDetails"));
        assertNotNull(stats.get("userPresenceDetails"));
    }

    @Test
    void testCleanupWhenLastUserLeavesRoom() {
        // Given
        String user1 = "user1";
        String user2 = "user2";
        Long roomId = 1L;
        userPresenceService.markUserActiveInRoom(user1, roomId);
        userPresenceService.markUserActiveInRoom(user2, roomId);

        // When - remove first user
        userPresenceService.markUserInactiveInRoom(user1, roomId);

        // Then - room should still exist with one user
        assertEquals(1, userPresenceService.getActiveUsersInRoom(roomId).size());
        assertTrue(userPresenceService.getActiveUsersInRoom(roomId).contains(user2));

        // When - remove last user
        userPresenceService.markUserInactiveInRoom(user2, roomId);

        // Then - room should be cleaned up
        assertTrue(userPresenceService.getActiveUsersInRoom(roomId).isEmpty());
    }

    @Test
    void testCleanupWhenUserLeavesAllRooms() {
        // Given
        String username = "testUser";
        Long roomId1 = 1L;
        Long roomId2 = 2L;
        userPresenceService.markUserActiveInRoom(username, roomId1);
        userPresenceService.markUserActiveInRoom(username, roomId2);

        // When - remove from first room
        userPresenceService.markUserInactiveInRoom(username, roomId1);

        // Then - user should still be tracked with one room
        assertEquals(1, userPresenceService.getActiveRoomsForUser(username).size());
        assertTrue(userPresenceService.getActiveRoomsForUser(username).contains(roomId2));

        // When - remove from last room
        userPresenceService.markUserInactiveInRoom(username, roomId2);

        // Then - user should be cleaned up
        assertTrue(userPresenceService.getActiveRoomsForUser(username).isEmpty());
    }
}
