package com.chatapp.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request object for creating a chat room via WebSocket.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomRequest {
    private String name;
    private boolean isPrivate;
    private List<Long> participantIds;
}
