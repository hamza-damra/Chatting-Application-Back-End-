# 📸 Profile Image GET Endpoints Documentation

## 🎯 **OVERVIEW**

This document describes the new GET endpoints for retrieving user profile images that have been added to the chat application backend.

## 📋 **NEW ENDPOINTS**

### **1. Get Current User's Profile Image**

```http
GET /api/users/me/profile-image/view
Authorization: Bearer JWT_TOKEN
```

**Description**: Retrieves the profile image of the currently authenticated user.

**Response**:
- **200 OK**: Returns the profile image file with appropriate content type headers
- **404 Not Found**: User has no profile image set
- **401 Unauthorized**: Invalid or missing JWT token

**Headers**:
- `Content-Type`: Automatically set based on image type (e.g., `image/jpeg`, `image/png`)
- `Content-Disposition`: Set to `inline` for direct display in browsers

### **2. Get Any User's Profile Image**

```http
GET /api/users/{id}/profile-image/view
Authorization: Bearer JWT_TOKEN
```

**Description**: Retrieves the profile image of a specific user by their ID.

**Parameters**:
- `id` (path): The user ID whose profile image to retrieve

**Response**:
- **200 OK**: Returns the profile image file with appropriate content type headers
- **404 Not Found**: User not found or user has no profile image set
- **401 Unauthorized**: Invalid or missing JWT token

**Headers**:
- `Content-Type`: Automatically set based on image type (e.g., `image/jpeg`, `image/png`)
- `Content-Disposition`: Set to `inline` for direct display in browsers

## 🔧 **IMPLEMENTATION DETAILS**

### **Backend Architecture**

The new endpoints are implemented with the following components:

1. **UserController**: New GET endpoints for profile image retrieval
2. **UserService**: Business logic for fetching and serving profile images
3. **FileMetadataService**: Integration for file metadata lookup
4. **Resource Handling**: Direct file serving using Spring's Resource abstraction

### **File Resolution Process**

1. **URL Parsing**: Extract filename from user's `profilePicture` field
2. **Metadata Lookup**: Find file metadata using `FileMetadataService.findByFileName()`
3. **File Access**: Locate actual file using metadata's `filePath`
4. **Resource Creation**: Create Spring `Resource` for file serving
5. **Content Type**: Determine MIME type from file metadata

### **Security**

- **Authentication Required**: All endpoints require valid JWT token
- **Role-Based Access**: Requires `USER` role
- **File Access Control**: Only serves files that exist in the file metadata database
- **Path Validation**: Prevents directory traversal attacks through proper file resolution

## 🚀 **USAGE EXAMPLES**

### **Example 1: Get Current User's Profile Image**

```bash
curl -X GET "http://localhost:8080/api/users/me/profile-image" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### **Example 2: Get Specific User's Profile Image**

```bash
curl -X GET "http://localhost:8080/api/users/123/profile-image" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### **Example 3: Display in HTML**

```html
<img src="http://localhost:8080/api/users/me/profile-image"
     alt="Profile Image"
     style="width: 100px; height: 100px; border-radius: 50%;">
```

### **Example 4: Use in Flutter/Dart**

```dart
// Service method to get profile image URL
String getProfileImageUrl(int? userId) {
  final baseUrl = 'http://localhost:8080';
  if (userId != null) {
    return '$baseUrl/api/users/$userId/profile-image';
  } else {
    return '$baseUrl/api/users/me/profile-image';
  }
}

// Widget usage
NetworkImage(
  getProfileImageUrl(user.id),
  headers: {'Authorization': 'Bearer $token'},
)
```

## 🔍 **ERROR HANDLING**

### **Common Error Scenarios**

1. **No Profile Image**: Returns 404 when user has no profile image set
2. **File Not Found**: Returns 404 when profile image file is missing from disk
3. **Invalid User ID**: Returns 404 when user doesn't exist
4. **Unauthorized Access**: Returns 401 when JWT token is invalid/missing
5. **File Access Error**: Returns 404 when file exists but can't be read

### **Logging**

The endpoints provide detailed logging for debugging:

```
PROFILE_IMAGE: Getting profile image for current user
PROFILE_IMAGE: Successfully retrieved profile image for current user
PROFILE_IMAGE: User has no profile image
PROFILE_IMAGE: Error getting profile image for user: [error details]
```

## 📊 **COMPARISON WITH EXISTING ENDPOINTS**

| Feature | New GET Endpoints | Existing `/api/files/download/{filename}` |
|---------|-------------------|-------------------------------------------|
| **Purpose** | Profile image specific | Generic file download |
| **URL Format** | `/api/users/{id}/profile-image` | `/api/files/download/{filename}` |
| **User Context** | User-centric (by user ID) | File-centric (by filename) |
| **Authentication** | Required | Not required |
| **Error Handling** | User-specific errors | File-specific errors |
| **Logging** | Profile-specific logging | Generic file logging |

## 🎯 **BENEFITS**

1. **User-Centric**: Access profile images by user ID instead of complex filenames
2. **Simplified Frontend**: No need to parse URLs or manage filenames
3. **Better Security**: Authentication required for profile image access
4. **Consistent API**: Follows RESTful patterns for user resources
5. **Error Clarity**: Clear error messages for profile image specific issues
6. **Direct Integration**: Can be used directly in `<img>` tags and image widgets

## 🔄 **INTEGRATION WITH EXISTING FEATURES**

### **Profile Image Upload Flow**

1. **Upload**: `POST /api/users/me/profile-image` (existing)
2. **Update**: `PUT /api/users/me/profile-image` (existing)
3. **Retrieve**: `GET /api/users/me/profile-image` (new)
4. **Get Others**: `GET /api/users/{id}/profile-image` (new)

### **Frontend Integration**

The new endpoints complement the existing profile image management system:

- **Upload/Update**: Use existing POST/PUT endpoints
- **Display**: Use new GET endpoints for direct image serving
- **User Lists**: Use GET endpoints to show profile images in user lists
- **Chat Interface**: Use GET endpoints to display profile images in chat

## 📝 **NOTES**

- Profile images are served with `Content-Disposition: inline` for direct browser display
- Content type is automatically determined from file metadata
- Endpoints return actual image files, not JSON responses
- File access is validated through the existing file metadata system
- All existing profile image functionality remains unchanged
