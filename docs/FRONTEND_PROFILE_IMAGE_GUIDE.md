# Frontend Profile Image Upload Guide

This guide provides complete implementation details for integrating the profile image upload feature in your frontend application.

## 📋 Overview

Two new endpoints have been added for profile image management:
- **POST** `/api/users/me/profile-image` - Add profile image
- **PUT** `/api/users/me/profile-image` - Update profile image

Both endpoints accept image files and return the updated user profile with the new image URL.

## 🔧 API Endpoints

### Add Profile Image
```
POST /api/users/me/profile-image
Content-Type: multipart/form-data
Authorization: Bearer {token}
```

### Update Profile Image
```
PUT /api/users/me/profile-image
Content-Type: multipart/form-data
Authorization: Bearer {token}
```

### Request Parameters
- `file` (required): Image file (JPEG, PNG, GIF, WebP)
- Maximum file size: 1GB

### Response Format
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

## 🎯 Flutter Implementation

### 1. Add Dependencies
```yaml
# pubspec.yaml
dependencies:
  http: ^1.1.0
  image_picker: ^1.0.4
  path: ^1.8.3
```

### 2. User Model Update
```dart
class UserResponse {
  final int id;
  final String username;
  final String email;
  final String fullName;
  final String? profilePicture;
  final DateTime? lastSeen;
  final bool isOnline;

  UserResponse({
    required this.id,
    required this.username,
    required this.email,
    required this.fullName,
    this.profilePicture,
    this.lastSeen,
    required this.isOnline,
  });

  factory UserResponse.fromJson(Map<String, dynamic> json) {
    return UserResponse(
      id: json['id'],
      username: json['username'],
      email: json['email'],
      fullName: json['fullName'],
      profilePicture: json['profilePicture'],
      lastSeen: json['lastSeen'] != null ? DateTime.parse(json['lastSeen']) : null,
      isOnline: json['isOnline'] ?? false,
    );
  }
}
```

### 3. Profile Image Service
```dart
import 'dart:io';
import 'package:http/http.dart' as http;
import 'dart:convert';

class ProfileImageService {
  final String baseUrl;
  final String token;

  ProfileImageService({required this.baseUrl, required this.token});

  Future<UserResponse> addProfileImage(File imageFile) async {
    return _uploadImage('POST', imageFile);
  }

  Future<UserResponse> updateProfileImage(File imageFile) async {
    return _uploadImage('PUT', imageFile);
  }

  Future<UserResponse> _uploadImage(String method, File imageFile) async {
    try {
      var request = http.MultipartRequest(
        method,
        Uri.parse('$baseUrl/api/users/me/profile-image'),
      );

      // Add authorization header
      request.headers['Authorization'] = 'Bearer $token';

      // Add file
      request.files.add(
        await http.MultipartFile.fromPath('file', imageFile.path),
      );

      // Send request
      var streamedResponse = await request.send();
      var response = await http.Response.fromStream(streamedResponse);

      if (response.statusCode == 200) {
        Map<String, dynamic> jsonResponse = json.decode(response.body);
        return UserResponse.fromJson(jsonResponse);
      } else {
        Map<String, dynamic> errorResponse = json.decode(response.body);
        throw Exception(errorResponse['message'] ?? 'Failed to upload image');
      }
    } catch (e) {
      throw Exception('Network error: $e');
    }
  }

  String getFullImageUrl(String? profilePicture) {
    if (profilePicture == null || profilePicture.isEmpty) {
      return '';
    }
    return '$baseUrl$profilePicture';
  }
}
```

