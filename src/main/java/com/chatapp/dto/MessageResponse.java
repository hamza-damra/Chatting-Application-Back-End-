package com.chatapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    private Long id;
    private String content;
    private String contentType;
    private String attachmentUrl;
    private UserResponse sender;
    private Long chatRoomId;
    private LocalDateTime sentAt;
    private com.chatapp.model.MessageStatus.Status status;
    private List<MessageStatusResponse> messageStatuses;
}
