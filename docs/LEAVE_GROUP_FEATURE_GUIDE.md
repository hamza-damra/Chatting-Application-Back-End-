# 🚪 Leave Group Feature - Frontend Implementation Guide

## Overview

This document provides comprehensive documentation for implementing the **Leave Group** feature in the frontend. The backend already supports this functionality through REST API endpoints that allow users to remove themselves from chat rooms/groups.

## 🏗️ Backend Support

The leave group feature is **fully implemented** in the backend with proper authorization, validation, and database operations.

### Core Components
- **REST API Endpoint**: `DELETE /api/chatrooms/{id}/participants/{userId}`
- **Service Logic**: `ChatRoomService.removeParticipant()`
- **Authorization**: Users can remove themselves or creators can remove others
- **Database Operations**: Bidirectional relationship updates

## 🌐 REST API Endpoint

### Leave Group (Remove Participant)

```http
DELETE /api/chatrooms/{roomId}/participants/{userId}
```

**Purpose**: Remove a user from a chat room/group permanently

**Authentication**: Required (JWT Bearer token)

**Authorization Rules**:
- ✅ Users can remove **themselves** from any group they're in
- ✅ Group **creators** can remove any participant
- ❌ Regular participants **cannot** remove others

**Parameters**:
- `roomId` (path) - The chat room/group ID
- `userId` (path) - The user ID to remove

**Headers**:
```http
Authorization: Bearer {jwt_token}
Content-Type: application/json
```

**Response**:
- **Success**: `204 No Content` (empty body)
- **Error**: Various HTTP status codes with error details

## 📋 Response Codes & Error Handling

| Status Code | Description | Action Required |
|-------------|-------------|-----------------|
| `204` | Success - User removed from group | Update UI, remove group from list |
| `400` | Bad Request - Invalid parameters | Check roomId and userId validity |
| `401` | Unauthorized - Invalid/missing token | Re-authenticate user |
| `403` | Forbidden - No permission to remove | Show permission error message |
| `404` | Not Found - Room or user doesn't exist | Handle gracefully, refresh data |
| `500` | Internal Server Error | Show generic error, retry option |

### Error Response Format
```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 403,
  "error": "Forbidden",
  "message": "You don't have permission to remove this participant",
  "path": "/api/chatrooms/123/participants/456"
}
```

## 🔧 Frontend Implementation Examples

### JavaScript/Web Implementation

#### Basic Leave Group Function
```javascript
class GroupService {
  constructor(baseUrl, authToken) {
    this.baseUrl = baseUrl;
    this.authToken = authToken;
  }

  async leaveGroup(roomId, userId) {
    try {
      const response = await fetch(
        `${this.baseUrl}/api/chatrooms/${roomId}/participants/${userId}`,
        {
          method: 'DELETE',
          headers: {
            'Authorization': `Bearer ${this.authToken}`,
            'Content-Type': 'application/json'
          }
        }
      );

      if (response.ok) {
        return { success: true, message: 'Successfully left the group' };
      } else {
        const errorData = await response.json();
        return {
          success: false,
          error: errorData.message || 'Failed to leave group',
          status: response.status
        };
      }
    } catch (error) {
      return {
        success: false,
        error: 'Network error: ' + error.message
      };
    }
  }

  async removeParticipant(roomId, userId) {
    // Same implementation as leaveGroup - can be used by group creators
    return this.leaveGroup(roomId, userId);
  }
}
```

#### Usage Example
```javascript
const groupService = new GroupService('http://localhost:8080', userToken);

// User leaving a group
async function handleLeaveGroup(roomId) {
  const currentUserId = getCurrentUserId(); // Get from auth context

  const result = await groupService.leaveGroup(roomId, currentUserId);

  if (result.success) {
    // Update UI - remove group from list
    removeGroupFromUI(roomId);
    showSuccessMessage('You have left the group');

    // Navigate away from group if currently viewing it
    if (getCurrentRoomId() === roomId) {
      navigateToGroupsList();
    }
  } else {
    showErrorMessage(result.error);
  }
}

// Group creator removing a participant
async function handleRemoveParticipant(roomId, participantId) {
  const result = await groupService.removeParticipant(roomId, participantId);

  if (result.success) {
    // Update participants list in UI
    removeParticipantFromUI(participantId);
    showSuccessMessage('Participant removed successfully');
  } else {
    showErrorMessage(result.error);
  }
}
```