### 4. Image Picker Widget
```dart
import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'dart:io';

class ProfileImagePicker extends StatefulWidget {
  final String? currentImageUrl;
  final Function(UserResponse) onImageUploaded;
  final ProfileImageService profileImageService;

  const ProfileImagePicker({
    Key? key,
    this.currentImageUrl,
    required this.onImageUploaded,
    required this.profileImageService,
  }) : super(key: key);

  @override
  _ProfileImagePickerState createState() => _ProfileImagePickerState();
}

class _ProfileImagePickerState extends State<ProfileImagePicker> {
  final ImagePicker _picker = ImagePicker();
  bool _isUploading = false;

  Future<void> _pickAndUploadImage() async {
    try {
      final XFile? image = await _picker.pickImage(
        source: ImageSource.gallery,
        maxWidth: 1024,
        maxHeight: 1024,
        imageQuality: 85,
      );

      if (image != null) {
        setState(() {
          _isUploading = true;
        });

        File imageFile = File(image.path);

        // Check file size (1GB limit)
        int fileSizeInBytes = await imageFile.length();
        if (fileSizeInBytes > 1024 * 1024 * 1024) {
          throw Exception('File size cannot exceed 1GB');
        }

        UserResponse updatedUser;
        if (widget.currentImageUrl == null || widget.currentImageUrl!.isEmpty) {
          updatedUser = await widget.profileImageService.addProfileImage(imageFile);
        } else {
          updatedUser = await widget.profileImageService.updateProfileImage(imageFile);
        }

        widget.onImageUploaded(updatedUser);

        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Profile image updated successfully!')),
        );
      }
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Error: $e')),
      );
    } finally {
      setState(() {
        _isUploading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: _isUploading ? null : _pickAndUploadImage,
      child: Stack(
        children: [
          CircleAvatar(
            radius: 50,
            backgroundColor: Colors.grey[300],
            backgroundImage: widget.currentImageUrl != null && widget.currentImageUrl!.isNotEmpty
                ? NetworkImage(widget.profileImageService.getFullImageUrl(widget.currentImageUrl))
                : null,
            child: widget.currentImageUrl == null || widget.currentImageUrl!.isEmpty
                ? const Icon(Icons.person, size: 50, color: Colors.grey)
                : null,
          ),
          if (_isUploading)
            Positioned.fill(
              child: Container(
                decoration: BoxDecoration(
                  color: Colors.black54,
                  borderRadius: BorderRadius.circular(50),
                ),
                child: const Center(
                  child: CircularProgressIndicator(color: Colors.white),
                ),
              ),
            ),
          Positioned(
            bottom: 0,
            right: 0,
            child: Container(
              decoration: BoxDecoration(
                color: Theme.of(context).primaryColor,
                borderRadius: BorderRadius.circular(15),
              ),
              child: const Icon(
                Icons.camera_alt,
                color: Colors.white,
                size: 20,
              ),
              padding: const EdgeInsets.all(5),
            ),
          ),
        ],
      ),
    );
  }
}
```

### 5. Usage Example
```dart
class ProfileScreen extends StatefulWidget {
  @override
  _ProfileScreenState createState() => _ProfileScreenState();
}

class _ProfileScreenState extends State<ProfileScreen> {
  UserResponse? currentUser;
  late ProfileImageService profileImageService;

  @override
  void initState() {
    super.initState();
    profileImageService = ProfileImageService(
      baseUrl: 'http://your-server-url:8080',
      token: 'your-jwt-token',
    );
    _loadCurrentUser();
  }

  void _loadCurrentUser() {
    // Load current user data
    // Set currentUser state
  }

  void _onImageUploaded(UserResponse updatedUser) {
    setState(() {
      currentUser = updatedUser;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('Profile')),
      body: currentUser == null
          ? Center(child: CircularProgressIndicator())
          : Column(
              children: [
                SizedBox(height: 20),
                ProfileImagePicker(
                  currentImageUrl: currentUser!.profilePicture,
                  onImageUploaded: _onImageUploaded,
                  profileImageService: profileImageService,
                ),
                SizedBox(height: 20),
                Text(
                  currentUser!.fullName,
                  style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
                ),
                Text(currentUser!.email),
                // Other profile information
              ],
            ),
    );
  }
}
```

## 🌐 JavaScript/React Implementation

