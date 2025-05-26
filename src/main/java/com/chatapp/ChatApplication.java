package com.chatapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.chatapp.config.JwtConfig;
import com.chatapp.config.WebSocketProperties;
import com.chatapp.config.CorsProperties;

@SpringBootApplication
@EnableConfigurationProperties({JwtConfig.class, WebSocketProperties.class, CorsProperties.class})
public class ChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatApplication.class, args);
    }
}