### React Implementation

#### Hook for Leave Group
```jsx
import { useState } from 'react';
import { useAuth } from './AuthContext';

export const useLeaveGroup = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const { authToken, baseUrl } = useAuth();

  const leaveGroup = async (roomId, userId) => {
    setLoading(true);
    setError(null);

    try {
      const response = await fetch(
        `${baseUrl}/api/chatrooms/${roomId}/participants/${userId}`,
        {
          method: 'DELETE',
          headers: {
            'Authorization': `Bearer ${authToken}`,
            'Content-Type': 'application/json'
          }
        }
      );

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || 'Failed to leave group');
      }

      return true;
    } catch (err) {
      setError(err.message);
      return false;
    } finally {
      setLoading(false);
    }
  };

  return { leaveGroup, loading, error };
};
```

#### React Component Example
```jsx
import React from 'react';
import { useLeaveGroup } from './hooks/useLeaveGroup';
import { useAuth } from './AuthContext';

const GroupActions = ({ roomId, isCreator, onGroupLeft }) => {
  const { leaveGroup, loading, error } = useLeaveGroup();
  const { user } = useAuth();

  const handleLeaveGroup = async () => {
    const confirmed = window.confirm(
      'Are you sure you want to leave this group? You will no longer receive messages from this group.'
    );

    if (confirmed) {
      const success = await leaveGroup(roomId, user.id);
      if (success) {
        onGroupLeft(roomId);
      }
    }
  };

  return (
    <div className="group-actions">
      <button
        onClick={handleLeaveGroup}
        disabled={loading}
        className="btn btn-danger"
      >
        {loading ? 'Leaving...' : 'Leave Group'}
      </button>

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

#### Service Class
```dart
class GroupService {
  final String baseUrl;
  final String authToken;
  final http.Client httpClient;

  GroupService({
    required this.baseUrl,
    required this.authToken,
    http.Client? httpClient,
  }) : httpClient = httpClient ?? http.Client();

  Future<GroupActionResult> leaveGroup(int roomId, int userId) async {
    try {
      final response = await httpClient.delete(
        Uri.parse('$baseUrl/api/chatrooms/$roomId/participants/$userId'),
        headers: {
          'Authorization': 'Bearer $authToken',
          'Content-Type': 'application/json',
        },
      );

      if (response.statusCode == 204) {
        return GroupActionResult.success('Successfully left the group');
      } else {
        final errorData = jsonDecode(response.body);
        return GroupActionResult.error(
          errorData['message'] ?? 'Failed to leave group',
          response.statusCode,
        );
      }
    } catch (e) {
      return GroupActionResult.error('Network error: $e');
    }
  }

  Future<GroupActionResult> removeParticipant(int roomId, int userId) async {
    return leaveGroup(roomId, userId);
  }
}

class GroupActionResult {
  final bool success;
  final String message;
  final int? statusCode;

  GroupActionResult.success(this.message)
      : success = true, statusCode = null;

  GroupActionResult.error(this.message, [this.statusCode])
      : success = false;
}
```

#### Flutter Widget Example
```dart
class GroupActionsWidget extends StatefulWidget {
  final int roomId;
  final bool isCreator;
  final VoidCallback onGroupLeft;

  const GroupActionsWidget({
    Key? key,
    required this.roomId,
    required this.isCreator,
    required this.onGroupLeft,
  }) : super(key: key);

  @override
  _GroupActionsWidgetState createState() => _GroupActionsWidgetState();
}

class _GroupActionsWidgetState extends State<GroupActionsWidget> {
  bool _isLoading = false;
  final GroupService _groupService = GetIt.instance<GroupService>();

