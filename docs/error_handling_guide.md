# Error Handling Guide

This document describes the error handling mechanisms in the Chat Application API and provides guidance on how to handle different types of errors.

## HTTP Status Codes

The API uses standard HTTP status codes to indicate the success or failure of requests:

| Status Code | Description |
|-------------|-------------|
| 200 OK | The request was successful |
| 201 Created | The resource was successfully created |
| 204 No Content | The request was successful but there is no content to return |
| 400 Bad Request | The request was invalid or cannot be served |
| 401 Unauthorized | Authentication is required and has failed or has not been provided |
| 403 Forbidden | The authenticated user does not have access to the requested resource |
| 404 Not Found | The requested resource could not be found |
| 405 Method Not Allowed | The HTTP method used is not supported for this resource |
| 415 Unsupported Media Type | The content type of the request is not supported |
| 500 Internal Server Error | An error occurred on the server |

## Error Response Format

All error responses follow a consistent format:

```json
{
  "timestamp": "2025-05-18T12:49:06.4659258",
  "status": 400,
  "error": "Bad Request",
  "message": "Detailed error message",
  "path": "uri=/api/resource",
  "validationErrors": {
    "field1": "Error message for field1",
    "field2": "Error message for field2"
  }
}
```

- `timestamp`: The time when the error occurred
- `status`: The HTTP status code
- `error`: A short description of the error
- `message`: A detailed error message
- `path`: The path of the request that caused the error
- `validationErrors`: A map of field-specific validation errors (only present for validation errors)

## Common Error Types

### Authentication Errors

#### 401 Unauthorized

Returned when authentication fails or is not provided.

```json
{
  "timestamp": "2025-05-18T12:49:06.4659258",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid username or password",
  "path": "uri=/api/auth/login"
}
```

#### 403 Forbidden

Returned when the authenticated user does not have permission to access a resource.

```json
{
  "timestamp": "2025-05-18T12:49:06.4659258",
  "status": 403,
  "error": "Forbidden",
  "message": "You don't have permission to access this resource",
  "path": "uri=/api/chatrooms/1"
}
```

### Validation Errors

#### 400 Bad Request

Returned when the request contains invalid data.

```json
{
  "timestamp": "2025-05-18T12:49:06.4659258",
  "status": 400,
  "error": "Validation Error",
  "message": "Validation failed for request parameters",
  "path": "uri=/api/messages",
  "validationErrors": {
    "content": "Message content is required"
  }
}
```

### Resource Errors

#### 404 Not Found

Returned when the requested resource does not exist.

```json
{
  "timestamp": "2025-05-18T12:49:06.4659258",
  "status": 404,
  "error": "Not Found",
  "message": "Chat room not found with id: 123",
  "path": "uri=/api/chatrooms/123"
}
```

### Business Logic Errors

#### 400 Bad Request

Returned for business logic violations.

```json
{
  "timestamp": "2025-05-18T12:49:06.4659258",
  "status": 400,
  "error": "Bad Request",
  "message": "User is already a participant in this chat room",
  "path": "uri=/api/chatrooms/1/participants/2"
}
```

#### 403 Forbidden

Returned for chat room access violations.

```json
{
  "timestamp": "2025-05-18T12:49:06.4659258",
  "status": 403,
  "error": "Forbidden",
  "message": "User is not a participant in this chat room",
  "path": "uri=/api/messages/chatroom/1"
}
```

## Specific Error Scenarios

### Message Status Update Errors

When updating message status with the `{"status": "READ"}` payload format, the following errors may occur:

#### Invalid Status Value

```json
{
  "timestamp": "2025-05-18T12:49:06.4659258",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid status value. Allowed values are: SENT, DELIVERED, READ",
  "path": "uri=/api/messages/1/status"
}
```

#### User Not in Chat Room

```json
{
  "timestamp": "2025-05-18T12:49:06.4659258",
  "status": 403,
  "error": "Forbidden",
  "message": "User is not a participant in this chat room",
  "path": "uri=/api/messages/1/status"
}
```

#### Updating Own Message

```json
{
  "timestamp": "2025-05-18T12:49:06.4659258",
  "status": 400,
  "error": "Bad Request",
  "message": "Cannot update status for your own messages",
  "path": "uri=/api/messages/1/status"
}
```

### WebSocket Error Handling

WebSocket errors are sent to the client through a dedicated error channel. Clients should subscribe to `/user/queue/errors` to receive error messages.

```json
{
  "type": "ACCESS_DENIED",
  "message": "User is not a participant in this chat room",
  "timestamp": 1747558457955
}
```

Error types include:
- `NOT_FOUND`: Resource not found
- `ACCESS_DENIED`: User does not have access to the resource
- `INVALID_STATUS`: Invalid message status
- `DELIVERY_FAILED`: Message delivery failed
- `CONNECTION_ERROR`: WebSocket connection error
- `ERROR`: General error

## Handling Errors in Clients

### REST API Errors

1. Check the HTTP status code to determine the type of error
2. Parse the error response JSON to get detailed information
3. Display appropriate error messages to the user
4. For validation errors, highlight the affected fields

### WebSocket Errors

1. Subscribe to the `/user/queue/errors` destination
2. Handle errors based on the `type` field
3. Display appropriate error messages to the user
4. Implement reconnection logic for connection errors

## Best Practices

1. Always check the HTTP status code before processing the response
2. Handle all possible error scenarios in your client application
3. Provide clear error messages to the user
4. Implement retry logic for transient errors
5. Log errors for debugging purposes
