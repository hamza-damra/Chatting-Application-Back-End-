# Chat Application API Endpoints Reference

This document provides a comprehensive reference of all API endpoints available in the Chat Application.

## Base URL

All API endpoints are relative to the base URL:

```
http://localhost:8080
```

## Authentication

All protected endpoints require a valid JWT token in the Authorization header:

```
Authorization: Bearer {token}
```

You can obtain a token by calling the login endpoint.

## Authentication Endpoints

### Register a New User

```
POST /api/auth/register
```

**Request Body:**
```json
{
  "username": "string",
  "email": "string",
  "password": "string",
  "fullName": "string"
}
```

**Response:**
```json
{
  "id": "number",
  "username": "string",
  "email": "string",
  "fullName": "string",
  "profilePicture": "string",
  "lastSeen": "string (ISO date)",
  "online": "boolean"
}
```

**Status Codes:**
- 201: Created
- 400: Bad Request (validation error)
- 409: Conflict (username or email already exists)

### Login

```
POST /api/auth/login
```

**Request Body:**
```json
{
  "usernameOrEmail": "string",
  "password": "string"
}
```

> Note: The `usernameOrEmail` field accepts either a username or an email address.

**Response:**
```json
{
  "accessToken": "string",
  "refreshToken": "string",
  "tokenType": "string",
  "expiresIn": "number",
  "user": {
    "id": "number",
    "username": "string",
    "email": "string",
    "fullName": "string",
    "profilePicture": "string",
    "lastSeen": "string (ISO date)",
    "online": "boolean"
  }
}
```

**Status Codes:**
- 200: OK
- 401: Unauthorized (invalid credentials)

### Refresh Token

```
POST /api/auth/refresh
```

**Request Body:**
```json
{
  "refreshToken": "string"
}
```

**Response:**
```json
{
  "accessToken": "string",
  "refreshToken": "string",
  "tokenType": "string",
  "expiresIn": "number"
}
```

**Status Codes:**
- 200: OK
- 401: Unauthorized (invalid refresh token)

### Logout

```
POST /api/auth/logout
```

**Headers:**
```
Authorization: Bearer {token}
```

**Status Codes:**
- 200: OK
- 401: Unauthorized

## User Endpoints

### Get Current User

```
GET /api/users/me
```

**Headers:**
```
Authorization: Bearer {token}
```

**Response:**
```json
{
  "id": "number",
  "username": "string",
  "email": "string",
  "fullName": "string",
  "profilePicture": "string",
  "lastSeen": "string (ISO date)",
  "online": "boolean"
}
```

**Status Codes:**
- 200: OK
- 401: Unauthorized

### Update Current User

```
PUT /api/users/me
```

**Headers:**
```
Authorization: Bearer {token}
```

**Request Body:**
```json
{
  "fullName": "string",
  "profilePicture": "string",
  "password": "string",
  "currentPassword": "string"
}
```

**Response:**
```json
{
  "id": "number",
  "username": "string",
  "email": "string",
  "fullName": "string",
  "profilePicture": "string",
  "lastSeen": "string (ISO date)",
  "online": "boolean"
}
```

**Status Codes:**
- 200: OK
- 400: Bad Request
- 401: Unauthorized

### Get All Users

```
GET /api/users
```

**Headers:**
```
Authorization: Bearer {token}
```

**Query Parameters:**
- `page`: Page number (default: 0)
- `size`: Page size (default: 20)

**Response:**
```json
{
  "content": [
    {
      "id": "number",
      "username": "string",
      "email": "string",
      "fullName": "string",
      "profilePicture": "string",
      "lastSeen": "string (ISO date)",
      "online": "boolean"
    }
  ],
  "pageable": {
    "pageNumber": "number",
    "pageSize": "number",
    "sort": {
      "empty": "boolean",
      "sorted": "boolean",
      "unsorted": "boolean"
    }
  },
  "totalElements": "number",
  "totalPages": "number",
  "last": "boolean",
  "first": "boolean",
  "empty": "boolean"
}
```

**Status Codes:**
- 200: OK
- 401: Unauthorized

### Get User by ID

```
GET /api/users/{userId}
```

**Headers:**
```
Authorization: Bearer {token}
```

**Response:**
```json
{
  "id": "number",
  "username": "string",
  "email": "string",
  "fullName": "string",
  "profilePicture": "string",
  "lastSeen": "string (ISO date)",
  "online": "boolean"
}
```

**Status Codes:**
- 200: OK
- 401: Unauthorized
- 404: Not Found

## Chat Room Endpoints

### Create Chat Room

```
POST /api/chatrooms
```

**Headers:**
```
Authorization: Bearer {token}
```

