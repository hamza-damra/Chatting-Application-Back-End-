# Updated WebSocket Testing Guide

This guide provides instructions for testing the WebSocket functionality and verifying that messages are being stored in the database.

## Testing via Postman

To test using Postman:

1. Start the Spring Boot application
2. Open Postman
3. Create a new WebSocket request
4. Connect to `ws://localhost:8080/ws`
5. Send the following frames in order:

### Connect Frame
```
CONNECT
accept-version:1.2
heart-beat:10000,10000

```

### Subscribe Frame
```
SUBSCRIBE
id:sub-0
destination:/topic/chatrooms/1

```

### Send Message Frame
```
SEND
destination:/app/chat.sendMessage/1
content-type:application/json

{"content":"Hello via WebSocket!","contentType":"TEXT"}
```

6. Check the database to verify the message was stored

## Verifying Database Storage

To check if messages are being stored in the database:

1. Open your MySQL client (e.g., MySQL Workbench, phpMyAdmin)
2. Connect to your database
3. Run the queries in `check-database-tables.sql`

Key tables to check:
- `users` - Should contain the test user
- `chat_rooms` - Should contain the test chat room
- `user_chatrooms` - Should show the test user is a participant in the chat room
- `messages` - Should contain the messages you sent
- `message_statuses` - Should contain status records for the messages

## Troubleshooting

If messages are not being stored:

1. Check the application logs for errors
2. Verify that the WebSocket connection is established
3. Verify that the STOMP protocol is working correctly
4. Check if there are any transaction rollbacks
5. Use the API endpoints directly to test database access

## Common Issues

1. **WebSocket Connection Issues**
   - Check CORS configuration
   - Verify the WebSocket endpoint is correct
   - Check browser console for errors

2. **STOMP Protocol Issues**
   - Verify the STOMP frame format is correct
   - Check destination prefixes (/app, /topic)
   - Verify content-type headers

3. **Database Issues**
   - Check database connection
   - Verify transaction management
   - Check for constraint violations

4. **Authentication Issues**
   - For anonymous connections, verify the test user is created
   - For authenticated connections, verify the JWT token is valid
