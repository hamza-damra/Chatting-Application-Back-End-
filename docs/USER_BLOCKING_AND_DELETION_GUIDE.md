# 🚫 User Blocking & Account Deletion - Implementation Guide

## Overview

This document provides comprehensive documentation for the **User Blocking** and **Account Deletion** features implemented in the chat application backend. These features enhance user safety and provide proper account management capabilities.

## 🚫 User Blocking System

### Features Implemented

#### ✅ **Core Blocking Functionality**
- **Block User** - Prevent communication with specific users
- **Unblock User** - Remove blocking restrictions
- **View Blocked Users** - List all blocked users
- **Block Status Check** - Check if a user is blocked
- **Message Filtering** - Prevent messages between blocked users

#### ✅ **Database Schema**
- **BlockedUser Entity** - Stores blocking relationships
- **Unique Constraints** - Prevents duplicate blocks
- **Self-Block Prevention** - Users cannot block themselves
- **Cascade Deletion** - Cleanup when users are deleted

### API Endpoints

#### **Base URL**: `/api/users/blocking`

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/block` | Block a user | ✅ USER |
| DELETE | `/unblock/{userId}` | Unblock a user | ✅ USER |
| GET | `/blocked` | Get blocked users list | ✅ USER |
| GET | `/is-blocked/{userId}` | Check if user is blocked | ✅ USER |
| GET | `/count` | Get blocked users count | ✅ USER |

### Request/Response Examples

#### **Block User**
```http
POST /api/users/blocking/block
Content-Type: application/json
Authorization: Bearer {token}

{
  "userId": 123,
  "reason": "Inappropriate behavior"
}
```

**Response:**
```json
{
  "id": 1,
  "blockedUser": {
    "id": 123,
    "username": "user123",
    "email": "user123@example.com",
    "fullName": "John Doe",
    "profilePicture": null,
    "lastSeen": "2024-01-15T10:30:00",
    "isOnline": false
  },
  "blockedAt": "2024-01-15T10:30:00",
  "reason": "Inappropriate behavior"
}
```

#### **Get Blocked Users**
```http
GET /api/users/blocking/blocked
Authorization: Bearer {token}
```

**Response:**
```json
[
  {
    "id": 1,
    "blockedUser": {
      "id": 123,
      "username": "user123",
      "fullName": "John Doe"
    },
    "blockedAt": "2024-01-15T10:30:00",
    "reason": "Inappropriate behavior"
  }
]
```

#### **Check Block Status**
```http
GET /api/users/blocking/is-blocked/123
Authorization: Bearer {token}
```

**Response:**
```json
{
  "isBlocked": true
}
```

### Integration with Messaging

The blocking system is integrated with the messaging functionality:

- **Message Prevention**: Blocked users cannot send messages to each other
- **Chat Room Restrictions**: Users cannot send messages in chat rooms where blocked users are present
- **Real-time Enforcement**: Blocking is enforced immediately without requiring reconnection

## 🗑️ Account Deletion System

### Features Implemented

#### ✅ **Self-Deletion**
- **Password Verification** - Users must confirm with password
- **Data Options** - Choose to delete data or just deactivate
- **Reason Tracking** - Optional reason for deletion

#### ✅ **Admin Management**
- **Delete User Account** - Admins can delete any user
- **Deactivate Account** - Soft deletion option
- **Reactivate Account** - Restore deactivated accounts
- **Account Status Check** - View account status

#### ✅ **Data Cleanup**
- **Block Relationships** - Cleanup all blocking data
- **Chat Room Removal** - Remove from all chat rooms
- **Message Handling** - Anonymize or preserve messages
- **File Cleanup** - Handle uploaded files

### API Endpoints

#### **Base URL**: `/api/users/management`

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| DELETE | `/delete-account` | Delete current user account | ✅ USER |
| DELETE | `/delete-user/{userId}` | Delete user account (admin) | ✅ ADMIN |
| PUT | `/deactivate/{userId}` | Deactivate user account | ✅ ADMIN |
| PUT | `/reactivate/{userId}` | Reactivate user account | ✅ ADMIN |
| GET | `/status/{userId}` | Check account status | ✅ ADMIN |

### Request/Response Examples

#### **Delete Own Account**
```http
DELETE /api/users/management/delete-account
Content-Type: application/json
Authorization: Bearer {token}

