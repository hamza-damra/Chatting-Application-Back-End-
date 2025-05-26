# Real-Time Unread Messages System

## Overview

This system provides real-time unread message notifications to clients via WebSocket. Users receive instant updates about unread message counts when:
- New messages are received
- Messages are marked as read
- Multiple messages are marked as read (bulk read)
- User connects/reconnects to the application

## Architecture

### Components

1. **UnreadCountResponse DTO** - Structured response for unread count updates
2. **UnreadMessageService** - Core service managing unread counts and notifications
3. **Enhanced ChatController** - WebSocket endpoints for unread operations
4. **WebSocketEventListener** - Automatic unread count delivery on connection

### Data Flow

```
New Message → ChatController.sendMessage() → UnreadMessageService.notifyParticipantsOfNewMessage() → WebSocket → Client
Message Read → ChatController.updateMessageStatus() → UnreadMessageService.notifyMessageRead() → WebSocket → Client
User Connect → WebSocketEventListener → UnreadMessageService.sendInitialUnreadCounts() → WebSocket → Client
```

## WebSocket Endpoints

### 1. Get Initial Unread Counts
```
SEND /app/chat.getUnreadCounts
```
**Purpose**: Request initial unread counts for all chat rooms
**Authentication**: Required
**Response**: Sent to `/user/queue/unread`

### 2. Mark Chat Room as Read
```
SEND /app/chat.markRoomAsRead/{roomId}
```
**Purpose**: Mark all messages in a chat room as read
**Authentication**: Required
**Parameters**: `roomId` - Chat room ID
**Response**: Updated unread counts sent to `/user/queue/unread`

### 3. Subscribe to Unread Updates
```
SUBSCRIBE /user/queue/unread
```
**Purpose**: Receive real-time unread count updates
**Authentication**: Required

## Response Format

### UnreadCountResponse Structure
```json
{
  "chatRoomId": 123,
  "chatRoomName": "General Chat",
  "unreadCount": 5,
  "totalUnreadCount": 12,
  "latestMessageId": 456,
  "latestMessageContent": "Hello, how are you?",
  "latestMessageSender": "john_doe",
  "timestamp": "2024-01-15T10:30:00",
  "updateType": "NEW_MESSAGE",
  "userId": 789
}
```

### Update Types
- `NEW_MESSAGE` - A new message was received
- `MESSAGE_READ` - A message was marked as read
- `BULK_READ` - Multiple messages were marked as read
- `ROOM_OPENED` - User opened a chat room
- `INITIAL_COUNT` - Initial unread count when user connects

## Client Integration

### JavaScript/TypeScript Example
```javascript
// Subscribe to unread count updates
stompClient.subscribe('/user/queue/unread', function(update) {
    const data = JSON.parse(update.body);
    
    // Update UI based on update type
    switch(data.updateType) {
        case 'NEW_MESSAGE':
            updateChatRoomBadge(data.chatRoomId, data.unreadCount);
            updateTotalUnreadBadge(data.totalUnreadCount);
            showNotification(data.latestMessageSender, data.latestMessageContent);
            break;
            
        case 'MESSAGE_READ':
        case 'BULK_READ':
            updateChatRoomBadge(data.chatRoomId, data.unreadCount);
            updateTotalUnreadBadge(data.totalUnreadCount);
            break;
            
        case 'INITIAL_COUNT':
            initializeChatRoomBadge(data.chatRoomId, data.unreadCount);
            break;
    }
});

// Request initial unread counts
function requestInitialUnreadCounts() {
    stompClient.send('/app/chat.getUnreadCounts', {}, '{}');
}

// Mark chat room as read
function markChatRoomAsRead(roomId) {
    stompClient.send(`/app/chat.markRoomAsRead/${roomId}`, {}, '{}');
}
```

### Flutter Example
```dart
// Subscribe to unread updates
stompClient.subscribe(
  destination: '/user/queue/unread',
  callback: (StompFrame frame) {
    if (frame.body != null) {
      final data = jsonDecode(frame.body!);
      _handleUnreadUpdate(data);
    }
  },
);

void _handleUnreadUpdate(Map<String, dynamic> data) {
  switch (data['updateType']) {
    case 'NEW_MESSAGE':
      _updateChatRoomBadge(data['chatRoomId'], data['unreadCount']);
      _updateTotalUnreadBadge(data['totalUnreadCount']);
      _showNotification(data['latestMessageSender'], data['latestMessageContent']);
      break;
      
    case 'MESSAGE_READ':
    case 'BULK_READ':
      _updateChatRoomBadge(data['chatRoomId'], data['unreadCount']);
      _updateTotalUnreadBadge(data['totalUnreadCount']);
      break;
      
    case 'INITIAL_COUNT':
      _initializeChatRoomBadge(data['chatRoomId'], data['unreadCount']);
      break;
  }
}
```

## Features

### ✅ Real-Time Updates
- Instant unread count updates when messages are sent/read
- No polling required - push-based notifications
- Automatic updates for all participants

### ✅ Comprehensive Coverage
- Per-chat-room unread counts
- Total unread count across all rooms
- Latest message preview for notifications

### ✅ Automatic Initialization
- Initial unread counts sent when user connects
- Handles reconnection scenarios
- No manual refresh needed

### ✅ Bulk Operations
- Mark entire chat rooms as read
- Efficient bulk read notifications
- Optimized for performance

### ✅ Security
- Authentication required for all operations
- Participant verification for chat room access
- User-specific notifications only

## Performance Considerations

### Database Optimization
- Efficient queries using indexed columns
- Minimal database calls per operation
- Optimized unread count calculations

### WebSocket Efficiency
- Targeted notifications (only to relevant users)
- Structured JSON responses
- Minimal payload size

### Memory Usage
- Stateless service design
- No in-memory caching of unread counts
- Database-backed consistency

## Error Handling

All unread message operations include comprehensive error handling:
- Authentication failures
- Chat room access violations
- Database connection issues
- WebSocket delivery failures

Errors are logged with detailed context and don't affect other users.

## Testing

### Manual Testing
1. Connect two users to the same chat room
2. Send a message from User A
3. Verify User B receives unread count update
4. Mark message as read from User B
5. Verify unread count decreases

### Automated Testing
- Unit tests for UnreadMessageService
- Integration tests for WebSocket endpoints
- End-to-end tests for client scenarios

## Benefits

1. **Enhanced User Experience** - Users know immediately when they have new messages
2. **Real-Time Feedback** - No delays or polling required
3. **Accurate Counts** - Database-backed consistency ensures accuracy
4. **Scalable Design** - Efficient WebSocket-based delivery
5. **Cross-Platform** - Works with any WebSocket-capable client

This real-time unread message system provides a modern, responsive chat experience with instant notifications and accurate unread counts.
