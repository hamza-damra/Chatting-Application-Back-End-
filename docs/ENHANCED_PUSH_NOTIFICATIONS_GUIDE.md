# Enhanced Push Notification System

## Overview

This document describes the enhanced push notification system that provides comprehensive real-time notifications for chat applications using WebSockets. The system includes persistent notifications, user preferences, and multiple notification types.

## Architecture

### Core Components

1. **NotificationService** - Central service for creating and managing notifications
2. **NotificationPreferencesService** - Manages user notification preferences
3. **Notification Model** - Persistent notification entity
4. **NotificationPreferences Model** - User preference settings
5. **NotificationController** - REST API endpoints
6. **WebSocket NotificationController** - Real-time WebSocket endpoints

### Integration Points

- **ChatController** - Integrated with existing message sending
- **UserPresenceService** - Uses presence tracking for smart delivery
- **UnreadMessageService** - Works alongside existing unread system

## Features

### 1. Persistent Notifications
- Notifications are stored in database for offline users
- Automatic delivery when users come online
- Configurable expiration times
- Read/unread status tracking

### 2. Multiple Notification Types
- `NEW_MESSAGE` - Standard new message
- `PRIVATE_MESSAGE` - Private chat messages
- `GROUP_MESSAGE` - Group chat messages
- `MENTION` - User mentions (future)
- `CHAT_ROOM_INVITE` - Room invitations
- `FILE_SHARED` - File sharing notifications
- `SYSTEM_ANNOUNCEMENT` - System messages

### 3. Priority Levels
- `LOW` - Non-urgent notifications
- `NORMAL` - Standard notifications
- `HIGH` - Important notifications
- `URGENT` - Critical notifications

### 4. User Preferences
- Enable/disable notification types
- Do not disturb mode with time ranges (supports overnight ranges)
- Sound and vibration settings
- Preview settings
- Maximum offline notification limits

#### Do Not Disturb Time Ranges
The system supports flexible DND time ranges:
- **Same-day range**: e.g., "09:00" to "17:00" (9 AM to 5 PM)
- **Overnight range**: e.g., "22:00" to "08:00" (10 PM to 8 AM next day)
- **All-day suppression**: Set start and end times to the same value
- **Fallback behavior**: Invalid time formats fall back to the DND flag

### 5. Smart Delivery
- Only sends notifications to users not actively viewing the chat room
- Integrates with existing presence tracking
- Immediate WebSocket delivery when possible
- Fallback to database storage for offline users

## API Endpoints

### REST Endpoints

#### Get Notifications
```http
GET /api/notifications?page=0&size=20
```

#### Get Unread Notifications
```http
GET /api/notifications/unread
```

#### Get Unread Count
```http
GET /api/notifications/unread/count
```

#### Mark Notification as Read
```http
PUT /api/notifications/{notificationId}/read
```

#### Mark All as Read
```http
PUT /api/notifications/read-all
```

#### Get Preferences
```http
GET /api/notifications/preferences
```

#### Update Preferences
```http
PUT /api/notifications/preferences
Content-Type: application/json

{
  "pushNotificationsEnabled": true,
  "newMessageNotifications": true,
  "doNotDisturb": true,
  "dndStartTime": "22:00",
  "dndEndTime": "08:00",
  "soundEnabled": true,
  "vibrationEnabled": false,
  "showPreview": true,
  "maxOfflineNotifications": 50
}
```

#### DND Time Range Examples
```json
// Same-day range (9 AM to 5 PM)
{
  "doNotDisturb": true,
  "dndStartTime": "09:00",
  "dndEndTime": "17:00"
}

// Overnight range (10 PM to 8 AM next day)
{
  "doNotDisturb": true,
  "dndStartTime": "22:00",
  "dndEndTime": "08:00"
}

// All-day suppression
{
  "doNotDisturb": true,
  "dndStartTime": "00:00",
  "dndEndTime": "00:00"
}

// DND enabled without time restrictions
{
  "doNotDisturb": true,
  "dndStartTime": null,
  "dndEndTime": null
}
```

### WebSocket Endpoints

#### Subscribe to Notifications
```javascript
stompClient.subscribe('/user/notifications', function(notification) {
    // Handle incoming notification
});
```

#### Get Unread Notifications
```javascript
stompClient.send('/app/notifications.getUnread', {}, {});
```

#### Mark as Read
```javascript
stompClient.send('/app/notifications.markAsRead', {},
    JSON.stringify({notificationId: 123}));
```

#### Mark All as Read
```javascript
stompClient.send('/app/notifications.markAllAsRead', {}, {});
```

#### Get Unread Count
```javascript
stompClient.send('/app/notifications.getUnreadCount', {}, {});
```

## WebSocket Subscription Channels

### User-Specific Channels
- `/user/notifications` - Incoming notifications
- `/user/notifications/unread` - Unread notifications response
- `/user/notifications/unread-count` - Unread count updates
- `/user/notifications/read-confirmation` - Read confirmations
- `/user/notifications/error` - Error messages

