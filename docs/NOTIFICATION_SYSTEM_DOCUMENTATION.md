# 🔔 Notification System Documentation

## Overview

This document provides comprehensive documentation for the real-time notification system in the chat application. The system supports both REST API and WebSocket-based notifications with user preferences, multiple notification types, and priority levels.

## 🏗️ Architecture

### Core Components

1. **NotificationService** - Central service for creating and managing notifications
2. **NotificationPreferencesService** - Manages user notification preferences
3. **NotificationController** (REST) - HTTP API endpoints
4. **NotificationController** (WebSocket) - Real-time WebSocket endpoints
5. **Notification Model** - Persistent notification entity
6. **NotificationPreferences Model** - User preference settings

### Database Schema

The notification system uses two main tables:
- `notifications` - Stores notification data
- `notification_preferences` - Stores user notification settings

## 📡 WebSocket Configuration

### Connection Details
- **Endpoint**: `/ws`
- **Protocol**: STOMP over SockJS
- **Authentication**: JWT Bearer token in headers

### WebSocket Setup
```javascript
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

stompClient.connect(
    { 'Authorization': 'Bearer ' + authToken },
    function(frame) {
        console.log('Connected: ' + frame);

        // Subscribe to notification channels
        stompClient.subscribe('/user/notifications', function(notification) {
            const data = JSON.parse(notification.body);
            handleNotification(data);
        });

        stompClient.subscribe('/user/notifications/unread-count', function(message) {
            const data = JSON.parse(message.body);
            updateUnreadCount(data.unreadCount);
        });
    }
);
```

## 🎯 WebSocket Endpoints

### Subscription Channels (Client subscribes to these)

| Channel | Purpose | Data Format |
|---------|---------|-------------|
| `/user/notifications` | Real-time notifications | `NotificationResponse` |
| `/user/notifications/unread` | Unread notifications list | `List<NotificationResponse>` |
| `/user/notifications/unread-count` | Unread count updates | `{"unreadCount": number}` |
| `/user/notifications/read-all-confirmation` | Mark all as read confirmation | `{"updatedCount": number, "status": "all_read"}` |
| `/user/notifications/error` | Error messages | `{"error": string, "timestamp": number}` |

### Message Destinations (Client sends to these)

| Destination | Purpose | Payload |
|-------------|---------|---------|
| `/app/notifications.getUnread` | Get unread notifications | None |
| `/app/notifications.markAsRead` | Mark notification as read | `{"notificationId": number}` |
| `/app/notifications.markAllAsRead` | Mark all as read | None |
| `/app/notifications.getUnreadCount` | Get unread count | None |

## 🌐 REST API Endpoints

### Base URL: `/api/notifications`

| Method | Endpoint | Description | Request Body | Response |
|--------|----------|-------------|--------------|----------|
| GET | `/` | Get paginated notifications | Query params: `page`, `size` | `Page<NotificationResponse>` |
| GET | `/unread` | Get unread notifications | None | `List<NotificationResponse>` |
| GET | `/unread/count` | Get unread count | None | `{"unreadCount": number}` |
| PUT | `/{id}/read` | Mark notification as read | None | `NotificationResponse` |
| PUT | `/read-all` | Mark all as read | None | `{"updatedCount": number}` |
| GET | `/preferences` | Get user preferences | None | `NotificationPreferencesResponse` |
| PUT | `/preferences` | Update preferences | `NotificationPreferencesRequest` | `NotificationPreferencesResponse` |

## 📋 Data Models

### NotificationResponse
```json
{
  "id": 123,
  "title": "New Message",
  "content": "You have a new message from John",
  "data": "{\"chatRoomId\": 456}",
  "notificationType": "NEW_MESSAGE",
  "priority": "NORMAL",
  "isRead": false,
  "isDelivered": true,
  "createdAt": "2024-01-15T10:30:00",
  "deliveredAt": "2024-01-15T10:30:01",
  "readAt": null,
  "expiresAt": "2024-01-22T10:30:00",
  "relatedMessageId": 789,
  "relatedChatRoomId": 456,
  "relatedChatRoomName": "General Chat",
  "triggeredByUserId": 101,
  "triggeredByUsername": "john_doe",
  "triggeredByFullName": "John Doe"
}
```

### NotificationPreferencesRequest
```json
{
  "pushNotificationsEnabled": true,
  "newMessageNotifications": true,
  "privateMessageNotifications": true,
  "groupMessageNotifications": true,
  "mentionNotifications": true,
  "chatRoomInviteNotifications": true,
  "fileSharingNotifications": true,
  "systemAnnouncementNotifications": true,
  "doNotDisturb": false,
  "dndStartTime": "22:00",
  "dndEndTime": "08:00"
}
```

## 🏷️ Notification Types

