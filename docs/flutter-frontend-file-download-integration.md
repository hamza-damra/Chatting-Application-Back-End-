# Flutter Frontend File Download Integration Guide

## 🚨 URGENT: Critical Error Fix Required

**Current Issue:** Flutter client is causing backend errors by trying to download files for text messages that don't have attachments.

**Error in Backend Logs:**
```
ERROR: Message 1181 does not have an attachment
com.chatapp.exception.ResourceNotFoundException: Message does not have an attachment
```

**Quick Fix:** Update Flutter code to check `message.downloadUrl != null` before attempting any file operations.

## Overview

This guide explains the required changes in the Flutter frontend to integrate with the new file download system that fixes the issue where old videos and files couldn't be fetched from chat rooms.

## Problem Background

**Current Error in Logs:**
```
2025-05-27 13:52:51.923 ERROR 20308 --- [nio-8080-exec-6] com.chatapp.controller.FileController    : FILE DOWNLOAD: Message 1181 does not have an attachment
com.chatapp.exception.ResourceNotFoundException: Message does not have an attachment
```

**Original Issue:**
```
I/flutter (15622): INFO: CustomChatWidgetNew - Using fallback video URL: http://abusaker.zapto.org:8080/api/files/download/1178.mp4
I/flutter (15622): INFO: CustomChatWidgetNew - Fetched video URL from backend: http://abusaker.zapto.org:8080/api/files/download/1178.mp4
```

**Root Cause:**
1. Flutter client was constructing file URLs using message IDs instead of actual filenames
2. **NEW ISSUE**: Flutter client is trying to download files for text messages that don't have attachments
3. Client is not properly checking if a message has a `downloadUrl` before attempting download

**Solution:** Backend now provides a `downloadUrl` field in `MessageResponse` that contains the correct download endpoint, but **only for messages that actually have attachments**.

## Required Frontend Changes

### 1. Update Message Model

Add the new `downloadUrl` field to your Flutter message model:

```dart
class Message {
  final int id;
  final String content;
  final String? contentType;
  final String? attachmentUrl;
  final String? downloadUrl; // NEW FIELD - Add this
  final User sender;
  final int chatRoomId;
  final DateTime sentAt;
  final MessageStatus? status;

  Message({
    required this.id,
    required this.content,
    this.contentType,
    this.attachmentUrl,
    this.downloadUrl, // NEW FIELD - Add this
    required this.sender,
    required this.chatRoomId,
    required this.sentAt,
    this.status,
  });

  factory Message.fromJson(Map<String, dynamic> json) {
    return Message(
      id: json['id'],
      content: json['content'],
      contentType: json['contentType'],
      attachmentUrl: json['attachmentUrl'],
      downloadUrl: json['downloadUrl'], // NEW FIELD - Add this
      sender: User.fromJson(json['sender']),
      chatRoomId: json['chatRoomId'],
      sentAt: DateTime.parse(json['sentAt']),
      status: json['status'] != null ? MessageStatus.fromString(json['status']) : null,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'content': content,
      'contentType': contentType,
      'attachmentUrl': attachmentUrl,
      'downloadUrl': downloadUrl, // NEW FIELD - Add this
      'sender': sender.toJson(),
      'chatRoomId': chatRoomId,
      'sentAt': sentAt.toIso8601String(),
      'status': status?.toString(),
    };
  }
}
```

### 2. Update File URL Generation

Replace the problematic URL construction with the new `downloadUrl` field:

#### **Before (Broken):**
```dart
class FileUrlHelper {
  static String getVideoUrl(Message message) {
    // This was causing 404 errors for old files
    return "http://abusaker.zapto.org:8080/api/files/download/${message.id}.mp4";
  }

  static String getImageUrl(Message message) {
    // This was also problematic
    return "http://abusaker.zapto.org:8080/api/files/download/${message.id}.jpg";
  }
}
```