{
  "password": "userpassword",
  "reason": "No longer need the account",
  "deleteData": true
}
```

**Response:** `204 No Content`

#### **Admin Delete User**
```http
DELETE /api/users/management/delete-user/123?reason=Violation&deleteData=true
Authorization: Bearer {admin_token}
```

**Response:** `204 No Content`

#### **Deactivate User**
```http
PUT /api/users/management/deactivate/123?reason=Temporary suspension
Authorization: Bearer {admin_token}
```

**Response:** `204 No Content`

## 🔧 Frontend Integration Examples

### JavaScript/Web Implementation

#### **Block User Function**
```javascript
class UserBlockingService {
  constructor(baseUrl, authToken) {
    this.baseUrl = baseUrl;
    this.authToken = authToken;
  }

  async blockUser(userId, reason = '') {
    try {
      const response = await fetch(`${this.baseUrl}/api/users/blocking/block`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${this.authToken}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ userId, reason })
      });

      if (response.ok) {
        const result = await response.json();
        return { success: true, data: result };
      } else {
        const error = await response.json();
        return { success: false, error: error.message };
      }
    } catch (error) {
      return { success: false, error: 'Network error: ' + error.message };
    }
  }

  async unblockUser(userId) {
    try {
      const response = await fetch(`${this.baseUrl}/api/users/blocking/unblock/${userId}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${this.authToken}`
        }
      });

      return { success: response.ok };
    } catch (error) {
      return { success: false, error: 'Network error: ' + error.message };
    }
  }

  async getBlockedUsers() {
    try {
      const response = await fetch(`${this.baseUrl}/api/users/blocking/blocked`, {
        headers: {
          'Authorization': `Bearer ${this.authToken}`
        }
      });

      if (response.ok) {
        const blockedUsers = await response.json();
        return { success: true, data: blockedUsers };
      } else {
        return { success: false, error: 'Failed to fetch blocked users' };
      }
    } catch (error) {
      return { success: false, error: 'Network error: ' + error.message };
    }
  }

  async isUserBlocked(userId) {
    try {
      const response = await fetch(`${this.baseUrl}/api/users/blocking/is-blocked/${userId}`, {
        headers: {
          'Authorization': `Bearer ${this.authToken}`
        }
      });

      if (response.ok) {
        const result = await response.json();
        return { success: true, isBlocked: result.isBlocked };
      } else {
        return { success: false, error: 'Failed to check block status' };
      }
    } catch (error) {
      return { success: false, error: 'Network error: ' + error.message };
    }
  }
}
```

#### **Account Deletion Function**
```javascript
class UserManagementService {
  constructor(baseUrl, authToken) {
    this.baseUrl = baseUrl;
    this.authToken = authToken;
  }

  async deleteAccount(password, reason = '', deleteData = true) {
    try {
      const response = await fetch(`${this.baseUrl}/api/users/management/delete-account`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${this.authToken}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ password, reason, deleteData })
      });

      if (response.ok) {
        return { success: true, message: 'Account deleted successfully' };
      } else {
        const error = await response.json();
        return { success: false, error: error.message };
      }
    } catch (error) {
      return { success: false, error: 'Network error: ' + error.message };
    }
  }
}
```

### React Implementation

#### **Block User Hook**
```jsx
import { useState } from 'react';
import { useAuth } from './AuthContext';

