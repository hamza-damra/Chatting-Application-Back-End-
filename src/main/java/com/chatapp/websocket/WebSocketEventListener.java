package com.chatapp.websocket;

import com.chatapp.model.User;
import com.chatapp.service.UnreadMessageService;
import com.chatapp.service.UserPresenceService;
import com.chatapp.service.UserService;
import com.chatapp.service.WebSocketMonitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;
    private final UnreadMessageService unreadMessageService;
    private final UserPresenceService userPresenceService;
    private final WebSocketMonitorService monitorService;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        Principal principal = event.getUser();
        if (principal != null) {
            String username = principal.getName();
            log.info("User connected: {}", username);

            // Record connection in monitor
            monitorService.recordConnection(username);

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

            // Send initial unread message counts to the connected user
            try {
                unreadMessageService.sendInitialUnreadCounts(user);
                log.info("WEBSOCKET: Sent initial unread counts to connected user: {}", username);
            } catch (Exception e) {
                log.error("WEBSOCKET: Failed to send initial unread counts to user: {}", username, e);
            }
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        Principal principal = event.getUser();
        if (principal != null) {
            String username = principal.getName();
            log.info("User disconnected: {}", username);

            try {
                // Record disconnection in monitor
                monitorService.recordDisconnection(username);

                // Update user status to offline
                User user = userService.getUserByUsername(username);
                userService.updateUserStatus(user.getId(), false);

                // Get active rooms before removing user from presence tracking
                Set<Long> activeRooms = userPresenceService.getActiveRoomsForUser(username);

                // Remove user from all room presence tracking
                userPresenceService.markUserOffline(username);

                // Broadcast user offline status (single message)
                ChatEventResponse offlineResponse = ChatEventResponse.builder()
                    .type(ChatEventResponse.EventType.OFFLINE)
                    .userId(user.getId())
                    .username(username)
                    .timestamp(LocalDateTime.now())
                    .build();

                messagingTemplate.convertAndSend("/topic/public/status", offlineResponse);

                // Send leave events only to rooms where user was actually active
                for (Long roomId : activeRooms) {
                    ChatEventResponse leaveEvent = ChatEventResponse.builder()
                        .type(ChatEventResponse.EventType.LEAVE)
                        .userId(user.getId())
                        .username(username)
                        .roomId(roomId)
                        .timestamp(LocalDateTime.now())
                        .build();

                    messagingTemplate.convertAndSend("/topic/chatrooms/" + roomId, leaveEvent);
                    log.debug("WEBSOCKET: Sent leave event for user {} from room {}", username, roomId);
                }

                log.debug("WEBSOCKET: User {} disconnected from {} rooms", username, activeRooms.size());

            } catch (Exception e) {
                log.error("WEBSOCKET: Error handling user disconnect for {}", username, e);
            }
        }
    }
}
