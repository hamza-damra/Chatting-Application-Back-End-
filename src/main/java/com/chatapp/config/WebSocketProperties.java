package com.chatapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "spring.websocket")
public class WebSocketProperties {
    private int maxTextMessageSize;
    private int maxBinaryMessageSize;
}