  Future<void> _handleLeaveGroup() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('Leave Group'),
        content: Text(
          'Are you sure you want to leave this group? '
          'You will no longer receive messages from this group.'
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: Text('Cancel'),
          ),
          TextButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: Text('Leave'),
          ),
        ],
      ),
    );

    if (confirmed == true) {
      setState(() => _isLoading = true);

      final currentUserId = AuthService.instance.currentUser?.id;
      if (currentUserId != null) {
        final result = await _groupService.leaveGroup(
          widget.roomId,
          currentUserId
        );

        if (result.success) {
          widget.onGroupLeft();
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(result.message)),
          );
        } else {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(result.message),
              backgroundColor: Colors.red,
            ),
          );
        }
      }

      setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return ElevatedButton(
      onPressed: _isLoading ? null : _handleLeaveGroup,
      style: ElevatedButton.styleFrom(
        backgroundColor: Colors.red,
        foregroundColor: Colors.white,
      ),
      child: _isLoading
          ? SizedBox(
              width: 20,
              height: 20,
              child: CircularProgressIndicator(strokeWidth: 2),
            )
          : Text('Leave Group'),
    );
  }
}
```

## 🎯 UI/UX Implementation Guidelines

### User Experience Considerations

#### 1. **Confirmation Dialog**
Always show a confirmation dialog before leaving a group:
```
Title: "Leave Group"
Message: "Are you sure you want to leave [Group Name]? You will no longer receive messages from this group."
Actions: [Cancel] [Leave Group]
```

#### 2. **Loading States**
- Show loading indicator during API call
- Disable the leave button to prevent multiple requests
- Provide visual feedback for the action

#### 3. **Success Feedback**
- Show success message: "You have left the group"
- Remove the group from the user's group list immediately
- Navigate away from the group if currently viewing it

#### 4. **Error Handling**
- Display user-friendly error messages
- Provide retry option for network errors
- Handle different error scenarios appropriately

### UI Components Placement

#### 1. **Group Settings Screen**
```
Group Settings
├── Group Info
├── Participants List
├── Notifications Settings
└── [Leave Group] (Red button at bottom)
```

#### 2. **Group Chat Screen**
```
Group Chat Header
├── Group Name
├── Participants Count
└── Menu (⋮)
    ├── Group Info
    ├── Mute Notifications
    └── Leave Group
```

#### 3. **Participant Management (For Creators)**
```
Participants List
├── User 1 [Remove] (if creator)
├── User 2 [Remove] (if creator)
├── Current User [Leave Group]
└── [Add Participants] (if creator)
```

## 🔄 State Management

### Frontend State Updates After Leaving

#### 1. **Remove Group from Lists**
```javascript
// Remove from user's groups list
const updatedGroups = userGroups.filter(group => group.id !== roomId);
setUserGroups(updatedGroups);

// Update unread counts
const updatedUnreadCounts = { ...unreadCounts };
delete updatedUnreadCounts[roomId];
setUnreadCounts(updatedUnreadCounts);
```

#### 2. **WebSocket Cleanup**
```javascript
// Unsubscribe from group-specific WebSocket channels
if (stompClient && stompClient.connected) {
  stompClient.unsubscribe(`/topic/chatrooms/${roomId}`);
}

