# File Upload Guide

## WebSocket File Upload Process

1. Connect to WebSocket endpoint:
   ```javascript
   const socket = new WebSocket('ws://localhost:8080/ws');
   ```

2. Subscribe to progress and error topics:
   ```javascript
   stompClient.subscribe('/user/queue/files.progress', onProgress);
   stompClient.subscribe('/user/queue/errors', onError);
   stompClient.subscribe('/user/queue/files', onComplete);
   ```

3. Split file into chunks:
   - Recommended chunk size: 64KB
   - Convert each chunk to Base64
   - Send chunks sequentially

4. Send each chunk:
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

5. Handle responses:
   - Progress updates on `/user/queue/files.progress`
   - Upload completion on `/user/queue/files`
   - Errors on `/user/queue/errors`

## Supported File Types

- Images: jpeg, png, gif
- Documents: pdf, doc, docx, xls, xlsx, txt
- Audio: mp3, wav
- Video: mp4, mpeg

## Size Limits

- Maximum file size: 10MB
- Recommended chunk size: 64KB
- WebSocket message limits:
  - Text messages: 5MB
  - Binary messages: 50MB

## Error Handling

- Check file type before upload
- Verify file size within limits
- Handle connection errors
- Monitor progress updates
- Process error messages from server

## Testing

1. Test with different file types
2. Verify files appear in correct subdirectories
3. Check progress updates
4. Verify error handling
5. Test concurrent uploads
