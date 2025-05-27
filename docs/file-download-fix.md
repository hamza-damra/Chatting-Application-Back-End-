# File Download Fix Documentation

## Problem Description

The Flutter client was unable to fetch old videos and files from chat rooms because it was trying to use message IDs instead of actual file names in download URLs.

### Issue Details

**Flutter Client Behavior:**
```
I/flutter (15622): INFO: CustomChatWidgetNew - Using fallback video URL: http://abusaker.zapto.org:8080/api/files/download/1178.mp4
I/flutter (15622): INFO: CustomChatWidgetNew - Fetched video URL from backend: http://abusaker.zapto.org:8080/api/files/download/1178.mp4
```

**Working Endpoint Format:**
```
GET http://abusaker.zapto.org:8080/api/files/download/20250527-121214-42.mp4-f4df8da3.mp4
```

**Problem:**
- Client was trying to fetch files using message IDs (e.g., `1178.mp4`)
- Server expected actual file names (e.g., `20250527-121214-42.mp4-f4df8da3.mp4`)
- Client didn't have access to the actual file names stored on the server

## Root Cause Analysis

1. **Message Storage**: Messages store `attachmentUrl` with full file paths like `uploads/video/20250527-121214-42.mp4-f4df8da3.mp4`
2. **Client Logic**: Flutter client was constructing URLs using message IDs instead of file names
3. **Missing Download URL**: `MessageResponse` DTO didn't provide a proper download URL for the client
4. **Endpoint Mismatch**: Existing `/api/files/download/{filename}` endpoint required actual filenames

## Solution Implemented

### 1. New Message-Based Download Endpoint

**File**: `src/main/java/com/chatapp/controller/FileController.java`

Added a new endpoint that serves files by message ID:

```java
@GetMapping("/message/{messageId}")
@PreAuthorize("hasRole('USER')")
public ResponseEntity<Resource> getFileByMessageId(@PathVariable Long messageId) {
    // Get current user for access control
    User currentUser = userService.getCurrentUser();

    // Find the message
    Message message = messageRepository.findById(messageId)
            .orElseThrow(() -> new ResourceNotFoundException("Message not found with id: " + messageId));

    // Verify user has access to this message (must be participant in the chat room)
    if (!message.getChatRoom().getParticipants().contains(currentUser)) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    // Serve the file using the stored attachmentUrl
    String attachmentUrl = message.getAttachmentUrl();
    // ... file serving logic
}
```

**Key Features:**
- **Access Control**: Verifies user is a participant in the chat room
- **Security**: Prevents unauthorized access to files
- **Logging**: Comprehensive logging for debugging
- **Error Handling**: Proper error responses for various failure scenarios

### 2. Enhanced MessageResponse DTO

**File**: `src/main/java/com/chatapp/dto/MessageResponse.java`

Added a new `downloadUrl` field:

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    private Long id;
    private String content;
    private String contentType;
    private String attachmentUrl;
    private String downloadUrl; // New field for proper download URL
    private UserResponse sender;
    private Long chatRoomId;
    private LocalDateTime sentAt;
    private com.chatapp.model.MessageStatus.Status status;
    private List<MessageStatusResponse> messageStatuses;
}
```

### 3. Updated DTO Converter Service

**File**: `src/main/java/com/chatapp/service/DtoConverterService.java`

Enhanced to generate proper download URLs:

```java
// Generate proper download URL for files
String downloadUrl = null;
if (message.getAttachmentUrl() != null && !message.getAttachmentUrl().isEmpty()) {
    downloadUrl = "/api/files/message/" + message.getId();
}

return MessageResponse.builder()
    .id(message.getId())
    .content(message.getContent())
    .contentType(message.getContentType())
    .attachmentUrl(message.getAttachmentUrl())
    .downloadUrl(downloadUrl) // Include proper download URL
    .sender(userService.convertToUserResponse(message.getSender()))
    .chatRoomId(message.getChatRoom().getId())
    .sentAt(message.getSentAt())
    .status(userStatus)
    .messageStatuses(messageStatuses)
    .build();
