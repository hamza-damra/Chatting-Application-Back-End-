# Backend API Changes - Real-Time Unread Message Notifications

## 🆕 New WebSocket Endpoints

### 1. Enter Room (Presence Tracking)
```
SEND /app/chat.enterRoom/{roomId}
```
**Purpose**: Mark user as actively viewing a specific chat room
**Authentication**: Required (JWT)
**Parameters**: 
- `roomId` (path parameter) - The chat room ID
**Body**: Empty
**Response**: None (success indicated by no error)
**Usage**: Call when user opens/navigates to a chat room

**Example**:
```javascript
stompClient.send('/app/chat.enterRoom/123', {});
```

### 2. Leave Room (Presence Tracking)
```
SEND /app/chat.leaveRoom/{roomId}
```
**Purpose**: Mark user as no longer actively viewing a specific chat room
**Authentication**: Required (JWT)
**Parameters**: 
- `roomId` (path parameter) - The chat room ID
**Body**: Empty
**Response**: None (success indicated by no error)
**Usage**: Call when user closes/navigates away from a chat room

**Example**:
```javascript
stompClient.send('/app/chat.leaveRoom/123', {});
```

### 3. Get Room Presence (Debug/Monitoring)
```
SEND /app/chat.getRoomPresence/{roomId}
```
**Purpose**: Get list of users currently active in a room (for debugging/monitoring)
**Authentication**: Required (JWT)
**Parameters**: 
- `roomId` (path parameter) - The chat room ID
**Body**: Empty
**Response**: Sent to `/user/queue/presence`
**Usage**: Optional - for debugging presence tracking

**Example**:
```javascript
stompClient.send('/app/chat.getRoomPresence/123', {});
```

**Response Format**:
```json
{
  "roomId": 123,
  "activeUsers": ["user1", "user2"],
  "activeUserCount": 2,
  "timestamp": "2024-01-15T10:30:00"
}
```

## 🔔 New Subscription Channel

### Unread Message Notifications
```
SUBSCRIBE /user/unread-messages
```
**Purpose**: Receive rich unread message notifications for messages sent to rooms where the user is NOT currently active
**Authentication**: Required (JWT)
**Message Format**: `UnreadMessageNotification` object

**Example**:
```javascript
stompClient.subscribe('/user/unread-messages', (message) => {
  const notification = JSON.parse(message.body);
  // Handle notification
});
```

