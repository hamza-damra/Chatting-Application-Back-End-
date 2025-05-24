package com.chatapp.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageRequest {
    private String content;
    private String contentType; // TEXT, IMAGE, FILE
    private String attachmentUrl;
}
