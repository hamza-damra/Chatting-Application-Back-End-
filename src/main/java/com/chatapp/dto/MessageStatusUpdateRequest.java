package com.chatapp.dto;

import com.chatapp.model.MessageStatus;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating message status via REST API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageStatusUpdateRequest {
    
    @NotNull(message = "Status is required")
    private MessageStatus.Status status;
}
