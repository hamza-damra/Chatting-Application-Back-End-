# 📱 Flutter Push Notification Integration Guide

## Overview

This document provides comprehensive guidance for Flutter developers to integrate with the enhanced Spring Boot push notification system. The backend has been specifically configured to support Flutter's real-time notification requirements through WebSocket connections and REST APIs.

## 🔧 Backend Configuration Changes

### 1. **Notification System Components Added**

#### **Core Services**
- **`NotificationService`** - Central service for creating, managing, and delivering notifications
- **`NotificationPreferencesService`** - Manages user notification preferences and settings
- **`UserPresenceService`** - Tracks user activity to enable smart notification delivery

#### **Database Models**
- **`Notification`** - Persistent notification entity with comprehensive metadata
- **`NotificationPreferences`** - User-specific notification settings and preferences

#### **Controllers**
- **`NotificationController`** (REST) - HTTP endpoints for notification management
- **`NotificationController`** (WebSocket) - Real-time WebSocket message handling
- **`NotificationTestController`** - Testing and diagnostic endpoints

#### **Database Tables**
```sql
-- Notifications table
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

-- Notification preferences table
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
    max_offline_notifications INT NOT NULL DEFAULT 100
);
```

### 2. **Smart Notification Logic**
- **Presence-based delivery** - Only sends notifications to users not actively viewing the chat room
- **User preference enforcement** - Respects individual notification settings
- **Do Not Disturb mode** - Supports time-based notification suppression
- **Persistent storage** - Notifications saved for offline users

## 🔌 Flutter Integration Support

### 1. **WebSocket Endpoint Compatibility**

The Spring Boot backend provides the exact WebSocket endpoint your Flutter client expects:

```dart
// Flutter client configuration
static const String baseUrl = 'http://abusaker.zapto.org:8080';
static const String webSocketUrl = 'ws://abusaker.zapto.org:8080/ws';
```

**Backend WebSocket Configuration:**
```java
@Override
public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws")
            .setAllowedOrigins(corsProperties.getAllowedOrigins().split(","))
            .withSockJS();
}
```

### 2. **Subscription Channels**

The backend supports all subscription channels used by your Flutter client:

| Flutter Client Constant | Backend Endpoint | Purpose |
|-------------------------|------------------|---------|
| `stompUnreadTopic` | `/user/queue/unread` | Unread message notifications |
| `stompUnreadMessagesEndpoint` | `/user/unread-messages` | Unread message count updates |
| `stompUserStatusTopic` | `/user/queue/notifications` | General notifications |
| Enhanced notifications | `/user/notifications` | Rich push notifications |

### 3. **Message Format Compatibility**

**Flutter Expected Format:**
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
}
```

**Backend Message Format:**
```json
{
  "id": 123,
  "title": "New Message",
  "content": "You have a new message",
  "notificationType": "NEW_MESSAGE",
  "priority": "NORMAL",
  "isRead": false,
  "isDelivered": true,
  "createdAt": "2024-01-01T12:00:00",
  "relatedMessageId": 456,
  "relatedChatRoomId": 789,
  "triggeredByUserId": 101,
  "triggeredByUsername": "sender"
}
```

### 4. **Authentication Integration**

The backend supports JWT authentication for WebSocket connections:

```dart
// Flutter WebSocket connection with JWT
stompClient.connect(
  {'Authorization': 'Bearer $authToken'},
  onConnect: (frame) {
    // Subscribe to notification channels
  }
);
```

## 📡 API Endpoints

### 1. **Notification Management Endpoints**

#### **Get Notifications**
```http
GET /api/notifications?page=0&size=20
Authorization: Bearer {jwt_token}
```

#### **Get Unread Notifications**
```http
GET /api/notifications/unread
Authorization: Bearer {jwt_token}
```

#### **Get Unread Count**
```http
GET /api/notifications/unread/count
Authorization: Bearer {jwt_token}

