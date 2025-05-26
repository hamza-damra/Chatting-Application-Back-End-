# Flutter Frontend Updates for Real-Time Unread Message Notifications

## Overview

This document outlines the necessary updates to the Flutter frontend to support the new real-time unread message notification system. The backend now provides smart notifications that are only sent to users who are NOT currently active in a chat room.

## 🆕 New Backend Features

### 1. User Presence Tracking
- Backend now tracks which users are actively viewing specific chat rooms
- Only sends notifications to users who are away from the room

### 2. New WebSocket Endpoints
- `/app/chat.enterRoom/{roomId}` - Mark user as active in room
- `/app/chat.leaveRoom/{roomId}` - Mark user as inactive in room
- `/app/chat.getRoomPresence/{roomId}` - Get active users in room (debug)

### 3. New Notification Channel
- `/user/unread-messages` - Dedicated channel for rich unread message notifications
- Separate from existing `/user/queue/unread` (unread counts)

### 4. Rich Notification Data
- Sender details, content preview, room context, timestamps
- Different notification types (GROUP_MESSAGE, PRIVATE_MESSAGE)

## 📱 Required Flutter Frontend Changes

### 1. Create UnreadMessageNotification Model

Create `lib/models/unread_message_notification.dart`:

```dart
class UnreadMessageNotification {
  final int messageId;
  final int chatRoomId;
  final String chatRoomName;
  final int senderId;
  final String senderUsername;
  final String? senderFullName;
  final String? contentPreview;
  final String contentType;
  final DateTime sentAt;
  final DateTime notificationTimestamp;
  final int unreadCount;
  final int totalUnreadCount;
  final int recipientUserId;
  final bool isPrivateChat;
  final int participantCount;
  final String? attachmentUrl;
  final NotificationType notificationType;

  UnreadMessageNotification({
    required this.messageId,
    required this.chatRoomId,
    required this.chatRoomName,
    required this.senderId,
    required this.senderUsername,
    this.senderFullName,
    this.contentPreview,
    required this.contentType,
    required this.sentAt,
    required this.notificationTimestamp,
    required this.unreadCount,
    required this.totalUnreadCount,
    required this.recipientUserId,
    required this.isPrivateChat,
    required this.participantCount,
    this.attachmentUrl,
    required this.notificationType,
  });

  factory UnreadMessageNotification.fromJson(Map<String, dynamic> json) {
    return UnreadMessageNotification(
      messageId: json['messageId'],
      chatRoomId: json['chatRoomId'],
      chatRoomName: json['chatRoomName'],
      senderId: json['senderId'],
      senderUsername: json['senderUsername'],
      senderFullName: json['senderFullName'],
      contentPreview: json['contentPreview'],
      contentType: json['contentType'],
      sentAt: DateTime.parse(json['sentAt']),
      notificationTimestamp: DateTime.parse(json['notificationTimestamp']),
      unreadCount: json['unreadCount'],
      totalUnreadCount: json['totalUnreadCount'],
      recipientUserId: json['recipientUserId'],
      isPrivateChat: json['isPrivateChat'],
      participantCount: json['participantCount'],
      attachmentUrl: json['attachmentUrl'],
      notificationType: NotificationType.fromString(json['notificationType']),
    );
  }
}

enum NotificationType {
  newMessage('NEW_MESSAGE'),
  mention('MENTION'),
  privateMessage('PRIVATE_MESSAGE'),
  groupMessage('GROUP_MESSAGE');

  const NotificationType(this.value);
  final String value;

  static NotificationType fromString(String value) {
    return NotificationType.values.firstWhere(
      (type) => type.value == value,
      orElse: () => NotificationType.newMessage,
    );
  }
}
```

### 2. Update WebSocket Service

Update your WebSocket service (e.g., `lib/services/websocket_service.dart`):

