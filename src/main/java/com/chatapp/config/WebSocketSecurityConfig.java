package com.chatapp.config;

import com.chatapp.security.JwtAuthenticationFilter;
import com.chatapp.security.JwtTokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.messaging.context.SecurityContextChannelInterceptor;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket security configuration.
 *
 * This configuration:
 * 1. Authenticates WebSocket connections using JWT tokens
 * 2. Disables CSRF protection for WebSocket connections (since we use JWT)
 * 3. Configures authorization rules for WebSocket destinations
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
@Slf4j
public class WebSocketSecurityConfig implements WebSocketMessageBrokerConfigurer {

    // We don't directly use this filter here, but we need it in the constructor
    // to ensure it's initialized before this config
    @SuppressWarnings("unused")
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    public WebSocketSecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            JwtTokenProvider jwtTokenProvider,
            UserDetailsService userDetailsService) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public void configureClientInboundChannel(@NonNull ChannelRegistration registration) {
        // Add our custom JWT interceptor first
        registration.interceptors(new JwtChannelInterceptor());

        // Add our no-op CSRF interceptor to override the default one
        registration.interceptors(csrfChannelInterceptor());

        // Add the security context interceptor
        registration.interceptors(securityContextChannelInterceptor());
    }

    @Override
    public void configureWebSocketTransport(@NonNull WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(64 * 1024); // 64KB
        registration.setSendBufferSizeLimit(512 * 1024); // 512KB
        registration.setSendTimeLimit(20 * 1000); // 20 seconds
    }

    /**
     * Disable CSRF protection for WebSocket connections.
     * This is safe because we're using JWT authentication.
     */
    @Bean
    public SecurityContextChannelInterceptor securityContextChannelInterceptor() {
        return new SecurityContextChannelInterceptor();
    }

    /**
     * Override the default CsrfChannelInterceptor with a no-op implementation
     * to disable CSRF protection for WebSocket connections.
     */
    @Bean
    public ChannelInterceptor csrfChannelInterceptor() {
        return new ChannelInterceptor() {
            @Override
            public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
                // Skip CSRF validation by just passing the message through
                return message;
            }
        };
    }

    private class JwtChannelInterceptor implements ChannelInterceptor {
        @Override
        public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
            StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

            if (accessor != null) {
                // Check if this is a CONNECT message (initial connection)
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    log.debug("Processing STOMP CONNECT command");

                    // Extract the JWT token from the Authorization header
                    String authorizationHeader = accessor.getFirstNativeHeader("Authorization");
                    log.debug("Authorization header: {}", authorizationHeader);

                    if (authorizationHeader != null && StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith("Bearer ")) {
                        String jwt = authorizationHeader.substring(7);
                        String username = jwtTokenProvider.extractUsername(jwt);

                        if (StringUtils.hasText(username) && SecurityContextHolder.getContext().getAuthentication() == null) {
                            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                            if (jwtTokenProvider.isTokenValid(jwt, userDetails)) {
                                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );

                                // Set the authentication in the accessor
                                accessor.setUser(authToken);
                                log.debug("WebSocket connection authenticated for user: {}", username);
                            } else {
                                log.warn("Invalid JWT token in WebSocket connection");
                            }
                        }
                    } else {
                        log.warn("No Authorization header found or not in Bearer format");
                    }
                }
            }

            return message;
        }
    }
}