Response:
{
  "unreadCount": 5
}
```

#### **Mark Notification as Read**
```http
PUT /api/notifications/{notificationId}/read
Authorization: Bearer {jwt_token}
```

#### **Mark All Notifications as Read**
```http
PUT /api/notifications/read-all
Authorization: Bearer {jwt_token}
```

### 2. **Notification Preferences Endpoints**

#### **Get User Preferences**
```http
GET /api/notifications/preferences
Authorization: Bearer {jwt_token}
```

#### **Update User Preferences**
```http
PUT /api/notifications/preferences
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "pushNotificationsEnabled": true,
  "newMessageNotifications": true,
  "doNotDisturb": false,
  "dndStartTime": "22:00",
  "dndEndTime": "08:00",
  "soundEnabled": true,
  "vibrationEnabled": true
}
```

### 3. **Testing Endpoints (Development Only)**

#### **Send Test Notification**
```http
POST /api/test/notifications/send-test?type=NEW_MESSAGE&title=Test&content=Hello
Authorization: Bearer {jwt_token}
```

#### **Backend Diagnostic**
```http
GET /api/test/notifications/diagnostic
Authorization: Bearer {jwt_token}
```

## 🔌 WebSocket Configuration

### 1. **STOMP Protocol Setup**

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable message broker for Flutter client
        config.enableSimpleBroker("/topic", "/queue", "/user");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }
}
```

### 2. **Flutter WebSocket Integration**

```dart
// Connect to WebSocket
final socket = SockJS('ws://abusaker.zapto.org:8080/ws');
stompClient = Stomp.over(socket);

// Connect with authentication
stompClient.connect(
  {'Authorization': 'Bearer $authToken'},
  onConnect: (frame) {
    // Subscribe to all notification channels
    stompClient.subscribe('/user/queue/unread', onUnreadMessage);
    stompClient.subscribe('/user/unread-messages', onUnreadCount);
    stompClient.subscribe('/user/notifications', onNotification);
    stompClient.subscribe('/user/queue/notifications', onUserStatus);
  }
);
```

### 3. **Message Handling**

```dart
void onNotification(StompFrame frame) {
  final notification = NotificationResponse.fromJson(
    jsonDecode(frame.body!)
  );

  // Display local notification
  NotificationService.showLocalNotification(
    id: notification.id,
    title: notification.title,
    body: notification.content,
  );

  // Update UI
  updateNotificationList(notification);
}
```

## 🧪 Testing and Diagnostics

### 1. **Web Testing Interface**

Access the comprehensive testing interface at:
```
http://localhost:8080/notification-test.html
```

**Features:**
- User authentication testing
- WebSocket connection verification
- Real-time notification testing
- Backend diagnostic checks
- Notification preference testing

### 2. **Backend Diagnostic Endpoint**

```http
GET /api/test/notifications/diagnostic
Authorization: Bearer {jwt_token}

Response:
{
  "status": "SUCCESS",
  "notificationServiceAvailable": true,
  "userServiceAvailable": true,
  "userAuthenticated": true,
  "webSocketEndpoint": "/ws",
  "expectedSubscriptions": [
    "/user/queue/unread",
    "/user/unread-messages",
    "/user/notifications",
    "/user/queue/notifications"
  ],
  "restApiEndpoints": [...],
  "message": "Backend notification system is properly configured"
}
```

### 3. **Flutter Integration Testing**

```dart
// Test WebSocket connection
Future<bool> testWebSocketConnection() async {
  try {
    await connectWebSocket();
    return stompClient.connected;
  } catch (e) {
    print('WebSocket connection failed: $e');
    return false;
  }
}

// Test notification reception
Future<void> testNotificationReception() async {
  final response = await http.post(
    Uri.parse('$baseUrl/api/test/notifications/send-test'),
    headers: {'Authorization': 'Bearer $authToken'},
  );

  if (response.statusCode == 200) {
    print('Test notification sent successfully');
  }
}
```

## ⚙️ Configuration Requirements

### 1. **Application Configuration (application.yml)**

```yaml
# CORS Configuration for Flutter client
cors:
  allowed-origins: "http://localhost:3000,http://abusaker.zapto.org:8080"
  allowed-methods: "GET,POST,PUT,DELETE,OPTIONS"
  allowed-headers: "*"

# WebSocket Configuration
websocket:
  max-text-message-size: 8192
  max-binary-message-size: 8192

# Database Configuration
spring:
  sql:
    init:
      schema-locations:
        - classpath:db/create-notification-tables.sql

# Logging for debugging
logging:
  level:
    com.chatapp.service.NotificationService: DEBUG
    com.chatapp.websocket: DEBUG
```

