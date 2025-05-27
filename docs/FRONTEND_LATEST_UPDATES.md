# 🚀 Latest Backend Updates for Frontend Integration

**Last Updated**: December 2024
**Version**: Latest
**Status**: ✅ Ready for Integration

## 📋 **RECENT CHANGES SUMMARY**

### **🆕 NEW FEATURE: Profile Image GET Endpoints**

Added dedicated endpoints for retrieving user profile images directly, eliminating the need for complex URL parsing and filename management.

---

## 🔗 **NEW API ENDPOINTS**

### **1. Get Current User Profile Image**

```http
GET /api/users/me/profile-image/view
Authorization: Bearer {JWT_TOKEN}
```

**Response**: Direct image file (JPEG, PNG, GIF, WebP)
**Status Codes**:
- `200 OK` - Image returned successfully
- `404 Not Found` - User has no profile image
- `401 Unauthorized` - Invalid/missing token

### **2. Get Any User Profile Image**

```http
GET /api/users/{userId}/profile-image/view
Authorization: Bearer {JWT_TOKEN}
```

**Parameters**:
- `userId` (path) - Target user ID

**Response**: Direct image file (JPEG, PNG, GIF, WebP)
**Status Codes**:
- `200 OK` - Image returned successfully
- `404 Not Found` - User not found or no profile image
- `401 Unauthorized` - Invalid/missing token

---

## 💻 **FRONTEND INTEGRATION GUIDE**

### **Flutter/Dart Implementation**

#### **1. Service Layer**

```dart
class UserService {
  final String baseUrl = 'http://abusaker.zapto.org:8080';
  final String? token;

  UserService({required this.token});

  // Get current user's profile image URL
  String getCurrentUserProfileImageUrl() {
    return '$baseUrl/api/users/me/profile-image/view';
  }

  // Get specific user's profile image URL
  String getUserProfileImageUrl(int userId) {
    return '$baseUrl/api/users/$userId/profile-image/view';
  }

  // Get headers with authentication
  Map<String, String> getAuthHeaders() {
    return {
      'Authorization': 'Bearer $token',
      'Content-Type': 'application/json',
    };
  }

  // Check if profile image exists
  Future<bool> hasProfileImage({int? userId}) async {
    try {
      final url = userId != null
        ? getUserProfileImageUrl(userId)
        : getCurrentUserProfileImageUrl();

      final response = await http.head(
        Uri.parse(url),
        headers: getAuthHeaders(),
      );

      return response.statusCode == 200;
    } catch (e) {
      return false;
    }
  }
}
```

#### **2. Widget Implementation**

```dart
class ProfileImageWidget extends StatelessWidget {
  final int? userId;
  final double size;
  final String? fallbackAsset;

  const ProfileImageWidget({
    Key? key,
    this.userId,
    this.size = 50.0,
    this.fallbackAsset = 'assets/images/default_avatar.png',
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final userService = Provider.of<UserService>(context);

    final imageUrl = userId != null
        ? userService.getUserProfileImageUrl(userId!)
        : userService.getCurrentUserProfileImageUrl();

    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        border: Border.all(color: Colors.grey.shade300, width: 1),
      ),
      child: ClipOval(
        child: Image.network(
          imageUrl,
          headers: userService.getAuthHeaders(),
          width: size,
          height: size,
          fit: BoxFit.cover,
          loadingBuilder: (context, child, loadingProgress) {
            if (loadingProgress == null) return child;
            return Center(
              child: CircularProgressIndicator(
                value: loadingProgress.expectedTotalBytes != null
                    ? loadingProgress.cumulativeBytesLoaded /
                        loadingProgress.expectedTotalBytes!
                    : null,
              ),
            );
          },
          errorBuilder: (context, error, stackTrace) {
            return Image.asset(
              fallbackAsset ?? 'assets/images/default_avatar.png',
              width: size,
              height: size,
              fit: BoxFit.cover,
            );
          },
        ),
      ),
    );
  }
}
```

#### **3. Usage Examples**

```dart
// In user profile screen
ProfileImageWidget(
  size: 100.0,
  fallbackAsset: 'assets/images/default_avatar.png',
)

// In chat user list
ListView.builder(
  itemBuilder: (context, index) {
    final user = users[index];
    return ListTile(
      leading: ProfileImageWidget(
        userId: user.id,
        size: 40.0,
      ),
      title: Text(user.username),
      subtitle: Text(user.email),
    );
  },
)

// In chat message bubbles
Row(
  children: [
    ProfileImageWidget(
      userId: message.senderId,
      size: 30.0,
    ),
    SizedBox(width: 8),
    Expanded(child: MessageBubble(message: message)),
  ],
)
```

### **React/JavaScript Implementation**

#### **1. Service Layer**

