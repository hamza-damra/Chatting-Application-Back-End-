# Real-Time Unread Message Notifications System

## Overview

This system provides real-time unread message notifications to users who are **not currently active** in a chat room when new messages are sent. It complements the existing unread count system by sending detailed notifications only to users who are away from the room.

## Key Features

1. **User Presence Tracking**: Tracks which users are currently viewing/active in specific chat rooms
2. **Smart Notifications**: Only sends notifications to users who are NOT active in the room
3. **Detailed Message Info**: Includes sender details, content preview, room info, and timestamps
4. **Dedicated Channel**: Uses `/user/{username}/unread-messages` for notifications
5. **Integration**: Works seamlessly with existing WebSocket infrastructure

## Architecture

### Components

1. **UserPresenceService** - Tracks user presence in chat rooms
2. **UnreadMessageNotification DTO** - Structured notification payload
3. **Enhanced UnreadMessageService** - Manages notification delivery
4. **Enhanced ChatController** - Presence tracking endpoints
5. **Enhanced WebSocketEventListener** - Cleanup on disconnect

### Data Flow

```
New Message → ChatController.sendMessage() → 
UnreadMessageService.sendUnreadMessageNotificationsToAbsentUsers() → 
UserPresenceService.isUserActiveInRoom() → 
Send notification to absent users only
```

## WebSocket Endpoints

### 1. Enter Room (Presence Tracking)
```
SEND /app/chat.enterRoom/{roomId}
```
**Purpose**: Mark user as active in a specific chat room
**Authentication**: Required
**Parameters**: `roomId` - Chat room ID
**Usage**: Call when user opens/views a chat room

### 2. Leave Room (Presence Tracking)
```
SEND /app/chat.leaveRoom/{roomId}
```
**Purpose**: Mark user as no longer active in a specific chat room
**Authentication**: Required
**Parameters**: `roomId` - Chat room ID
**Usage**: Call when user closes/leaves a chat room view

### 3. Get Room Presence (Debug/Monitoring)
```
SEND /app/chat.getRoomPresence/{roomId}
```
**Purpose**: Get list of users currently active in a room
**Authentication**: Required
**Parameters**: `roomId` - Chat room ID
**Response**: Sent to `/user/queue/presence`

### 4. Subscribe to Unread Message Notifications
```
SUBSCRIBE /user/unread-messages
```
**Purpose**: Receive real-time unread message notifications
**Authentication**: Required
**Payload**: UnreadMessageNotification object

## Notification Format

### UnreadMessageNotification Structure
```json
{
  "messageId": 123,
  "chatRoomId": 456,
  "chatRoomName": "General Chat",
  "senderId": 789,
  "senderUsername": "john_doe",
  "senderFullName": "John Doe",
  "contentPreview": "Hello, how are you?",
  "contentType": "TEXT",
  "sentAt": "2024-01-15T10:30:00",
  "notificationTimestamp": "2024-01-15T10:30:01",
  "unreadCount": 5,
  "totalUnreadCount": 12,
  "recipientUserId": 101,
  "isPrivateChat": false,
  "participantCount": 8,
  "attachmentUrl": null,
  "notificationType": "GROUP_MESSAGE"
}
```

### Notification Types
- `NEW_MESSAGE`: Standard new message notification
- `PRIVATE_MESSAGE`: New message in a private chat
- `GROUP_MESSAGE`: New message in a group chat
- `MENTION`: User was mentioned (future enhancement)

## Implementation Details

### User Presence Tracking

The `UserPresenceService` maintains two concurrent maps:
- `roomPresence`: Maps room IDs to sets of active usernames
- `userPresence`: Maps usernames to sets of active room IDs

This allows efficient lookups in both directions and automatic cleanup.

### Notification Logic

1. When a message is sent, the system:
   - Identifies all participants in the chat room
   - Excludes the sender
   - Checks each participant's presence in the room
   - Sends notifications only to absent participants

2. Notifications include:
   - Complete message details
   - Current unread counts
   - Room context information
   - Sender information

### Integration Points

1. **Message Sending**: Integrated into `ChatController.sendMessage()`
2. **Presence Updates**: Integrated into room join/leave operations
3. **Disconnect Cleanup**: Integrated into `WebSocketEventListener`
4. **Existing Unread System**: Works alongside existing unread count updates

## Client Implementation Guide

### JavaScript/TypeScript Example

```javascript
// Subscribe to unread message notifications
stompClient.subscribe('/user/unread-messages', (message) => {
  const notification = JSON.parse(message.body);
  
  // Handle the notification
  showNotification({
    title: `New message from ${notification.senderUsername}`,
    body: notification.contentPreview,
    roomName: notification.chatRoomName,
    unreadCount: notification.unreadCount
  });
});

// Mark user as active in a room when opening it
function openChatRoom(roomId) {
  stompClient.send(`/app/chat.enterRoom/${roomId}`, {});
  // ... open room UI
}

// Mark user as inactive when leaving a room
function closeChatRoom(roomId) {
  stompClient.send(`/app/chat.leaveRoom/${roomId}`, {});
  // ... close room UI
}
```

### Flutter Example

```dart
// Subscribe to notifications
_stompClient.subscribe(
  destination: '/user/unread-messages',
  callback: (StompFrame frame) {
    if (frame.body != null) {
      final notification = UnreadMessageNotification.fromJson(
        jsonDecode(frame.body!)
      );
      _handleUnreadMessageNotification(notification);
    }
  },
);

// Handle presence tracking
void enterRoom(int roomId) {
  _stompClient.send(
    destination: '/app/chat.enterRoom/$roomId',
    body: '',
  );
}

void leaveRoom(int roomId) {
  _stompClient.send(
    destination: '/app/chat.leaveRoom/$roomId',
    body: '',
  );
}
```

## Benefits

1. **Reduced Noise**: Only notifies users who need to be notified
2. **Rich Context**: Provides detailed message information
3. **Real-Time**: Instant delivery via WebSocket
4. **Scalable**: Efficient presence tracking with minimal overhead
5. **Flexible**: Easy to extend with additional notification types
6. **Integrated**: Works with existing authentication and authorization

## Monitoring and Debugging

### Presence Statistics
Call `UserPresenceService.getPresenceStats()` to get current presence information:
```json
{
  "totalActiveRooms": 5,
  "totalActiveUsers": 12,
  "roomPresenceDetails": { "1": ["user1", "user2"], "2": ["user3"] },
  "userPresenceDetails": { "user1": [1, 3], "user2": [1] }
}
```

### Logging
The system provides comprehensive logging with prefixes:
- `PRESENCE:` - Presence tracking operations
- `UNREAD_NOTIFICATION:` - Notification delivery
- `WEBSOCKET:` - General WebSocket operations

## Testing

### Manual Testing
1. Connect two users to the same chat room
2. Have User A enter the room (`/app/chat.enterRoom/{roomId}`)
3. Have User B leave the room (`/app/chat.leaveRoom/{roomId}`)
4. Send a message from User A
5. Verify User B receives a notification but User A does not
6. Have User B enter the room
7. Send another message from User A
8. Verify User B does not receive a notification this time

### Integration Testing
- Test presence tracking across multiple rooms
- Test notification delivery with various message types
- Test cleanup on user disconnect
- Test error handling for invalid room access

## Future Enhancements

1. **Mention Notifications**: Special handling for @mentions
2. **Push Notifications**: Integration with mobile push services
3. **Notification Preferences**: User-configurable notification settings
4. **Presence Timeouts**: Automatic presence cleanup after inactivity
5. **Typing Indicators**: Enhanced presence with typing status
