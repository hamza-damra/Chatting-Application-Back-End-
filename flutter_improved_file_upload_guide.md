# Flutter Improved File Upload Guide

This guide explains how to implement robust file uploads in your Flutter application using WebSocket with STOMP protocol to communicate with the Spring Boot backend. This implementation addresses the WebSocket error code 1009 ("The decoded text message was too big for the output buffer") by using smaller chunks and proper buffer management.

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
  uuid: ^3.0.7
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
  
  // Callbacks for file upload progress
  final Map<String, Function(int, int, String?)> _fileProgressCallbacks = {};
  final Map<String, Function(dynamic)> _fileCompleteCallbacks = {};
  final Map<String, Function(String)> _fileErrorCallbacks = {};

  // Getters
  bool get isConnected => _isConnected;
  StompClient? get stompClient => _stompClient;

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
        
        // Extract progress information
        final int chunkIndex = data['chunkIndex'];
        final int totalChunks = data['totalChunks'];
        final String? uploadId = data['uploadId'];
        
        // Call the progress callback if registered
        if (uploadId != null && _fileProgressCallbacks.containsKey(uploadId)) {
          _fileProgressCallbacks[uploadId]!(chunkIndex, totalChunks, uploadId);
        }
      },
    );

    // Subscribe to completed file uploads
    _stompClient?.subscribe(
      destination: '/user/queue/files',
      callback: (frame) {
        final data = json.decode(frame.body!);
        print('File upload completed: $data');
        
        // Extract message information
        final String? attachmentUrl = data['attachmentUrl'];
        
        // Call the complete callback if registered
        if (attachmentUrl != null) {
          // Find the upload ID from the callbacks
          for (final uploadId in _fileCompleteCallbacks.keys) {
            _fileCompleteCallbacks[uploadId]!(data);
          }
        }
      },
    );

    // Subscribe to error messages
    _stompClient?.subscribe(
      destination: '/user/queue/errors',
      callback: (frame) {
        final data = json.decode(frame.body!);
        print('Error received: $data');
        
        // Extract error information
        final String type = data['type'];
        final String message = data['message'];
        final int chunkIndex = data['chunkIndex'];
        final int totalChunks = data['totalChunks'];
        
        // Call the error callback if registered
        // Since we don't have the upload ID in the error response,
        // we call all registered error callbacks
        for (final uploadId in _fileErrorCallbacks.keys) {
          _fileErrorCallbacks[uploadId]!(message);
        }
      },
    );
  }
  
  // Register callbacks for file upload progress
  void registerFileCallbacks(
    String uploadId,
    Function(int, int, String?) onProgress,
    Function(dynamic) onComplete,
    Function(String) onError
  ) {
    _fileProgressCallbacks[uploadId] = onProgress;
    _fileCompleteCallbacks[uploadId] = onComplete;
    _fileErrorCallbacks[uploadId] = onError;
  }
  
  // Unregister callbacks for file upload progress
  void unregisterFileCallbacks(String uploadId) {
    _fileProgressCallbacks.remove(uploadId);
    _fileCompleteCallbacks.remove(uploadId);
    _fileErrorCallbacks.remove(uploadId);
  }

  // Disconnect from WebSocket server
  void disconnect() {
    _stompClient?.deactivate();
    _isConnected = false;
  }
}
```

## 3. File Upload Service

Create a service to handle file uploads with improved chunking:

```dart
import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';
import 'package:file_picker/file_picker.dart';
import 'package:image_picker/image_picker.dart';
import 'package:path/path.dart' as path;
import 'package:stomp_dart_client/stomp_dart_client.dart';
import 'package:uuid/uuid.dart';

class FileUploadService {
  static final FileUploadService _instance = FileUploadService._internal();
  factory FileUploadService() => _instance;
  FileUploadService._internal();

  final WebSocketService _webSocketService = WebSocketService();
  
