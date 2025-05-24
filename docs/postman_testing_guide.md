# Chat Application API Testing Guide with Postman

This guide provides instructions on how to test the Chat Application backend API using Postman. It covers authentication, user management, chat rooms, and WebSocket messaging.

## Table of Contents

1. [Setup](#setup)
2. [Authentication](#authentication)
3. [User Management](#user-management)
4. [Chat Rooms](#chat-rooms)
5. [Messages](#messages)
6. [WebSocket Testing](#websocket-testing)

## Setup

1. Download and install [Postman](https://www.postman.com/downloads/)
2. Import the Postman collection (optional)
3. Set up environment variables:
   - `baseUrl`: `http://localhost:8080`
   - `token`: (will be populated after login)

## Authentication

### Register a New User

```
POST {{baseUrl}}/api/auth/register
```

**Headers:**
```
Content-Type: application/json
```

**Body:**
```json
{
  "username": "testuser",
  "email": "test@example.com",
  "password": "password123",
  "fullName": "Test User"
}
```

### Login

```
POST {{baseUrl}}/api/auth/login
```

**Headers:**
```
Content-Type: application/json
```

**Body:**
```json
{
  "usernameOrEmail": "testuser",
  "password": "password123"
}
```

> Note: You can use either a username or an email address in the `usernameOrEmail` field.

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "user": {
    "id": 2,
    "username": "testuser",
    "email": "test@example.com",
    "fullName": "Test User",
    "profilePicture": null,
    "lastSeen": "2025-05-18T12:30:00",
    "online": true
  }
}
```

**After login:**
1. Save the `accessToken` to your Postman environment variable `token`
2. All subsequent requests should include the Authorization header:
   ```
   Authorization: Bearer {{token}}
   ```

### Refresh Token

```
POST {{baseUrl}}/api/auth/refresh
```

**Headers:**
```
Content-Type: application/json
Authorization: Bearer {{token}}
```

**Body:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

### Logout

```
POST {{baseUrl}}/api/auth/logout
```

**Headers:**
```
Authorization: Bearer {{token}}
```

## User Management

### Get Current User

```
GET {{baseUrl}}/api/users/me
```

**Headers:**
```
Authorization: Bearer {{token}}
```

### Update User Profile

```
PUT {{baseUrl}}/api/users/me
```

**Headers:**
```
Content-Type: application/json
Authorization: Bearer {{token}}
```

**Body:**
```json
{
  "fullName": "Updated Name",
  "profilePicture": "https://example.com/avatar.jpg"
}
```

### Change Password

```
PUT {{baseUrl}}/api/users/me
```

**Headers:**
```
Content-Type: application/json
Authorization: Bearer {{token}}
```

**Body:**
```json
{
  "currentPassword": "password123",
  "password": "newPassword123"
}
```

### Get All Users

```
GET {{baseUrl}}/api/users
```

**Headers:**
```
Authorization: Bearer {{token}}
```

### Get User by ID

```
GET {{baseUrl}}/api/users/{userId}
```

**Headers:**
```
Authorization: Bearer {{token}}
```

## Chat Rooms

### Create Chat Room

```
POST {{baseUrl}}/api/chatrooms
```

**Headers:**
```
Content-Type: application/json
Authorization: Bearer {{token}}
```

**Body:**
```json
{
  "name": "Test Chat Room",
  "isPrivate": false,
  "participantIds": [2, 3]
}
```

### Get All Chat Rooms

```
GET {{baseUrl}}/api/chatrooms
```

**Headers:**
```
Authorization: Bearer {{token}}
```

### Get Chat Room by ID

```
GET {{baseUrl}}/api/chatrooms/{chatRoomId}
```

**Headers:**
```
Authorization: Bearer {{token}}
```

### Update Chat Room

```
PUT {{baseUrl}}/api/chatrooms/{chatRoomId}
```

**Headers:**
```
Content-Type: application/json
Authorization: Bearer {{token}}
```

**Body:**
```json
{
  "name": "Updated Chat Room Name",
  "isPrivate": true
}
```

### Add Participant to Chat Room

```
POST {{baseUrl}}/api/chatrooms/{chatRoomId}/participants/{userId}
```

**Headers:**
```
Authorization: Bearer {{token}}
```

### Remove Participant from Chat Room

```
DELETE {{baseUrl}}/api/chatrooms/{chatRoomId}/participants/{userId}
```

**Headers:**
```
Authorization: Bearer {{token}}
```

## Messages

### Send Message to Chat Room

```
POST {{baseUrl}}/api/messages
```

**Headers:**
```
Content-Type: application/json
Authorization: Bearer {{token}}
```

**Body:**
```json
{
  "chatRoomId": 1,
  "content": "Hello, this is a test message!",
  "contentType": "TEXT"
}
```

### Get Messages for Chat Room

```
GET {{baseUrl}}/api/messages/chatroom/{chatRoomId}
```

**Headers:**
```
Authorization: Bearer {{token}}
```

**Query Parameters:**
```
page=0
size=20
```

### Update Message Status

```
PUT {{baseUrl}}/api/messages/{messageId}/status
```

**Headers:**
```
Content-Type: application/json
Authorization: Bearer {{token}}
```

**Body:**
```json
{
  "status": "READ"
}
```