### 1. Profile Image Service
```javascript
class ProfileImageService {
  constructor(baseUrl, token) {
    this.baseUrl = baseUrl;
    this.token = token;
  }

  async addProfileImage(file) {
    return this._uploadImage('POST', file);
  }

  async updateProfileImage(file) {
    return this._uploadImage('PUT', file);
  }

  async _uploadImage(method, file) {
    // Validate file size (1GB)
    if (file.size > 1024 * 1024 * 1024) {
      throw new Error('File size cannot exceed 1GB');
    }

    // Validate file type
    const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp'];
    if (!allowedTypes.includes(file.type)) {
      throw new Error('Only image files are allowed (JPEG, PNG, GIF, WebP)');
    }

    const formData = new FormData();
    formData.append('file', file);

    const response = await fetch(`${this.baseUrl}/api/users/me/profile-image`, {
      method: method,
      headers: {
        'Authorization': `Bearer ${this.token}`
      },
      body: formData
    });

    if (response.ok) {
      return await response.json();
    } else {
      const error = await response.json();
      throw new Error(error.message || 'Failed to upload image');
    }
  }

  getFullImageUrl(profilePicture) {
    if (!profilePicture) return '';
    return `${this.baseUrl}${profilePicture}`;
  }
}
```

### 2. React Component
```jsx
import React, { useState } from 'react';

const ProfileImageUpload = ({ currentUser, onUserUpdate, profileImageService }) => {
  const [isUploading, setIsUploading] = useState(false);

  const handleFileSelect = async (event) => {
    const file = event.target.files[0];
    if (!file) return;

    try {
      setIsUploading(true);

      let updatedUser;
      if (currentUser.profilePicture) {
        updatedUser = await profileImageService.updateProfileImage(file);
      } else {
        updatedUser = await profileImageService.addProfileImage(file);
      }

      onUserUpdate(updatedUser);
      alert('Profile image updated successfully!');
    } catch (error) {
      alert(`Error: ${error.message}`);
    } finally {
      setIsUploading(false);
    }
  };

  return (
    <div className="profile-image-upload">
      <div className="image-container">
        <img
          src={currentUser.profilePicture
            ? profileImageService.getFullImageUrl(currentUser.profilePicture)
            : '/default-avatar.png'
          }
          alt="Profile"
          className="profile-image"
        />
        {isUploading && (
          <div className="upload-overlay">
            <div className="spinner">Uploading...</div>
          </div>
        )}
      </div>

      <input
        type="file"
        accept="image/*"
        onChange={handleFileSelect}
        disabled={isUploading}
        style={{ display: 'none' }}
        id="profile-image-input"
      />

      <label htmlFor="profile-image-input" className="upload-button">
        {isUploading ? 'Uploading...' : 'Change Photo'}
      </label>
    </div>
  );
};

export default ProfileImageUpload;
```

## ⚠️ Error Handling

### Common Error Responses
```json
// File too large
{
  "error": "Bad Request",
  "message": "File size cannot exceed 1GB",
  "timestamp": "2024-12-01T14:30:22"
}

// Invalid file type
{
  "error": "Bad Request",
  "message": "Only image files are allowed (JPEG, PNG, GIF, WebP)",
  "timestamp": "2024-12-01T14:30:22"
}

// Empty file
{
  "error": "Bad Request",
  "message": "File cannot be empty",
  "timestamp": "2024-12-01T14:30:22"
}

// Unauthorized
{
  "error": "Unauthorized",
  "message": "Access denied",
  "timestamp": "2024-12-01T14:30:22"
}
```

## 🔒 Security Notes

1. **Authentication Required**: All endpoints require valid JWT token
2. **File Validation**: Server validates file type and size
3. **User Isolation**: Users can only update their own profile image
4. **Content Type Validation**: Server checks MIME types

## 📱 Best Practices

1. **Image Optimization**: Resize images before upload to reduce file size
2. **Progress Indicators**: Show upload progress to users
3. **Error Handling**: Provide clear error messages
4. **Caching**: Cache profile images to improve performance
5. **Fallback Images**: Use default avatars when no profile image exists

## 🧪 Testing

### Test Cases
1. Upload valid image (JPEG, PNG, GIF, WebP)
2. Try uploading file > 5MB (should fail)
3. Try uploading non-image file (should fail)
4. Upload without authentication (should fail)
5. Update existing profile image
6. Display profile image in UI

### Manual Testing with cURL
```bash
# Add profile image
curl -X POST "http://localhost:8080/api/users/me/profile-image" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@test-image.jpg"

# Update profile image
curl -X PUT "http://localhost:8080/api/users/me/profile-image" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@new-image.png"
```