export const useUserBlocking = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const { authToken, baseUrl } = useAuth();

  const blockUser = async (userId, reason = '') => {
    setLoading(true);
    setError(null);

    try {
      const response = await fetch(`${baseUrl}/api/users/blocking/block`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${authToken}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ userId, reason })
      });

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || 'Failed to block user');
      }

      const result = await response.json();
      return result;
    } catch (err) {
      setError(err.message);
      throw err;
    } finally {
      setLoading(false);
    }
  };

  const unblockUser = async (userId) => {
    setLoading(true);
    setError(null);

    try {
      const response = await fetch(`${baseUrl}/api/users/blocking/unblock/${userId}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${authToken}`
        }
      });

      if (!response.ok) {
        throw new Error('Failed to unblock user');
      }

      return true;
    } catch (err) {
      setError(err.message);
      throw err;
    } finally {
      setLoading(false);
    }
  };

  return { blockUser, unblockUser, loading, error };
};
```

#### **Block User Component**
```jsx
import React, { useState } from 'react';
import { useUserBlocking } from './hooks/useUserBlocking';

const UserBlockingComponent = ({ userId, username, onUserBlocked }) => {
  const [reason, setReason] = useState('');
  const [showBlockDialog, setShowBlockDialog] = useState(false);
  const { blockUser, unblockUser, loading, error } = useUserBlocking();

  const handleBlockUser = async () => {
    try {
      await blockUser(userId, reason);
      setShowBlockDialog(false);
      setReason('');
      onUserBlocked(userId);
    } catch (error) {
      // Error is handled by the hook
    }
  };

  const handleUnblockUser = async () => {
    try {
      await unblockUser(userId);
      onUserBlocked(userId, false);
    } catch (error) {
      // Error is handled by the hook
    }
  };

  return (
    <div className="user-blocking">
      <button
        onClick={() => setShowBlockDialog(true)}
        className="btn btn-danger"
        disabled={loading}
      >
        Block User
      </button>

      {showBlockDialog && (
        <div className="modal">
          <div className="modal-content">
            <h3>Block {username}?</h3>
            <p>This user will not be able to send you messages.</p>

            <textarea
              placeholder="Reason for blocking (optional)"
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              maxLength={500}
            />

            <div className="modal-actions">
              <button
                onClick={() => setShowBlockDialog(false)}
                className="btn btn-secondary"
              >
                Cancel
              </button>
              <button
                onClick={handleBlockUser}
                className="btn btn-danger"
                disabled={loading}
              >
                {loading ? 'Blocking...' : 'Block User'}
              </button>
            </div>
          </div>
        </div>
      )}

      {error && (
        <div className="error-message">
          {error}
        </div>
      )}
    </div>
  );
};
```

### Flutter Implementation

#### **User Blocking Service**
```dart
class UserBlockingService {
  final String baseUrl;
  final String authToken;
  final http.Client httpClient;

  UserBlockingService({
    required this.baseUrl,
    required this.authToken,
    http.Client? httpClient,
  }) : httpClient = httpClient ?? http.Client();

  Future<BlockUserResult> blockUser(int userId, {String? reason}) async {
    try {
      final response = await httpClient.post(
        Uri.parse('$baseUrl/api/users/blocking/block'),
        headers: {
          'Authorization': 'Bearer $authToken',
          'Content-Type': 'application/json',
        },
        body: jsonEncode({
          'userId': userId,
          if (reason != null) 'reason': reason,
        }),
      );

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        return BlockUserResult.success(BlockedUserResponse.fromJson(data));
      } else {
        final errorData = jsonDecode(response.body);
        return BlockUserResult.error(errorData['message'] ?? 'Failed to block user');
      }
    } catch (e) {
      return BlockUserResult.error('Network error: $e');
    }
  }

  Future<bool> unblockUser(int userId) async {
    try {
      final response = await httpClient.delete(
        Uri.parse('$baseUrl/api/users/blocking/unblock/$userId'),
        headers: {'Authorization': 'Bearer $authToken'},
      );
      return response.statusCode == 204;
    } catch (e) {
      return false;
    }
  }

  Future<List<BlockedUserResponse>> getBlockedUsers() async {
    try {
      final response = await httpClient.get(
        Uri.parse('$baseUrl/api/users/blocking/blocked'),
        headers: {'Authorization': 'Bearer $authToken'},
      );

      if (response.statusCode == 200) {
        final List<dynamic> data = jsonDecode(response.body);
        return data.map((json) => BlockedUserResponse.fromJson(json)).toList();
      }
      return [];
    } catch (e) {
      return [];
    }
  }
}

