# N+1 Query Optimization Documentation

## Overview

This document explains the N+1 query problem that was occurring during file uploads and the optimizations implemented to resolve it.

## The Problem

During file uploads, the application was executing hundreds of repetitive database queries, particularly:

```sql
SELECT messagesta0_.id, messagesta0_.message_id, messagesta0_.status, messagesta0_.updated_at, messagesta0_.user_id 
FROM message_statuses messagesta0_ 
WHERE messagesta0_.message_id=? AND messagesta0_.user_id=?
```

This was happening because:

1. **File upload completion** triggered message broadcasting to all chat room participants
2. **DTO conversion** was fetching message statuses individually for each message
3. **Chat room loading** was checking unread counts by querying message statuses one by one
4. **Frontend requests** were triggering additional status queries for each message

## Root Causes

### 1. Individual Message Status Queries
The `DtoConverterService.convertToMessageResponse()` method was not including message status information, causing the frontend to request it separately.

### 2. Unread Count Calculation
The `ChatRoomService.convertToChatRoomResponse()` method was calling `messageStatusService.getMessageStatusForUser()` for each message individually.

### 3. Lack of Batch Fetching
No batch queries existed to fetch message statuses for multiple messages at once.

## Solutions Implemented

### 1. Enhanced DTO Conversion

**File**: `src/main/java/com/chatapp/service/DtoConverterService.java`

#### Single Message Conversion
```java
public MessageResponse convertToMessageResponse(Message message, User currentUser) {
    // Get message status for current user if provided
    MessageStatus.Status userStatus = null;
    if (currentUser != null) {
        userStatus = messageStatusRepository.findByMessageAndUser(message, currentUser)
            .map(MessageStatus::getStatus)
            .orElse(null);
    }
    
    // Include status in response to avoid additional queries
    return MessageResponse.builder()
        .id(message.getId())
        .content(message.getContent())
        // ... other fields
        .status(userStatus)
        .messageStatuses(messageStatuses)
        .build();
}
```

#### Batch Message Conversion
```java
public List<MessageResponse> convertToMessageResponses(List<Message> messages, User currentUser) {
    // Batch fetch message statuses to avoid N+1 queries
    Map<Long, MessageStatus.Status> userStatuses = new HashMap<>();
    
    if (currentUser != null) {
        // Fetch user's statuses for all messages in one query
        List<MessageStatus> userMessageStatuses = messageStatusRepository.findByMessagesAndUser(messages, currentUser);
        userStatuses = userMessageStatuses.stream()
            .collect(Collectors.toMap(
                ms -> ms.getMessage().getId(),
                MessageStatus::getStatus
            ));
    }
    
    // Convert using pre-fetched data
    return messages.stream()
        .map(message -> MessageResponse.builder()
            .id(message.getId())
            .status(userStatuses.get(message.getId()))
            .build())
        .collect(Collectors.toList());
}
```

### 2. Batch Repository Queries

**File**: `src/main/java/com/chatapp/repository/MessageStatusRepository.java`

```java
/**
 * Batch fetch message statuses for multiple messages and a specific user
 * This helps avoid N+1 query problems when loading message lists
 */
@Query("SELECT ms FROM MessageStatus ms WHERE ms.message IN :messages AND ms.user = :user")
List<MessageStatus> findByMessagesAndUser(@Param("messages") List<Message> messages, @Param("user") User user);

/**
 * Batch fetch all message statuses for multiple messages
 * This helps avoid N+1 query problems when loading message lists for senders
 */
@Query("SELECT ms FROM MessageStatus ms WHERE ms.message IN :messages")
List<MessageStatus> findByMessages(@Param("messages") List<Message> messages);
```

### 3. Optimized Chat Room Service

**File**: `src/main/java/com/chatapp/service/ChatRoomService.java`

