package com.chatapp.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class WebSocketMessageBrokerConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureWebSocketTransport(@NonNull WebSocketTransportRegistration registry) {
        registry.setMessageSizeLimit(5 * 1024 * 1024) // 5MB
               .setSendBufferSizeLimit(50 * 1024 * 1024) // 50MB
               .setSendTimeLimit(20_000); // 20 seconds
    }

    @Override
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
               .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(@NonNull MessageBrokerRegistry config) {
        config.setApplicationDestinationPrefixes("/app")
              .enableSimpleBroker("/topic", "/queue");
    }

    @Override
    public void configureClientInboundChannel(@NonNull ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
                log.debug("WEBSOCKET INBOUND: Received message: {}", message);
                return message;
            }

            @Override
            public void postSend(@NonNull Message<?> message, @NonNull MessageChannel channel, boolean sent) {
                log.debug("WEBSOCKET INBOUND: Message processed: {}, sent: {}", message, sent);
            }
        });
    }

    @Override
    public void configureClientOutboundChannel(@NonNull ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
                log.debug("WEBSOCKET OUTBOUND: Sending message: {}", message);
                return message;
            }

            @Override
            public void postSend(@NonNull Message<?> message, @NonNull MessageChannel channel, boolean sent) {
                log.debug("WEBSOCKET OUTBOUND: Message sent: {}, success: {}", message, sent);
            }
        });
    }
}