## Database Schema

### notifications Table
```sql
CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_id BIGINT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    data TEXT,
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    is_delivered BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    delivered_at TIMESTAMP NULL,
    read_at TIMESTAMP NULL,
    expires_at TIMESTAMP NULL,
    related_message_id BIGINT NULL,
    related_chatroom_id BIGINT NULL,
    triggered_by_user_id BIGINT NULL
);
```

### notification_preferences Table
```sql
CREATE TABLE notification_preferences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    push_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    new_message_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    private_message_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    group_message_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    mention_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    chat_room_invite_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    file_sharing_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    system_announcement_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    do_not_disturb BOOLEAN NOT NULL DEFAULT FALSE,
    dnd_start_time VARCHAR(5) NULL,
    dnd_end_time VARCHAR(5) NULL,
    sound_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    vibration_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    show_preview BOOLEAN NOT NULL DEFAULT TRUE,
    max_offline_notifications INT NOT NULL DEFAULT 100,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

## Usage Examples

### Creating Custom Notifications

```java
@Autowired
private NotificationService notificationService;

// Create a system announcement
notificationService.createAndSendNotification(
    user,                                    // recipient
    Notification.NotificationType.SYSTEM_ANNOUNCEMENT,
    "System Maintenance",                    // title
    "System will be down for maintenance",   // content
    Map.of("maintenanceTime", "2024-01-01T02:00:00"), // data
    Notification.Priority.HIGH,              // priority
    null,                                    // related message
    null,                                    // related chat room
    null                                     // triggered by user
);
```

### Checking User Preferences

```java
@Autowired
private NotificationPreferencesService preferencesService;

// Check if user has notifications enabled
boolean enabled = preferencesService.isPushNotificationsEnabled(user);

// Get full preferences
NotificationPreferencesResponse prefs = preferencesService.getPreferencesForUser(user);
```

## Integration with Existing System

The enhanced notification system works alongside the existing `UnreadMessageService`:

1. **Message Notifications**: Both systems send notifications for new messages
2. **Presence Tracking**: Both use `UserPresenceService` for smart delivery
3. **WebSocket Delivery**: Both use the same WebSocket infrastructure
4. **User Management**: Both integrate with existing user authentication

## Configuration

Add to `application.yml`:

```yaml
spring:
  sql:
    init:
      schema-locations:
        - classpath:db/create-notification-tables.sql
```

## Benefits

1. **Comprehensive Coverage** - Supports all types of notifications
2. **User Control** - Granular preference settings
3. **Persistent Storage** - No lost notifications for offline users
4. **Smart Delivery** - Only notifies when necessary
5. **Extensible** - Easy to add new notification types
6. **Performance** - Efficient database queries and indexing
7. **Real-time** - Immediate WebSocket delivery
8. **Backward Compatible** - Works with existing notification system

## Flutter Client Integration

### 1. Subscribe to Notifications

```dart
// Subscribe to notifications
stompClient.subscribe('/user/notifications', (frame) {
  final notification = NotificationResponse.fromJson(jsonDecode(frame.body));
  _handleIncomingNotification(notification);
});

// Subscribe to unread count updates
stompClient.subscribe('/user/notifications/unread-count', (frame) {
  final data = jsonDecode(frame.body);
  _updateUnreadCount(data['unreadCount']);
});
```

### 2. Notification Model

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
    );
  }
}
```

### 3. Notification Service

```dart
class NotificationService {
  static final StompClient _stompClient = WebSocketService.instance.stompClient;

  static void getUnreadNotifications() {
    _stompClient.send(destination: '/app/notifications.getUnread');
  }

  static void markAsRead(int notificationId) {
    _stompClient.send(
      destination: '/app/notifications.markAsRead',
      body: jsonEncode({'notificationId': notificationId}),
    );
  }

  static void markAllAsRead() {
    _stompClient.send(destination: '/app/notifications.markAllAsRead');
  }

  static void getUnreadCount() {
    _stompClient.send(destination: '/app/notifications.getUnreadCount');
  }
}
```

### 4. Local Notifications Integration

```dart
void _handleIncomingNotification(NotificationResponse notification) {
  // Show local notification
  NotificationService.showLocalNotification(
    id: notification.id,
    title: notification.title,
    body: notification.content,
    payload: jsonEncode({
      'type': 'chat_notification',
      'notificationId': notification.id,
      'chatRoomId': notification.relatedChatRoomId,
    }),
  );

  // Update UI state
  _addNotificationToList(notification);
  _updateUnreadBadge();
}
```

## Future Enhancements

1. **Push Notifications** - Integration with mobile push services
2. **Email Notifications** - Fallback email delivery
3. **Notification Templates** - Customizable notification formats
4. **Bulk Operations** - Batch notification management
5. **Analytics** - Notification delivery and engagement metrics
6. **Mentions** - @username mention detection and notifications
