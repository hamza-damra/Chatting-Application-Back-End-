# 🚨 CRITICAL: File Upload Issue and Fix Guide for Flutter Client

## ⚠️ Current Problem

Your Flutter client is **NOT** using the proper WebSocket file upload system. Instead, it's sending file paths like `uploads/auto_generated/1748075304631/37.jpg` as regular chat messages, which causes:

- **Files don't exist on the server** (only placeholder files are created)
- **Poor user experience** (files appear as text messages instead of proper attachments)
- **Server errors** and missing file issues
- **Inconsistent file storage** (files not organized in proper directories)

## 🔍 How to Identify the Problem in Your Code

Look for these **INCORRECT** patterns in your Flutter code:

### ❌ WRONG: Sending File Paths as Messages
```dart
// DON'T DO THIS - This is what's causing the problem
void sendFileAsMessage(String filePath) {
  stompClient.send(
    destination: '/app/chat.sendMessage/$chatRoomId',
    body: jsonEncode({
      'content': 'uploads/auto_generated/1748075304631/37.jpg', // ❌ WRONG!
      'contentType': 'text/plain'
    }),
  );
}
```

### ❌ WRONG: Creating Auto-Generated Paths
```dart
// DON'T DO THIS - Server doesn't create these paths
String generateFilePath(String fileName) {
  return 'uploads/auto_generated/${DateTime.now().millisecondsSinceEpoch}/$fileName';
}
```

### ❌ WRONG: Using HTTP Upload Instead of WebSocket
```dart
// DON'T DO THIS for chat file uploads
Future<void> uploadFileViaHttp(File file) async {
  var request = http.MultipartRequest('POST', Uri.parse('$baseUrl/api/files/upload'));
  // This bypasses the proper WebSocket system
}
```

## ✅ CORRECT Solution: Use WebSocket File Upload

### 1. Proper File Upload Implementation

```dart
class ChatFileUploader {
  static const int CHUNK_SIZE = 32768; // 32KB chunks

  Future<void> uploadFile(
    StompClient stompClient,
    File file,
    int chatRoomId, {
    Function(int, int)? onProgress,
    Function(dynamic)? onComplete,
    Function(String)? onError,
  }) async {
    try {
      String fileName = path.basename(file.path);
      String contentType = _getContentType(fileName);
      List<int> fileBytes = await file.readAsBytes();
      int totalChunks = (fileBytes.length / CHUNK_SIZE).ceil();
      String uploadId = const Uuid().v4(); // Generate unique upload ID

      print('📤 Starting WebSocket file upload: $fileName');
      print('📊 File size: ${fileBytes.length} bytes, Chunks: $totalChunks');

      // Send chunks via WebSocket
      for (int i = 0; i < totalChunks; i++) {
        int start = i * CHUNK_SIZE;
        int end = math.min(start + CHUNK_SIZE, fileBytes.length);
        List<int> chunkBytes = fileBytes.sublist(start, end);
        String base64Chunk = base64Encode(chunkBytes);

        // ✅ CORRECT: Send to WebSocket file upload endpoint
        Map<String, dynamic> chunkData = {
          'fileName': fileName,
          'contentType': contentType,
          'fileSize': fileBytes.length,
          'chunkIndex': i + 1,
          'totalChunks': totalChunks,
          'data': base64Chunk,
          'chatRoomId': chatRoomId,
          'uploadId': uploadId,
        };

        stompClient.send(
          destination: '/app/file.chunk', // ✅ CORRECT endpoint
          body: jsonEncode(chunkData),
        );

        if (onProgress != null) {
          onProgress(i + 1, totalChunks);
        }

        // Small delay between chunks to prevent overwhelming
        await Future.delayed(const Duration(milliseconds: 50));
      }

      print('✅ File upload completed: $fileName');

    } catch (e) {
      print('❌ File upload error: $e');
      if (onError != null) {
        onError('Upload failed: $e');
      }
    }
  }

  String _getContentType(String fileName) {
    String extension = fileName.toLowerCase().split('.').last;
    switch (extension) {
      case 'jpg':
      case 'jpeg':
        return 'image/jpeg';
      case 'png':
        return 'image/png';
      case 'gif':
        return 'image/gif';
      case 'pdf':
        return 'application/pdf';
      case 'txt':
        return 'text/plain';
      case 'mp4':
        return 'video/mp4';
      case 'mp3':
        return 'audio/mpeg';
      default:
        return 'application/octet-stream';
    }
  }
}
```

### 2. Proper WebSocket Subscriptions

```dart
void setupFileUploadSubscriptions(StompClient stompClient) {
  // ✅ CORRECT: Subscribe to file upload responses
  stompClient.subscribe(
    destination: '/user/queue/files',
    callback: (frame) {
      print('✅ File upload completed: ${frame.body}');
      // Handle successful file upload
      var response = jsonDecode(frame.body!);
      _handleFileUploadComplete(response);
    },
  );

  // ✅ CORRECT: Subscribe to upload progress
  stompClient.subscribe(
    destination: '/user/queue/files.progress',
    callback: (frame) {
      print('📊 Upload progress: ${frame.body}');
      var progress = jsonDecode(frame.body!);
      _handleUploadProgress(progress);
    },
  );

  // ✅ CORRECT: Subscribe to upload errors
  stompClient.subscribe(
    destination: '/user/queue/errors',
    callback: (frame) {
      print('❌ Upload error: ${frame.body}');
      var error = jsonDecode(frame.body!);
      _handleUploadError(error);
    },
  );
}
```