```dart
class WebSocketService {
  StompClient? _stompClient;
  final Set<int> _activeRooms = <int>{};

  // Existing code...

  void subscribeToNotifications() {
    // Subscribe to rich unread message notifications
    _stompClient?.subscribe(
      destination: '/user/unread-messages',
      callback: (StompFrame frame) {
        if (frame.body != null) {
          try {
            final notification = UnreadMessageNotification.fromJson(
              jsonDecode(frame.body!)
            );
            _handleUnreadMessageNotification(notification);
          } catch (e) {
            print('Error parsing unread message notification: $e');
          }
        }
      },
    );

    // Keep existing subscription to unread counts
    _stompClient?.subscribe(
      destination: '/user/queue/unread',
      callback: (StompFrame frame) {
        // Existing unread count handling
      },
    );
  }

  // New: Mark user as active in room
  void enterRoom(int roomId) {
    _stompClient?.send(
      destination: '/app/chat.enterRoom/$roomId',
      body: '',
    );
    _activeRooms.add(roomId);
    print('Entered room: $roomId');
  }

  // New: Mark user as inactive in room
  void leaveRoom(int roomId) {
    _stompClient?.send(
      destination: '/app/chat.leaveRoom/$roomId',
      body: '',
    );
    _activeRooms.remove(roomId);
    print('Left room: $roomId');
  }

  // New: Get room presence (for debugging)
  void getRoomPresence(int roomId) {
    _stompClient?.send(
      destination: '/app/chat.getRoomPresence/$roomId',
      body: '',
    );
  }

  // New: Handle unread message notifications
  void _handleUnreadMessageNotification(UnreadMessageNotification notification) {
    // Show local notification
    _showLocalNotification(notification);

    // Update UI state
    _notifyListeners(notification);

    // Log for debugging
    print('Received unread message notification: ${notification.senderUsername} in ${notification.chatRoomName}');
  }

  // Clean up when disconnecting
  void disconnect() {
    // Leave all active rooms
    for (final roomId in _activeRooms) {
      leaveRoom(roomId);
    }
    _activeRooms.clear();

    // Existing disconnect logic...
  }
}
```

### 3. Update Chat Room Screen

Update your chat room screen to handle presence tracking:

```dart
class ChatRoomScreen extends StatefulWidget {
  final int roomId;
  // ... other properties
}

class _ChatRoomScreenState extends State<ChatRoomScreen>
    with WidgetsBindingObserver {

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);

    // Mark user as active in this room
    WebSocketService.instance.enterRoom(widget.roomId);
  }

  @override
  void dispose() {
    // Mark user as inactive in this room
    WebSocketService.instance.leaveRoom(widget.roomId);
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    super.didChangeAppLifecycleState(state);

    switch (state) {
      case AppLifecycleState.paused:
      case AppLifecycleState.detached:
        // App is backgrounded, leave room
        WebSocketService.instance.leaveRoom(widget.roomId);
        break;
      case AppLifecycleState.resumed:
        // App is foregrounded, re-enter room
        WebSocketService.instance.enterRoom(widget.roomId);
        break;
      default:
        break;
    }
  }

  // Rest of your chat room implementation...
}
```

### 4. Add Local Notification Support

Add local notifications for unread messages. First, add dependencies to `pubspec.yaml`:

```yaml
dependencies:
  flutter_local_notifications: ^17.0.0
  # ... other dependencies
```

Create `lib/services/notification_service.dart`:

```dart
import 'package:flutter_local_notifications/flutter_local_notifications.dart';

class NotificationService {
  static final FlutterLocalNotificationsPlugin _notifications =
      FlutterLocalNotificationsPlugin();

  static Future<void> initialize() async {
    const androidSettings = AndroidInitializationSettings('@mipmap/ic_launcher');
    const iosSettings = DarwinInitializationSettings(
      requestAlertPermission: true,
      requestBadgePermission: true,
      requestSoundPermission: true,
    );

    const settings = InitializationSettings(
      android: androidSettings,
      iOS: iosSettings,
    );

    await _notifications.initialize(settings);
  }

  static Future<void> showUnreadMessageNotification(
    UnreadMessageNotification notification,
  ) async {
    final androidDetails = AndroidNotificationDetails(
      'unread_messages',
      'Unread Messages',
      channelDescription: 'Notifications for unread chat messages',
      importance: Importance.high,
      priority: Priority.high,
      showWhen: true,
    );

    final iosDetails = DarwinNotificationDetails(
      presentAlert: true,
      presentBadge: true,
      presentSound: true,
    );

    final details = NotificationDetails(
      android: androidDetails,
      iOS: iosDetails,
    );

    final title = notification.isPrivateChat
        ? 'New message from ${notification.senderUsername}'
        : 'New message in ${notification.chatRoomName}';

    final body = notification.contentPreview ?? 'New message received';

    await _notifications.show(
      notification.messageId,
      title,
      body,
      details,
      payload: jsonEncode({
        'roomId': notification.chatRoomId,
        'messageId': notification.messageId,
      }),
    );
  }
}
```

### 5. Update Main App Initialization

Update your main app to initialize notifications:

