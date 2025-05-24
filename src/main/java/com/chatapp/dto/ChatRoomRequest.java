package com.chatapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomRequest {

    @NotBlank(message = "Chat room name is required")
    private String name;

    private boolean isPrivate;

    private List<Long> participantIds;
}
