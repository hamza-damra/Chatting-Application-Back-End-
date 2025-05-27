# Profile Image Upload - Quick Reference

## 🚀 Quick Start

### Endpoints
```
POST /api/users/me/profile-image    # Add profile image
PUT  /api/users/me/profile-image    # Update profile image
```

### Request Format
```
Content-Type: multipart/form-data
Authorization: Bearer {token}
Body: file={image_file}
```

### Response
```json
{
  "id": 1,
  "username": "user",
  "email": "user@example.com",
  "fullName": "User Name",
  "profilePicture": "/api/files/download/filename.jpg",
  "lastSeen": "2024-12-01T14:30:22",
  "isOnline": true
}
```

## 📋 Validation Rules

- **File Types**: JPEG, PNG, GIF, WebP only
- **Max Size**: 1GB
- **Required**: Authentication (JWT token)
- **Required**: Non-empty file

## 🎯 Flutter Quick Implementation

```dart
// Service method
Future<UserResponse> uploadProfileImage(File imageFile, bool isUpdate) async {
  var request = http.MultipartRequest(
    isUpdate ? 'PUT' : 'POST',
    Uri.parse('$baseUrl/api/users/me/profile-image'),
  );

  request.headers['Authorization'] = 'Bearer $token';
  request.files.add(await http.MultipartFile.fromPath('file', imageFile.path));

  var response = await request.send();
  var responseBody = await response.stream.bytesToString();

  if (response.statusCode == 200) {
    return UserResponse.fromJson(json.decode(responseBody));
  } else {
    throw Exception(json.decode(responseBody)['message']);
  }
}

// Widget usage
ProfileImagePicker(
  currentImageUrl: user.profilePicture,
  onImageUploaded: (updatedUser) {
    setState(() { user = updatedUser; });
  },
)
```

## 🌐 JavaScript Quick Implementation

```javascript
// Upload function
async function uploadProfileImage(file, isUpdate = false) {
  const formData = new FormData();
  formData.append('file', file);

  const response = await fetch('/api/users/me/profile-image', {
    method: isUpdate ? 'PUT' : 'POST',
    headers: { 'Authorization': `Bearer ${token}` },
    body: formData
  });

  if (response.ok) {
    return await response.json();
  } else {
    const error = await response.json();
    throw new Error(error.message);
  }
}

// Usage
document.getElementById('file-input').addEventListener('change', async (e) => {
  try {
    const file = e.target.files[0];
    const updatedUser = await uploadProfileImage(file, hasExistingImage);
    updateUserProfile(updatedUser);
  } catch (error) {
    alert('Error: ' + error.message);
  }
});
```

## ⚠️ Common Errors

| Error | Cause | Solution |
|-------|-------|----------|
| "File size cannot exceed 1GB" | File too large | Compress image before upload |
| "Only image files are allowed" | Wrong file type | Use JPEG, PNG, GIF, or WebP |
| "File cannot be empty" | No file selected | Ensure file is selected |
| "Access denied" | Missing/invalid token | Check authentication |

## 🔧 Image Display

### Full URL Construction
```
Backend returns: "/api/files/download/filename.jpg"
Full URL: "http://your-server:8080/api/files/download/filename.jpg"
```

### Flutter
```dart
NetworkImage('$baseUrl${user.profilePicture}')
```

### JavaScript
```javascript
<img src={`${baseUrl}${user.profilePicture}`} alt="Profile" />
```

## 📱 UI Best Practices

1. **Show upload progress** during file upload
2. **Validate file size/type** before sending
3. **Use circular avatars** for profile images
4. **Provide default avatar** when no image exists
5. **Add camera/edit icon** to indicate clickable area
6. **Show success/error messages** after upload

## 🧪 Testing Checklist

- [ ] Upload JPEG image
- [ ] Upload PNG image
- [ ] Try uploading 2GB file (should fail)
- [ ] Try uploading PDF file (should fail)
- [ ] Upload without authentication (should fail)
- [ ] Update existing profile image
- [ ] Display image in UI correctly
- [ ] Handle network errors gracefully

## 📞 Support

For issues or questions:
1. Check error message in response
2. Verify file meets validation rules
3. Confirm authentication token is valid
4. Test with cURL to isolate frontend issues

```bash
# Test with cURL
curl -X POST "http://localhost:8080/api/users/me/profile-image" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "file=@test.jpg"
```
