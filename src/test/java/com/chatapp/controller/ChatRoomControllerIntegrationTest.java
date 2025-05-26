package com.chatapp.controller;

import com.chatapp.dto.ChatRoomRequest;
import com.chatapp.dto.ChatRoomResponse;
import com.chatapp.model.User;
import com.chatapp.repository.UserRepository;
import com.chatapp.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
public class ChatRoomControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User testUser;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        // Setup MockMvc
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // Create a test user
        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password(passwordEncoder.encode("password"))
                .fullName("Test User")
                .isOnline(false)
                .roles(new HashSet<>(Set.of("USER")))
                .build();

        testUser = userRepository.save(testUser);

        // Create UserDetails for JWT token generation
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(testUser.getUsername())
                .password(testUser.getPassword())
                .authorities(testUser.getRoles().stream()
                        .map(role -> "ROLE_" + role)
                        .toArray(String[]::new))
                .build();

        // Generate JWT token for the test user
        jwtToken = jwtTokenProvider.generateToken(userDetails);
    }

    @Test
    void createChatRoom_WithIsPrivateTrue_ShouldCreatePrivateRoom() throws Exception {
        // Arrange
        ChatRoomRequest request = ChatRoomRequest.builder()
                .name("Private Test Room")
                .isPrivate(true)
                .build();

        // Act & Assert
        MvcResult result = mockMvc.perform(post("/api/chatrooms")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("Private Test Room"))
                .andExpect(jsonPath("$.isPrivate").value(true))
                .andReturn();

        // Parse the response to verify the isPrivate field
        String responseContent = result.getResponse().getContentAsString();
        ChatRoomResponse response = objectMapper.readValue(responseContent, ChatRoomResponse.class);

        assertNotNull(response);
        assertEquals("Private Test Room", response.getName());
        assertTrue(response.isPrivate(), "Chat room should be private but was public");
        assertNotNull(response.getId());
    }

    @Test
    void createChatRoom_WithIsPrivateFalse_ShouldCreatePublicRoom() throws Exception {
        // Arrange
        ChatRoomRequest request = ChatRoomRequest.builder()
                .name("Public Test Room")
                .isPrivate(false)
                .build();

        // Act & Assert
        MvcResult result = mockMvc.perform(post("/api/chatrooms")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("Public Test Room"))
                .andExpect(jsonPath("$.isPrivate").value(false))
                .andReturn();

        // Parse the response to verify the isPrivate field
        String responseContent = result.getResponse().getContentAsString();
        ChatRoomResponse response = objectMapper.readValue(responseContent, ChatRoomResponse.class);

        assertNotNull(response);
        assertEquals("Public Test Room", response.getName());
        assertFalse(response.isPrivate(), "Chat room should be public but was private");
        assertNotNull(response.getId());
    }

    @Test
    void createChatRoom_WithoutIsPrivateField_ShouldDefaultToFalse() throws Exception {
        // Arrange - Create request without explicitly setting isPrivate
        String requestJson = "{\"name\":\"Default Test Room\"}";

        // Act & Assert
        MvcResult result = mockMvc.perform(post("/api/chatrooms")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("Default Test Room"))
                .andExpect(jsonPath("$.isPrivate").value(false))
                .andReturn();

        // Parse the response to verify the isPrivate field
        String responseContent = result.getResponse().getContentAsString();
        ChatRoomResponse response = objectMapper.readValue(responseContent, ChatRoomResponse.class);

        assertNotNull(response);
        assertEquals("Default Test Room", response.getName());
        assertFalse(response.isPrivate(), "Chat room should default to public when isPrivate is not specified");
        assertNotNull(response.getId());
    }
}