### 2. **Environment Variables**

```bash
# Database Configuration
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/chatapp
SPRING_DATASOURCE_USERNAME=your_username
SPRING_DATASOURCE_PASSWORD=your_password

# JWT Configuration
JWT_SECRET=your_jwt_secret_key
JWT_EXPIRATION=86400000

# CORS Configuration
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://abusaker.zapto.org:8080
```

### 3. **Flutter Client Configuration**

```dart
class ApiConfig {
  static const String baseUrl = 'http://abusaker.zapto.org:8080';
  static const String webSocketUrl = 'ws://abusaker.zapto.org:8080/ws';

  // WebSocket subscription endpoints
  static const String stompUnreadTopic = '/user/queue/unread';
  static const String stompUnreadMessagesEndpoint = '/user/unread-messages';
  static const String stompUserStatusTopic = '/user/queue/notifications';
  static const String stompNotificationTopic = '/user/notifications';
}
```

## 🚀 Quick Start Integration

### 1. **Flutter Dependencies**

```yaml
dependencies:
  stomp_dart_client: ^1.0.0
  flutter_local_notifications: ^16.3.2
  permission_handler: ^11.2.0
  http: ^1.1.0
```

### 2. **Initialize Notification Service**

```dart
class NotificationService {
  static late StompClient stompClient;

  static Future<void> initialize(String authToken) async {
    // Request permissions
    await Permission.notification.request();

    // Initialize local notifications
    await initializeLocalNotifications();

    // Connect to WebSocket
    await connectWebSocket(authToken);
  }

  static Future<void> connectWebSocket(String authToken) async {
    final socket = SockJS('${ApiConfig.webSocketUrl}');
    stompClient = Stomp.over(socket);

    stompClient.connect(
      {'Authorization': 'Bearer $authToken'},
      onConnect: (frame) {
        subscribeToNotifications();
      }
    );
  }

  static void subscribeToNotifications() {
    stompClient.subscribe(ApiConfig.stompNotificationTopic, (frame) {
      final notification = NotificationResponse.fromJson(
        jsonDecode(frame.body!)
      );
      handleNotification(notification);
    });
  }
}
```

### 3. **Handle Notifications**

```dart
static void handleNotification(NotificationResponse notification) {
  // Show local notification
  showLocalNotification(
    id: notification.id,
    title: notification.title,
    body: notification.content,
    payload: jsonEncode({
      'notificationId': notification.id,
      'chatRoomId': notification.relatedChatRoomId,
    }),
  );

  // Update app state
  NotificationProvider.instance.addNotification(notification);
}
```

## 🔍 Troubleshooting

### Common Issues and Solutions

1. **WebSocket Connection Fails**
   - Verify CORS configuration includes your Flutter client origin
   - Check JWT token is valid and properly formatted
   - Ensure WebSocket endpoint is accessible

2. **Notifications Not Received**
   - Check user notification preferences
   - Verify user is not active in the chat room
   - Check subscription endpoints match backend configuration

3. **Authentication Issues**
   - Ensure JWT token is included in WebSocket connection headers
   - Verify token is not expired
   - Check user exists and is authenticated

## 📚 Additional Resources

- [Backend Notification System Audit Report](./NOTIFICATION_SYSTEM_AUDIT_REPORT.md)
- [Push Notification Testing Guide](./PUSH_NOTIFICATION_TESTING_GUIDE.md)
- [Enhanced Push Notifications Guide](./ENHANCED_PUSH_NOTIFICATIONS_GUIDE.md)
- [Backend Notification Verification](./BACKEND_NOTIFICATION_VERIFICATION.md)

## 🎯 Advanced Features

### 1. **Notification Types and Priorities**

The backend supports multiple notification types that your Flutter app can handle differently:

