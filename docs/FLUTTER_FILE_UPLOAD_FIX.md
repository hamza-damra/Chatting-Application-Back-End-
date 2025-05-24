# 🔧 Flutter File Upload Fix Guide

## 🚨 **CRITICAL ISSUE IDENTIFIED**

Your Flutter client is sending file paths like `uploads/auto_generated/1748078722007/39.jpg` as WebSocket messages instead of properly uploading files. This causes:

- ❌ Invalid image files that can't be displayed
- ❌ Placeholder files being created on the server
- ❌ Poor user experience with broken images

## 🎯 **ROOT CAUSE**

The Flutter client is bypassing the proper file upload flow:

**❌ WRONG FLOW (Current):**
1. Client generates file path
2. Sends path as WebSocket message
3. Server creates placeholder file
4. Image is invalid/broken

**✅ CORRECT FLOW (Required):**
1. Client uploads file via REST API (`/api/files/upload`)
2. Server returns file metadata/URL
3. Client sends file URL/metadata via WebSocket
4. Image displays correctly

## 🛠️ **FLUTTER CLIENT FIX**

### **Step 1: Update File Upload Service**

Create or update your `ApiFileService`:

```dart
class ApiFileService {
  final String baseUrl;
  final String? token;

  ApiFileService({required this.baseUrl, this.token});

  Future<FileUploadResponse> uploadFile(File file, int chatRoomId) async {
    try {
      var request = http.MultipartRequest(
        'POST',
        Uri.parse('$baseUrl/api/files/upload'),
      );

      // Add headers
      if (token != null) {
        request.headers['Authorization'] = 'Bearer $token';
      }

      // Add file
      request.files.add(await http.MultipartFile.fromPath(
        'file',
        file.path,
        filename: path.basename(file.path),
      ));

      // Add chat room ID
      request.fields['chatRoomId'] = chatRoomId.toString();

      // Send request
      var response = await request.send();
      var responseBody = await response.stream.bytesToString();

      if (response.statusCode == 200) {
        var jsonResponse = json.decode(responseBody);
        return FileUploadResponse.fromJson(jsonResponse);
      } else {
        throw Exception('File upload failed: ${response.statusCode}');
      }
    } catch (e) {
      throw Exception('File upload error: $e');
    }
  }
}

class FileUploadResponse {
  final String fileName;
  final String fileUrl;
  final String contentType;
  final int fileSize;

  FileUploadResponse({
    required this.fileName,
    required this.fileUrl,
    required this.contentType,
    required this.fileSize,
  });

  factory FileUploadResponse.fromJson(Map<String, dynamic> json) {
    return FileUploadResponse(
      fileName: json['fileName'],
      fileUrl: json['fileUrl'],
      contentType: json['contentType'],
      fileSize: json['fileSize'],
    );
  }
}
```

### **Step 2: Update Message Sending Logic**

Update your chat service to upload files first:

```dart
class ChatService {
  final ApiFileService fileService;
  final WebSocketService webSocketService;

  ChatService({required this.fileService, required this.webSocketService});

  Future<void> sendImageMessage(File imageFile, int chatRoomId) async {
    try {
      // Step 1: Upload file via REST API
      FileUploadResponse uploadResponse = await fileService.uploadFile(
        imageFile, 
        chatRoomId
      );

      // Step 2: Send message with file URL via WebSocket
      var message = {
        'content': uploadResponse.fileUrl, // Use the actual file URL
        'contentType': uploadResponse.contentType,
        'attachmentUrl': uploadResponse.fileUrl,
        'fileName': uploadResponse.fileName,
      };

      webSocketService.sendMessage('/app/chat.sendMessage/$chatRoomId', message);
      
    } catch (e) {
      print('Error sending image message: $e');
      // Handle error appropriately
    }
  }

  Future<void> sendTextMessage(String text, int chatRoomId) async {
    var message = {
      'content': text,
      'contentType': 'text/plain',
    };

    webSocketService.sendMessage('/app/chat.sendMessage/$chatRoomId', message);
  }
}
```

### **Step 3: Update UI Components**

Update your image picker and message sending:

```dart
class ChatScreen extends StatefulWidget {
  // ... existing code

  Future<void> _pickAndSendImage() async {
    try {
      final ImagePicker picker = ImagePicker();
      final XFile? image = await picker.pickImage(source: ImageSource.gallery);
      
      if (image != null) {
        File imageFile = File(image.path);
        
        // Show loading indicator
        setState(() {
          _isUploading = true;
        });

        // Upload and send via proper flow
        await chatService.sendImageMessage(imageFile, widget.chatRoomId);
        
        setState(() {
          _isUploading = false;
        });
      }
    } catch (e) {
      setState(() {
        _isUploading = false;
      });
      
      // Show error to user
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Failed to send image: $e')),
      );
    }
  }
}
```

### **Step 4: Update Message Display**

Update how you display images in messages:

```dart
class MessageWidget extends StatelessWidget {
  final Message message;

  const MessageWidget({Key? key, required this.message}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    if (message.contentType?.startsWith('image/') == true) {
      return _buildImageMessage();
    } else {
      return _buildTextMessage();
    }
  }

  Widget _buildImageMessage() {
    // Use the proper file URL from the server
    String imageUrl = message.content; // This should now be a proper URL
    
    return Container(
      constraints: BoxConstraints(maxWidth: 200, maxHeight: 200),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(8),
        child: Image.network(
          imageUrl,
          fit: BoxFit.cover,
          loadingBuilder: (context, child, loadingProgress) {
            if (loadingProgress == null) return child;
            return Center(child: CircularProgressIndicator());
          },
          errorBuilder: (context, error, stackTrace) {
            return Container(
              height: 100,
              color: Colors.grey[300],
              child: Icon(Icons.error, color: Colors.red),
            );
          },
        ),
      ),
    );
  }

  Widget _buildTextMessage() {
    return Text(message.content);
  }
}
```

## 🎯 **EXPECTED RESULT**

After implementing these fixes:

- ✅ Files upload properly via REST API
- ✅ Images display correctly in chat
- ✅ No more placeholder files
- ✅ Proper file metadata tracking
- ✅ Better error handling

## 🔍 **TESTING THE FIX**

1. **Upload an image** using the updated flow
2. **Check server logs** - should see proper file upload, not placeholder creation
3. **Verify image display** - images should load correctly
4. **Check file system** - actual image files should exist, not placeholders

## 🚨 **IMMEDIATE ACTION REQUIRED**

The server now **rejects** file path messages and sends error responses. Update your Flutter client immediately to use the proper upload flow, or users will see error messages when trying to send images.

## 📞 **NEED HELP?**

If you need assistance implementing these changes in your Flutter code:

1. Share your current file upload/message sending code
2. I can provide specific fixes for your implementation
3. We can test the integration step by step

The key is to **upload files first via REST API**, then **send the file URL via WebSocket** - never send file paths directly!
