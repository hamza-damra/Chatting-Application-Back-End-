# Profile Image Upload Endpoints

This document describes the new profile image upload endpoints added to the User API.

## Overview

Two new endpoints have been added to allow users to upload and update their profile images:

- `POST /api/users/me/profile-image` - Add a new profile image
- `PUT /api/users/me/profile-image` - Update existing profile image

## Endpoints

### Add Profile Image

**Endpoint:** `POST /api/users/me/profile-image`

**Authentication:** Required (Bearer token)

**Content-Type:** `multipart/form-data`

**Parameters:**
- `file` (required): The image file to upload

**Request Example:**
```bash
curl -X POST "http://localhost:8080/api/users/me/profile-image" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@/path/to/your/image.jpg"
```

**Response:**
```json
{
  "id": 1,
  "username": "john_doe",
  "email": "john@example.com",
  "fullName": "John Doe",
  "profilePicture": "/api/files/download/20241201-143022-profile.jpg-a1b2c3d4.jpg",
  "lastSeen": "2024-12-01T14:30:22",
  "isOnline": true
}
```

### Update Profile Image

**Endpoint:** `PUT /api/users/me/profile-image`

**Authentication:** Required (Bearer token)

**Content-Type:** `multipart/form-data`

**Parameters:**
- `file` (required): The new image file to upload

**Request Example:**
```bash
curl -X PUT "http://localhost:8080/api/users/me/profile-image" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@/path/to/your/new-image.png"
```

**Response:**
```json
{
  "id": 1,
  "username": "john_doe",
  "email": "john@example.com",
  "fullName": "John Doe",
  "profilePicture": "/api/files/download/20241201-143525-new-profile.png-e5f6g7h8.png",
  "lastSeen": "2024-12-01T14:35:25",
  "isOnline": true
}
```

## File Validation

### Supported Image Types
- **JPEG** (image/jpeg, image/jpg, image/pjpeg)
- **PNG** (image/png, image/x-png)
- **GIF** (image/gif)
- **WebP** (image/webp)
- **HEIC/HEIF** (image/heic, image/heif) - Modern iPhone/Android formats
- **BMP** (image/bmp)
- **TIFF** (image/tiff, image/tif)

### Android Camera Support
The system now supports images captured directly from Android cameras, including:
- Various content-type variations used by different Android devices
- Fallback validation using file extensions when content-type is unreliable
- Enhanced logging for debugging content-type issues

### File Size Limit
- Maximum file size: 1GB

### Validation Rules
1. File cannot be empty
2. File must be a valid image type (validated by content-type OR file extension)
3. File size must not exceed 1GB
4. Supports both content-type validation and file extension fallback

## Error Responses

### 400 Bad Request - Invalid File Type
```json
{
  "error": "Invalid File Type",
  "message": "Only image files are allowed. Supported formats: JPEG, PNG, GIF, WebP, HEIC, BMP, TIFF. Received content-type: 'application/octet-stream', filename: 'photo.jpg'",
  "timestamp": "2024-12-01T14:30:22"
}
```

### 400 Bad Request - File Too Large
```json
{
  "error": "Bad Request",
  "message": "File size cannot exceed 1GB",
  "timestamp": "2024-12-01T14:30:22"
}
```

### 400 Bad Request - Empty File
```json
{
  "error": "Bad Request",
  "message": "File cannot be empty",
  "timestamp": "2024-12-01T14:30:22"
}
```

### 401 Unauthorized
```json
{
  "error": "Unauthorized",
  "message": "Access denied",
  "timestamp": "2024-12-01T14:30:22"
}
```

## Implementation Details

### File Storage
- Files are stored using the existing `FileMetadataService`
- Images are saved in the `uploads/images/` directory
- Unique filenames are generated with timestamp and UUID
- File metadata is stored in the database

### Database Updates
- The user's `profilePicture` field is updated with the file download URL
- The URL format is: `/api/files/download/{filename}`

### Security
- Endpoints require USER role authentication
- Only the current user can update their own profile image
- File validation prevents malicious uploads

## Frontend Integration

### Flutter Example
```dart
Future<UserResponse> uploadProfileImage(File imageFile) async {
  var request = http.MultipartRequest(
    'POST',
    Uri.parse('$baseUrl/api/users/me/profile-image'),
  );

  request.headers['Authorization'] = 'Bearer $token';
  request.files.add(await http.MultipartFile.fromPath('file', imageFile.path));

  var response = await request.send();
  var responseBody = await response.stream.bytesToString();

  if (response.statusCode == 200) {
    return UserResponse.fromJson(json.decode(responseBody));
  } else {
    throw Exception('Failed to upload profile image');
  }
}
```

### JavaScript Example
```javascript
async function uploadProfileImage(file) {
  const formData = new FormData();
  formData.append('file', file);

  const response = await fetch('/api/users/me/profile-image', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`
    },
    body: formData
  });

  if (response.ok) {
    return await response.json();
  } else {
    throw new Error('Failed to upload profile image');
  }
}
```

## Testing

### Manual Testing with cURL
```bash
# Add profile image
curl -X POST "http://localhost:8080/api/users/me/profile-image" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@test-image.jpg"

# Update profile image
curl -X PUT "http://localhost:8080/api/users/me/profile-image" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@new-test-image.png"
```

### Testing with Postman
1. Set method to POST or PUT
2. Set URL to `http://localhost:8080/api/users/me/profile-image`
3. Add Authorization header: `Bearer YOUR_JWT_TOKEN`
4. In Body tab, select "form-data"
5. Add key "file" with type "File"
6. Select your image file
7. Send request

## Notes

- Both endpoints return the same response format (updated UserResponse)
- The difference between POST and PUT is semantic - both will update the profile image
- Old profile images are not automatically deleted (this could be added as a future enhancement)
- The profile image URL can be used directly in `<img>` tags or image widgets
- Images are served through the existing `/api/files/download/{filename}` endpoint
