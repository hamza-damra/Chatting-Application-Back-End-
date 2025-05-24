# Using Path Variables in API Requests

This guide explains how to properly use path variables when making requests to the Chat Application API.

## What Are Path Variables?

Path variables (also called URL parameters) are parts of a URL that can change dynamically. In our API, they are denoted with curly braces in the endpoint definitions, like:

```
/api/chatrooms/{chatRoomId}/participants/{userId}
```

In this example, `{chatRoomId}` and `{userId}` are path variables that should be replaced with actual values when making a request.

## Common Mistakes

### 1. Using the Literal Placeholder

❌ **INCORRECT:**
```
POST /api/chatrooms/2/participants/{userId}
```

This sends the literal string `{userId}` to the server, which causes a type conversion error because the server expects a number.

### 2. Incomplete Replacement

❌ **INCORRECT:**
```
POST /api/chatrooms/2/participants/3{userId}
```

This sends `3{userId}` to the server, which is not a valid number.

## Correct Usage

### In Postman

#### Method 1: Using Path Variables in Postman

✅ **CORRECT:**
1. Enter the URL with colon-prefixed variables:
   ```
   {{baseUrl}}/api/chatrooms/:chatRoomId/participants/:userId
   ```

2. In the "Path Variables" section below the URL field, enter:
   - chatRoomId: 2
   - userId: 3

#### Method 2: Using Environment Variables

✅ **CORRECT:**
1. Set up environment variables in Postman for `chatRoomId` and `userId`
2. Use the URL:
   ```
   {{baseUrl}}/api/chatrooms/{{chatRoomId}}/participants/{{userId}}
   ```

### In cURL

✅ **CORRECT:**
```bash
curl -X POST "http://localhost:8080/api/chatrooms/2/participants/3" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### In JavaScript Fetch

✅ **CORRECT:**
```javascript
const chatRoomId = 2;
const userId = 3;

fetch(`http://localhost:8080/api/chatrooms/${chatRoomId}/participants/${userId}`, {
  method: 'POST',
  headers: {
    'Authorization': 'Bearer YOUR_TOKEN'
  }
})
```

## Troubleshooting

If you receive a 400 Bad Request error with a message like:

```
Parameter 'userId' should be a valid Long value. Provided value '3{userId}3' could not be converted.
```

This indicates that you're not properly replacing the path variable placeholder with an actual value.

### Steps to Fix:

1. Check your URL to ensure all placeholders are replaced with actual values
2. In Postman, verify that you're using the correct syntax for path variables
3. Make sure the values you're providing are of the correct type (e.g., numbers for IDs)

## API Endpoints with Path Variables

Here are the main API endpoints that use path variables:

| Endpoint | Method | Path Variables | Description |
|----------|--------|----------------|-------------|
| `/api/chatrooms/{id}` | GET | `id`: Chat room ID | Get a chat room by ID |
| `/api/chatrooms/{id}` | PUT | `id`: Chat room ID | Update a chat room |
| `/api/chatrooms/{id}` | DELETE | `id`: Chat room ID | Delete a chat room |
| `/api/chatrooms/{id}/participants` | GET | `id`: Chat room ID | Get chat room participants |
| `/api/chatrooms/{id}/participants/{userId}` | POST | `id`: Chat room ID, `userId`: User ID | Add a participant to a chat room |
| `/api/chatrooms/{id}/participants/{userId}` | DELETE | `id`: Chat room ID, `userId`: User ID | Remove a participant from a chat room |
| `/api/chatrooms/private/{userId}` | POST | `userId`: User ID | Create or get a private chat with a user |
| `/api/messages/chatroom/{chatRoomId}` | GET | `chatRoomId`: Chat room ID | Get messages for a chat room |
| `/api/users/{id}` | GET | `id`: User ID | Get a user by ID |

Remember to always replace the path variables with actual values when making requests to these endpoints.
