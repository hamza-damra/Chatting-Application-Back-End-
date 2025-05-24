package com.chatapp.websocket;

import com.chatapp.model.User;
import com.chatapp.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        Principal principal = event.getUser();
        if (principal != null) {
            String username = principal.getName();
            log.info("User connected: {}", username);

            // Update user status to online
            User user = userService.getUserByUsername(username);
            userService.updateUserStatus(user.getId(), true);

            // Broadcast user online status
            ChatEventResponse response = ChatEventResponse.builder()
                .type(ChatEventResponse.EventType.ONLINE)
                .userId(user.getId())
                .username(username)
                .timestamp(LocalDateTime.now())
                .build();

            messagingTemplate.convertAndSend("/topic/public/status", response);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        Principal principal = event.getUser();
        if (principal != null) {
            String username = principal.getName();
            log.info("User disconnected: {}", username);

            // Update user status to offline
            User user = userService.getUserByUsername(username);
            userService.updateUserStatus(user.getId(), false);

            // Get session attributes
            SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
            Long roomId = null;
            var sessionAttributes = headerAccessor.getSessionAttributes();
            if (sessionAttributes != null) {
                roomId = (Long) sessionAttributes.get("roomId");
            }

            // Broadcast user offline status
            ChatEventResponse response = ChatEventResponse.builder()
                .type(ChatEventResponse.EventType.OFFLINE)
                .userId(user.getId())
                .username(username)
                .roomId(roomId)
                .timestamp(LocalDateTime.now())
                .build();

            messagingTemplate.convertAndSend("/topic/public/status", response);

            // If user was in a room, notify room participants
            if (roomId != null) {
                ChatEventResponse leaveEvent = ChatEventResponse.builder()
                    .type(ChatEventResponse.EventType.LEAVE)
                    .userId(user.getId())
                    .username(username)
                    .roomId(roomId)
                    .timestamp(LocalDateTime.now())
                    .build();

                messagingTemplate.convertAndSend("/topic/chatrooms/" + roomId, leaveEvent);
            }
        }
    }
}