| Type | Description | Use Case |
|------|-------------|----------|
| `NEW_MESSAGE` | New message in any chat room | General message notifications |
| `PRIVATE_MESSAGE` | New private/direct message | 1-on-1 conversations |
| `GROUP_MESSAGE` | New group message | Group chat notifications |
| `MENTION` | User was mentioned (@username) | Important attention-required messages |
| `CHAT_ROOM_INVITE` | Invited to join a chat room | Room invitations |
| `CHAT_ROOM_ADDED` | Added to a chat room | Automatic room additions |
| `CHAT_ROOM_REMOVED` | Removed from a chat room | Room removal notifications |
| `USER_JOINED` | User joined a chat room | Member activity |
| `USER_LEFT` | User left a chat room | Member activity |
| `FILE_SHARED` | File was shared in chat | File sharing notifications |
| `SYSTEM_ANNOUNCEMENT` | System-wide announcement | Important system messages |
| `FRIEND_REQUEST` | Friend request received | Social features |
| `FRIEND_ACCEPTED` | Friend request accepted | Social features |

## ⚡ Priority Levels

| Priority | Description | Behavior |
|----------|-------------|----------|
| `LOW` | Non-urgent notifications | Standard delivery |
| `NORMAL` | Standard notifications | Default priority |
| `HIGH` | Important notifications | Emphasized display |
| `URGENT` | Critical notifications | Immediate delivery, special UI treatment |

## 🔧 Implementation Examples

### Flutter WebSocket Integration
```dart
class NotificationService {
  StompClient? _stompClient;

  Future<void> initializeWebSocket(String serverUrl, String token) async {
    _stompClient = StompClient(
      config: StompConfig(
        url: 'ws://$serverUrl/ws',
        onConnect: _onConnect,
        stompConnectHeaders: {
          'Authorization': 'Bearer $token',
        },
      ),
    );
    _stompClient?.activate();
  }

  void _onConnect(StompFrame frame) {
    // Subscribe to notifications
    _stompClient?.subscribe(
      destination: '/user/notifications',
      callback: (frame) {
        final notification = NotificationResponse.fromJson(
          jsonDecode(frame.body!)
        );
        _handleNotification(notification);
      },
    );

    // Subscribe to unread count
    _stompClient?.subscribe(
      destination: '/user/notifications/unread-count',
      callback: (frame) {
        final data = jsonDecode(frame.body!);
        _updateUnreadCount(data['unreadCount']);
      },
    );
  }

  void markAsRead(int notificationId) {
    _stompClient?.send(
      destination: '/app/notifications.markAsRead',
      body: jsonEncode({'notificationId': notificationId}),
    );
  }
}
```

### JavaScript WebSocket Integration
```javascript
class NotificationManager {
  constructor(serverUrl, token) {
    this.serverUrl = serverUrl;
    this.token = token;
    this.stompClient = null;
  }

  connect() {
    const socket = new SockJS(`${this.serverUrl}/ws`);
    this.stompClient = Stomp.over(socket);

    this.stompClient.connect(
      { 'Authorization': `Bearer ${this.token}` },
      (frame) => this.onConnect(frame),
      (error) => this.onError(error)
    );
  }

  onConnect(frame) {
    // Subscribe to notifications
    this.stompClient.subscribe('/user/notifications', (message) => {
      const notification = JSON.parse(message.body);
      this.handleNotification(notification);
    });

    // Get initial unread count
    this.stompClient.send('/app/notifications.getUnreadCount');
  }

  markAsRead(notificationId) {
    this.stompClient.send('/app/notifications.markAsRead', {},
      JSON.stringify({ notificationId: notificationId })
    );
  }
}
```

## 🛠️ Error Handling

### WebSocket Error Handling

The system provides comprehensive error handling for WebSocket operations:

#### Error Channels
- `/user/notifications/error` - User-specific errors
- `/user/queue/errors` - General error queue
- `/topic/errors` - Global error topic (for debugging)

#### Error Response Format
```json
{
  "type": "ERROR_TYPE",
  "message": "Human readable error message",
  "timestamp": 1642234567890
}
```

#### Common Error Types
| Error Type | Description | Resolution |
|------------|-------------|------------|
| `AUTHENTICATION_REQUIRED` | Missing or invalid JWT token | Re-authenticate |
| `NOTIFICATION_NOT_FOUND` | Notification ID doesn't exist | Refresh notification list |
| `INVALID_REQUEST` | Malformed request payload | Check request format |
| `PERMISSION_DENIED` | User lacks permission | Check user permissions |
| `INTERNAL_ERROR` | Server-side error | Retry or contact support |

### REST API Error Handling

#### Standard HTTP Error Responses
```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed for request parameters",
  "path": "/api/notifications/preferences",
  "validationErrors": {
    "dndStartTime": "Time must be in HH:MM format (24-hour)"
  }
}
```

## 🔐 Authentication & Authorization

### WebSocket Authentication
- **Method**: JWT Bearer token in connection headers
- **Header**: `Authorization: Bearer <token>`
- **Validation**: Token validated on each WebSocket message

### REST API Authentication
- **Method**: JWT Bearer token in HTTP headers
- **Header**: `Authorization: Bearer <token>`
- **Scope**: All notification endpoints require authentication

### User Authorization
- Users can only access their own notifications
- Users can only modify their own notification preferences
- System administrators can send system announcements

## 📊 Monitoring & Testing

