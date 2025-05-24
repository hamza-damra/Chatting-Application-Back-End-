# WebSocket Binary File Upload Implementation

This document explains how to use the binary file upload feature via WebSocket in the chat application.

## Overview

The application supports uploading large files (images, documents, etc.) via WebSocket by splitting them into smaller chunks. This approach has several advantages:

1. **Bypasses WebSocket message size limitations**: Most WebSocket implementations have message size limits.
2. **Progress tracking**: Allows for real-time progress updates during uploads.
3. **Resume capability**: Theoretically allows for resuming interrupted uploads (not implemented yet).
4. **Efficient memory usage**: Processes files in small chunks rather than loading the entire file into memory.

## Server-Side Implementation

### Components

1. **FileChunk Model**: Represents a chunk of a file being uploaded.
2. **FileChunkService**: Handles processing and assembling file chunks.
3. **BinaryFileController**: WebSocket controller that receives file chunks.
4. **FileStorageProperties**: Configuration for file storage.

### Message Flow

1. Client connects to WebSocket server and authenticates.
2. Client splits a file into chunks and sends them to `/app/file.chunk`.
3. Server processes each chunk and stores it in memory.
4. When all chunks are received, the server assembles the file and saves it to disk.
5. Server creates a message with the file attachment and sends it to all participants in the chat room.
6. Server sends progress updates to the client during the upload.

### Configuration

The server is configured with the following properties in `application.yml`:

```yaml
spring:
  websocket:
    max-text-message-size: 65536     # 64KB for text messages
    max-binary-message-size: 1048576 # 1MB for binary messages

app:
  file-storage:
    upload-dir: uploads
    max-file-size: 10485760  # 10MB max file size
    allowed-content-types: image/jpeg,image/png,image/gif,application/pdf,...
```

## Client-Side Implementation

### Web Client (JavaScript)

The web client uses the following approach:

1. Connect to the WebSocket server using STOMP over WebSocket.
2. Split the file into chunks of 64KB.
3. Convert each chunk to Base64.
4. Send each chunk to the server with metadata.
5. Subscribe to progress updates and completion notifications.

Example chunk format:

```json
{
  "chatRoomId": 1,
  "chunkIndex": 1,
  "totalChunks": 5,
  "fileName": "example.jpg",
  "contentType": "image/jpeg",
  "fileSize": 320000,
  "data": "<base64-encoded-chunk>"
}
```

### Mobile Client (Flutter)

The Flutter client follows a similar approach:

1. Use the `stomp_dart_client` package to connect to the WebSocket server.
2. Use `file_picker` to select files.
3. Read the file in chunks and convert to Base64.
4. Send chunks to the server with metadata.
5. Subscribe to progress updates and completion notifications.

## Subscriptions

Clients should subscribe to the following destinations:

1. `/user/queue/files.progress`: Receives progress updates during upload.
2. `/user/queue/files`: Receives notification when the file upload is complete.
3. `/user/queue/errors`: Receives error notifications.

## Error Handling

The server handles various error conditions:

1. **Authentication errors**: If the user is not authenticated.
2. **Access denied errors**: If the user is not a participant in the chat room.
3. **File size errors**: If the file exceeds the maximum allowed size.
4. **Content type errors**: If the file type is not allowed.
5. **Missing chunks**: If not all chunks are received.

## Security Considerations

1. **Authentication**: All WebSocket connections require authentication.
2. **Authorization**: Users can only upload files to chat rooms they are participants in.
3. **File validation**: Files are validated for size and content type.
4. **Path traversal prevention**: Files are saved with generated UUIDs, not original filenames.

## Performance Considerations

1. **Chunk size**: The default chunk size is 64KB, which balances network overhead with memory usage.
2. **Concurrent uploads**: The server can handle multiple concurrent uploads from different users.
3. **Memory usage**: Chunks are stored in memory until the file is complete, then cleaned up.
4. **Disk space**: Files are stored in the configured upload directory.

## Future Improvements

1. **Upload resumption**: Allow resuming interrupted uploads.
2. **Chunk acknowledgment**: Send acknowledgments for each chunk.
3. **Expiration policy**: Automatically clean up incomplete uploads after a timeout.
4. **Cloud storage**: Store files in cloud storage instead of local disk.
5. **Image processing**: Generate thumbnails for images.
6. **Virus scanning**: Scan files for viruses before saving.

## Example Usage

### JavaScript

```javascript
// Send a file chunk
const chunk = {
  chatRoomId: 1,
  chunkIndex: 1,
  totalChunks: 5,
  fileName: "example.jpg",
  contentType: "image/jpeg",
  fileSize: 320000,
  data: base64data
};

stompClient.send('/app/file.chunk', {}, JSON.stringify(chunk));
```

### Flutter

```dart
// Send a file chunk
final chunk = {
  'chatRoomId': 1,
  'chunkIndex': 1,
  'totalChunks': 5,
  'fileName': 'example.jpg',
  'contentType': 'image/jpeg',
  'fileSize': 320000,
  'data': base64data
};

stompClient.send(
  destination: '/app/file.chunk',
  body: jsonEncode(chunk),
);
```
