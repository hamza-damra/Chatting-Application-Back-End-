package com.chatapp.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ChatRoomRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testJsonDeserializationWithIsPrivateTrue() throws Exception {
        // Arrange
        String json = "{\"name\":\"Private Room\",\"isPrivate\":true}";

        // Act
        ChatRoomRequest request = objectMapper.readValue(json, ChatRoomRequest.class);

        // Assert
        assertNotNull(request);
        assertEquals("Private Room", request.getName());
        assertTrue(request.isPrivate(), "isPrivate should be true when set to true in JSON");
    }

    @Test
    void testJsonDeserializationWithIsPrivateFalse() throws Exception {
        // Arrange
        String json = "{\"name\":\"Public Room\",\"isPrivate\":false}";

        // Act
        ChatRoomRequest request = objectMapper.readValue(json, ChatRoomRequest.class);

        // Assert
        assertNotNull(request);
        assertEquals("Public Room", request.getName());
        assertFalse(request.isPrivate(), "isPrivate should be false when set to false in JSON");
    }

    @Test
    void testJsonDeserializationWithoutIsPrivateField() throws Exception {
        // Arrange
        String json = "{\"name\":\"Default Room\"}";

        // Act
        ChatRoomRequest request = objectMapper.readValue(json, ChatRoomRequest.class);

        // Assert
        assertNotNull(request);
        assertEquals("Default Room", request.getName());
        assertFalse(request.isPrivate(), "isPrivate should default to false when not specified");
    }

    @Test
    void testJsonSerializationWithIsPrivateTrue() throws Exception {
        // Arrange
        ChatRoomRequest request = ChatRoomRequest.builder()
                .name("Private Room")
                .isPrivate(true)
                .build();

        // Act
        String json = objectMapper.writeValueAsString(request);

        // Assert
        assertNotNull(json);
        assertTrue(json.contains("\"isPrivate\":true"), "JSON should contain isPrivate:true");
        assertTrue(json.contains("\"name\":\"Private Room\""), "JSON should contain the room name");
    }

    @Test
    void testJsonSerializationWithIsPrivateFalse() throws Exception {
        // Arrange
        ChatRoomRequest request = ChatRoomRequest.builder()
                .name("Public Room")
                .isPrivate(false)
                .build();

        // Act
        String json = objectMapper.writeValueAsString(request);

        // Assert
        assertNotNull(json);
        assertTrue(json.contains("\"isPrivate\":false"), "JSON should contain isPrivate:false");
        assertTrue(json.contains("\"name\":\"Public Room\""), "JSON should contain the room name");
    }

    @Test
    void testBuilderWithIsPrivateTrue() {
        // Arrange & Act
        ChatRoomRequest request = ChatRoomRequest.builder()
                .name("Test Room")
                .isPrivate(true)
                .build();

        // Assert
        assertNotNull(request);
        assertEquals("Test Room", request.getName());
        assertTrue(request.isPrivate(), "Builder should correctly set isPrivate to true");
    }

    @Test
    void testBuilderWithIsPrivateFalse() {
        // Arrange & Act
        ChatRoomRequest request = ChatRoomRequest.builder()
                .name("Test Room")
                .isPrivate(false)
                .build();

        // Assert
        assertNotNull(request);
        assertEquals("Test Room", request.getName());
        assertFalse(request.isPrivate(), "Builder should correctly set isPrivate to false");
    }

    @Test
    void testDefaultConstructorAndSetters() {
        // Arrange & Act
        ChatRoomRequest request = new ChatRoomRequest();
        request.setName("Test Room");
        request.setPrivate(true);

        // Assert
        assertNotNull(request);
        assertEquals("Test Room", request.getName());
        assertTrue(request.isPrivate(), "Setter should correctly set isPrivate to true");
    }
}
