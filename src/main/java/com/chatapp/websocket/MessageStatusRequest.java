package com.chatapp.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.chatapp.model.MessageStatus.Status;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageStatusRequest {
    private Long messageId;
    private Status status;
}
