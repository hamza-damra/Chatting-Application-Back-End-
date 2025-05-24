# WebSocket Binary File Upload Documentation for Flutter

This document provides instructions for implementing binary file uploads via WebSocket in your Flutter application, compatible with the recently added backend functionality.

## Overview

The backend now supports chunked file uploads via WebSocket, allowing for efficient transfer of large files, real-time progress updates, and improved reliability. This approach is particularly useful for mobile applications where network connections may be unstable.

## Implementation Steps

### 1. Dependencies

Add the following dependencies to your `pubspec.yaml`:

```yaml
dependencies:
  stomp_dart_client: ^0.4.4
  file_picker: ^5.2.5
  path: ^1.8.2
  http: ^0.13.5
```

### 2. WebSocket Connection Setup

```dart
import 'package:stomp_dart_client/stomp_dart_client.dart';
import 'package:stomp_dart_client/stomp_config.dart';
import 'package:stomp_dart_client/stomp_frame.dart';

class WebSocketService {
  static const String SOCKET_URL = 'ws://YOUR_SERVER_IP:8080/ws';
  StompClient? stompClient;
  String? authToken;

  // Initialize with auth token
  void initialize(String token) {
    authToken = token;
    
    stompClient = StompClient(
      config: StompConfig(
        url: SOCKET_URL,
        onConnect: onConnect,
        onDisconnect: onDisconnect,
        onWebSocketError: (error) => print('Error: $error'),
        stompConnectHeaders: {
          'Authorization': 'Bearer $authToken',
        },
        webSocketConnectHeaders: {
          'Authorization': 'Bearer $authToken',
        },
      ),
    );
    
    stompClient!.activate();
  }
  
  void onConnect(StompFrame frame) {
    print('Connected to WebSocket');
    
    // Subscribe to user-specific destinations
    stompClient!.subscribe(
      destination: '/user/queue/files.progress',
      callback: (frame) => handleFileProgress(frame),
    );
    
    stompClient!.subscribe(
      destination: '/user/queue/files',
      callback: (frame) => handleFileComplete(frame),
    );
    
    stompClient!.subscribe(
      destination: '/user/queue/errors',
      callback: (frame) => handleError(frame),
    );
  }
  
  void onDisconnect(StompFrame frame) {
    print('Disconnected from WebSocket');
  }
  
  void disconnect() {
    if (stompClient != null && stompClient!.connected) {
      stompClient!.deactivate();
    }
  }
  
  // Handlers will be implemented in the file upload service
  void handleFileProgress(StompFrame frame) {}
  void handleFileComplete(StompFrame frame) {}
  void handleError(StompFrame frame) {}
}
```

### 3. File Upload Service

```dart
import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';
import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';

class FileUploadService {
  static const int CHUNK_SIZE = 64 * 1024; // 64KB chunks
  final WebSocketService webSocketService;
  
  // Upload state
  bool isUploading = false;
  double uploadProgress = 0.0;
  String statusMessage = '';
  Function(double)? onProgressUpdate;
  Function(String)? onComplete;
  Function(String)? onError;
  
  FileUploadService(this.webSocketService) {
    // Override handlers in WebSocketService
    webSocketService.handleFileProgress = _handleFileProgress;
    webSocketService.handleFileComplete = _handleFileComplete;
    webSocketService.handleError = _handleError;
  }
  
  Future<void> pickAndUploadFile(int chatRoomId) async {
    if (!webSocketService.stompClient!.connected) {
      onError?.call('Not connected to server');
      return;
    }
    
    // Pick a file
    FilePickerResult? result = await FilePicker.platform.pickFiles();
    
    if (result != null) {
      File file = File(result.files.single.path!);
      String fileName = result.files.single.name;
      String mimeType = result.files.single.extension ?? 'application/octet-stream';
      
      // Check file size
      int fileSize = await file.length();
      if (fileSize > 10 * 1024 * 1024) { // 10MB limit
        onError?.call('File size exceeds the maximum allowed (10MB)');
        return;
      }
      
      // Start upload
      isUploading = true;
      uploadProgress = 0.0;
      statusMessage = 'Preparing upload...';
      onProgressUpdate?.call(uploadProgress);
      
      // Calculate total chunks
      int totalChunks = (fileSize / CHUNK_SIZE).ceil();
      
      // Upload chunks
      for (int chunkIndex = 1; chunkIndex <= totalChunks; chunkIndex++) {
        // Calculate chunk boundaries
        int start = (chunkIndex - 1) * CHUNK_SIZE;
        int end = start + CHUNK_SIZE;
        if (end > fileSize) end = fileSize;
        
        // Read chunk
        RandomAccessFile reader = await file.open(mode: FileMode.read);
        await reader.setPosition(start);
        Uint8List buffer = await reader.read(end - start);
        await reader.close();
        
        // Convert to base64
        String base64data = base64Encode(buffer);
        
        // Create chunk payload
        final chunk = {
          'chatRoomId': chatRoomId,
          'chunkIndex': chunkIndex,
          'totalChunks': totalChunks,
          'fileName': fileName,
          'contentType': mimeType,
          'fileSize': fileSize,
          'data': base64data
        };
        
        // Send chunk
        webSocketService.stompClient!.send(
          destination: '/app/file.chunk',
          body: jsonEncode(chunk),
        );
        
        // Update progress (for UI feedback even before server responds)
        uploadProgress = chunkIndex / totalChunks;
        statusMessage = 'Uploading: ${(uploadProgress * 100).toInt()}% ($chunkIndex/$totalChunks)';
        onProgressUpdate?.call(uploadProgress);
        
        // Small delay to prevent overwhelming the server
        await Future.delayed(Duration(milliseconds: 100));
      }
    }
  }
  
  void _handleFileProgress(StompFrame frame) {
    final progress = jsonDecode(frame.body!);
    
    uploadProgress = progress['chunkIndex'] / progress['totalChunks'];
    statusMessage = 'Uploading: ${(uploadProgress * 100).toInt()}% (${progress['chunkIndex']}/${progress['totalChunks']})';
    onProgressUpdate?.call(uploadProgress);
  }
  
  void _handleFileComplete(StompFrame frame) {
    final response = jsonDecode(frame.body!);
    
    isUploading = false;
    uploadProgress = 1.0;
    statusMessage = 'Upload complete!';
    onProgressUpdate?.call(uploadProgress);
    onComplete?.call(response['attachmentUrl']);
  }
  
  void _handleError(StompFrame frame) {
    final errorData = jsonDecode(frame.body!);
    final errorMessage = errorData['message'] ?? 'Unknown error';
    
    isUploading = false;
    statusMessage = 'Error: $errorMessage';
    onError?.call(errorMessage);
  }
}
```

