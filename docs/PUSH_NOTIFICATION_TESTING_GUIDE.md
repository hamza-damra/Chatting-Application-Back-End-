# Push Notification Testing Guide

## Overview
This guide provides comprehensive instructions for testing the enhanced push notification system in the chat application.

## 🧪 **Testing Methods**

### 1. **Automated Integration Tests**
Location: `src/test/java/com/chatapp/integration/PushNotificationIntegrationTest.java`

Run the tests:
```bash
mvn test -Dtest=PushNotificationIntegrationTest
```

**Test Coverage:**
- ✅ Create and send notifications
- ✅ Message notifications to inactive users
- ✅ Skip notifications for active users
- ✅ Respect user notification preferences
- ✅ Do Not Disturb mode functionality
- ✅ Pagination and ordering
- ✅ Mark notifications as read
- ✅ Unread count tracking
- ✅ Private vs group message notifications

### 2. **Manual REST API Testing**
Location: `src/main/java/com/chatapp/controller/NotificationTestController.java`

**Test Endpoints:**
- `POST /api/test/notifications/send-test` - Send test notification
- `POST /api/test/notifications/send-to-user` - Send to specific user
- `POST /api/test/notifications/test-all-types` - Test all notification types
- `GET /api/test/notifications/stats` - Get notification statistics

### 3. **Web Interface Testing**
Location: `src/main/resources/static/notification-test.html`

Access: `http://localhost:8080/notification-test.html`

**Features:**
- 🔐 User authentication
- 🔌 WebSocket connection management
- 📤 Send test notifications
- 📊 View notification statistics
- 🔴 Live notification display
- ⚙️ Test different notification types and priorities

## 🚀 **Step-by-Step Testing Process**

### **Step 1: Start the Application**
```bash
mvn spring-boot:run
```

### **Step 2: Create Test Users**
Use the registration endpoint or database to create test users:
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser1",
    "email": "test1@example.com",
    "fullName": "Test User 1",
    "password": "password123"
  }'

curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser2",
    "email": "test2@example.com",
    "fullName": "Test User 2",
    "password": "password123"
  }'
```

### **Step 3: Test Authentication**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser1",
    "password": "password123"
  }'
```
Save the returned JWT token for subsequent requests.

### **Step 4: Test Basic Notification Functionality**

#### Send Test Notification
```bash
curl -X POST "http://localhost:8080/api/test/notifications/send-test?type=NEW_MESSAGE&title=Test&content=Hello" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

#### Get Notifications
```bash
curl -X GET "http://localhost:8080/api/notifications?page=0&size=10" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

#### Get Unread Count
```bash
curl -X GET "http://localhost:8080/api/notifications/unread/count" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### **Step 5: Test WebSocket Notifications**

1. Open the test page: `http://localhost:8080/notification-test.html`
2. Login with test credentials
3. Connect WebSocket
4. Send test notifications
5. Observe real-time delivery

### **Step 6: Test User Preferences**

#### Get Current Preferences
```bash
curl -X GET "http://localhost:8080/api/notifications/preferences" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

#### Update Preferences
```bash
curl -X PUT "http://localhost:8080/api/notifications/preferences" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "pushNotificationsEnabled": true,
    "doNotDisturb": true,
    "dndStartTime": "22:00",
    "dndEndTime": "08:00"
  }'
```

### **Step 7: Test Chat Integration**

1. Create a chat room
2. Add multiple users to the room
3. Send messages
4. Verify notifications are sent to inactive users only

## 🔍 **Test Scenarios**

### **Scenario 1: Basic Notification Flow**
1. User A sends a message in a chat room
2. User B (not active in room) should receive notification
3. User C (active in room) should NOT receive notification
4. Notification should be stored in database
5. WebSocket should deliver notification immediately if user is online

### **Scenario 2: User Preferences**
1. User disables push notifications
2. Send message to that user
3. Verify no notification is created
4. Enable notifications again
5. Verify notifications work

### **Scenario 3: Do Not Disturb Mode**
1. Set DND mode for user (e.g., 22:00-08:00)
2. During DND hours, send notifications
3. Verify notifications are suppressed
4. Outside DND hours, verify notifications work

### **Scenario 4: Different Notification Types**
1. Test NEW_MESSAGE notifications
2. Test PRIVATE_MESSAGE notifications
3. Test SYSTEM_ANNOUNCEMENT notifications
4. Test FILE_SHARED notifications
5. Verify each type respects user preferences

### **Scenario 5: Priority Levels**
1. Send LOW priority notification
2. Send NORMAL priority notification
3. Send HIGH priority notification
4. Send URGENT priority notification
5. Verify all are delivered (priority affects client handling)

## 📊 **Expected Results**

### **Successful Notification Creation**
```json
{
  "success": true,
  "message": "Test notification sent successfully",
  "notificationId": 123,
  "type": "NEW_MESSAGE",
  "priority": "NORMAL",
  "delivered": true
}
```

### **WebSocket Notification Format**
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

### **Notification List Response**
```json
{
  "content": [
    {
      "id": 123,
      "title": "New Message",
      "content": "You have a new message",
      "notificationType": "NEW_MESSAGE",
      "priority": "NORMAL",
      "isRead": false,
      "createdAt": "2024-01-01T12:00:00"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```

## 🐛 **Troubleshooting**

### **Common Issues**

1. **WebSocket Connection Fails**
   - Check JWT token is valid
   - Verify WebSocket endpoint is accessible
   - Check browser console for errors

2. **Notifications Not Received**
   - Check user preferences (notifications might be disabled)
   - Verify user is not active in the chat room
   - Check DND mode settings

3. **Database Errors**
   - Verify notification tables exist
   - Check foreign key constraints
   - Ensure proper database permissions

4. **Authentication Issues**
   - Verify JWT token is included in requests
   - Check token expiration
   - Ensure user exists and is active

### **Debug Logging**
Enable debug logging in `application.yml`:
```yaml
logging:
  level:
    com.chatapp.service.NotificationService: DEBUG
    com.chatapp.websocket.NotificationController: DEBUG
```

## ✅ **Test Checklist**

- [ ] Application starts without errors
- [ ] Database tables are created correctly
- [ ] User registration and login work
- [ ] Basic notification creation works
- [ ] WebSocket connection establishes
- [ ] Real-time notifications are delivered
- [ ] User preferences are respected
- [ ] DND mode functions correctly
- [ ] Different notification types work
- [ ] Priority levels are handled
- [ ] Pagination works for notification list
- [ ] Mark as read functionality works
- [ ] Unread count is accurate
- [ ] Chat integration sends notifications
- [ ] Inactive users receive notifications
- [ ] Active users don't receive notifications

## 🎯 **Success Criteria**

The push notification system is working correctly if:

1. ✅ Notifications are created and stored in database
2. ✅ WebSocket delivers notifications in real-time
3. ✅ User preferences control notification delivery
4. ✅ DND mode suppresses notifications during specified hours
5. ✅ Different notification types work as expected
6. ✅ Chat integration only notifies inactive users
7. ✅ REST API endpoints function correctly
8. ✅ Web interface demonstrates all features
9. ✅ No errors in application logs
10. ✅ Performance is acceptable under load