```dart
enum NotificationType {
  NEW_MESSAGE,
  PRIVATE_MESSAGE,
  GROUP_MESSAGE,
  MENTION,
  CHAT_ROOM_INVITE,
  CHAT_ROOM_ADDED,
  CHAT_ROOM_REMOVED,
  USER_JOINED,
  USER_LEFT,
  FILE_SHARED,
  SYSTEM_ANNOUNCEMENT,
  FRIEND_REQUEST,
  FRIEND_ACCEPTED
}

enum Priority {
  LOW,      // Background notifications
  NORMAL,   // Standard notifications
  HIGH,     // Important notifications
  URGENT    // Critical notifications with sound/vibration
}
```

### 2. **Do Not Disturb (DND) Mode**

The backend supports sophisticated DND functionality:

```dart
class NotificationPreferences {
  bool doNotDisturb;
  String? dndStartTime; // "22:00"
  String? dndEndTime;   // "08:00"

  // Supports overnight ranges (22:00 to 08:00)
  // Supports same-day ranges (09:00 to 17:00)
  // Supports all-day DND (same start/end time)
}
```

### 3. **Offline Notification Storage**

The backend automatically stores notifications for offline users:

- Notifications persist in database when user is offline
- Automatic delivery when user comes online
- Configurable maximum offline notifications per user
- Automatic cleanup of old notifications

### 4. **User Presence Integration**

Smart notification delivery based on user activity:

```java
// Backend only sends notifications to users NOT actively viewing the chat room
boolean isActiveInRoom = userPresenceService.isUserActiveInRoom(username, chatRoomId);
if (!isActiveInRoom) {
    // Send notification
}
```

## 🏗️ Production Deployment

### 1. **Security Considerations**

```yaml
# Production application.yml
spring:
  security:
    require-ssl: true

# Use HTTPS for WebSocket in production
websocket:
  endpoint: "wss://your-domain.com/ws"
```

```dart
// Flutter production configuration
class ApiConfig {
  static const String baseUrl = 'https://your-domain.com';
  static const String webSocketUrl = 'wss://your-domain.com/ws';
}
```

### 2. **Performance Optimization**

```yaml
# WebSocket performance tuning
websocket:
  max-text-message-size: 8192
  max-binary-message-size: 8192
  send-buffer-size-limit: 524288
  message-size-limit: 65536

# Database connection pooling
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
```

### 3. **Monitoring and Logging**

```yaml
# Production logging
logging:
  level:
    com.chatapp.service.NotificationService: INFO
    com.chatapp.websocket: WARN
  pattern:
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/notification-service.log
```

## 📱 Flutter Implementation Examples

### 1. **Complete Notification Service**

```dart
class NotificationService {
  static late FlutterLocalNotificationsPlugin _notifications;
  static late StompClient _stompClient;

  static Future<void> initialize(String userId, String authToken) async {
    await _initializeLocalNotifications();
    await _connectWebSocket(authToken);
    await _subscribeToChannels();
  }

  static Future<void> _initializeLocalNotifications() async {
    _notifications = FlutterLocalNotificationsPlugin();

    const androidSettings = AndroidInitializationSettings('@mipmap/ic_launcher');
    const iosSettings = DarwinInitializationSettings(
      requestAlertPermission: true,
      requestBadgePermission: true,
      requestSoundPermission: true,
    );

    await _notifications.initialize(
      const InitializationSettings(
        android: androidSettings,
        iOS: iosSettings,
      ),
      onDidReceiveNotificationResponse: _onNotificationTapped,
    );
  }

  static void _onNotificationTapped(NotificationResponse response) {
    if (response.payload != null) {
      final data = jsonDecode(response.payload!);
      // Navigate to chat room or handle notification action
      NavigationService.navigateToChatRoom(data['chatRoomId']);
    }
  }

  static Future<void> showLocalNotification({
    required int id,
    required String title,
    required String body,
    String? payload,
  }) async {
    const androidDetails = AndroidNotificationDetails(
      'chat_messages',
      'Chat Messages',
      channelDescription: 'Notifications for new chat messages',
      importance: Importance.high,
      priority: Priority.high,
      showWhen: true,
    );

    const iosDetails = DarwinNotificationDetails(
      presentAlert: true,
      presentBadge: true,
      presentSound: true,
    );

    await _notifications.show(
      id,
      title,
      body,
      const NotificationDetails(
        android: androidDetails,
        iOS: iosDetails,
      ),
      payload: payload,
    );
  }
}
```

