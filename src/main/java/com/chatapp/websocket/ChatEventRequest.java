package com.chatapp.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request object for chat room events like joining or leaving
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatEventRequest {
    private Long roomId;
    private String username;
    private String type; // JOIN, LEAVE
}