```javascript
class UserService {
  constructor(token) {
    this.baseUrl = 'http://abusaker.zapto.org:8080';
    this.token = token;
  }

  // Get current user's profile image URL
  getCurrentUserProfileImageUrl() {
    return `${this.baseUrl}/api/users/me/profile-image/view`;
  }

  // Get specific user's profile image URL
  getUserProfileImageUrl(userId) {
    return `${this.baseUrl}/api/users/${userId}/profile-image/view`;
  }

  // Get headers with authentication
  getAuthHeaders() {
    return {
      'Authorization': `Bearer ${this.token}`,
      'Content-Type': 'application/json',
    };
  }

  // Check if profile image exists
  async hasProfileImage(userId = null) {
    try {
      const url = userId
        ? this.getUserProfileImageUrl(userId)
        : this.getCurrentUserProfileImageUrl();

      const response = await fetch(url, {
        method: 'HEAD',
        headers: this.getAuthHeaders(),
      });

      return response.ok;
    } catch (error) {
      return false;
    }
  }
}
```

#### **2. React Component**

```jsx
import React, { useState } from 'react';

const ProfileImage = ({
  userId = null,
  size = 50,
  fallbackSrc = '/assets/default-avatar.png',
  className = '',
  alt = 'Profile Image'
}) => {
  const [hasError, setHasError] = useState(false);
  const userService = new UserService(localStorage.getItem('token'));

  const imageUrl = userId
    ? userService.getUserProfileImageUrl(userId)
    : userService.getCurrentUserProfileImageUrl();

  const handleError = () => {
    setHasError(true);
  };

  const handleLoad = () => {
    setHasError(false);
  };

  return (
    <div
      className={`profile-image-container ${className}`}
      style={{
        width: size,
        height: size,
        borderRadius: '50%',
        overflow: 'hidden',
        border: '1px solid #ddd',
      }}
    >
      {!hasError ? (
        <img
          src={imageUrl}
          alt={alt}
          style={{
            width: '100%',
            height: '100%',
            objectFit: 'cover',
          }}
          onError={handleError}
          onLoad={handleLoad}
        />
      ) : (
        <img
          src={fallbackSrc}
          alt={alt}
          style={{
            width: '100%',
            height: '100%',
            objectFit: 'cover',
          }}
        />
      )}
    </div>
  );
};

export default ProfileImage;
```

#### **3. Usage Examples**

```jsx
// In user profile
<ProfileImage size={100} />

// In user list
{users.map(user => (
  <div key={user.id} className="user-item">
    <ProfileImage userId={user.id} size={40} />
    <span>{user.username}</span>
  </div>
))}

// In chat messages
<div className="message">
  <ProfileImage userId={message.senderId} size={30} />
  <div className="message-content">{message.content}</div>
</div>
```

---

## 🔄 **MIGRATION FROM OLD APPROACH**

### **Before (Complex URL Parsing)**

```dart
// OLD WAY - Don't use this anymore
String getProfileImageUrl(UserResponse user) {
  if (user.profilePicture != null && user.profilePicture!.isNotEmpty()) {
    // Complex parsing of "/api/files/download/filename.jpg"
    return 'http://abusaker.zapto.org:8080${user.profilePicture}';
  }
  return 'assets/images/default_avatar.png';
}
```

### **After (Simple Direct Access)**

```dart
// NEW WAY - Use this approach
String getProfileImageUrl(int userId) {
  return userService.getUserProfileImageUrl(userId);
}
```

---

## ⚠️ **IMPORTANT NOTES**

### **Authentication Required**
- All profile image endpoints require valid JWT token
- Include `Authorization: Bearer {token}` header in all requests

### **Error Handling**
- Always implement fallback images for 404 responses
- Handle network errors gracefully
- Consider caching strategies for better performance

### **Content Types Supported**
- JPEG (`image/jpeg`)
- PNG (`image/png`)
- GIF (`image/gif`)
- WebP (`image/webp`)

### **Performance Considerations**
- Images are served directly from backend storage
- Consider implementing client-side caching
- Use appropriate image sizes for different UI contexts

---

## 🧪 **TESTING ENDPOINTS**

### **Using cURL**

```bash
# Test current user profile image
curl -X GET "http://abusaker.zapto.org:8080/api/users/me/profile-image/view" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  --output "my_profile.jpg"

# Test specific user profile image
curl -X GET "http://abusaker.zapto.org:8080/api/users/123/profile-image/view" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  --output "user_123_profile.jpg"
```

### **Using Postman**
1. Set method to `GET`
2. Set URL to `http://abusaker.zapto.org:8080/api/users/me/profile-image/view`
3. Add Authorization header: `Bearer YOUR_JWT_TOKEN`
4. Send request - should return image file directly

---

## 📞 **SUPPORT**

If you encounter any issues with the new endpoints:

1. **Check Authentication**: Ensure JWT token is valid and included
2. **Verify User Has Profile Image**: Check if user has uploaded a profile image
3. **Review Logs**: Backend provides detailed logging for debugging
4. **Test with cURL**: Verify endpoints work outside your application

The new profile image GET endpoints are now ready for frontend integration and provide a much cleaner, more efficient way to access user profile images!