```dart
void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // Initialize notification service
  await NotificationService.initialize();

  runApp(MyApp());
}
```

### 6. Update Navigation Handling

Update your navigation to properly handle room presence:

```dart
class NavigationService {
  static void navigateToChatRoom(BuildContext context, int roomId) {
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => ChatRoomScreen(roomId: roomId),
      ),
    ).then((_) {
      // Ensure we leave the room when navigation completes
      WebSocketService.instance.leaveRoom(roomId);
    });
  }
}
```

### 7. Add Notification State Management

Create a state management solution for notifications:

```dart
class NotificationProvider extends ChangeNotifier {
  final List<UnreadMessageNotification> _notifications = [];

  List<UnreadMessageNotification> get notifications =>
      List.unmodifiable(_notifications);

  void addNotification(UnreadMessageNotification notification) {
    _notifications.insert(0, notification);
    notifyListeners();

    // Show local notification
    NotificationService.showUnreadMessageNotification(notification);
  }

  void clearNotificationsForRoom(int roomId) {
    _notifications.removeWhere((n) => n.chatRoomId == roomId);
    notifyListeners();
  }

  void clearAllNotifications() {
    _notifications.clear();
    notifyListeners();
  }
}
```

## 🔄 Migration Steps

### Step 1: Update Dependencies
```yaml
# pubspec.yaml
dependencies:
  flutter_local_notifications: ^17.0.0
  provider: ^6.1.1  # If not already added
```

### Step 2: Add New Model Files
- Create `lib/models/unread_message_notification.dart`
- Create `lib/services/notification_service.dart`
- Create `lib/providers/notification_provider.dart`

### Step 3: Update Existing Services
- Update WebSocket service with new subscription and presence methods
- Update navigation service to handle room presence

### Step 4: Update UI Components
- Update chat room screens to call `enterRoom`/`leaveRoom`
- Add notification handling to app lifecycle
- Update main app initialization

### Step 5: Test Integration
- Test presence tracking (enter/leave rooms)
- Test notification delivery (only to absent users)
- Test local notifications
- Test app lifecycle handling

## 🧪 Testing Checklist

### Presence Tracking
- [ ] User enters room → Backend receives `/app/chat.enterRoom/{roomId}`
- [ ] User leaves room → Backend receives `/app/chat.leaveRoom/{roomId}`
- [ ] App backgrounded → User leaves current room
- [ ] App foregrounded → User re-enters current room

### Notification Delivery
- [ ] User A active in room, User B away → Only B gets notification
- [ ] User A sends message while B viewing room → B gets no notification
- [ ] User leaves room then receives message → Gets notification
- [ ] Local notification shows with correct title/content

### Edge Cases
- [ ] App crash/force close → Presence cleaned up on reconnect
- [ ] Network disconnect/reconnect → Presence restored
- [ ] Multiple rooms → Correct presence tracking per room
- [ ] Rapid room switching → No duplicate notifications

## 🚨 Important Notes

### 1. Backward Compatibility
- Existing unread count system (`/user/queue/unread`) still works
- New notification system is additive, not replacing existing features

### 2. Performance Considerations
- Presence tracking uses minimal memory (Set<int> for room IDs)
- Local notifications are throttled by the system
- WebSocket messages are lightweight

### 3. Privacy & Permissions
- Local notifications require user permission
- Handle permission denial gracefully
- Respect user notification preferences

### 4. Error Handling
```dart
// Example error handling in WebSocket service
void _handleUnreadMessageNotification(UnreadMessageNotification notification) {
  try {
    // Process notification
    _showLocalNotification(notification);
  } catch (e) {
    print('Error handling notification: $e');
    // Fallback: at least log the notification
    _logNotificationReceived(notification);
  }
}
```

## 📋 Summary of Changes

### New Features Added:
✅ **Smart Notifications** - Only notify absent users
✅ **Rich Notification Data** - Sender, content, room context
✅ **Presence Tracking** - Know who's viewing each room
✅ **Local Notifications** - Native mobile notifications
✅ **Lifecycle Handling** - Proper presence on app background/foreground

### Existing Features Preserved:
✅ **Unread Counts** - Still available via `/user/queue/unread`
✅ **Message Delivery** - All existing message functionality
✅ **Authentication** - Same security model
✅ **Room Management** - All existing room features

The Flutter frontend will now provide a much better user experience with smart notifications that only appear when users are actually away from the conversation, reducing notification noise while ensuring important messages are never missed.