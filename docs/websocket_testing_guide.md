# WebSocket Testing Guide for Chat Application

This guide provides instructions on how to test the WebSocket functionality of the Chat Application using Postman and other WebSocket clients.

## Table of Contents

1. [WebSocket Overview](#websocket-overview)
2. [Testing with Postman](#testing-with-postman)
3. [Testing with WebSocket Client Tools](#testing-with-websocket-client-tools)
4. [Message Formats](#message-formats)
5. [Common Issues](#common-issues)
6. [Mobile Client Troubleshooting](#mobile-client-troubleshooting)

## WebSocket Overview

The Chat Application uses STOMP over WebSocket for real-time messaging. The main WebSocket endpoint is:

```
ws://localhost:8080/ws
```

STOMP destinations used in the application:

- `/app/chat.sendMessage` - Send a message to a chat room
- `/app/chat.addUser` - Notify when a user joins a chat room
- `/app/chat.leaveRoom` - Notify when a user leaves a chat room
- `/topic/chatroom.{roomId}` - Subscribe to messages in a specific chat room
- `/user/queue/notifications` - User-specific notifications

## Testing with Postman

Postman supports WebSocket testing in recent versions:

1. Open Postman and create a new WebSocket request
2. Enter the WebSocket URL: `ws://localhost:8080/ws`
3. Click "Connect" to establish the WebSocket connection

### Authentication with STOMP

1. After connecting, send a STOMP CONNECT frame:
   ```
   CONNECT
   accept-version:1.2
   heart-beat:10000,10000
   Authorization:Bearer {{token}}


   ```
   (Note: The extra blank line at the end is required)

2. You should receive a CONNECTED frame in response

### Subscribe to a Chat Room

```
SUBSCRIBE
id:sub-0
destination:/topic/chatroom.1

```

### Send a Message

```
SEND
destination:/app/chat.sendMessage
content-type:application/json

{"chatRoomId":1,"content":"Hello via WebSocket!","contentType":"TEXT"}
```

### Receive Messages

After subscribing, you'll automatically receive messages sent to the chat room in the Postman WebSocket interface.

## Testing with WebSocket Client Tools

### Using wscat (Command Line)

1. Install wscat:
   ```
   npm install -g wscat
   ```

2. Connect to the WebSocket endpoint:
   ```
   wscat -c ws://localhost:8080/ws
   ```

3. Send a STOMP CONNECT frame:
   ```
   CONNECT
   accept-version:1.2
   heart-beat:10000,10000
   Authorization:Bearer your_token_here


   ```

### Using WebSocket King (GUI Client)

1. Download and install [WebSocket King](https://websocketking.com/)
2. Create a new connection to `ws://localhost:8080/ws`
3. Use the interface to send STOMP frames similar to the Postman examples

## Message Formats

### Chat Message

```json
{
  "type": "CHAT",
  "chatRoomId": 1,
  "senderId": 2,
  "content": "Hello everyone!",
  "contentType": "TEXT",
  "timestamp": "2025-05-18T12:45:30"
}
```

### User Join Notification

```json
{
  "type": "JOIN",
  "chatRoomId": 1,
  "senderId": 2,
  "content": "User has joined the chat",
  "timestamp": "2025-05-18T12:40:15"
}
```

### User Leave Notification

```json
{
  "type": "LEAVE",
  "chatRoomId": 1,
  "senderId": 2,
  "content": "User has left the chat",
  "timestamp": "2025-05-18T13:10:45"
}
```

### User Status Update

```json
{
  "type": "STATUS",
  "userId": 2,
  "online": true,
  "lastSeen": "2025-05-18T12:40:00"
}
```

## Common Issues

### Authentication Problems

If you receive a 401 Unauthorized error:
- Ensure your JWT token is valid and not expired
- Check that you're including the token in the STOMP CONNECT frame with the format `Authorization:Bearer your_token_here`
- Make sure there are no spaces before or after the colon in the header
- Try refreshing your token and reconnecting

### Connection Issues

If you can't establish a WebSocket connection:
- Verify the server is running
- Check that the WebSocket endpoint is correct
- Ensure no firewall or proxy is blocking WebSocket connections
- Make sure to send the STOMP CONNECT frame immediately after establishing the WebSocket connection
- Check the server logs for any authentication or authorization errors

### 400 Bad Request Errors

If you receive a 400 Bad Request error:
- Make sure you're not sending the Authorization header in the initial WebSocket handshake (HTTP headers)
- The JWT token should only be sent in the STOMP CONNECT frame after the WebSocket connection is established
- Check that your STOMP frame format is correct, including the required blank line at the end
- Verify that you're using the correct WebSocket URL (`ws://localhost:8080/ws`)

### Message Delivery Problems

If messages aren't being delivered:
- Verify you've subscribed to the correct destination
- Check that the message format is correct
- Ensure the chat room exists and you have permission to access it

### CORS Issues

If you're testing from a browser and encounter CORS errors:
- Ensure the server's CORS configuration allows your origin
- Check that the WebSocket endpoint is properly configured for CORS

## Mobile Client Troubleshooting

This section provides guidance for troubleshooting WebSocket issues in mobile clients, specifically for Flutter applications.

### Handling WebSocket Connection in Flutter

#### Proper Connection Management

When implementing WebSocket connections in Flutter, it's important to properly manage the connection lifecycle:

```dart
import 'dart:async';
import 'dart:convert';
import 'dart:math';
import 'package:stomp_dart_client/stomp_dart_client.dart';

// Simple AuthService class for token management
class AuthService {
  String? _token;

  Future<String> getValidToken() async {
    // In a real app, check if token is expired and refresh if needed
    if (_token == null) {
      // Fetch a new token from the server
      return await _fetchNewToken();
    }
    return _token!;
  }

  Future<String> _fetchNewToken() async {
    // Simulate API call to refresh token
    await Future.delayed(Duration(milliseconds: 500));
    _token = 'new_jwt_token_here';
    return _token!;
  }
}

// Simple ChatMessage class for the examples
class ChatMessage {
  final int chatRoomId;
  final String content;
  final String contentType;
  final int? senderId;
  final DateTime? timestamp;

  ChatMessage({
    required this.chatRoomId,
    required this.content,
    this.contentType = 'TEXT',
    this.senderId,
    this.timestamp,
  });

  Map<String, dynamic> toJson() {
    return {
      'chatRoomId': chatRoomId,
      'content': content,
      'contentType': contentType,
      'senderId': senderId,
      'timestamp': timestamp?.toIso8601String(),
    };
  }
}

class WebSocketService {
  StompClient? _stompClient;
  bool _isConnected = false;
  int _reconnectAttempts = 0;
  static const int _maxReconnectAttempts = 5;

  // Reference to the auth service for token refresh
  final AuthService _authService;

  // Message queue for storing messages when connection is lost
  final List<ChatMessage> _messageQueue = [];

  // Connection state stream
  final _connectionStateController = StreamController<bool>.broadcast();
  Stream<bool> get connectionState => _connectionStateController.stream;

  WebSocketService(this._authService);

  Future<void> connect(String token) async {
    if (_stompClient != null) {
      await disconnect();
    }

    _stompClient = StompClient(
      config: StompConfig(
        url: 'ws://your-server-url/ws',
        onConnect: _onConnect,
        onDisconnect: _onDisconnect,
        onWebSocketError: _onWebSocketError,
        stompConnectHeaders: {
          'Authorization': 'Bearer $token',
        },
        webSocketConnectHeaders: {
          'Authorization': 'Bearer $token',
        },
      ),
    );

    try {
      _stompClient!.activate();
    } catch (e) {
      print('Error connecting to WebSocket: $e');
      _connectionStateController.add(false);
    }
  }

  void _onConnect(StompFrame frame) {
    _isConnected = true;
    _reconnectAttempts = 0;
    _connectionStateController.add(true);
    print('Connected to WebSocket');

    // Subscribe to topics
    _subscribeToTopics();

    // Process any queued messages
    _processQueuedMessages();
  }

  void _subscribeToTopics() {
    // Subscribe to chat room topics
    _stompClient!.subscribe(
      destination: '/topic/chatroom.1',
      callback: (StompFrame frame) {
        if (frame.body != null) {
          final message = jsonDecode(frame.body!);
          print('Received message: $message');
          // Process the message (e.g., add to chat history, notify UI)
        }
      },
    );

    // Subscribe to user-specific notifications
    _stompClient!.subscribe(
      destination: '/user/queue/notifications',
      callback: (StompFrame frame) {
        if (frame.body != null) {
          final notification = jsonDecode(frame.body!);
          print('Received notification: $notification');
          // Process the notification
        }
      },
    );
  }

  void _onDisconnect(StompFrame frame) {
    _isConnected = false;
    _connectionStateController.add(false);
    print('Disconnected from WebSocket');

    // Attempt to reconnect with exponential backoff
    _attemptReconnect();
  }

  void _onWebSocketError(dynamic error) {
    print('WebSocket error: $error');
    _isConnected = false;
    _connectionStateController.add(false);

    // Attempt to reconnect with exponential backoff
    _attemptReconnect();
  }

  Future<void> disconnect() async {
    if (_stompClient != null && _isConnected) {
      await _stompClient!.deactivate();
    }
    _isConnected = false;
    _connectionStateController.add(false);
  }

  bool get isConnected => _isConnected;

  // Other methods...
}
```

### Reconnecting When Connection is Lost

Implement a robust reconnection mechanism with exponential backoff:

```dart
void _attemptReconnect() async {
  if (_reconnectAttempts >= _maxReconnectAttempts) {
    print('Maximum reconnection attempts reached');
    return;
  }

  _reconnectAttempts++;

  // Calculate delay with exponential backoff
  final delay = Duration(milliseconds: 1000 * pow(2, _reconnectAttempts - 1).toInt());
  print('Attempting to reconnect in ${delay.inSeconds} seconds (attempt $_reconnectAttempts)');

  await Future.delayed(delay);

  if (!_isConnected) {
    // Get a fresh token if needed
    final token = await _authService.getValidToken();
    connect(token);
  }
}
```

### Implementing a Fallback Mechanism for Failed Messages

When WebSocket messages fail to send, implement a fallback mechanism:

```dart
Future<bool> sendMessage(ChatMessage message) async {
  if (!_isConnected) {
    // Store message for later sending
    _messageQueue.add(message);
    print('WebSocket not connected. Message queued for later sending.');
    return false;
  }

  try {
    _stompClient!.send(
      destination: '/app/chat.sendMessage',
      body: jsonEncode(message.toJson()),
      headers: {'content-type': 'application/json'},
    );
    return true;
  } catch (e) {
    print('Error sending message: $e');
    // Store message for later sending
    _messageQueue.add(message);
    return false;
  }
}

// Process queued messages when connection is restored
void _processQueuedMessages() {
  if (_messageQueue.isEmpty || !_isConnected) return;

  print('Processing ${_messageQueue.length} queued messages');

  final messagesToSend = List<ChatMessage>.from(_messageQueue);
  _messageQueue.clear();

  for (final message in messagesToSend) {
    sendMessage(message);
  }
}
```

### Handling 500 Server Errors During Message Sending

When encountering server errors, implement a retry mechanism:

```dart
Future<bool> sendMessageWithRetry(ChatMessage message, {int maxRetries = 3}) async {
  int attempts = 0;
  bool success = false;

  while (attempts < maxRetries && !success) {
    attempts++;

    try {
      if (!_isConnected) {
        await _waitForConnection(timeout: Duration(seconds: 5));
      }

      _stompClient!.send(
        destination: '/app/chat.sendMessage',
        body: jsonEncode(message.toJson()),
        headers: {'content-type': 'application/json'},
      );

      success = true;
    } catch (e) {
      print('Error sending message (attempt $attempts): $e');

      if (attempts < maxRetries) {
        // Wait before retrying with exponential backoff
        final delay = Duration(milliseconds: 1000 * pow(2, attempts - 1).toInt());
        await Future.delayed(delay);
      }
    }
  }

  if (!success) {
    // Store message for later sending
    _messageQueue.add(message);
    print('Failed to send message after $maxRetries attempts. Message queued.');
  }

  return success;
}

Future<bool> _waitForConnection({required Duration timeout}) async {
  if (_isConnected) return true;

  final completer = Completer<bool>();

  final subscription = connectionState.listen((connected) {
    if (connected) {
      completer.complete(true);
    }
  });

  // Set timeout
  Future.delayed(timeout, () {
    if (!completer.isCompleted) {
      completer.complete(false);
    }
  });

  final result = await completer.future;
  subscription.cancel();
  return result;
}
```

### Debugging Common Flutter WebSocket Implementation Issues

#### "WebSocket not connected" Error

This error occurs when trying to send messages over a closed WebSocket connection:

1. **Always check connection state before sending:**

```dart
void sendMessage(String message) {
  if (!_isConnected) {
    print('Cannot send message: WebSocket not connected');
    // Handle the error (queue message, show error to user, etc.)
    return;
  }

  // Proceed with sending
  _stompClient!.send(...);
}
```

2. **Implement connection state monitoring:**

```dart
// In your UI code
@override
Widget build(BuildContext context) {
  return StreamBuilder<bool>(
    stream: webSocketService.connectionState,
    builder: (context, snapshot) {
      final isConnected = snapshot.data ?? false;

      return Column(
        children: [
          // Connection status indicator
          Container(
            height: 10,
            width: 10,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: isConnected ? Colors.green : Colors.red,
            ),
          ),

          // Message input with send button
          Row(
            children: [
              Expanded(
                child: TextField(
                  controller: _messageController,
                  decoration: InputDecoration(
                    hintText: 'Type a message',
                  ),
                ),
              ),
              IconButton(
                icon: Icon(Icons.send),
                onPressed: isConnected
                    ? () => _sendMessage(_messageController.text)
                    : null, // Disable when not connected
              ),
            ],
          ),

          // Connection status message
          if (!isConnected)
            Text(
              'Reconnecting...',
              style: TextStyle(color: Colors.red),
            ),
        ],
      );
    },
  );
}
```

#### Handling Network Changes

Monitor network connectivity changes to reconnect when the network becomes available:

```dart
import 'package:connectivity_plus/connectivity_plus.dart';

void _setupConnectivityMonitoring() {
  Connectivity().onConnectivityChanged.listen((ConnectivityResult result) {
    if (result != ConnectivityResult.none && !_isConnected) {
      print('Network connection restored. Attempting to reconnect WebSocket...');
      _reconnectAttempts = 0; // Reset reconnect attempts
      _attemptReconnect();
    }
  });
}
```

#### Implementing a Heartbeat Mechanism

To detect silent connection drops, implement a heartbeat mechanism:

```dart
void _setupHeartbeat() {
  // Send a heartbeat every 30 seconds
  Timer.periodic(Duration(seconds: 30), (timer) {
    if (_isConnected) {
      try {
        _stompClient!.send(
          destination: '/app/heartbeat',
          body: '',
        );
      } catch (e) {
        print('Heartbeat failed: $e');
        _isConnected = false;
        _connectionStateController.add(false);
        _attemptReconnect();
      }
    }
  });
}
```

### Testing WebSocket Connections in Flutter

To properly test WebSocket connections in your Flutter application:

#### 1. Use the Flutter DevTools Network Profiler

The Flutter DevTools Network Profiler can help you monitor WebSocket connections:

1. Run your app in debug mode
2. Open DevTools (via `flutter run -d chrome --web-browser-flag="--remote-debugging-port=9222"`)
3. Go to the Network tab
4. Filter for WebSocket connections
5. Monitor the frames being sent and received

#### 2. Implement Logging for WebSocket Events

Add comprehensive logging to track WebSocket events:

```dart
void _setupLogging() {
  _stompClient!.config.onDebugMessage = (String msg) {
    print('STOMP debug: $msg');
  };

  _stompClient!.config.onWebSocketError = (dynamic error) {
    print('WebSocket error: $error');
  };

  _stompClient!.config.onStompError = (StompFrame frame) {
    print('STOMP error: ${frame.body}');
  };
}
```

#### 3. Create a Test Mode for WebSocket Connections

Implement a test mode that simulates different connection scenarios:

```dart
enum ConnectionTestMode {
  normal,
  slowConnection,
  intermittentFailure,
  completeFailure
}

void setTestMode(ConnectionTestMode mode) {
  switch (mode) {
    case ConnectionTestMode.normal:
      // Normal operation
      break;
    case ConnectionTestMode.slowConnection:
      // Simulate slow connection
      _stompClient!.config.connectionTimeout = Duration(seconds: 10);
      break;
    case ConnectionTestMode.intermittentFailure:
      // Simulate intermittent failures
      Timer.periodic(Duration(minutes: 2), (_) {
        if (_isConnected) {
          print('Test mode: Simulating connection drop');
          _stompClient!.deactivate();
        }
      });
      break;
    case ConnectionTestMode.completeFailure:
      // Simulate complete failure
      disconnect();
      break;
  }
}
```

#### 4. Test Specific Scenarios

Make sure to test these specific scenarios:

- Connection during poor network conditions
- Reconnection after network loss
- Message delivery during connection interruptions
- Token expiration and renewal during active connection
- App going to background and returning to foreground
- Device sleep mode and wake-up

### Best Practices for Robust WebSocket Implementation

1. **Always monitor connection state** before sending messages
2. **Queue messages** when the connection is lost
3. **Implement exponential backoff** for reconnection attempts
4. **Use a heartbeat mechanism** to detect silent connection drops
5. **Monitor network connectivity** to reconnect when the network is restored
6. **Provide visual feedback** to users about the connection state
7. **Implement proper error handling** for all WebSocket operations
8. **Use a persistent storage** for critical messages to prevent data loss
9. **Implement message acknowledgment** for important messages
10. **Consider using SockJS** as a fallback for environments where WebSockets are blocked
