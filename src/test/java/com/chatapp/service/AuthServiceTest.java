package com.chatapp.service;

import com.chatapp.dto.AuthRequest;
import com.chatapp.dto.AuthResponse;
import com.chatapp.dto.RegisterRequest;
import com.chatapp.dto.UserResponse;
import com.chatapp.exception.BadRequestException;
import com.chatapp.exception.UnauthorizedException;
import com.chatapp.model.User;
import com.chatapp.repository.UserRepository;
import com.chatapp.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserService userService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private User user;
    private UserResponse userResponse;
    private AuthRequest authRequest;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password")
                .fullName("Test User")
                .build();

        user = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .fullName("Test User")
                .isOnline(false)
                .createdAt(LocalDateTime.now())
                .lastSeen(LocalDateTime.now())
                .roles(new HashSet<>(Set.of("ROLE_USER")))
                .build();

        userResponse = UserResponse.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .fullName("Test User")
                .isOnline(false)
                .build();

        authRequest = AuthRequest.builder()
                .usernameOrEmail("testuser")
                .password("password")
                .build();
    }

    @Test
    void register_ShouldCreateNewUser_WhenUsernameAndEmailAreUnique() {
        // Arrange
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userService.convertToUserResponse(user)).thenReturn(userResponse);

        // Act
        UserResponse result = authService.register(registerRequest);

        // Assert
        assertNotNull(result);
        assertEquals(userResponse.getId(), result.getId());
        assertEquals(userResponse.getUsername(), result.getUsername());
        assertEquals(userResponse.getEmail(), result.getEmail());

        verify(userRepository).existsByUsername(registerRequest.getUsername());
        verify(userRepository).existsByEmail(registerRequest.getEmail());
        verify(passwordEncoder).encode(registerRequest.getPassword());
        verify(userRepository).save(any(User.class));
        verify(userService).convertToUserResponse(any(User.class));
    }

    @Test
    void register_ShouldThrowBadRequestException_WhenUsernameExists() {
        // Arrange
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(true);

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            authService.register(registerRequest);
        });

        assertEquals("Username is already taken", exception.getMessage());
        verify(userRepository).existsByUsername(registerRequest.getUsername());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_ShouldThrowBadRequestException_WhenEmailExists() {
        // Arrange
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            authService.register(registerRequest);
        });

        assertEquals("Email is already registered", exception.getMessage());
        verify(userRepository).existsByUsername(registerRequest.getUsername());
        verify(userRepository).existsByEmail(registerRequest.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_ShouldReturnAuthResponse_WhenCredentialsAreValidWithUsername() {
        // Arrange
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("testuser")
                .password("encodedPassword")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(tokenProvider.generateToken(userDetails)).thenReturn("accessToken");
        when(tokenProvider.generateRefreshToken(userDetails)).thenReturn("refreshToken");
        when(userRepository.findByUsername(userDetails.getUsername())).thenReturn(Optional.of(user));
        when(userService.convertToUserResponse(user)).thenReturn(userResponse);

        // Act
        AuthResponse result = authService.login(authRequest);

        // Assert
        assertNotNull(result);
        assertEquals("accessToken", result.getAccessToken());
        assertEquals("refreshToken", result.getRefreshToken());
        assertEquals("Bearer", result.getTokenType());
        assertNotNull(result.getUser());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(tokenProvider).generateToken(userDetails);
        verify(tokenProvider).generateRefreshToken(userDetails);
        verify(userRepository).findByUsername(userDetails.getUsername());
        verify(userRepository).save(any(User.class));
        verify(userService).convertToUserResponse(user);
    }

    @Test
    void login_ShouldReturnAuthResponse_WhenCredentialsAreValidWithEmail() {
        // Arrange
        AuthRequest emailAuthRequest = AuthRequest.builder()
                .usernameOrEmail("test@example.com")
                .password("password")
                .build();

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("testuser")
                .password("encodedPassword")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(tokenProvider.generateToken(userDetails)).thenReturn("accessToken");
        when(tokenProvider.generateRefreshToken(userDetails)).thenReturn("refreshToken");
        when(userRepository.findByUsername(userDetails.getUsername())).thenReturn(Optional.of(user));
        when(userService.convertToUserResponse(user)).thenReturn(userResponse);

        // Act
        AuthResponse result = authService.login(emailAuthRequest);

        // Assert
        assertNotNull(result);
        assertEquals("accessToken", result.getAccessToken());
        assertEquals("refreshToken", result.getRefreshToken());
        assertEquals("Bearer", result.getTokenType());
        assertNotNull(result.getUser());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(tokenProvider).generateToken(userDetails);
        verify(tokenProvider).generateRefreshToken(userDetails);
        verify(userRepository).findByUsername(userDetails.getUsername());
        verify(userRepository).save(any(User.class));
        verify(userService).convertToUserResponse(user);
    }

    @Test
    void refreshToken_ShouldReturnNewAuthResponse_WhenRefreshTokenIsValid() {
        // Arrange
        String refreshToken = "Bearer validRefreshToken";

        when(tokenProvider.extractUsername("validRefreshToken")).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(tokenProvider.isTokenValid(eq("validRefreshToken"), any(UserDetails.class))).thenReturn(true);
        when(tokenProvider.generateToken(any(UserDetails.class))).thenReturn("newAccessToken");
        when(tokenProvider.generateRefreshToken(any(UserDetails.class))).thenReturn("newRefreshToken");
        when(userService.convertToUserResponse(user)).thenReturn(userResponse);

        // Act
        AuthResponse result = authService.refreshToken(refreshToken);

        // Assert
        assertNotNull(result);
        assertEquals("newAccessToken", result.getAccessToken());
        assertEquals("newRefreshToken", result.getRefreshToken());
        assertEquals("Bearer", result.getTokenType());
        assertNotNull(result.getUser());

        verify(tokenProvider).extractUsername("validRefreshToken");
        verify(userRepository).findByUsername("testuser");
        verify(tokenProvider).isTokenValid(eq("validRefreshToken"), any(UserDetails.class));
        verify(tokenProvider).generateToken(any(UserDetails.class));
        verify(tokenProvider).generateRefreshToken(any(UserDetails.class));
        verify(userService).convertToUserResponse(user);
    }

    @Test
    void refreshToken_ShouldThrowUnauthorizedException_WhenRefreshTokenIsInvalid() {
        // Arrange
        String refreshToken = "Bearer invalidRefreshToken";
        when(tokenProvider.extractUsername("invalidRefreshToken")).thenReturn(null);

        // Act & Assert
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            authService.refreshToken(refreshToken);
        });

        assertEquals("Invalid refresh token", exception.getMessage());
        verify(tokenProvider).extractUsername("invalidRefreshToken");
        verify(userRepository, never()).findByUsername(anyString());
    }

    @Test
    void logout_ShouldUpdateUserStatus_WhenTokenIsValid() {
        // Arrange
        String token = "Bearer validToken";
        when(tokenProvider.extractUsername("validToken")).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        // Act
        authService.logout(token);

        // Assert
        verify(tokenProvider).extractUsername("validToken");
        verify(userRepository).findByUsername("testuser");
        verify(userRepository).save(user);
        assertFalse(user.isOnline());
        assertNotNull(user.getLastSeen());
    }

    @Test
    void logout_ShouldThrowUnauthorizedException_WhenTokenIsInvalid() {
        // Arrange
        String token = "Bearer invalidToken";
        when(tokenProvider.extractUsername("invalidToken")).thenReturn(null);

        // Act & Assert
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            authService.logout(token);
        });

        assertEquals("Invalid token", exception.getMessage());
        verify(tokenProvider).extractUsername("invalidToken");
        verify(userRepository, never()).findByUsername(anyString());
        verify(userRepository, never()).save(any(User.class));
    }
}
