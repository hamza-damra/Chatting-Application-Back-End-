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
public class MessageStatusResponse {
    private Long messageId;
    private Long userId;
    private com.chatapp.model.MessageStatus.Status status;
    private LocalDateTime timestamp;
}
