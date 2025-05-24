package com.chatapp.controller;

import com.chatapp.dto.AuthRequest;
import com.chatapp.dto.AuthResponse;
import com.chatapp.dto.RegisterRequest;
import com.chatapp.dto.UserResponse;
import com.chatapp.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private RegisterRequest registerRequest;
    private AuthRequest authRequest;
    private UserResponse userResponse;
    private AuthResponse authResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        
        // Configure ObjectMapper for LocalDateTime serialization
        objectMapper.findAndRegisterModules();
        
        registerRequest = RegisterRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password")
                .fullName("Test User")
                .build();

        authRequest = AuthRequest.builder()
                .usernameOrEmail("testuser")
                .password("password")
                .build();

        userResponse = UserResponse.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .fullName("Test User")
                .isOnline(false)
                .lastSeen(LocalDateTime.now())
                .build();

        authResponse = AuthResponse.builder()
                .accessToken("accessToken")
                .refreshToken("refreshToken")
                .tokenType("Bearer")
                .expiresIn(86400000L)
                .user(userResponse)
                .build();
    }

    @Test
    void register_ShouldReturnCreatedStatus_WhenRegistrationIsSuccessful() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenReturn(userResponse);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(userResponse.getId()))
                .andExpect(jsonPath("$.username").value(userResponse.getUsername()))
                .andExpect(jsonPath("$.email").value(userResponse.getEmail()))
                .andExpect(jsonPath("$.fullName").value(userResponse.getFullName()));
    }

    @Test
    void login_ShouldReturnAuthResponse_WhenCredentialsAreValid() throws Exception {
        when(authService.login(any(AuthRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(authResponse.getAccessToken()))
                .andExpect(jsonPath("$.refreshToken").value(authResponse.getRefreshToken()))
                .andExpect(jsonPath("$.tokenType").value(authResponse.getTokenType()))
                .andExpect(jsonPath("$.expiresIn").value(authResponse.getExpiresIn()))
                .andExpect(jsonPath("$.user.id").value(userResponse.getId()))
                .andExpect(jsonPath("$.user.username").value(userResponse.getUsername()));
    }

    @Test
    void refreshToken_ShouldReturnNewAuthResponse_WhenRefreshTokenIsValid() throws Exception {
        when(authService.refreshToken(any(String.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/refresh")
                .header("Authorization", "Bearer refreshToken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(authResponse.getAccessToken()))
                .andExpect(jsonPath("$.refreshToken").value(authResponse.getRefreshToken()))
                .andExpect(jsonPath("$.tokenType").value(authResponse.getTokenType()))
                .andExpect(jsonPath("$.expiresIn").value(authResponse.getExpiresIn()));
    }

    @Test
    void logout_ShouldReturnNoContent_WhenLogoutIsSuccessful() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", "Bearer accessToken"))
                .andExpect(status().isNoContent());
    }
}
