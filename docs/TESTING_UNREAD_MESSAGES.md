# Testing Real-Time Unread Messages

## Manual Testing Guide

### Prerequisites
1. Two users logged into the chat application
2. Both users connected via WebSocket
3. At least one shared chat room

### Test Scenarios

#### Test 1: New Message Unread Count
1. **Setup**: User A and User B are in the same chat room
2. **Action**: User A sends a message
3. **Expected Result**: 
   - User B receives unread count update via `/user/queue/unread`
   - Unread count increases by 1
   - Latest message preview is included

#### Test 2: Mark Message as Read
1. **Setup**: User B has unread messages
2. **Action**: User B marks a message as read
3. **Expected Result**:
   - User B receives unread count update
   - Unread count decreases by 1

#### Test 3: Bulk Mark as Read
1. **Setup**: User B has multiple unread messages
2. **Action**: User B calls `/app/chat.markRoomAsRead/{roomId}`
3. **Expected Result**:
   - User B receives unread count update
   - Unread count becomes 0 for that room

#### Test 4: Initial Unread Counts on Connect
1. **Setup**: User has unread messages before connecting
2. **Action**: User connects to WebSocket
3. **Expected Result**:
   - User automatically receives initial unread counts
   - All chat rooms with unread messages are reported

### WebSocket Testing Commands

#### Subscribe to Unread Updates
```javascript
stompClient.subscribe('/user/queue/unread', function(message) {
    console.log('Unread update:', JSON.parse(message.body));
});
```

#### Request Initial Unread Counts
```javascript
stompClient.send('/app/chat.getUnreadCounts', {}, '{}');
```

#### Mark Room as Read
```javascript
stompClient.send('/app/chat.markRoomAsRead/123', {}, '{}');
```

### Expected Response Format
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

### Verification Points

#### ✅ Real-Time Updates
- [ ] Unread counts update immediately when messages are sent
- [ ] Unread counts update immediately when messages are read
- [ ] No polling required - updates are push-based

#### ✅ Accuracy
- [ ] Unread counts match actual unread messages
- [ ] Total unread count is sum of all room unread counts
- [ ] Latest message preview is correct

#### ✅ Performance
- [ ] Updates are delivered within 100ms
- [ ] No duplicate notifications
- [ ] Minimal database queries

#### ✅ Error Handling
- [ ] Invalid room IDs are handled gracefully
- [ ] Unauthorized access is blocked
- [ ] Network failures don't break the system

### Troubleshooting

#### No Unread Updates Received
1. Check WebSocket connection status
2. Verify subscription to `/user/queue/unread`
3. Check authentication token validity
4. Review server logs for errors

#### Incorrect Unread Counts
1. Verify message status data in database
2. Check participant membership in chat rooms
3. Review unread count calculation logic
4. Test with fresh data

#### Performance Issues
1. Monitor database query performance
2. Check WebSocket connection limits
3. Review memory usage patterns
4. Optimize unread count calculations

### Database Verification

#### Check Message Status
```sql
SELECT m.id, m.content, ms.status, ms.user_id 
FROM messages m 
LEFT JOIN message_statuses ms ON m.id = ms.message_id 
WHERE m.chat_room_id = ? 
ORDER BY m.sent_at DESC;
```

#### Check Unread Counts
```sql
SELECT 
    cr.name as room_name,
    COUNT(CASE WHEN ms.status IS NULL OR ms.status != 'READ' THEN 1 END) as unread_count
FROM chat_rooms cr
JOIN messages m ON cr.id = m.chat_room_id
LEFT JOIN message_statuses ms ON m.id = ms.message_id AND ms.user_id = ?
WHERE cr.id IN (SELECT chatroom_id FROM user_chatrooms WHERE user_id = ?)
GROUP BY cr.id, cr.name;
```

### Success Criteria

The unread message system is working correctly when:

1. **Real-Time Delivery**: Updates arrive within 100ms of trigger events
2. **Accurate Counts**: Unread counts match database state
3. **Complete Coverage**: All scenarios (new message, read, bulk read) work
4. **Error Resilience**: System handles errors gracefully
5. **Performance**: No noticeable lag or resource issues

### Integration with Client Applications

#### Flutter Integration
```dart
// Subscribe to unread updates
stompClient.subscribe(
  destination: '/user/queue/unread',
  callback: (StompFrame frame) {
    final data = jsonDecode(frame.body!);
    updateUnreadBadges(data);
  },
);
```

#### React/JavaScript Integration
```javascript
// Subscribe to unread updates
stompClient.subscribe('/user/queue/unread', (update) => {
    const data = JSON.parse(update.body);
    updateUnreadBadges(data);
});
```

This testing guide ensures comprehensive validation of the real-time unread message system across all scenarios and platforms.