#### **After (Fixed):**
```dart
class FileUrlHelper {
  static const String baseUrl = "http://abusaker.zapto.org:8080";

  static String? getFileUrl(Message message) {
    // CRITICAL: Only use downloadUrl provided by the backend
    // This field is null for text messages without attachments
    if (message.downloadUrl != null && message.downloadUrl!.isNotEmpty) {
      return "$baseUrl${message.downloadUrl}";
    }
    return null; // No file available for this message
  }

  static bool hasAttachment(Message message) {
    // CRITICAL: Check downloadUrl, not just contentType
    // Backend only sets downloadUrl for messages with actual attachments
    return message.downloadUrl != null && message.downloadUrl!.isNotEmpty;
  }

  static bool isVideo(Message message) {
    return hasAttachment(message) &&
           (message.contentType?.startsWith('video/') ?? false);
  }

  static bool isImage(Message message) {
    return hasAttachment(message) &&
           (message.contentType?.startsWith('image/') ?? false);
  }

  static bool isDocument(Message message) {
    return hasAttachment(message) &&
           !(isVideo(message) || isImage(message));
  }
}
```

### 3. Critical Fix: Stop Downloading Files for Text Messages

**⚠️ URGENT FIX NEEDED**: The current error shows Flutter is trying to download files for text messages:

```
ERROR: Message 1181 does not have an attachment
```

**Problem Code Pattern to Find and Fix:**
```dart
// BAD: This tries to download files for ALL messages
for (Message message in messages) {
  String downloadUrl = "/api/files/message/${message.id}";
  // This will fail for text messages!
}

// BAD: This assumes all messages have files
Widget buildMessage(Message message) {
  String fileUrl = "http://abusaker.zapto.org:8080/api/files/message/${message.id}";
  return Image.network(fileUrl); // Will fail for text messages
}
```

**Correct Implementation:**
```dart
// GOOD: Only process messages that actually have attachments
for (Message message in messages) {
  if (FileUrlHelper.hasAttachment(message)) {
    String? fileUrl = FileUrlHelper.getFileUrl(message);
    if (fileUrl != null) {
      // Process the file
    }
  }
}

// GOOD: Check before building file widgets
Widget buildMessage(Message message) {
  if (FileUrlHelper.hasAttachment(message)) {
    return buildFileWidget(message);
  } else {
    return buildTextWidget(message);
  }
}
```

### 4. Update CustomChatWidgetNew

Modify your chat widget to use the new URL system:

#### **Before:**
```dart
class CustomChatWidgetNew extends StatelessWidget {
  Widget buildMessageContent(Message message) {
    // Old problematic logic
    if (message.contentType?.startsWith('video/') ?? false) {
      String videoUrl = "http://abusaker.zapto.org:8080/api/files/download/${message.id}.mp4";
      return VideoPlayerWidget(url: videoUrl);
    }

    if (message.contentType?.startsWith('image/') ?? false) {
      String imageUrl = "http://abusaker.zapto.org:8080/api/files/download/${message.id}.jpg";
      return ImageWidget(url: imageUrl);
    }

    return Text(message.content);
  }
}
```

#### **After (CRITICAL FIX):**
```dart
class CustomChatWidgetNew extends StatelessWidget {
  Widget buildMessageContent(Message message) {
    // CRITICAL: Always check hasAttachment() first!
    // This prevents the "Message does not have an attachment" error
    if (FileUrlHelper.hasAttachment(message)) {
      String? fileUrl = FileUrlHelper.getFileUrl(message);

      if (fileUrl == null) {
        return Text("File not available");
      }

      if (FileUrlHelper.isVideo(message)) {
        return VideoPlayerWidget(url: fileUrl);
      } else if (FileUrlHelper.isImage(message)) {
        return ImageWidget(url: fileUrl);
      } else if (FileUrlHelper.isDocument(message)) {
        return DocumentWidget(url: fileUrl, filename: message.content);
      }
    }

    // Regular text message - this handles messages without attachments
    return Text(message.content ?? "");
  }

  // CRITICAL: Never try to download files without checking hasAttachment first
  void onMessageTap(Message message) {
    if (FileUrlHelper.hasAttachment(message)) {
      // Only then try to download or open the file
      String? fileUrl = FileUrlHelper.getFileUrl(message);
      if (fileUrl != null) {
        // Handle file download/opening
      }
    }
    // For text messages, do nothing or show text actions
  }
}
```

