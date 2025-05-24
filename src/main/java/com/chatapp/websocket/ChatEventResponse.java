package com.chatapp.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatEventResponse {
    private EventType type;
    private Long userId;
    private String username;
    private Long roomId;
    private LocalDateTime timestamp;
    
    public enum EventType {
        JOIN,
        LEAVE,
        TYPING,
        ONLINE,
        OFFLINE
    }
}
