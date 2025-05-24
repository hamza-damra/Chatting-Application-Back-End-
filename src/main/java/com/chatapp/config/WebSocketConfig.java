package com.chatapp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.lang.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final CorsProperties corsProperties;
    private final WebSocketProperties webSocketProperties;

    @Override
    public void configureMessageBroker(@NonNull MessageBrokerRegistry config) {
        // Enable a simple in-memory message broker to carry messages back to the client
        // Prefix for messages FROM server TO client
        config.enableSimpleBroker("/topic", "/queue", "/user");

        // Prefix for messages FROM client TO server
        config.setApplicationDestinationPrefixes("/app");

        // Enable user-specific messaging with the /user/ prefix
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        // Register STOMP endpoints where clients will connect
        registry.addEndpoint("/ws")
                .setAllowedOrigins(corsProperties.getAllowedOrigins().split(","));

        // Also register with SockJS for fallback
        registry.addEndpoint("/ws")
                .setAllowedOrigins(corsProperties.getAllowedOrigins().split(","))
                .withSockJS();  // Enable SockJS fallback options
    }

    @Override
    public void configureWebSocketTransport(@NonNull WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(webSocketProperties.getMaxTextMessageSize());
        registration.setSendBufferSizeLimit(webSocketProperties.getMaxBinaryMessageSize());
    }

    @Override
    public void configureClientInboundChannel(@NonNull org.springframework.messaging.simp.config.ChannelRegistration registration) {
        registration.interceptors(new org.springframework.messaging.support.ChannelInterceptor() {
            @Override
            public org.springframework.messaging.Message<?> preSend(@NonNull org.springframework.messaging.Message<?> message,
                                                                   @NonNull org.springframework.messaging.MessageChannel channel) {
                org.springframework.messaging.simp.SimpMessageHeaderAccessor accessor =
                    org.springframework.messaging.simp.SimpMessageHeaderAccessor.wrap(message);

                // Log all incoming messages to help debug routing issues
                if (accessor.getMessageType() == org.springframework.messaging.simp.SimpMessageType.MESSAGE) {
                    String destination = accessor.getDestination();
                    log.debug("WEBSOCKET INBOUND: Received message to destination: {}", destination);
                    log.debug("WEBSOCKET INBOUND: Message headers: {}", message.getHeaders());
                    log.debug("WEBSOCKET INBOUND: Message payload: {}", message.getPayload());

                    // Log user information if available
                    java.security.Principal user = accessor.getUser();
                    if (user != null) {
                        try {
                            log.debug("WEBSOCKET INBOUND: Message from user: {}", user.getName());
                        } catch (Exception e) {
                            log.debug("WEBSOCKET INBOUND: User object exists but getName() failed");
                        }
                    } else {
                        log.debug("WEBSOCKET INBOUND: No user information available");
                    }
                }

                return message;
            }
        });
    }

    @Override
    public void configureClientOutboundChannel(@NonNull org.springframework.messaging.simp.config.ChannelRegistration registration) {
        registration.interceptors(new org.springframework.messaging.support.ChannelInterceptor() {
            @Override
            public org.springframework.messaging.Message<?> preSend(@NonNull org.springframework.messaging.Message<?> message,
                                                                   @NonNull org.springframework.messaging.MessageChannel channel) {
                org.springframework.messaging.simp.SimpMessageHeaderAccessor accessor =
                    org.springframework.messaging.simp.SimpMessageHeaderAccessor.wrap(message);

                // Log all outgoing messages to help debug routing issues
                if (accessor.getMessageType() == org.springframework.messaging.simp.SimpMessageType.MESSAGE) {
                    String destination = accessor.getDestination();
                    log.debug("WEBSOCKET OUTBOUND: Sending message to destination: {}", destination);
                    log.debug("WEBSOCKET OUTBOUND: Message headers: {}", message.getHeaders());

                    // Don't log the entire payload as it might be large, just log that a message is being sent
                    log.debug("WEBSOCKET OUTBOUND: Sending message payload to destination: {}", destination);

                    // Log user information if available
                    java.security.Principal user = accessor.getUser();
                    if (user != null) {
                        try {
                            log.debug("WEBSOCKET OUTBOUND: Message to user: {}", user.getName());
                        } catch (Exception e) {
                            log.debug("WEBSOCKET OUTBOUND: User object exists but getName() failed");
                        }
                    } else {
                        log.debug("WEBSOCKET OUTBOUND: No user information available");
                    }
                }

                return message;
            }
        });
    }
}
