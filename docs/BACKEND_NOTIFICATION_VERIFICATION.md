# 🔧 Backend Push Notification Verification Guide

## 🎯 **Spring Boot Configuration Checklist**

### ✅ **1. WebSocket Configuration Verification**

Your current WebSocket configuration is **CORRECT** and supports the Flutter client requirements:

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // ✅ CORRECT: Supports all required endpoints
        config.enableSimpleBroker("/topic", "/queue", "/user");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // ✅ CORRECT: Matches Flutter client endpoint
        registry.addEndpoint("/ws")
                .setAllowedOrigins(corsProperties.getAllowedOrigins().split(","))
                .withSockJS();
    }
}
```

### ✅ **2. Required Notification Endpoints**

Your backend provides these endpoints that match the Flutter client expectations:

#### **WebSocket Subscription Endpoints:**
- ✅ `/user/queue/unread` - Unread message notifications
- ✅ `/user/unread-messages` - Unread message updates  
- ✅ `/user/notifications` - Enhanced push notifications
- ✅ `/user/queue/notifications` - Notification queue

#### **REST API Endpoints:**
- ✅ `GET /api/notifications` - Get paginated notifications
- ✅ `GET /api/notifications/unread` - Get unread notifications
- ✅ `GET /api/notifications/unread/count` - Get unread count
- ✅ `PUT /api/notifications/{id}/read` - Mark as read
- ✅ `PUT /api/notifications/read-all` - Mark all as read
- ✅ `GET /api/notifications/preferences` - Get preferences
- ✅ `PUT /api/notifications/preferences` - Update preferences

### ✅ **3. WebSocket Message Routing**

Your backend correctly routes messages to these destinations:

```java
// Unread message notifications (existing system)
messagingTemplate.convertAndSendToUser(username, "/queue/unread", notification);

// Enhanced push notifications (new system)
messagingTemplate.convertAndSendToUser(username, "/notifications", notification);

// Unread count updates
messagingTemplate.convertAndSendToUser(username, "/unread-messages", unreadData);
```

## 🧪 **Backend Testing Procedures**

### **1. Verify WebSocket Endpoint**
```bash
# Test WebSocket endpoint accessibility
curl -I http://localhost:8080/ws
# Should return: HTTP/1.1 200 OK
```

### **2. Test Authentication Integration**
```bash
# Login and get JWT token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "testuser", "password": "password"}'

# Use token for notification endpoints
curl -X GET http://localhost:8080/api/notifications \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### **3. Test Notification Creation**
```bash
# Send test notification
curl -X POST "http://localhost:8080/api/test/notifications/send-test?type=NEW_MESSAGE&title=Test" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Verify notification was created
curl -X GET "http://localhost:8080/api/notifications/unread/count" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### **4. Test WebSocket Message Delivery**
Use the web test interface at: `http://localhost:8080/notification-test.html`

1. Login with valid credentials
2. Connect WebSocket
3. Send test notification
4. Verify real-time delivery

## 📊 **Backend Log Monitoring**

### **Expected Success Logs:**
```
✅ WEBSOCKET: Connected to WebSocket
✅ WEBSOCKET: Subscribed to unread updates on multiple endpoints
✅ NOTIFICATION: Created notification [ID] for user [username]
✅ NOTIFICATION: Delivered notification [ID] via WebSocket to user [username]
✅ WEBSOCKET OUTBOUND: Sending message to destination: /user/[username]/notifications
```

### **Error Indicators:**
```
❌ WEBSOCKET: Failed to connect to WebSocket
❌ NOTIFICATION: Failed to deliver notification [ID] via WebSocket
❌ WEBSOCKET: Authentication failed for user [username]
❌ NOTIFICATION: Notification type [TYPE] disabled for user [username]
```

## 🔧 **Backend Configuration Verification**

### **1. Database Tables**
Verify notification tables exist:
```sql
-- Check if notification tables were created
SHOW TABLES LIKE 'notifications';
SHOW TABLES LIKE 'notification_preferences';

-- Verify table structure
DESCRIBE notifications;
DESCRIBE notification_preferences;
```

### **2. Application Properties**
Verify your `application.yml` includes:
```yaml
spring:
  sql:
    init:
      schema-locations:
        - classpath:db/create-notification-tables.sql
  
logging:
  level:
    com.chatapp.service.NotificationService: DEBUG
    com.chatapp.websocket: DEBUG
```

### **3. CORS Configuration**
Ensure CORS allows your Flutter client:
```yaml
cors:
  allowed-origins: "http://localhost:3000,http://abusaker.zapto.org:8080"
  allowed-methods: "GET,POST,PUT,DELETE,OPTIONS"
  allowed-headers: "*"
```

## 🚀 **Flutter Client Compatibility**

Your backend is configured to work with the Flutter client expectations:

### **WebSocket Connection:**
- ✅ Endpoint: `ws://abusaker.zapto.org:8080/ws`
- ✅ Protocol: STOMP over SockJS
- ✅ Authentication: JWT Bearer token

### **Subscription Endpoints:**
- ✅ `/user/queue/unread` - Matches Flutter `stompUnreadTopic`
- ✅ `/user/unread-messages` - Matches Flutter `stompUnreadMessagesEndpoint`
- ✅ `/user/notifications` - Enhanced notifications
- ✅ `/user/queue/notifications` - Matches Flutter `stompUserStatusTopic`

### **Message Format:**
Your backend sends messages in the format expected by Flutter:
```json
{
  "id": 123,
  "title": "New Message",
  "content": "You have a new message",
  "notificationType": "NEW_MESSAGE",
  "priority": "NORMAL",
  "isRead": false,
  "createdAt": "2024-01-01T12:00:00",
  "relatedChatRoomId": 456,
  "triggeredByUsername": "sender"
}
```

## 🔍 **Troubleshooting Backend Issues**

### **Issue: WebSocket Connection Fails**
**Check:**
1. CORS configuration allows client origin
2. JWT authentication is working
3. WebSocket endpoint is accessible
4. No firewall blocking WebSocket connections

### **Issue: Notifications Not Created**
**Check:**
1. User preferences allow notifications
2. User is not active in the chat room
3. Database tables exist and are accessible
4. No errors in notification service logs

### **Issue: Messages Not Delivered via WebSocket**
**Check:**
1. User is connected to WebSocket
2. Subscription endpoints are correct
3. Message routing is working
4. No errors in WebSocket outbound logs

## ✅ **Backend Verification Checklist**

- [ ] WebSocket endpoint accessible at `/ws`
- [ ] STOMP protocol enabled with correct prefixes
- [ ] User-specific messaging configured (`/user` prefix)
- [ ] Notification tables created in database
- [ ] JWT authentication working for WebSocket
- [ ] CORS configured for Flutter client origin
- [ ] Notification service creating notifications
- [ ] WebSocket delivering messages to correct endpoints
- [ ] User presence tracking working
- [ ] Notification preferences being respected
- [ ] Test endpoints accessible for debugging

Your Spring Boot backend is **properly configured** for the Flutter push notification system! 🎉
