# Full Documentation: Uploading Files from Client to Server

## 1. Overview of File Upload Process

This document describes the complete process of uploading files from the client to the server in the ChatApp project. The upload is performed using WebSocket communication, where files are split into chunks on the client side and sent sequentially to the server. The server processes each chunk, reconstructs the file, and stores it in an organized directory structure. Uploaded files can then be accessed via HTTP endpoints.

---

## 2. Client-Side WebSocket File Upload Steps

The client uploads files using WebSocket with the following steps:

1. **Connect to WebSocket endpoint:**
   ```javascript
   const socket = new WebSocket('ws://localhost:8080/ws');
   ```

2. **Subscribe to progress and error topics:**
   ```javascript
   stompClient.subscribe('/user/queue/files.progress', onProgress);
   stompClient.subscribe('/user/queue/errors', onError);
   stompClient.subscribe('/user/queue/files', onComplete);
   ```

3. **Split file into chunks:**
   - Recommended chunk size: 64KB
   - Convert each chunk to Base64
   - Send chunks sequentially

4. **Send each chunk:**
   ```javascript
   stompClient.send("/app/file.chunk", {}, JSON.stringify({
     messageId: null,  // null for new messages
     chatRoomId: roomId,
     chunkIndex: currentChunk,
     totalChunks: totalChunks,
     fileName: file.name,
     contentType: file.type,
     data: base64Data,
     fileSize: file.size,
     uploadId: uploadId  // null for first chunk, use server response for subsequent chunks
   }));
   ```

5. **Handle responses:**
   - Progress updates on `/user/queue/files.progress`
   - Upload completion on `/user/queue/files`
   - Errors on `/user/queue/errors`

---

## 3. Server-Side WebSocket Chunk Handling

The server handles file chunk uploads in the `BinaryFileController` class:

- The endpoint `/app/file.chunk` receives file chunks via WebSocket.
- Authentication is required; unauthenticated users are denied.
- Each chunk is processed by the `FileChunkService`.
- When all chunks are received, the file is reconstructed and saved.
- A `Message` entity is created with the file attachment URL and saved to the database.
- The message is broadcast to the chat room topic `/topic/chatrooms/{chatRoomId}`.
- The uploader receives confirmation on `/user/queue/files`.
- Progress updates and errors are sent to the uploader on `/user/queue/files.progress` and `/user/queue/errors` respectively.

---

## 4. File Storage Directory Structure and Configuration

Files are stored on the server in an organized directory structure configured via `FileStorageProperties`:

- Base upload directory (configurable)
- Subdirectories for file categories:
  - `images`
  - `documents`
  - `audio`
  - `video`
  - `other`
  - `temp` (for temporary files and chunk assembly)

The server ensures these directories exist and are writable. Files are saved in their respective category folders.

---

## 5. File Serving Endpoints

The `FileController` class provides HTTP endpoints to serve uploaded files:

- **Check upload directory status:**
  - `GET /api/files/status`
  - Returns information about the upload directory, subdirectories, file counts, allowed content types, and max file size.

- **Test file creation:**
  - `GET /api/files/test-write`
  - Creates a test file in the `temp` directory to verify write permissions.

- **Serve file by message ID:**
  - `GET /api/files/{messageId}`
  - Retrieves the file attached to the message with the given ID.

- **Serve file by category and filename:**
  - `GET /api/files/category/{category}/{filename}`
  - Serves files directly from the organized directory structure.
  - Valid categories: `images`, `documents`, `audio`, `video`, `other`.

---

## 6. Supported File Types and Size Limits

- Supported file types include:
  - Images: jpeg, png, gif
  - Documents: pdf, doc, docx, xls, xlsx, txt
  - Audio: mp3, wav
  - Video: mp4, mpeg

- Size limits:
  - Maximum file size: 10MB
  - Recommended chunk size: 64KB
  - WebSocket message limits:
    - Text messages: 5MB
    - Binary messages: 50MB

---

## 7. Error Handling and Progress Updates

- The client should validate file type and size before upload.
- The server sends progress updates for each chunk received.
- Errors during upload (e.g., authentication failure, file write errors) are sent to the client on `/user/queue/errors`.
- The server logs errors and exceptions for troubleshooting.

---

## 8. Testing Recommendations

- Test uploads with different file types and sizes.
- Verify files are saved in the correct subdirectories.
- Confirm progress updates and completion messages are received.
- Test error handling by simulating failures (e.g., disconnects, invalid files).
- Test concurrent uploads from multiple clients.

---

## References

- Client upload guide: `docs/file_upload_guide.md`
- Server WebSocket upload handler: `src/main/java/com/chatapp/websocket/BinaryFileController.java`
- File serving controller: `src/main/java/com/chatapp/controller/FileController.java`
- File storage configuration: `src/main/java/com/chatapp/config/FileStorageProperties.java`
