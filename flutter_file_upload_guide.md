# Flutter File Upload Guide for Chat Application

This guide explains how to implement file uploads in your Flutter application using WebSocket with STOMP protocol to communicate with the Spring Boot backend.

## 1. Required Dependencies

Add these dependencies to your `pubspec.yaml`:

```yaml
dependencies:
  flutter:
    sdk: flutter
  stomp_dart_client: ^0.4.4
  file_picker: ^5.2.5
  image_picker: ^0.8.7
  path: ^1.8.2
  http: ^0.13.5
  shared_preferences: ^2.0.18
```

## 2. WebSocket Service

Create a WebSocket service to handle the STOMP connection:

```dart
import 'dart:async';
import 'dart:convert';
import 'package:stomp_dart_client/stomp_dart_client.dart';
import 'package:shared_preferences/shared_preferences.dart';

class WebSocketService {
  static final WebSocketService _instance = WebSocketService._internal();
  factory WebSocketService() => _instance;
  WebSocketService._internal();

  StompClient? _stompClient;
  bool _isConnected = false;
  String? _token;
  String? _userId;

  // Getters
  bool get isConnected => _isConnected;

  // Initialize WebSocket connection
  Future<void> initializeWebSocket(String serverUrl, String token) async {
    _token = token;

    // Create STOMP client
    _stompClient = StompClient(
      config: StompConfig(
        url: 'ws://$serverUrl/ws',
        onConnect: _onConnect,
        onDisconnect: _onDisconnect,
        onWebSocketError: _onWebSocketError,
        stompConnectHeaders: {
          'Authorization': 'Bearer $token',
        },
        webSocketConnectHeaders: {
          'Authorization': 'Bearer $token',
        },
        heartbeatIncoming: Duration(seconds: 5),
        heartbeatOutgoing: Duration(seconds: 5),
      ),
    );

    // Connect to the server
    _stompClient?.activate();
  }

  // Handle successful connection
  void _onConnect(StompFrame frame) {
    _isConnected = true;
    
    // Subscribe to personal channels
    _subscribeToPersonalChannels();
    
    print('Connected to WebSocket server');
  }

  // Handle disconnection
  void _onDisconnect(StompFrame frame) {
    _isConnected = false;
    print('Disconnected from WebSocket server');
  }

  // Handle WebSocket errors
  void _onWebSocketError(dynamic error) {
    print('WebSocket error: $error');
  }

  // Subscribe to personal channels for file uploads
  void _subscribeToPersonalChannels() {
    // Subscribe to file progress updates
    _stompClient?.subscribe(
      destination: '/user/queue/files.progress',
      callback: (frame) {
        final data = json.decode(frame.body!);
        print('File progress update: $data');
        // Handle progress updates
      },
    );

    // Subscribe to completed file uploads
    _stompClient?.subscribe(
      destination: '/user/queue/files',
      callback: (frame) {
        final data = json.decode(frame.body!);
        print('File upload completed: $data');
        // Handle completed file
      },
    );

    // Subscribe to error messages
    _stompClient?.subscribe(
      destination: '/user/queue/errors',
      callback: (frame) {
        final data = json.decode(frame.body!);
        print('Error received: $data');
        // Handle error
      },
    );
  }

  // Disconnect from WebSocket server
  void disconnect() {
    _stompClient?.deactivate();
    _isConnected = false;
  }
}
```

## 3. File Upload Service

Create a service to handle file uploads:

```dart
import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';
import 'package:file_picker/file_picker.dart';
import 'package:image_picker/image_picker.dart';
import 'package:path/path.dart' as path;
import 'package:stomp_dart_client/stomp_dart_client.dart';

class FileUploadService {
  static final FileUploadService _instance = FileUploadService._internal();
  factory FileUploadService() => _instance;
  FileUploadService._internal();

  final WebSocketService _webSocketService = WebSocketService();
  final int _chunkSize = 64 * 1024; // 64KB chunks
  
  // Upload a file from the device
  Future<void> uploadFile(StompClient stompClient, long chatRoomId) async {
    try {
      // Pick a file
      FilePickerResult? result = await FilePicker.platform.pickFiles();
      
      if (result != null) {
        File file = File(result.files.single.path!);
        String fileName = path.basename(file.path);
        String contentType = _getContentType(fileName);
        
        await _uploadFileInChunks(stompClient, file, fileName, contentType, chatRoomId);
      }
    } catch (e) {
      print('Error picking file: $e');
    }
  }
  
  // Upload an image from the camera or gallery
  Future<void> uploadImage(StompClient stompClient, long chatRoomId, {ImageSource source = ImageSource.gallery}) async {
    try {
      // Pick an image
      final ImagePicker picker = ImagePicker();
      final XFile? image = await picker.pickImage(source: source);
      
      if (image != null) {
        File file = File(image.path);
        String fileName = path.basename(file.path);
        String contentType = 'image/jpeg'; // Default to JPEG
        
        if (fileName.toLowerCase().endsWith('.png')) {
          contentType = 'image/png';
        } else if (fileName.toLowerCase().endsWith('.gif')) {
          contentType = 'image/gif';
        }
        
        await _uploadFileInChunks(stompClient, file, fileName, contentType, chatRoomId);
      }
    } catch (e) {
      print('Error picking image: $e');
    }
  }
  
  // Upload a file in chunks
  Future<void> _uploadFileInChunks(StompClient stompClient, File file, String fileName, String contentType, long chatRoomId) async {
    try {
      // Get file size
      int fileSize = await file.length();
      
      // Read file as bytes
      Uint8List bytes = await file.readAsBytes();
      
      // Calculate number of chunks
      int totalChunks = (fileSize / _chunkSize).ceil();
      
      // Send chunks
      for (int i = 0; i < totalChunks; i++) {
        // Calculate chunk start and end
        int start = i * _chunkSize;
        int end = (i + 1) * _chunkSize;
        if (end > fileSize) {
          end = fileSize;
        }
        
        // Extract chunk
        Uint8List chunk = bytes.sublist(start, end);
        
        // Convert to base64
        String base64Chunk = base64Encode(chunk);
        
        // Create chunk message
        Map<String, dynamic> chunkMessage = {
          'messageId': null, // For new messages
          'chatRoomId': chatRoomId,
          'chunkIndex': i + 1, // 1-based index
          'totalChunks': totalChunks,
          'fileName': fileName,
          'contentType': contentType,
          'data': base64Chunk,
          'fileSize': fileSize,
        };
        
        // Send chunk
        stompClient.send(
          destination: '/app/file.chunk',
          body: json.encode(chunkMessage),
        );
        
        // Wait for acknowledgment (in a real app, you'd use a more sophisticated approach)
        await Future.delayed(Duration(milliseconds: 100));
      }
    } catch (e) {
      print('Error uploading file: $e');
    }
  }
  
  // Get content type based on file extension
  String _getContentType(String fileName) {
    String extension = path.extension(fileName).toLowerCase();
    
    switch (extension) {
      case '.jpg':
      case '.jpeg':
        return 'image/jpeg';
      case '.png':
        return 'image/png';
      case '.gif':
        return 'image/gif';
      case '.pdf':
        return 'application/pdf';
      case '.doc':
        return 'application/msword';
      case '.docx':
        return 'application/vnd.openxmlformats-officedocument.wordprocessingml.document';
      case '.xls':
        return 'application/vnd.ms-excel';
      case '.xlsx':
        return 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';
      case '.txt':
        return 'text/plain';
      case '.mp3':
        return 'audio/mpeg';
      case '.wav':
        return 'audio/wav';
      case '.mp4':
        return 'video/mp4';
      case '.mpeg':
        return 'video/mpeg';
      default:
        return 'application/octet-stream';
    }
  }
}
```

## 4. Using the File Upload Service in Your UI

```dart
import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';

class ChatScreen extends StatefulWidget {
  final long chatRoomId;
  
  ChatScreen({required this.chatRoomId});
  
  @override
  _ChatScreenState createState() => _ChatScreenState();
}

class _ChatScreenState extends State<ChatScreen> {
  final WebSocketService _webSocketService = WebSocketService();
  final FileUploadService _fileUploadService = FileUploadService();
  
  @override
  void initState() {
    super.initState();
    // Initialize WebSocket connection
    _initWebSocket();
  }
  
  Future<void> _initWebSocket() async {
    // Get token from shared preferences or other storage
    String token = await _getToken();
    
    // Initialize WebSocket connection
    await _webSocketService.initializeWebSocket('10.0.2.2:8080', token);
  }
  
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Chat'),
      ),
      body: Column(
        children: [
          // Chat messages list
          Expanded(
            child: ListView(
              // Chat messages
            ),
          ),
          
          // Input area
          Container(
            padding: EdgeInsets.all(8.0),
            child: Row(
              children: [
                // Attachment button
                IconButton(
                  icon: Icon(Icons.attach_file),
                  onPressed: _showAttachmentOptions,
                ),
                
                // Text input
                Expanded(
                  child: TextField(
                    // Text input properties
                  ),
                ),
                
                // Send button
                IconButton(
                  icon: Icon(Icons.send),
                  onPressed: _sendMessage,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
  
  void _showAttachmentOptions() {
    showModalBottomSheet(
      context: context,
      builder: (context) => Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          ListTile(
            leading: Icon(Icons.photo),
            title: Text('Photo from Gallery'),
            onTap: () {
              Navigator.pop(context);
              _uploadImage(ImageSource.gallery);
            },
          ),
          ListTile(
            leading: Icon(Icons.camera_alt),
            title: Text('Take Photo'),
            onTap: () {
              Navigator.pop(context);
              _uploadImage(ImageSource.camera);
            },
          ),
          ListTile(
            leading: Icon(Icons.attach_file),
            title: Text('File'),
            onTap: () {
              Navigator.pop(context);
              _uploadFile();
            },
          ),
        ],
      ),
    );
  }
  
  Future<void> _uploadImage(ImageSource source) async {
    await _fileUploadService.uploadImage(
      _webSocketService._stompClient!,
      widget.chatRoomId,
      source: source,
    );
  }
  
  Future<void> _uploadFile() async {
    await _fileUploadService.uploadFile(
      _webSocketService._stompClient!,
      widget.chatRoomId,
    );
  }
  
  void _sendMessage() {
    // Send text message
  }
}
```