### 3. Complete Working Example

```dart
import 'dart:convert';
import 'dart:io';
import 'dart:math' as math;
import 'package:file_picker/file_picker.dart';
import 'package:stomp_dart_client/stomp_dart_client.dart';
import 'package:uuid/uuid.dart';
import 'package:path/path.dart' as path;

class ChatScreen extends StatefulWidget {
  final int chatRoomId;
  final StompClient stompClient;

  const ChatScreen({Key? key, required this.chatRoomId, required this.stompClient}) : super(key: key);

  @override
  _ChatScreenState createState() => _ChatScreenState();
}

class _ChatScreenState extends State<ChatScreen> {
  final ChatFileUploader _fileUploader = ChatFileUploader();
  bool _isUploading = false;
  double _uploadProgress = 0.0;

  @override
  void initState() {
    super.initState();
    _setupWebSocketSubscriptions();
  }

  void _setupWebSocketSubscriptions() {
    // Subscribe to file upload responses
    widget.stompClient.subscribe(
      destination: '/user/queue/files',
      callback: (frame) {
        setState(() {
          _isUploading = false;
          _uploadProgress = 0.0;
        });
        _showSuccessMessage('File uploaded successfully!');
      },
    );

    // Subscribe to upload progress
    widget.stompClient.subscribe(
      destination: '/user/queue/files.progress',
      callback: (frame) {
        var progress = jsonDecode(frame.body!);
        setState(() {
          _uploadProgress = (progress['chunkIndex'] / progress['totalChunks']);
        });
      },
    );

    // Subscribe to upload errors
    widget.stompClient.subscribe(
      destination: '/user/queue/errors',
      callback: (frame) {
        setState(() {
          _isUploading = false;
          _uploadProgress = 0.0;
        });
        var error = jsonDecode(frame.body!);
        _showErrorMessage('Upload failed: ${error['message']}');
      },
    );
  }

  // ✅ CORRECT: Proper file upload method
  Future<void> _uploadFile() async {
    try {
      FilePickerResult? result = await FilePicker.platform.pickFiles();

      if (result != null) {
        File file = File(result.files.single.path!);

        setState(() {
          _isUploading = true;
          _uploadProgress = 0.0;
        });

        await _fileUploader.uploadFile(
          widget.stompClient,
          file,
          widget.chatRoomId,
          onProgress: (current, total) {
            setState(() {
              _uploadProgress = current / total;
            });
          },
          onComplete: (response) {
            setState(() {
              _isUploading = false;
              _uploadProgress = 0.0;
            });
            _showSuccessMessage('File uploaded successfully!');
          },
          onError: (error) {
            setState(() {
              _isUploading = false;
              _uploadProgress = 0.0;
            });
            _showErrorMessage('Upload failed: $error');
          },
        );
      }
    } catch (e) {
      setState(() {
        _isUploading = false;
        _uploadProgress = 0.0;
      });
      _showErrorMessage('Error picking file: $e');
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('Chat Room ${widget.chatRoomId}')),
      body: Column(
        children: [
          // Chat messages list here...

          // Upload progress indicator
          if (_isUploading)
            Padding(
              padding: const EdgeInsets.all(16.0),
              child: Column(
                children: [
                  Text('Uploading file... ${(_uploadProgress * 100).toInt()}%'),
                  LinearProgressIndicator(value: _uploadProgress),
                ],
              ),
            ),

          // Bottom input area
          Container(
            padding: const EdgeInsets.all(8.0),
            child: Row(
              children: [
                IconButton(
                  icon: const Icon(Icons.attach_file),
                  onPressed: _isUploading ? null : _uploadFile, // ✅ CORRECT
                ),
                // Text input field here...
              ],
            ),
          ),
        ],
      ),
    );
  }

  void _showSuccessMessage(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message), backgroundColor: Colors.green),
    );
  }

  void _showErrorMessage(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message), backgroundColor: Colors.red),
    );
  }
}
```

## 🔧 How to Fix Your Existing Code

### Step 1: Find and Remove Incorrect Code
Search your Flutter project for:
- `uploads/auto_generated`
- `sendMessage` with file paths
- HTTP file upload endpoints
- Any code that generates file paths client-side

### Step 2: Replace with WebSocket Upload
Replace any file upload code with the `ChatFileUploader` class above.

### Step 3: Update UI Components
Make sure your file picker buttons call the WebSocket upload method:

```dart
// ❌ WRONG
onPressed: () => _sendFilePathAsMessage(filePath),

// ✅ CORRECT
onPressed: () => _uploadFile(),
```

### Step 4: Test the Fix
1. Pick a file using the file picker
2. Verify chunks are sent to `/app/file.chunk`
3. Check server logs for proper file processing
4. Confirm files appear in correct directories (`uploads/images/`, etc.)

## 🎯 Expected Results After Fix

- ✅ Files properly uploaded via WebSocket chunks
- ✅ Files saved in organized server directories
- ✅ Real file content (not placeholder text)
- ✅ Proper progress indicators
- ✅ Error handling for failed uploads
- ✅ Files appear as attachments, not text messages

## 📞 Need Help?

If you're still having issues:
1. Check server logs for WebSocket file upload messages
2. Verify your StompClient is properly connected
3. Ensure you're subscribed to the correct WebSocket destinations
4. Test with small files first (< 1MB)

**Remember**: Never send file paths as chat messages. Always use the WebSocket file upload system!