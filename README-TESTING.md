# WebSocket Testing Instructions

## Setup

1. Make sure you have a JDK installed (not just a JRE)
2. Run the application from your IDE

## Testing with Postman

1. Open Postman
2. Create a new WebSocket request
3. Connect to `ws://localhost:8080/ws`
4. Send the following frames in order:

### 1. Connect Frame
```
CONNECT
accept-version:1.2
heart-beat:10000,10000

```

### 2. Subscribe Frame
```
SUBSCRIBE
id:sub-0
destination:/topic/chatrooms/1

```

### 3. Send Message Frame
```
SEND
destination:/app/chat.sendMessage/1
content-type:application/json

{"content":"Hello via WebSocket!","contentType":"TEXT"}
```

## Verifying Database Storage

Run these SQL queries to check if messages are being stored:

```sql
-- Check if the test user exists
SELECT * FROM users WHERE username = 'websocket_test_user';

-- Check if any chat rooms exist
SELECT * FROM chat_rooms;

-- Check if the test user is a participant in any chat rooms
SELECT * FROM user_chatrooms WHERE user_id IN (SELECT id FROM users WHERE username = 'websocket_test_user');

-- Check if any messages exist
SELECT * FROM messages ORDER BY sent_at DESC LIMIT 10;

-- Check message statuses
SELECT * FROM message_statuses ORDER BY updated_at DESC LIMIT 10;
```

## Troubleshooting

If messages are still not being stored:

1. Check the application logs for any errors
2. Verify that the database connection is working
3. Try creating a chat room and user manually through the REST API first
4. Check if there are any transaction rollbacks happening

## Testing with Browser

You can also test using the included HTML file:

1. Open `test-websocket.html` in a browser
2. Click "Connect"
3. Type a message and click "Send"
4. Check the database to see if the message was stored