class BlockUserResult {
  final bool success;
  final BlockedUserResponse? data;
  final String? error;

  BlockUserResult.success(this.data) : success = true, error = null;
  BlockUserResult.error(this.error) : success = false, data = null;
}
```

## 🔒 Security Considerations

### **User Blocking Security**
1. **Authorization**: Only authenticated users can block/unblock
2. **Self-Block Prevention**: Users cannot block themselves
3. **Unique Constraints**: Prevents duplicate blocking relationships
4. **Cascade Deletion**: Cleanup when users are deleted
5. **Message Filtering**: Real-time enforcement in messaging

### **Account Deletion Security**
1. **Password Verification**: Required for self-deletion
2. **Admin Authorization**: Only admins can delete other users
3. **Data Cleanup**: Proper cleanup of all related data
4. **Audit Trail**: Logging of all deletion activities
5. **Soft Delete Option**: Deactivation instead of hard deletion

## 🧪 Testing Guidelines

### **Unit Tests for Blocking**
```java
@Test
public void testBlockUser_Success() {
    // Given
    User blocker = createTestUser("blocker");
    User blocked = createTestUser("blocked");
    BlockUserRequest request = new BlockUserRequest(blocked.getId(), "Test reason");

    // When
    BlockedUserResponse response = userBlockingService.blockUser(request);

    // Then
    assertNotNull(response);
    assertEquals(blocked.getId(), response.getBlockedUser().getId());
    assertEquals("Test reason", response.getReason());
}

@Test
public void testBlockUser_CannotBlockSelf() {
    // Given
    User user = createTestUser("user");
    BlockUserRequest request = new BlockUserRequest(user.getId(), "Self block");

    // When & Then
    assertThrows(BadRequestException.class, () -> {
        userBlockingService.blockUser(request);
    });
}
```

## 📊 Database Schema

### **blocked_users Table**
```sql
CREATE TABLE blocked_users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    blocker_id BIGINT NOT NULL,
    blocked_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reason VARCHAR(500),

    CONSTRAINT fk_blocked_users_blocker FOREIGN KEY (blocker_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_blocked_users_blocked FOREIGN KEY (blocked_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_blocked_users_blocker_blocked UNIQUE (blocker_id, blocked_id),
    CONSTRAINT chk_blocked_users_no_self_block CHECK (blocker_id != blocked_id)
);
```

## 🚀 Implementation Summary

### ✅ **Features Implemented**

#### **User Blocking System**
- [x] Block/Unblock users
- [x] View blocked users list
- [x] Check block status
- [x] Message filtering integration
- [x] Database schema with constraints
- [x] REST API endpoints
- [x] Service layer implementation

#### **Account Deletion System**
- [x] Self-deletion with password verification
- [x] Admin user deletion
- [x] Account deactivation/reactivation
- [x] Data cleanup procedures
- [x] REST API endpoints
- [x] Service layer implementation

#### **Integration Points**
- [x] Message service integration
- [x] Chat room access control
- [x] Database relationships
- [x] Error handling
- [x] Security validations

### 🎯 **Ready for Frontend Integration**

Both features are **fully implemented** in the backend and ready for frontend integration. The documentation provides:

1. **Complete API Reference** - All endpoints with examples
2. **Frontend Code Examples** - JavaScript, React, and Flutter
3. **Security Guidelines** - Best practices and considerations
4. **Testing Examples** - Unit and integration tests
5. **Database Schema** - Complete table structures

---

*Last updated: January 2024*
*Version: 1.0*