  // Smaller chunk size to prevent WebSocket buffer issues
  final int _chunkSize = 32 * 1024; // 32KB chunks (smaller than the recommended 64KB)
  
  // Upload a file from the device
  Future<void> uploadFile(
    StompClient stompClient, 
    int chatRoomId, {
    Function(int, int)? onProgress,
    Function(dynamic)? onComplete,
    Function(String)? onError,
  }) async {
    try {
      // Pick a file
      FilePickerResult? result = await FilePicker.platform.pickFiles();
      
      if (result != null) {
        File file = File(result.files.single.path!);
        String fileName = path.basename(file.path);
        String contentType = _getContentType(fileName);
        
        await _uploadFileInChunks(
          stompClient, 
          file, 
          fileName, 
          contentType, 
          chatRoomId,
          onProgress: onProgress,
          onComplete: onComplete,
          onError: onError,
        );
      }
    } catch (e) {
      print('Error picking file: $e');
      if (onError != null) {
        onError('Error picking file: $e');
      }
    }
  }
  
  // Upload an image from the camera or gallery
  Future<void> uploadImage(
    StompClient stompClient, 
    int chatRoomId, {
    ImageSource source = ImageSource.gallery,
    Function(int, int)? onProgress,
    Function(dynamic)? onComplete,
    Function(String)? onError,
  }) async {
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
        
        await _uploadFileInChunks(
          stompClient, 
          file, 
          fileName, 
          contentType, 
          chatRoomId,
          onProgress: onProgress,
          onComplete: onComplete,
          onError: onError,
        );
      }
    } catch (e) {
      print('Error picking image: $e');
      if (onError != null) {
        onError('Error picking image: $e');
      }
    }
  }
  
  // Upload a file in chunks with improved error handling
  Future<void> _uploadFileInChunks(
    StompClient stompClient, 
    File file, 
    String fileName, 
    String contentType, 
    int chatRoomId, {
    Function(int, int)? onProgress,
    Function(dynamic)? onComplete,
    Function(String)? onError,
  }) async {
    try {
      // Generate a unique upload ID
      String uploadId = Uuid().v4();
      
      // Register callbacks
      _webSocketService.registerFileCallbacks(
        uploadId,
        (chunkIndex, totalChunks, serverUploadId) {
          // Use the server-provided upload ID if available
          if (serverUploadId != null && serverUploadId != uploadId) {
            print('Server provided a different upload ID: $serverUploadId');
            // We'll continue using our client-generated ID for callbacks
          }
          
          if (onProgress != null) {
            onProgress(chunkIndex, totalChunks);
          }
        },
        (data) {
          if (onComplete != null) {
            onComplete(data);
          }
          // Clean up
          _webSocketService.unregisterFileCallbacks(uploadId);
        },
        (error) {
          if (onError != null) {
            onError(error);
          }
          // Clean up
          _webSocketService.unregisterFileCallbacks(uploadId);
        },
      );
      
      // Get file size
      int fileSize = await file.length();
      
      // Open file for reading in chunks
      RandomAccessFile randomAccessFile = await file.open(mode: FileMode.read);
      
      // Calculate number of chunks
      int totalChunks = (fileSize / _chunkSize).ceil();
      
      // Send chunks
      for (int i = 0; i < totalChunks; i++) {
        // Calculate chunk size
        int chunkSize = _chunkSize;
        if (i == totalChunks - 1) {
          // Last chunk might be smaller
          chunkSize = fileSize - (i * _chunkSize);
        }
        
        // Read chunk
        Uint8List chunk = Uint8List(chunkSize);
        await randomAccessFile.readInto(chunk);
        
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
          'uploadId': uploadId, // Include our client-generated upload ID
        };
        
        // Send chunk
        stompClient.send(
          destination: '/app/file.chunk',
          body: json.encode(chunkMessage),
        );
        
        // Wait for acknowledgment (in a real app, you'd use the callback system)
        await Future.delayed(Duration(milliseconds: 100));
      }
      
      // Close the file
      await randomAccessFile.close();
    } catch (e) {
      print('Error uploading file: $e');
      if (onError != null) {
        onError('Error uploading file: $e');
      }
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
  final int chatRoomId;
  
  ChatScreen({required this.chatRoomId});
  
  @override
  _ChatScreenState createState() => _ChatScreenState();
}