### 2. **Background Service Integration**

```dart
class BackgroundNotificationService {
  static Future<void> initializeBackgroundService() async {
    final service = FlutterBackgroundService();

    await service.configure(
      androidConfiguration: AndroidConfiguration(
        onStart: onStart,
        autoStart: true,
        isForegroundMode: false,
      ),
      iosConfiguration: IosConfiguration(
        autoStart: true,
        onForeground: onStart,
        onBackground: onIosBackground,
      ),
    );
  }

  @pragma('vm:entry-point')
  static void onStart(ServiceInstance service) async {
    // Initialize WebSocket connection in background
    final authToken = await TokenService.getStoredToken();
    if (authToken != null) {
      await NotificationService.initialize(userId, authToken);
    }
  }
}
```

### 3. **State Management Integration**

```dart
class NotificationProvider extends ChangeNotifier {
  List<NotificationResponse> _notifications = [];
  int _unreadCount = 0;

  List<NotificationResponse> get notifications => _notifications;
  int get unreadCount => _unreadCount;

  void addNotification(NotificationResponse notification) {
    _notifications.insert(0, notification);
    if (!notification.isRead) {
      _unreadCount++;
    }
    notifyListeners();
  }

  Future<void> markAsRead(int notificationId) async {
    final response = await http.put(
      Uri.parse('${ApiConfig.baseUrl}/api/notifications/$notificationId/read'),
      headers: {'Authorization': 'Bearer ${TokenService.token}'},
    );

    if (response.statusCode == 200) {
      final index = _notifications.indexWhere((n) => n.id == notificationId);
      if (index != -1 && !_notifications[index].isRead) {
        _notifications[index] = _notifications[index].copyWith(isRead: true);
        _unreadCount--;
        notifyListeners();
      }
    }
  }

  Future<void> markAllAsRead() async {
    final response = await http.put(
      Uri.parse('${ApiConfig.baseUrl}/api/notifications/read-all'),
      headers: {'Authorization': 'Bearer ${TokenService.token}'},
    );

    if (response.statusCode == 200) {
      _notifications = _notifications.map((n) => n.copyWith(isRead: true)).toList();
      _unreadCount = 0;
      notifyListeners();
    }
  }
}
```

## 🔧 Maintenance and Updates

### 1. **Database Migrations**

When updating the notification system, use proper database migrations:

```sql
-- Example migration for adding new notification type
ALTER TABLE notifications
ADD COLUMN notification_category VARCHAR(50) DEFAULT 'GENERAL';

-- Add index for better performance
CREATE INDEX idx_notification_category ON notifications(notification_category);
```

### 2. **Backward Compatibility**

The backend maintains backward compatibility:

- Old Flutter clients continue to work with existing endpoints
- New features are additive, not breaking changes
- API versioning support for future major changes

### 3. **Performance Monitoring**

```java
@Component
public class NotificationMetrics {
    private final MeterRegistry meterRegistry;

    public void recordNotificationSent(String type) {
        Counter.builder("notifications.sent")
            .tag("type", type)
            .register(meterRegistry)
            .increment();
    }

    public void recordWebSocketConnection() {
        Gauge.builder("websocket.connections")
            .register(meterRegistry, this, NotificationMetrics::getActiveConnections);
    }
}
```

---

## 🎉 Summary

The Spring Boot backend is now **fully configured** to support your Flutter application's push notification requirements with:

✅ **Complete WebSocket Integration** - Real-time bidirectional communication
✅ **Comprehensive REST APIs** - Full notification management capabilities
✅ **Smart Delivery Logic** - Presence-based and preference-aware notifications
✅ **Robust Testing Tools** - Comprehensive diagnostic and testing endpoints
✅ **Production Ready** - Security, performance, and monitoring considerations
✅ **Flutter Optimized** - Message formats and endpoints designed for Flutter clients

Your Flutter app can now provide a seamless, real-time notification experience with full backend support! 🚀