### 4. UI Implementation Example

```dart
class FileUploadWidget extends StatefulWidget {
  final int chatRoomId;
  
  FileUploadWidget({required this.chatRoomId});
  
  @override
  _FileUploadWidgetState createState() => _FileUploadWidgetState();
}

class _FileUploadWidgetState extends State<FileUploadWidget> {
  final FileUploadService _uploadService;
  bool isUploading = false;
  double uploadProgress = 0.0;
  String statusMessage = 'Ready to upload';
  String? uploadedFileUrl;
  
  @override
  void initState() {
    super.initState();
    
    // Initialize upload service with callbacks
    _uploadService.onProgressUpdate = (progress) {
      setState(() {
        uploadProgress = progress;
      });
    };
    
    _uploadService.onComplete = (fileUrl) {
      setState(() {
        isUploading = false;
        uploadedFileUrl = fileUrl;
        statusMessage = 'Upload complete!';
      });
    };
    
    _uploadService.onError = (error) {
      setState(() {
        isUploading = false;
        statusMessage = 'Error: $error';
      });
      
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(error)),
      );
    };
  }
  
  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        // Upload button
        ElevatedButton(
          onPressed: isUploading ? null : () {
            setState(() {
              isUploading = true;
              uploadProgress = 0.0;
              statusMessage = 'Selecting file...';
            });
            _uploadService.pickAndUploadFile(widget.chatRoomId);
          },
          child: Text(isUploading ? 'Uploading...' : 'Upload File'),
        ),
        
        // Progress indicator
        if (isUploading)
          Column(
            children: [
              SizedBox(height: 16),
              LinearProgressIndicator(value: uploadProgress),
              SizedBox(height: 8),
              Text(statusMessage),
            ],
          ),
          
        // Display uploaded file
        if (uploadedFileUrl != null && uploadedFileUrl!.isNotEmpty)
          Column(
            children: [
              SizedBox(height: 16),
              Text('Uploaded File:'),
              SizedBox(height: 8),
              _isImageFile(uploadedFileUrl!)
                ? Image.network(uploadedFileUrl!, height: 200)
                : Text(uploadedFileUrl!),
            ],
          ),
      ],
    );
  }
  
  bool _isImageFile(String url) {
    final lowerCaseUrl = url.toLowerCase();
    return lowerCaseUrl.endsWith('.jpg') || 
           lowerCaseUrl.endsWith('.jpeg') || 
           lowerCaseUrl.endsWith('.png') || 
           lowerCaseUrl.endsWith('.gif');
  }
}
```

## Important Implementation Notes

1. **Server URL**: Replace `YOUR_SERVER_IP` with your actual server IP address or hostname. Mobile devices cannot use `localhost` as it refers to the device itself.

2. **Authentication**: The JWT token must be included in the WebSocket connection headers.

3. **Chunk Size**: The default chunk size is 64KB, which balances network overhead with memory usage. Adjust if needed.

4. **Error Handling**: Implement proper error handling and retry logic for network failures.

5. **File Types**: The server supports various file types including images, documents, and plain text files.

6. **File Size Limit**: The server has a 10MB file size limit by default.

## Reconnection Strategy

For a robust implementation, add reconnection logic:

```dart
void connectWithRetry() {
  int retryCount = 0;
  const maxRetries = 5;
  const initialDelay = 1000; // 1 second
  
  void attemptConnection() {
    try {
      initialize(authToken!);
    } catch (e) {
      if (retryCount < maxRetries) {
        retryCount++;
        int delay = initialDelay * (1 << retryCount); // Exponential backoff
        print('Connection attempt $retryCount failed. Retrying in ${delay}ms');
        Future.delayed(Duration(milliseconds: delay), attemptConnection);
      } else {
        print('Failed to connect after $maxRetries attempts');
      }
    }
  }
  
  attemptConnection();
}
```

## Testing the Implementation

1. Ensure your Flutter app and server are running on the same network.
2. Log in to get a valid JWT token.
3. Initialize the WebSocket connection with the token.
4. Navigate to a chat room and try uploading a file.
5. Monitor the progress updates and verify the file appears in the chat after upload.

## Troubleshooting

1. **Connection Issues**: Verify the server IP/hostname and ensure the device can reach the server.
2. **Authentication Errors**: Check that the JWT token is valid and properly formatted.
3. **Upload Failures**: Check file size and type restrictions.
4. **Network Timeouts**: Implement retry logic for unstable connections.

For any issues, check the server logs and Flutter console for detailed error messages.