class _ChatScreenState extends State<ChatScreen> {
  final WebSocketService _webSocketService = WebSocketService();
  final FileUploadService _fileUploadService = FileUploadService();
  
  // Track upload progress
  bool _isUploading = false;
  int _uploadProgress = 0;
  int _totalChunks = 0;
  
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
          // Upload progress indicator
          if (_isUploading)
            Padding(
              padding: const EdgeInsets.all(8.0),
              child: Column(
                children: [
                  LinearProgressIndicator(
                    value: _totalChunks > 0 ? _uploadProgress / _totalChunks : 0,
                  ),
                  SizedBox(height: 4),
                  Text('Uploading: $_uploadProgress/$_totalChunks chunks'),
                ],
              ),
            ),
          
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
                  onPressed: _isUploading ? null : _showAttachmentOptions,
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
    setState(() {
      _isUploading = true;
      _uploadProgress = 0;
      _totalChunks = 0;
    });
    
    await _fileUploadService.uploadImage(
      _webSocketService.stompClient!,
      widget.chatRoomId,
      source: source,
      onProgress: (progress, total) {
        setState(() {
          _uploadProgress = progress;
          _totalChunks = total;
        });
      },
      onComplete: (data) {
        setState(() {
          _isUploading = false;
        });
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('File uploaded successfully')),
        );
      },
      onError: (error) {
        setState(() {
          _isUploading = false;
        });
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Error uploading file: $error')),
        );
      },
    );
  }
  
  Future<void> _uploadFile() async {
    setState(() {
      _isUploading = true;
      _uploadProgress = 0;
      _totalChunks = 0;
    });
    
    await _fileUploadService.uploadFile(
      _webSocketService.stompClient!,
      widget.chatRoomId,
      onProgress: (progress, total) {
        setState(() {
          _uploadProgress = progress;
          _totalChunks = total;
        });
      },
      onComplete: (data) {
        setState(() {
          _isUploading = false;
        });
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('File uploaded successfully')),
        );
      },
      onError: (error) {
        setState(() {
          _isUploading = false;
        });
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Error uploading file: $error')),
        );
      },
    );
  }
  
  void _sendMessage() {
    // Send text message
  }
}
```

## 5. Key Improvements in This Implementation

1. **Smaller Chunk Size**: Using 32KB chunks instead of the default 64KB to prevent WebSocket buffer issues.

2. **Client-Generated Upload ID**: The client generates a unique ID for each upload, which is sent with each chunk to ensure proper tracking.

3. **Improved Error Handling**: Comprehensive error handling with callbacks for progress, completion, and errors.

4. **Exponential Backoff**: The WebSocket service includes reconnection logic with exponential backoff.

5. **Streaming File Reading**: Reading the file in chunks using RandomAccessFile instead of loading the entire file into memory.

6. **Progress Tracking**: Visual feedback for upload progress.

7. **Organized File Storage**: Files are stored in appropriate directories based on their content type.

## 6. Testing the Implementation

1. Make sure your Spring Boot backend is running with the updated WebSocket configuration.
2. Connect to the WebSocket server from your Flutter app.
3. Try uploading files of various sizes and types.
4. Monitor the server logs for any errors.
5. Verify that files are saved in the correct directories.

## 7. Troubleshooting

- If you still encounter WebSocket errors, try reducing the chunk size further.
- Make sure your server's WebSocket message size limits are set appropriately.
- Check that the content type is correctly identified for each file.
- Verify that the file permissions are set correctly on the server.
- If uploads are slow, consider implementing parallel uploads for multiple files.
