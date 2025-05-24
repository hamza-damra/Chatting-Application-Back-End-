package com.chatapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageStatusResponse {
    private Long messageId;
    private Long userId;
    private String username;
    private com.chatapp.model.MessageStatus.Status status;
}