```

## API Endpoints

### New Endpoint: Download File by Message ID

```
GET /api/files/message/{messageId}
```

**Parameters:**
- `messageId` (path): The ID of the message containing the file

**Response:**
- **200 OK**: File content with appropriate headers
- **403 Forbidden**: User not authorized to access this file
- **404 Not Found**: Message or file not found
- **500 Internal Server Error**: Server error

**Example:**
```
GET /api/files/message/1178
Authorization: Bearer <token>
```

### Existing Endpoint: Download File by Filename

```
GET /api/files/download/{filename}
```

**Parameters:**
- `filename` (path): The actual filename on the server

**Example:**
```
GET /api/files/download/20250527-121214-42.mp4-f4df8da3.mp4
Authorization: Bearer <token>
```

## Client Integration

### Flutter Client Changes Required

The Flutter client should now use the `downloadUrl` field from `MessageResponse`:

**Before:**
```dart
// Incorrect - using message ID
String videoUrl = "http://abusaker.zapto.org:8080/api/files/download/${messageId}.mp4";
```

**After:**
```dart
// Correct - using downloadUrl from MessageResponse
String videoUrl = "http://abusaker.zapto.org:8080${messageResponse.downloadUrl}";
```

### Example MessageResponse JSON

```json
{
  "id": 1178,
  "content": "video.mp4",
  "contentType": "video/mp4",
  "attachmentUrl": "uploads/video/20250527-121214-42.mp4-f4df8da3.mp4",
  "downloadUrl": "/api/files/message/1178",
  "sender": {
    "id": 1,
    "username": "john_doe",
    "fullName": "John Doe"
  },
  "chatRoomId": 5,
  "sentAt": "2024-12-01T14:30:22",
  "status": "READ",
  "messageStatuses": []
}
```

## Security Features

1. **Authentication Required**: All endpoints require valid JWT token
2. **Authorization Check**: Users can only access files from chat rooms they participate in
3. **Access Logging**: All file access attempts are logged
4. **Error Handling**: Secure error responses that don't leak sensitive information

## Benefits

1. **Backward Compatibility**: Existing filename-based endpoint still works
2. **Simplified Client Logic**: Client doesn't need to know actual filenames
3. **Enhanced Security**: Proper access control based on chat room participation
4. **Better Error Handling**: Clear error messages and proper HTTP status codes
5. **Comprehensive Logging**: Detailed logs for debugging and monitoring

## Testing

### Test Cases

1. **Valid Access**: User downloads file from chat room they participate in
2. **Unauthorized Access**: User tries to download file from chat room they don't participate in
3. **Non-existent Message**: Request for file with invalid message ID
4. **Non-existent File**: Message exists but file is missing from disk
5. **No Attachment**: Message exists but has no attachment

### Example Test Requests

```bash
# Valid download
curl -H "Authorization: Bearer <token>" \
     http://abusaker.zapto.org:8080/api/files/message/1178

# Should return 403 Forbidden for unauthorized user
curl -H "Authorization: Bearer <other_user_token>" \
     http://abusaker.zapto.org:8080/api/files/message/1178

# Should return 404 Not Found
curl -H "Authorization: Bearer <token>" \
     http://abusaker.zapto.org:8080/api/files/message/99999
```

## Migration Notes

1. **No Database Changes**: Solution uses existing message and file storage
2. **Client Update Required**: Flutter client needs to use `downloadUrl` field
3. **Backward Compatibility**: Old filename-based downloads still work
4. **Performance**: No impact on existing functionality

## Error Handling Improvements

### Fixed 500 Error Issue

**Problem**: Messages without attachments were causing 500 Internal Server Error instead of proper 404 responses.

**Root Cause**: The code was throwing `ResourceNotFoundException` for messages without attachments, but this was being caught by the general exception handler and returning 500 status.

**Solution**:
1. **Direct 404 Response**: Return `ResponseEntity.notFound().build()` directly for messages without attachments
2. **Proper Exception Handling**: Added specific `ResourceNotFoundException` catch block
3. **Validation in DTO**: Only generate `downloadUrl` for messages that actually have attachments

### Updated Error Responses

```java
// Before: Threw exception causing 500 error
if (attachmentUrl == null || attachmentUrl.isEmpty()) {
    throw new ResourceNotFoundException("Message does not have an attachment");
}

// After: Returns proper 404 response
if (attachmentUrl == null || attachmentUrl.isEmpty()) {
    log.error("FILE DOWNLOAD: Message {} does not have an attachment", messageId);
    return ResponseEntity.notFound().build();
}
```

## Troubleshooting

### Common Issues

1. **403 Forbidden**: User not participant in chat room
2. **404 Not Found**: Message ID doesn't exist, file missing from disk, or message has no attachment
3. **Empty downloadUrl**: Message has no attachment (this is now handled properly)
4. **500 Internal Server Error**: File exists but is not accessible (permissions issue)

### Log Analysis

Look for these log patterns:

**Successful Download:**
```
FILE DOWNLOAD: Request for file by message ID: 1178
FILE DOWNLOAD: Serving file from message 1178: uploads/video/20250527-121214-42.mp4-f4df8da3.mp4
FILE DOWNLOAD: Successfully serving file video.mp4 with content type video/mp4
```

**Message Without Attachment:**
```
FILE DOWNLOAD: Request for file by message ID: 1180
FILE DOWNLOAD: Message 1180 does not have an attachment
```

**File Not Found:**
```
FILE DOWNLOAD: Request for file by message ID: 1178
FILE DOWNLOAD: Serving file from message 1178: uploads/video/missing-file.mp4
FILE DOWNLOAD: File does not exist on disk: /path/to/uploads/video/missing-file.mp4
```

**Access Denied:**
```
FILE DOWNLOAD: Request for file by message ID: 1178
FILE DOWNLOAD: User unauthorized_user attempted to access file from chat room Private Chat without being a participant
```