**Notification Payload**:
```json
{
  "messageId": 456,
  "chatRoomId": 123,
  "chatRoomName": "General Chat",
  "senderId": 789,
  "senderUsername": "john_doe",
  "senderFullName": "John Doe",
  "contentPreview": "Hello, how are you doing?",
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

## 📊 Notification Types

| Type | Description | When Triggered |
|------|-------------|----------------|
| `NEW_MESSAGE` | Standard new message | Any new message to absent user |
| `GROUP_MESSAGE` | New message in group chat | Message in group room (>2 participants) |
| `PRIVATE_MESSAGE` | New message in private chat | Message in private room (2 participants) |
| `MENTION` | User was mentioned | Future enhancement for @mentions |

## 🔄 Updated Behavior

### Message Sending Flow
1. User sends message via `/app/chat.sendMessage/{roomId}`
2. Backend saves message and sends to all participants (existing behavior)
3. **NEW**: Backend checks presence for each participant
4. **NEW**: Sends rich notifications to `/user/unread-messages` for absent users only
5. Backend sends unread count updates to `/user/queue/unread` (existing behavior)

### Presence Tracking Logic
- Users marked as "active" in room when they call `/app/chat.enterRoom/{roomId}`
- Users marked as "inactive" when they call `/app/chat.leaveRoom/{roomId}`
- Users automatically marked as "offline" (removed from all rooms) on WebSocket disconnect
- Notifications only sent to users who are NOT active in the room where message was sent

## 🔧 Integration Points

### Existing Endpoints (Unchanged)
- `/app/chat.sendMessage/{roomId}` - Send message
- `/app/chat.addUser` - Join room (legacy)
- `/app/chat.updateStatus` - Update message status
- `/app/chat.createRoom` - Create chat room
- `/app/chat.getUnreadCounts` - Get initial unread counts
- `/app/chat.markRoomAsRead/{roomId}` - Mark room as read

### Existing Subscriptions (Unchanged)
- `/user/queue/unread` - Unread count updates
- `/user/queue/messages` - Message delivery receipts
- `/user/queue/status` - Message status updates
- `/user/queue/errors` - Error messages
- `/topic/chatrooms/{roomId}` - Room-specific messages
- `/topic/public/status` - Global user status

## 🛡️ Security & Authorization

### Authentication
- All new endpoints require JWT authentication
- Same security model as existing endpoints
- Users can only access rooms they are participants in

### Authorization Checks
- `enterRoom`/`leaveRoom`: Verifies user is participant in room
- `getRoomPresence`: Verifies user is participant in room
- Notifications: Only sent to room participants

### Error Handling
- `ChatRoomAccessDeniedException`: User not participant in room
- `AccessDeniedException`: Authentication required
- Errors sent to `/user/queue/errors` and `/topic/errors`

## 📈 Performance Considerations

### Memory Usage
- Presence tracking uses `ConcurrentHashMap` for thread safety
- Minimal memory footprint (usernames and room IDs only)
- Automatic cleanup on user disconnect

### Network Traffic
- Notifications only sent to absent users (reduces traffic)
- Rich notifications replace multiple smaller updates
- Throttling prevents excessive updates (1 second throttle)

### Database Impact
- No additional database queries for presence tracking
- Uses existing message and user data
- Presence data stored in memory only

## 🔍 Monitoring & Debugging

### Logging Prefixes
- `PRESENCE:` - Presence tracking operations
- `UNREAD_NOTIFICATION:` - Notification delivery
- `WEBSOCKET:` - General WebSocket operations

### Debug Endpoint
Use `/app/chat.getRoomPresence/{roomId}` to check current presence state:
```javascript
// Send request
stompClient.send('/app/chat.getRoomPresence/123', {});

// Listen for response
stompClient.subscribe('/user/queue/presence', (message) => {
  const presence = JSON.parse(message.body);
  console.log('Active users:', presence.activeUsers);
});
```

## 🧪 Testing Scenarios

### Basic Flow
1. User A enters room: `SEND /app/chat.enterRoom/1`
2. User B leaves room: `SEND /app/chat.leaveRoom/1`
3. User A sends message: `SEND /app/chat.sendMessage/1`
4. Result: Only User B receives notification on `/user/unread-messages`

### Edge Cases
- User disconnects without calling `leaveRoom` → Automatic cleanup
- User rapidly switches rooms → Correct presence tracking
- Multiple users in same room → Notifications only to absent users
- Network reconnection → Presence state reset, must call `enterRoom` again

## 🔄 Migration Guide

### For Existing Clients
1. **Optional**: Add presence tracking calls (`enterRoom`/`leaveRoom`)
2. **Optional**: Subscribe to `/user/unread-messages` for rich notifications
3. **No Breaking Changes**: All existing functionality preserved

### Recommended Implementation
1. Call `enterRoom` when opening chat room screen
2. Call `leaveRoom` when closing chat room screen
3. Subscribe to `/user/unread-messages` for notifications
4. Handle app lifecycle (background/foreground) for presence

### Backward Compatibility
- Existing unread count system continues to work
- Existing message delivery unchanged
- New features are additive, not replacing

## 📋 Quick Reference

### Client-Side Checklist
- [ ] Subscribe to `/user/unread-messages`
- [ ] Call `enterRoom` when opening chat room
- [ ] Call `leaveRoom` when closing chat room
- [ ] Handle notifications appropriately
- [ ] Test presence tracking works correctly

### Server-Side Features
- [x] User presence tracking service
- [x] Smart notification delivery
- [x] Rich notification payload
- [x] Automatic cleanup on disconnect
- [x] Security and authorization
- [x] Error handling and logging