### 4. Update Video Player Widget

Enhance your video player to handle the new URL system and errors:

```dart
class VideoPlayerWidget extends StatefulWidget {
  final String url;
  final String? filename;

  const VideoPlayerWidget({
    Key? key,
    required this.url,
    this.filename,
  }) : super(key: key);

  @override
  _VideoPlayerWidgetState createState() => _VideoPlayerWidgetState();
}

class _VideoPlayerWidgetState extends State<VideoPlayerWidget> {
  VideoPlayerController? _controller;
  bool _isLoading = true;
  bool _hasError = false;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    _initializeVideo();
  }

  Future<void> _initializeVideo() async {
    try {
      // Add authentication headers if needed
      final headers = await _getAuthHeaders();

      _controller = VideoPlayerController.network(
        widget.url,
        httpHeaders: headers,
      );

      await _controller!.initialize();

      setState(() {
        _isLoading = false;
        _hasError = false;
      });
    } catch (e) {
      setState(() {
        _isLoading = false;
        _hasError = true;
        _errorMessage = "Failed to load video: $e";
      });
    }
  }

  Future<Map<String, String>> _getAuthHeaders() async {
    // Add your authentication token here
    final token = await AuthService.getToken();
    return {
      'Authorization': 'Bearer $token',
      'Content-Type': 'application/json',
    };
  }

  @override
  Widget build(BuildContext context) {
    if (_isLoading) {
      return Container(
        height: 200,
        child: Center(child: CircularProgressIndicator()),
      );
    }

    if (_hasError) {
      return Container(
        height: 200,
        child: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(Icons.error, color: Colors.red),
              SizedBox(height: 8),
              Text(
                _errorMessage ?? "Video not available",
                style: TextStyle(color: Colors.red),
                textAlign: TextAlign.center,
              ),
              SizedBox(height: 8),
              ElevatedButton(
                onPressed: _initializeVideo,
                child: Text("Retry"),
              ),
            ],
          ),
        ),
      );
    }

    return Container(
      height: 200,
      child: VideoPlayer(_controller!),
    );
  }

  @override
  void dispose() {
    _controller?.dispose();
    super.dispose();
  }
}
```

### 5. Update Image Widget

Enhance your image widget with proper error handling:

```dart
class ImageWidget extends StatelessWidget {
  final String url;
  final String? filename;

  const ImageWidget({
    Key? key,
    required this.url,
    this.filename,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Container(
      constraints: BoxConstraints(maxHeight: 300),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(8),
        child: Image.network(
          url,
          headers: _getAuthHeaders(),
          fit: BoxFit.cover,
          loadingBuilder: (context, child, loadingProgress) {
            if (loadingProgress == null) return child;
            return Container(
              height: 200,
              child: Center(
                child: CircularProgressIndicator(
                  value: loadingProgress.expectedTotalBytes != null
                      ? loadingProgress.cumulativeBytesLoaded /
                          loadingProgress.expectedTotalBytes!
                      : null,
                ),
              ),
            );
          },
          errorBuilder: (context, error, stackTrace) {
            return Container(
              height: 200,
              child: Center(
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Icon(Icons.broken_image, color: Colors.grey, size: 48),
                    SizedBox(height: 8),
                    Text(
                      "Image not available",
                      style: TextStyle(color: Colors.grey),
                    ),
                  ],
                ),
              ),
            );
          },
        ),
      ),
    );
  }

  Map<String, String> _getAuthHeaders() {
    // Add your authentication token here
    final token = AuthService.getCurrentToken();
    return {
      'Authorization': 'Bearer $token',
    };
  }
}

### 6. Update Document Widget

Create or update your document widget for file downloads:

```dart
class DocumentWidget extends StatelessWidget {
  final String url;
  final String filename;

