package com.chatapp.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ChatRoomResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testJsonSerializationWithIsPrivateTrue() throws Exception {
        // Arrange
        ChatRoomResponse response = ChatRoomResponse.builder()
                .id(1L)
                .name("Private Room")
                .isPrivate(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Act
        String json = objectMapper.writeValueAsString(response);

        // Assert
        assertNotNull(json);
        assertTrue(json.contains("\"isPrivate\":true"), "JSON should contain isPrivate:true");
        assertTrue(json.contains("\"name\":\"Private Room\""), "JSON should contain the room name");
        assertTrue(json.contains("\"id\":1"), "JSON should contain the room id");
    }

    @Test
    void testJsonSerializationWithIsPrivateFalse() throws Exception {
        // Arrange
        ChatRoomResponse response = ChatRoomResponse.builder()
                .id(2L)
                .name("Public Room")
                .isPrivate(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Act
        String json = objectMapper.writeValueAsString(response);

        // Assert
        assertNotNull(json);
        assertTrue(json.contains("\"isPrivate\":false"), "JSON should contain isPrivate:false");
        assertTrue(json.contains("\"name\":\"Public Room\""), "JSON should contain the room name");
        assertTrue(json.contains("\"id\":2"), "JSON should contain the room id");
    }

    @Test
    void testBuilderWithIsPrivateTrue() {
        // Arrange & Act
        ChatRoomResponse response = ChatRoomResponse.builder()
                .id(1L)
                .name("Test Room")
                .isPrivate(true)
                .build();

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Test Room", response.getName());
        assertTrue(response.isPrivate(), "Builder should correctly set isPrivate to true");
    }

    @Test
    void testBuilderWithIsPrivateFalse() {
        // Arrange & Act
        ChatRoomResponse response = ChatRoomResponse.builder()
                .id(2L)
                .name("Test Room")
                .isPrivate(false)
                .build();

        // Assert
        assertNotNull(response);
        assertEquals(2L, response.getId());
        assertEquals("Test Room", response.getName());
        assertFalse(response.isPrivate(), "Builder should correctly set isPrivate to false");
    }

    @Test
    void testDefaultConstructorAndSetters() {
        // Arrange & Act
        ChatRoomResponse response = new ChatRoomResponse();
        response.setId(3L);
        response.setName("Test Room");
        response.setPrivate(true);

        // Assert
        assertNotNull(response);
        assertEquals(3L, response.getId());
        assertEquals("Test Room", response.getName());
        assertTrue(response.isPrivate(), "Setter should correctly set isPrivate to true");
    }
}
