# WebSocket Testing Guide

This guide provides comprehensive instructions for testing WebSocket connections in your chat application using various tools including Postman, browser-based clients, and mobile clients.

## Table of Contents

- [Testing with Postman](#testing-with-postman)
- [Testing with Browser Clients](#testing-with-browser-clients)
- [Testing with Mobile Clients](#testing-with-mobile-clients)
- [Troubleshooting](#troubleshooting)

## Testing with Postman

Postman provides excellent support for testing WebSocket connections, including STOMP over WebSocket.

### Setting Up a WebSocket Connection

1. **Open Postman** and create a new request
2. **Change the request type** from HTTP to WebSocket by clicking on the dropdown menu next to the request method
3. **Enter your WebSocket URL**: `ws://localhost:8080/ws`

### Testing STOMP WebSocket

STOMP (Simple Text Oriented Messaging Protocol) is a text-based protocol that works over WebSocket.

#### Step 1: Connect to the WebSocket

1. Enter the URL: `ws://localhost:8080/ws`
2. Click the **Connect** button

#### Step 2: Send a CONNECT Frame

After connecting to the WebSocket, you need to send a CONNECT frame to establish the STOMP session:

```
CONNECT
accept-version:1.1,1.0
heart-beat:10000,10000

```

> **Important**: Make sure to include an empty line at the end of the CONNECT frame (press Enter twice after the last header). This is required by the STOMP protocol.

#### Step 3: Subscribe to a Topic

To receive messages from a chat room, subscribe to its topic:

```
SUBSCRIBE
id:sub-0
destination:/topic/chatrooms/1

```

> **Note**: Press Enter twice after the last line to include the required empty line.

#### Step 4: Send a Message

To send a message to a chat room:

```
SEND
destination:/app/chat.sendMessage/1
content-type:application/json
content-length:44

{"content":"Test from Postman","contentType":"TEXT"}
```



### Testing with Authentication

If your WebSocket requires authentication:

Add your JWT token to the CONNECT frame:
```
CONNECT
accept-version:1.1,1.0
heart-beat:10000,10000
Authorization:Bearer your_jwt_token_here

```

### Common STOMP Commands

#### Join a Chat Room
```
SEND
destination:/app/chat.join/1
content-type:application/json
content-length:2

{}
```

#### Update Message Status
```
SEND
destination:/app/chat.updateStatus
content-type:application/json
content-length:58

{"messageId":1,"status":"READ"}
```

## Testing with Browser Clients

### Simple HTML/JavaScript Client

Create a file named `websocket-test.html` with the following content:

```html
<!DOCTYPE html>
<html>
<head>
    <title>WebSocket Test Client</title>
    <script src="https://cdn.jsdelivr.net/npm/stompjs@2.3.3/lib/stomp.min.js"></script>
</head>
<body>
    <h1>WebSocket Test Client</h1>

    <div>
        <button onclick="connect()">Connect</button>
        <button onclick="disconnect()">Disconnect</button>
    </div>

    <div style="margin-top: 20px;">
        <h3>Subscribe to Chat Room</h3>
        <input type="text" id="roomId" placeholder="Room ID" value="1">
        <button onclick="subscribe()">Subscribe</button>
    </div>

    <div style="margin-top: 20px;">
        <h3>Send Message</h3>
        <input type="text" id="message" placeholder="Message">
        <button onclick="sendMessage()">Send</button>
    </div>

    <div style="margin-top: 20px;">
        <h3>Messages</h3>
        <div id="output" style="border: 1px solid #ccc; padding: 10px; height: 300px; overflow-y: auto;"></div>
    </div>

    <script>
        let client;
        let subscription;

        function connect() {
            const socket = new WebSocket('ws://localhost:8080/ws');
            client = Stomp.over(socket);

            client.connect({}, frame => {
                document.getElementById('output').innerHTML += '<p>Connected: ' + frame + '</p>';
            }, error => {
                document.getElementById('output').innerHTML += '<p>Error: ' + error + '</p>';
            });
        }

        function disconnect() {
            if (client) {
                client.disconnect();
                document.getElementById('output').innerHTML += '<p>Disconnected</p>';
            }
        }

        function subscribe() {
            const roomId = document.getElementById('roomId').value;
            if (client && roomId) {
                subscription = client.subscribe('/topic/chatrooms/' + roomId, message => {
                    document.getElementById('output').innerHTML += '<p>Received: ' + message.body + '</p>';
                });
                document.getElementById('output').innerHTML += '<p>Subscribed to /topic/chatrooms/' + roomId + '</p>';
            }
        }

        function sendMessage() {
            const roomId = document.getElementById('roomId').value;
            const message = document.getElementById('message').value;

            if (client && roomId && message) {
                client.send("/app/chat.sendMessage/" + roomId, {},
                    JSON.stringify({content: message, contentType: "TEXT"}));
                document.getElementById('output').innerHTML += '<p>Message sent: ' + message + '</p>';
            }
        }
    </script>
</body>
</html>
```

## Testing with Mobile Clients

### Flutter WebSocket Testing

For Flutter applications, you need to use the STOMP over WebSockets library:

Use the `stomp_dart_client` package for STOMP over WebSockets

#### Important Notes for Mobile Testing:

1. **Use the correct IP address**: When testing from a mobile device, replace `localhost` with your computer's actual IP address (e.g., `192.168.1.100`)
2. **Handle reconnection**: Implement reconnection logic for mobile clients to handle network changes
3. **Implement proper error handling**: Mobile networks can be unstable, so implement robust error handling

## Troubleshooting

### Common Issues and Solutions

1. **Connection Refused**
   - Ensure the server is running
   - Check if the port is correct
   - Verify firewall settings

2. **Authentication Errors**
   - Ensure the JWT token is valid and not expired
   - Check that the token is correctly formatted in the headers

3. **STOMP Frame Format Errors**
   - Ensure each header line ends with a newline character
   - Include an empty line after headers (double newline)
   - Verify content-length matches the actual body length

4. **WebSocket Handshake Failed**
   - Check CORS configuration
   - Ensure the WebSocket endpoint is correctly configured

5. **Message Not Received**
   - Verify you're subscribed to the correct topic
   - Check if the message format is correct

### STOMP Frame Format

The STOMP protocol is very strict about frame format:

1. Each frame starts with a command (CONNECT, SUBSCRIBE, SEND)
2. Headers follow the command, one per line in the format `name:value`
3. An empty line (double newline) separates headers from the body
4. The body follows (if any)
5. A null byte (`\0`) terminates the frame (Postman handles this automatically)

### Debugging Tips

1. Enable debug logging in your application
2. Use browser developer tools to monitor WebSocket traffic
3. Use network monitoring tools like Wireshark for low-level debugging