  const DocumentWidget({
    Key? key,
    required this.url,
    required this.filename,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.all(12),
      decoration: BoxDecoration(
        border: Border.all(color: Colors.grey.shade300),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Row(
        children: [
          Icon(
            _getFileIcon(),
            color: Colors.blue,
            size: 32,
          ),
          SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  filename,
                  style: TextStyle(
                    fontWeight: FontWeight.w500,
                    fontSize: 14,
                  ),
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
                SizedBox(height: 4),
                Text(
                  "Tap to download",
                  style: TextStyle(
                    color: Colors.grey.shade600,
                    fontSize: 12,
                  ),
                ),
              ],
            ),
          ),
          IconButton(
            onPressed: () => _downloadFile(context),
            icon: Icon(Icons.download, color: Colors.blue),
          ),
        ],
      ),
    );
  }

  IconData _getFileIcon() {
    final extension = filename.split('.').last.toLowerCase();
    switch (extension) {
      case 'pdf':
        return Icons.picture_as_pdf;
      case 'doc':
      case 'docx':
        return Icons.description;
      case 'xls':
      case 'xlsx':
        return Icons.table_chart;
      case 'ppt':
      case 'pptx':
        return Icons.slideshow;
      case 'zip':
      case 'rar':
        return Icons.archive;
      default:
        return Icons.insert_drive_file;
    }
  }

  Future<void> _downloadFile(BuildContext context) async {
    try {
      // Show loading indicator
      showDialog(
        context: context,
        barrierDismissible: false,
        builder: (context) => AlertDialog(
          content: Row(
            children: [
              CircularProgressIndicator(),
              SizedBox(width: 16),
              Text("Downloading..."),
            ],
          ),
        ),
      );

      final headers = _getAuthHeaders();
      final response = await http.get(Uri.parse(url), headers: headers);

      Navigator.pop(context); // Close loading dialog

      if (response.statusCode == 200) {
        // Save file to device
        await _saveFile(response.bodyBytes);
        _showSuccessMessage(context);
      } else if (response.statusCode == 404) {
        _showErrorMessage(context, "File not found or no longer available");
      } else if (response.statusCode == 403) {
        _showErrorMessage(context, "You don't have permission to access this file");
      } else {
        _showErrorMessage(context, "Error downloading file: ${response.statusCode}");
      }
    } catch (e) {
      Navigator.pop(context); // Close loading dialog if still open
      _showErrorMessage(context, "Network error: $e");
    }
  }

  Map<String, String> _getAuthHeaders() {
    final token = AuthService.getCurrentToken();
    return {
      'Authorization': 'Bearer $token',
    };
  }

  Future<void> _saveFile(List<int> bytes) async {
    // Implement file saving logic based on your platform
    // For example, using path_provider and file system
    final directory = await getApplicationDocumentsDirectory();
    final file = File('${directory.path}/$filename');
    await file.writeAsBytes(bytes);
  }

  void _showSuccessMessage(BuildContext context) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text("File downloaded successfully"),
        backgroundColor: Colors.green,
      ),
    );
  }

  void _showErrorMessage(BuildContext context, String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: Colors.red,
      ),
    );
  }
}
```

### 7. Update Message List Loading

Ensure your message loading properly handles the new downloadUrl field:

```dart
class ChatService {
  static const String baseUrl = "http://abusaker.zapto.org:8080/api";

  Future<List<Message>> loadMessages(int chatRoomId) async {
    try {
      final headers = await _getAuthHeaders();
      final response = await http.get(
        Uri.parse("$baseUrl/chatrooms/$chatRoomId/messages"),
        headers: headers,
      );

      if (response.statusCode == 200) {
        final List<dynamic> jsonList = json.decode(response.body);
        return jsonList.map((json) => Message.fromJson(json)).toList();
      } else {
        throw Exception("Failed to load messages: ${response.statusCode}");
      }
    } catch (e) {
      throw Exception("Error loading messages: $e");
    }
  }