// Clear any cached messages for this group
clearGroupMessages(roomId);
```

#### 3. **Navigation Handling**
```javascript
// If currently viewing the group, navigate away
if (currentRoomId === roomId) {
  navigateToGroupsList(); // or navigate to home
}
```

## 🧪 Testing Guidelines

### Unit Tests

#### 1. **Service Layer Tests**
```javascript
describe('GroupService', () => {
  test('should successfully leave group', async () => {
    // Mock successful API response
    fetchMock.mockResponseOnce('', { status: 204 });

    const result = await groupService.leaveGroup(123, 456);

    expect(result.success).toBe(true);
    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/chatrooms/123/participants/456',
      expect.objectContaining({
        method: 'DELETE',
        headers: expect.objectContaining({
          'Authorization': 'Bearer test-token'
        })
      })
    );
  });

  test('should handle permission error', async () => {
    fetchMock.mockResponseOnce(
      JSON.stringify({ message: 'Permission denied' }),
      { status: 403 }
    );

    const result = await groupService.leaveGroup(123, 456);

    expect(result.success).toBe(false);
    expect(result.error).toBe('Permission denied');
  });
});
```

#### 2. **Component Tests**
```javascript
describe('GroupActionsComponent', () => {
  test('should show confirmation dialog when leave button clicked', async () => {
    render(<GroupActions roomId={123} onGroupLeft={mockCallback} />);

    fireEvent.click(screen.getByText('Leave Group'));

    expect(screen.getByText('Are you sure you want to leave this group?')).toBeInTheDocument();
  });

  test('should call onGroupLeft callback after successful leave', async () => {
    const mockOnGroupLeft = jest.fn();
    // Mock successful API call

    render(<GroupActions roomId={123} onGroupLeft={mockOnGroupLeft} />);

    fireEvent.click(screen.getByText('Leave Group'));
    fireEvent.click(screen.getByText('Leave')); // Confirm dialog

    await waitFor(() => {
      expect(mockOnGroupLeft).toHaveBeenCalledWith(123);
    });
  });
});
```

### Integration Tests

#### 1. **End-to-End Flow**
```javascript
describe('Leave Group Flow', () => {
  test('complete leave group workflow', async () => {
    // 1. Navigate to group
    await navigateToGroup(123);

    // 2. Open group menu
    await clickGroupMenu();

    // 3. Click leave group
    await clickLeaveGroup();

    // 4. Confirm action
    await confirmLeaveGroup();

    // 5. Verify group removed from list
    expect(await getGroupsList()).not.toContain('Test Group');

    // 6. Verify navigation away from group
    expect(getCurrentPath()).toBe('/groups');
  });
});
```

## 🚨 Error Scenarios & Handling

### Common Error Cases

#### 1. **Network Connectivity Issues**
```javascript
// Show retry option
if (error.includes('Network error')) {
  showErrorWithRetry('Connection failed. Please check your internet and try again.');
}
```

#### 2. **User Not in Group**
```javascript
// Handle gracefully - user might have been removed already
if (statusCode === 400 && error.includes('not a participant')) {
  // Remove group from local state anyway
  removeGroupFromLocalState(roomId);
  showMessage('You are no longer a member of this group.');
}
```

#### 3. **Group Deleted**
```javascript
// Handle group deletion
if (statusCode === 404) {
  removeGroupFromLocalState(roomId);
  showMessage('This group no longer exists.');
}
```

#### 4. **Permission Changes**
```javascript
// Handle permission changes
if (statusCode === 403) {
  showError('You no longer have permission to leave this group. Please contact the group admin.');
}
```

## 🔗 Related Features Integration

### 1. **Notification System**
```javascript
// After leaving group, update notification preferences
await notificationService.updatePreferences({
  groupNotifications: {
    [roomId]: false // Disable notifications for this group
  }
});
```

### 2. **WebSocket Integration**
```javascript
// Send leave event via WebSocket (for real-time updates)
if (stompClient.connected) {
  stompClient.send('/app/chat.leaveRoom/' + roomId);
}
```

### 3. **Offline Support**
```javascript
// Queue leave action for when back online
if (!navigator.onLine) {
  queueOfflineAction({
    type: 'LEAVE_GROUP',
    roomId: roomId,
    userId: userId,
    timestamp: Date.now()
  });
}
```

## 📚 Additional Resources

### Backend Documentation References
- [Chat Room API Endpoints](./api_endpoints_reference.md)
- [WebSocket Configuration](./WEBSOCKET_CONFIGURATION.md)
- [Authentication Guide](./AUTHENTICATION_GUIDE.md)

### Frontend Integration Guides
- [React WebSocket Integration](./REACT_WEBSOCKET_GUIDE.md)
- [Flutter Chat Implementation](./FLUTTER_INTEGRATION_GUIDE.md)
- [Error Handling Best Practices](./ERROR_HANDLING_GUIDE.md)

## 🎯 Quick Implementation Checklist

### ✅ **Backend Verification**
- [x] REST API endpoint available: `DELETE /api/chatrooms/{id}/participants/{userId}`
- [x] Authorization logic implemented
- [x] Database operations working
- [x] Error handling in place

### 📋 **Frontend Implementation Tasks**
- [ ] Create leave group service/API client
- [ ] Implement confirmation dialog
- [ ] Add loading states and error handling
- [ ] Update local state after successful leave
- [ ] Handle WebSocket cleanup
- [ ] Add UI components (buttons, menus)
- [ ] Implement navigation logic
- [ ] Add unit and integration tests
- [ ] Test error scenarios
- [ ] Verify offline support

### 🧪 **Testing Checklist**
- [ ] Test successful leave group flow
- [ ] Test permission denied scenarios
- [ ] Test network error handling
- [ ] Test UI state updates
- [ ] Test WebSocket cleanup
- [ ] Test navigation behavior
- [ ] Test confirmation dialog
- [ ] Test loading states

---

*Last updated: January 2024*
*Version: 1.0*