**Request Body:**
```json
{
  "name": "string",
  "isPrivate": "boolean",
  "participantIds": ["number"]
}
```

**Response:**
```json
{
  "id": "number",
  "name": "string",
  "isPrivate": "boolean",
  "createdAt": "string (ISO date)",
  "updatedAt": "string (ISO date)",
  "creator": {
    "id": "number",
    "username": "string",
    "email": "string",
    "fullName": "string",
    "profilePicture": "string",
    "lastSeen": "string (ISO date)",
    "online": "boolean"
  },
  "participants": [
    {
      "id": "number",
      "username": "string",
      "email": "string",
      "fullName": "string",
      "profilePicture": "string",
      "lastSeen": "string (ISO date)",
      "online": "boolean"
    }
  ],
  "lastMessage": null,
  "unreadCount": 0
}
```

**Status Codes:**
- 201: Created
- 400: Bad Request
- 401: Unauthorized

### Get All Chat Rooms

```
GET /api/chatrooms
```

**Headers:**
```
Authorization: Bearer {token}
```

**Response:**
```json
[
  {
    "id": "number",
    "name": "string",
    "isPrivate": "boolean",
    "createdAt": "string (ISO date)",
    "updatedAt": "string (ISO date)",
    "creator": {
      "id": "number",
      "username": "string",
      "fullName": "string",
      "profilePicture": "string",
      "online": "boolean"
    },
    "participants": [
      {
        "id": "number",
        "username": "string",
        "fullName": "string",
        "profilePicture": "string",
        "online": "boolean"
      }
    ],
    "lastMessage": {
      "id": "number",
      "content": "string",
      "contentType": "string",
      "sentAt": "string (ISO date)",
      "sender": {
        "id": "number",
        "username": "string",
        "fullName": "string"
      }
    },
    "unreadCount": "number"
  }
]
```

**Status Codes:**
- 200: OK
- 401: Unauthorized

### Get Chat Room by ID

```
GET /api/chatrooms/{chatRoomId}
```

**Headers:**
```
Authorization: Bearer {token}
```

**Response:**
```json
{
  "id": "number",
  "name": "string",
  "isPrivate": "boolean",
  "createdAt": "string (ISO date)",
  "updatedAt": "string (ISO date)",
  "creator": {
    "id": "number",
    "username": "string",
    "fullName": "string",
    "profilePicture": "string",
    "online": "boolean"
  },
  "participants": [
    {
      "id": "number",
      "username": "string",
      "fullName": "string",
      "profilePicture": "string",
      "online": "boolean"
    }
  ],
  "lastMessage": {
    "id": "number",
    "content": "string",
    "contentType": "string",
    "sentAt": "string (ISO date)",
    "sender": {
      "id": "number",
      "username": "string",
      "fullName": "string"
    }
  },
  "unreadCount": "number"
}
```

**Status Codes:**
- 200: OK
- 401: Unauthorized
- 404: Not Found

## Message Endpoints

### Send Message

```
POST /api/messages
```

**Headers:**
```
Authorization: Bearer {token}
```

**Request Body:**
```json
{
  "chatRoomId": "number",
  "content": "string",
  "contentType": "string"
}
```

**Response:**
```json
{
  "id": "number",
  "content": "string",
  "contentType": "string",
  "sentAt": "string (ISO date)",
  "sender": {
    "id": "number",
    "username": "string",
    "fullName": "string",
    "profilePicture": "string"
  },
  "chatRoom": {
    "id": "number",
    "name": "string"
  },
  "status": "string"
}
```

**Status Codes:**
- 201: Created
- 400: Bad Request
- 401: Unauthorized
- 404: Not Found (chat room not found)

### Get Messages for Chat Room

```
GET /api/messages/chatroom/{chatRoomId}
```

**Headers:**
```
Authorization: Bearer {token}
```

**Query Parameters:**
- `page`: Page number (default: 0)
- `size`: Page size (default: 20)

**Response:**
```json
{
  "content": [
    {
      "id": "number",
      "content": "string",
      "contentType": "string",
      "sentAt": "string (ISO date)",
      "sender": {
        "id": "number",
        "username": "string",
        "fullName": "string",
        "profilePicture": "string"
      },
      "status": "string"
    }
  ],
  "pageable": {
    "pageNumber": "number",
    "pageSize": "number",
    "sort": {
      "empty": "boolean",
      "sorted": "boolean",
      "unsorted": "boolean"
    }
  },
  "totalElements": "number",
  "totalPages": "number",
  "last": "boolean",
  "first": "boolean",
  "empty": "boolean"
}
```

**Status Codes:**
- 200: OK
- 401: Unauthorized
- 404: Not Found (chat room not found)