#### Before (N+1 Problem)
```java
for (Message message : messages) {
    if (!message.getSender().getId().equals(currentUser.getId())) {
        // This causes one query per message!
        MessageStatus.Status status = messageStatusService.getMessageStatusForUser(message, currentUser);
        if (status == null || status != MessageStatus.Status.READ) {
            unreadCount++;
        }
    }
}
```

#### After (Batch Optimized)
```java
// Filter out messages sent by current user
List<Message> messagesFromOthers = messages.stream()
    .filter(message -> !message.getSender().getId().equals(currentUser.getId()))
    .collect(Collectors.toList());
    
if (!messagesFromOthers.isEmpty()) {
    // Batch fetch message statuses to avoid N+1 queries
    List<MessageStatus> userStatuses = messageStatusRepository.findByMessagesAndUser(messagesFromOthers, currentUser);
    Set<Long> readMessageIds = userStatuses.stream()
        .filter(ms -> ms.getStatus() == MessageStatus.Status.READ)
        .map(ms -> ms.getMessage().getId())
        .collect(Collectors.toSet());
        
    // Count messages that are not read
    unreadCount = (int) messagesFromOthers.stream()
        .filter(message -> !readMessageIds.contains(message.getId()))
        .count();
}
```

### 4. Optimized File Upload Controller

**File**: `src/main/java/com/chatapp/websocket/BinaryFileController.java`

```java
// Convert to response DTO with current user context to avoid additional queries
MessageResponse response = dtoConverterService.convertToMessageResponse(message, currentUser);
```

## Performance Impact

### Before Optimization
- **File upload with 10 participants**: ~50-100 individual message status queries
- **Chat room list with 20 rooms**: ~200+ queries for unread counts
- **Message list with 50 messages**: ~50+ status queries per user

### After Optimization
- **File upload with 10 participants**: 1-2 batch queries
- **Chat room list with 20 rooms**: 20 batch queries (1 per room)
- **Message list with 50 messages**: 1-2 batch queries total

## Query Reduction Examples

### File Upload Scenario
**Before**: 
```
Query 1: findByMessageAndUser(message1, user1)
Query 2: findByMessageAndUser(message1, user2)
Query 3: findByMessageAndUser(message1, user3)
... (one per participant)
```

**After**:
```
Query 1: findByMessagesAndUser([message1], currentUser)
```

### Chat Room Loading
**Before**:
```
Query 1: findByMessageAndUser(msg1, user)
Query 2: findByMessageAndUser(msg2, user)
Query 3: findByMessageAndUser(msg3, user)
... (one per message)
```

**After**:
```
Query 1: findByMessagesAndUser([msg1, msg2, msg3, ...], user)
```

## Best Practices Applied

1. **Batch Fetching**: Always fetch related data in batches when possible
2. **Eager Loading**: Include necessary data in initial queries
3. **DTO Optimization**: Pre-populate DTOs with all required data
4. **Repository Design**: Create batch query methods for common access patterns
5. **Service Layer**: Optimize service methods to minimize database round trips

## Monitoring and Verification

To verify the optimizations are working:

1. **Enable SQL Logging**: Check `application.yml` has `show-sql: true`
2. **Monitor Query Count**: Look for reduced repetitive queries in logs
3. **Performance Testing**: Measure response times before and after
4. **Database Monitoring**: Use database tools to monitor query patterns

## Future Improvements

1. **Caching**: Implement Redis caching for frequently accessed message statuses
2. **Pagination**: Add pagination for large message lists
3. **Lazy Loading**: Implement smart lazy loading strategies
4. **Database Indexing**: Ensure proper indexes on message_statuses table
5. **Connection Pooling**: Optimize database connection management

## Related Files

- `src/main/java/com/chatapp/service/DtoConverterService.java`
- `src/main/java/com/chatapp/service/ChatRoomService.java`
- `src/main/java/com/chatapp/repository/MessageStatusRepository.java`
- `src/main/java/com/chatapp/websocket/BinaryFileController.java`
- `docs/enhanced-file-upload-logging.md`