### Test Endpoints
Base URL: `/api/test/notifications`

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/send-test` | Send test notification to authenticated user |
| POST | `/send-to-user` | Send test notification to specific user |
| POST | `/test-all-types` | Send notifications of all types |
| GET | `/stats` | Get notification system statistics |
| GET | `/diagnostic` | Get system diagnostic information |
| GET | `/websocket-stats` | Get WebSocket connection statistics |

### Health Check
```bash
GET /api/test/notifications/diagnostic
```

Response includes:
- WebSocket endpoint configuration
- Available subscription channels
- REST API endpoints
- System status and health

## 🚀 Best Practices

### Frontend Implementation

1. **Connection Management**
   - Implement automatic reconnection logic
   - Handle connection state changes gracefully
   - Store authentication token securely

2. **Notification Handling**
   - Display notifications based on priority levels
   - Implement proper notification persistence
   - Handle notification actions (mark as read, dismiss)

3. **User Experience**
   - Show unread count in UI
   - Provide notification preferences screen
   - Implement do-not-disturb functionality

4. **Performance**
   - Batch notification updates when possible
   - Implement efficient notification storage
   - Use pagination for notification history

### Backend Integration

1. **Notification Creation**
   - Always check user preferences before sending
   - Use appropriate notification types and priorities
   - Include relevant context data

2. **Error Handling**
   - Implement proper exception handling
   - Log notification delivery failures
   - Provide meaningful error messages

3. **Performance**
   - Use async processing for notification delivery
   - Implement notification batching for high-volume scenarios
   - Monitor WebSocket connection health

## 🔧 Configuration

### Application Properties
```properties
# WebSocket Configuration
websocket.max-text-message-size=64KB
websocket.max-binary-message-size=512KB
websocket.send-time-limit=20000

# CORS Configuration
cors.allowed-origins=http://localhost:3000,https://yourdomain.com

# Notification Settings
notification.default-expiry-days=7
notification.max-notifications-per-user=1000
```

### Environment Variables
```bash
# Database
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/chatapp
SPRING_DATASOURCE_USERNAME=your_username
SPRING_DATASOURCE_PASSWORD=your_password

# JWT
JWT_SECRET=your_jwt_secret_key
JWT_EXPIRATION=86400000

# Server
SERVER_PORT=8080
```

## 📱 Flutter Integration Guide

### Dependencies
Add to `pubspec.yaml`:
```yaml
dependencies:
  stomp_dart_client: ^1.0.0
  web_socket_channel: ^2.4.0
```

### Model Classes
```dart
class NotificationResponse {
  final int id;
  final String title;
  final String content;
  final String notificationType;
  final String priority;
  final bool isRead;
  final DateTime createdAt;
  final int? relatedMessageId;
  final int? relatedChatRoomId;
  final String? triggeredByUsername;

  NotificationResponse({
    required this.id,
    required this.title,
    required this.content,
    required this.notificationType,
    required this.priority,
    required this.isRead,
    required this.createdAt,
    this.relatedMessageId,
    this.relatedChatRoomId,
    this.triggeredByUsername,
  });

  factory NotificationResponse.fromJson(Map<String, dynamic> json) {
    return NotificationResponse(
      id: json['id'],
      title: json['title'],
      content: json['content'],
      notificationType: json['notificationType'],
      priority: json['priority'],
      isRead: json['isRead'],
      createdAt: DateTime.parse(json['createdAt']),
      relatedMessageId: json['relatedMessageId'],
      relatedChatRoomId: json['relatedChatRoomId'],
      triggeredByUsername: json['triggeredByUsername'],
    );
  }
}
```

## 🐛 Troubleshooting

### Common Issues

1. **WebSocket Connection Fails**
   - Check JWT token validity
   - Verify CORS configuration
   - Ensure WebSocket endpoint is accessible

2. **Notifications Not Received**
   - Check user notification preferences
   - Verify WebSocket subscription channels
   - Check server logs for delivery errors

3. **High Memory Usage**
   - Implement notification cleanup for old notifications
   - Limit notification history per user
   - Monitor WebSocket connection count

4. **Performance Issues**
   - Enable notification batching
   - Implement proper database indexing
   - Monitor notification delivery latency

### Debug Commands
```bash
# Check WebSocket connections
curl -X GET "http://localhost:8080/api/test/notifications/websocket-stats"

# Test notification delivery
curl -X POST "http://localhost:8080/api/test/notifications/send-test" \
  -H "Authorization: Bearer YOUR_TOKEN"

# Get system diagnostic
curl -X GET "http://localhost:8080/api/test/notifications/diagnostic"
```

## 📚 Related Documentation

- [Enhanced Push Notifications Guide](./ENHANCED_PUSH_NOTIFICATIONS_GUIDE.md)
- [Flutter Integration Guide](./FLUTTER_INTEGRATION_GUIDE.md)
- [Backend Notification Verification](./BACKEND_NOTIFICATION_VERIFICATION.md)
- [WebSocket Configuration Guide](./WEBSOCKET_CONFIGURATION.md)

---

*Last updated: January 2024*
*Version: 1.0*
```