## 5. Displaying Images and Files

Create widgets to display images and files:

```dart
import 'package:flutter/material.dart';

class ChatImageWidget extends StatelessWidget {
  final String uri;
  final String? token;
  
  ChatImageWidget({required this.uri, this.token});
  
  @override
  Widget build(BuildContext context) {
    if (uri.startsWith('http://') || uri.startsWith('https://')) {
      // Network image
      return Image.network(
        uri,
        headers: token != null ? {'Authorization': 'Bearer $token'} : null,
        loadingBuilder: (context, child, loadingProgress) {
          if (loadingProgress == null) return child;
          return Center(
            child: CircularProgressIndicator(
              value: loadingProgress.expectedTotalBytes != null
                  ? loadingProgress.cumulativeBytesLoaded / loadingProgress.expectedTotalBytes!
                  : null,
            ),
          );
        },
        errorBuilder: (context, error, stackTrace) {
          return Container(
            color: Colors.grey[300],
            child: Icon(Icons.error),
          );
        },
      );
    } else if (uri.startsWith('file://') || uri.startsWith('/')) {
      // Local file
      return Image.file(
        File(uri.startsWith('file://') ? uri.substring(7) : uri),
        errorBuilder: (context, error, stackTrace) {
          return Container(
            color: Colors.grey[300],
            child: Icon(Icons.error),
          );
        },
      );
    } else if (uri.startsWith('data:image')) {
      // Data URI
      final uriParts = uri.split(',');
      if (uriParts.length > 1) {
        final bytes = base64Decode(uriParts[1]);
        return Image.memory(
          bytes,
          errorBuilder: (context, error, stackTrace) {
            return Container(
              color: Colors.grey[300],
              child: Icon(Icons.error),
            );
          },
        );
      }
    }
    
    // Fallback
    return Container(
      color: Colors.grey[300],
      child: Icon(Icons.image_not_supported),
    );
  }
}

class ChatFileWidget extends StatelessWidget {
  final String fileName;
  final String? contentType;
  final String uri;
  final String? token;
  
  ChatFileWidget({
    required this.fileName,
    this.contentType,
    required this.uri,
    this.token,
  });
  
  @override
  Widget build(BuildContext context) {
    IconData icon = Icons.insert_drive_file;
    
    // Determine icon based on content type
    if (contentType != null) {
      if (contentType!.startsWith('image/')) {
        icon = Icons.image;
      } else if (contentType!.startsWith('audio/')) {
        icon = Icons.audio_file;
      } else if (contentType!.startsWith('video/')) {
        icon = Icons.video_file;
      } else if (contentType!.startsWith('application/pdf')) {
        icon = Icons.picture_as_pdf;
      } else if (contentType!.startsWith('application/msword') ||
                contentType!.startsWith('application/vnd.openxmlformats-officedocument.wordprocessingml.document')) {
        icon = Icons.description;
      } else if (contentType!.startsWith('application/vnd.ms-excel') ||
                contentType!.startsWith('application/vnd.openxmlformats-officedocument.spreadsheetml.sheet')) {
        icon = Icons.table_chart;
      } else if (contentType!.startsWith('text/')) {
        icon = Icons.text_snippet;
      }
    }
    
    return InkWell(
      onTap: () => _openFile(context),
      child: Container(
        padding: EdgeInsets.all(8.0),
        decoration: BoxDecoration(
          border: Border.all(color: Colors.grey),
          borderRadius: BorderRadius.circular(8.0),
        ),
        child: Row(
          children: [
            Icon(icon, size: 40),
            SizedBox(width: 8.0),
            Expanded(
              child: Text(
                fileName,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
              ),
            ),
          ],
        ),
      ),
    );
  }
  
  void _openFile(BuildContext context) {
    // Open file using url_launcher or other method
  }
}
```

## 6. Testing the Implementation

1. Make sure your Spring Boot backend is running
2. Connect to the WebSocket server
3. Try uploading an image or file
4. Check the server logs for any errors
5. Verify that the file is saved in the correct directory
6. Check that the file is displayed correctly in the chat

## 7. Troubleshooting

- If you're testing on a mobile device, make sure to use the correct IP address for the server (not localhost)
- Check the WebSocket connection status
- Look for errors in the Flutter console and server logs
- Verify that the file permissions are set correctly on the server
- Make sure the content type is correctly identified