  Future<Map<String, String>> _getAuthHeaders() async {
    final token = await AuthService.getToken();
    return {
      'Authorization': 'Bearer $token',
      'Content-Type': 'application/json',
    };
  }
}
```

## Testing Checklist

After implementing these changes, test the following scenarios:

### ✅ **Basic Functionality**
- [ ] Load chat room with old videos/images
- [ ] Load chat room with new file uploads
- [ ] Display text messages without attachments
- [ ] Handle messages with null/empty downloadUrl

### ✅ **File Types**
- [ ] Video files (.mp4, .mov, .avi)
- [ ] Image files (.jpg, .png, .gif)
- [ ] Document files (.pdf, .doc, .xls)
- [ ] Other file types

### ✅ **Error Scenarios**
- [ ] Messages without attachments (should not show download options)
- [ ] Deleted files (should show error message)
- [ ] Network errors (should show retry option)
- [ ] Unauthorized access (should show permission error)

### ✅ **Performance**
- [ ] Fast loading of message lists
- [ ] Smooth scrolling with media content
- [ ] Proper memory management for videos/images

## Migration Steps

1. **Update Message Model** - Add downloadUrl field
2. **Update JSON Parsing** - Include downloadUrl in fromJson/toJson
3. **Replace URL Construction** - Use downloadUrl instead of building URLs
4. **Update Widgets** - Modify video, image, and document widgets
5. **Add Error Handling** - Handle different HTTP status codes
6. **Test Thoroughly** - Verify all file types and error scenarios
7. **Deploy Gradually** - Consider feature flags for rollback capability

## Troubleshooting Current Error

### Error: "Message does not have an attachment"

**Symptoms:**
```
ERROR 20308 --- [nio-8080-exec-6] com.chatapp.controller.FileController : FILE DOWNLOAD: Message 1181 does not have an attachment
com.chatapp.exception.ResourceNotFoundException: Message does not have an attachment
```

**Root Cause:**
Flutter client is making HTTP requests to `/api/files/message/{messageId}` for text messages that don't have attachments.

**How to Find the Problem Code:**

1. **Search for these patterns in your Flutter code:**
   ```dart
   // Pattern 1: Direct URL construction with message ID
   "/api/files/message/${message.id}"
   "http://abusaker.zapto.org:8080/api/files/message/${message.id}"

   // Pattern 2: Automatic file download for all messages
   for (var message in messages) {
     downloadFile(message.id); // BAD: No attachment check
   }

   // Pattern 3: Image/Video widgets without attachment check
   Image.network("http://abusaker.zapto.org:8080/api/files/message/${message.id}")
   VideoPlayer.network("http://abusaker.zapto.org:8080/api/files/message/${message.id}")
   ```

2. **Look for missing null checks:**
   ```dart
   // BAD: No null check
   String fileUrl = message.downloadUrl; // Will crash if null

   // GOOD: Proper null check
   String? fileUrl = message.downloadUrl;
   if (fileUrl != null && fileUrl.isNotEmpty) {
     // Use fileUrl
   }
   ```

**Quick Fix Steps:**

1. **Add the downloadUrl field to your Message model** (if not already done)
2. **Replace all direct URL construction** with `FileUrlHelper.getFileUrl(message)`
3. **Add attachment checks** before any file operations:
   ```dart
   if (FileUrlHelper.hasAttachment(message)) {
     // Only then handle file operations
   }
   ```

4. **Test with a mix of text and file messages** to ensure no more 404 errors

### Verification

After implementing the fix, you should see:
- ✅ No more "Message does not have an attachment" errors
- ✅ Text messages display as text only
- ✅ File messages display with proper download URLs
- ✅ Old files can be downloaded successfully

## Benefits After Implementation

1. **Fixes Original Issue** - Old videos and files will load properly
2. **Better Error Handling** - Clear error messages for different scenarios
3. **More Reliable** - Backend controls URL structure and validation
4. **Future-Proof** - Easy to add new file types or change storage
5. **Better UX** - Proper loading states and error recovery
6. **Security** - Proper authentication for file access

## Support

If you encounter issues during implementation:

1. **Check Backend Logs** - Look for file download request logs
2. **Verify JSON Response** - Ensure downloadUrl field is present
3. **Test API Endpoints** - Use Postman to verify file download works
4. **Check Authentication** - Ensure JWT tokens are properly included
5. **Monitor Network Requests** - Use Flutter inspector to debug HTTP calls
```
