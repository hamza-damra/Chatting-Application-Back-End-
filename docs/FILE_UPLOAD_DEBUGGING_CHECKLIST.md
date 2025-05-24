# 🔍 File Upload Debugging Checklist

## Quick Diagnosis

### ❌ Signs You Have the Problem
- Files appear as text messages instead of attachments
- Server logs show paths like `uploads/auto_generated/...`
- Files don't exist in server directories
- Error messages about missing files
- Placeholder files with text content

### ✅ Signs the Fix is Working
- Server logs show `[processChunk]` messages
- Files appear in `uploads/images/`, `uploads/documents/`, etc.
- Real file content (not placeholder text)
- Progress indicators work
- Files display as proper attachments

## Server-Side Debugging

### Check Server Logs for These Messages:

**🚨 PROBLEM INDICATORS:**
```
WEBSOCKET: Message content appears to be a file path: uploads/auto_generated/...
WEBSOCKET: This suggests the client is not using the proper WebSocket file upload system
WEBSOCKET: Created placeholder file at: uploads/auto_generated/...
```

**✅ SUCCESS INDICATORS:**
```
[processChunk] START: userId=X, fileName=example.jpg, contentType=image/jpeg
[processChunk] All chunks received for uploadId: ...
[saveCompleteFile] File saved successfully: uploads/images/20250524-123456-example.jpg
WEBSOCKET: File upload complete, creating message for file: ...
```

### Check File System:
```bash
# Check if files exist in proper directories
ls -la uploads/images/
ls -la uploads/documents/
ls -la uploads/video/

# Check for auto_generated directories (should not exist after fix)
find uploads -name "*auto_generated*"

# Check file content (should not be placeholder text)
head uploads/images/latest_file.jpg
```

## Client-Side Debugging

### Flutter Debug Console Messages:

**🚨 PROBLEM INDICATORS:**
```
Sending message with file path: uploads/auto_generated/...
HTTP upload completed
File path generated: uploads/...
```

**✅ SUCCESS INDICATORS:**
```
📤 Starting WebSocket file upload: example.jpg
📊 File size: 12345 bytes, Chunks: 5
✅ File upload completed: example.jpg
📊 Upload progress: chunk 3/5
```

### WebSocket Network Traffic:
Check browser/app network tab for:
- **CORRECT**: Messages to `/app/file.chunk`
- **INCORRECT**: Messages to `/app/chat.sendMessage` with file paths

### Flutter Code Audit:
Search your code for these patterns:

**❌ REMOVE THESE:**
```dart
// File path generation
'uploads/auto_generated/${timestamp}/${fileName}'

// Sending file paths as messages
stompClient.send(destination: '/app/chat.sendMessage/...', body: filePath)

// HTTP uploads in chat context
http.MultipartRequest('POST', ...)
```

**✅ SHOULD HAVE THESE:**
```dart
// WebSocket file chunks
stompClient.send(destination: '/app/file.chunk', body: chunkData)

// Proper subscriptions
stompClient.subscribe(destination: '/user/queue/files', ...)
stompClient.subscribe(destination: '/user/queue/files.progress', ...)
```

## Testing Steps

### 1. Test Small File Upload
```dart
// Test with a small text file first
File testFile = File('test.txt');
await testFile.writeAsString('Hello World');
await _fileUploader.uploadFile(stompClient, testFile, chatRoomId);
```

### 2. Monitor Server Response
- Check server logs for chunk processing
- Verify file appears in correct directory
- Confirm file content is correct

### 3. Test Different File Types
- Image: `.jpg`, `.png`
- Document: `.pdf`, `.txt`
- Video: `.mp4` (if supported)

### 4. Test Error Scenarios
- Very large files (> max size)
- Unsupported file types
- Network interruption during upload

## Common Issues and Solutions

### Issue: "Upload ID not found"
**Cause**: Chunks sent too quickly or out of order
**Solution**: Add delay between chunks (50ms)

### Issue: "Content type not allowed"
**Cause**: File type not in server's allowed list
**Solution**: Check `FileStorageProperties.allowedContentTypes`

### Issue: "File size exceeds maximum"
**Cause**: File larger than server limit
**Solution**: Check `FileStorageProperties.maxFileSize`

### Issue: WebSocket disconnects during upload
**Cause**: Large files or slow connection
**Solution**: Implement reconnection logic and resume capability

## Quick Fix Verification

After implementing the fix, verify:

1. **No more auto_generated paths** in server logs
2. **Files exist** in proper server directories
3. **Real file content** (not placeholder text)
4. **WebSocket chunks** in network traffic
5. **Progress indicators** work in UI
6. **Error handling** works for failed uploads

## Emergency Workaround

If you need a quick temporary fix while implementing the proper solution:

```dart
// Temporary: Convert file to base64 and send as single message
// WARNING: Only for small files, not recommended for production
Future<void> _temporaryFileUpload(File file) async {
  if (file.lengthSync() > 100000) { // 100KB limit
    throw Exception('File too large for temporary upload method');
  }
  
  List<int> bytes = await file.readAsBytes();
  String base64File = base64Encode(bytes);
  String fileName = path.basename(file.path);
  
  // Send as special message type
  Map<String, dynamic> message = {
    'content': fileName,
    'contentType': 'application/octet-stream',
    'fileData': base64File, // Server needs to handle this field
    'isFileUpload': true,
  };
  
  stompClient.send(
    destination: '/app/chat.sendMessage/$chatRoomId',
    body: jsonEncode(message),
  );
}
```

**Note**: This workaround requires server-side changes and should only be used temporarily!
