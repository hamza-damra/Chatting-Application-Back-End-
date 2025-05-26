# 🚫 Frontend Implementation Guide - User Blocking System

## Overview

This document provides comprehensive frontend implementation guidance for the **User Blocking System**. The backend is fully implemented and ready for frontend integration.

## 🚫 User Blocking Feature

### Backend Support Status
✅ **FULLY IMPLEMENTED** - All endpoints and business logic are ready

### Core Functionality
- Block/unblock users to prevent communication
- View list of blocked users
- Check blocking status between users
- Automatic message filtering for blocked users
- Real-time enforcement without reconnection

### API Endpoints Reference

| Method | Endpoint | Purpose | Auth |
|--------|----------|---------|------|
| POST | `/api/users/blocking/block` | Block a user | USER |
| DELETE | `/api/users/blocking/unblock/{userId}` | Unblock a user | USER |
| GET | `/api/users/blocking/blocked` | Get blocked users list | USER |
| GET | `/api/users/blocking/is-blocked/{userId}` | Check if user is blocked | USER |
| GET | `/api/users/blocking/count` | Get blocked users count | USER |

### Request/Response Examples

#### Block User
```javascript
// Request
POST /api/users/blocking/block
{
  "userId": 123,
  "reason": "Inappropriate behavior"
}

// Response
{
  "id": 1,
  "blockedUser": {
    "id": 123,
    "username": "user123",
    "fullName": "John Doe",
    "profilePicture": null,
    "isOnline": false
  },
  "blockedAt": "2024-01-15T10:30:00",
  "reason": "Inappropriate behavior"
}
```

#### Unblock User
```javascript
// Request
DELETE /api/users/blocking/unblock/123

// Response: 204 No Content
```

#### Get Blocked Users
```javascript
// Request
GET /api/users/blocking/blocked

// Response
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

#### Check Block Status
```javascript
// Request
GET /api/users/blocking/is-blocked/123

// Response
{
  "isBlocked": true
}
```

#### Get Blocked Users Count
```javascript
// Request
GET /api/users/blocking/count

// Response
{
  "blockedUsersCount": 5
}
```

## 🔧 Frontend Implementation

### JavaScript Service Class
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

  async getBlockedUsersCount() {
    try {
      const response = await fetch(`${this.baseUrl}/api/users/blocking/count`, {
        headers: {
          'Authorization': `Bearer ${this.authToken}`
        }
      });

      if (response.ok) {
        const result = await response.json();
        return { success: true, count: result.blockedUsersCount };
      } else {
        return { success: false, error: 'Failed to get blocked users count' };
      }
    } catch (error) {
      return { success: false, error: 'Network error: ' + error.message };
    }
  }
}

// Usage Example
const blockingService = new UserBlockingService('http://localhost:8080', userToken);

// Block a user
const blockResult = await blockingService.blockUser(123, 'Spam messages');
if (blockResult.success) {
  console.log('User blocked:', blockResult.data);
} else {
  console.error('Block failed:', blockResult.error);
}

// Check if user is blocked
const statusResult = await blockingService.isUserBlocked(123);
if (statusResult.success) {
  console.log('Is blocked:', statusResult.isBlocked);
}
```

### React Hook Implementation
```jsx
import { useState, useCallback } from 'react';
import { useAuth } from './AuthContext';

export const useUserBlocking = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const { authToken, baseUrl } = useAuth();

  const blockUser = useCallback(async (userId, reason = '') => {
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
  }, [authToken, baseUrl]);

  const unblockUser = useCallback(async (userId) => {
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
  }, [authToken, baseUrl]);

  const getBlockedUsers = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await fetch(`${baseUrl}/api/users/blocking/blocked`, {
        headers: {
          'Authorization': `Bearer ${authToken}`
        }
      });

      if (!response.ok) {
        throw new Error('Failed to fetch blocked users');
      }

      const blockedUsers = await response.json();
      return blockedUsers;
    } catch (err) {
      setError(err.message);
      throw err;
    } finally {
      setLoading(false);
    }
  }, [authToken, baseUrl]);

  const isUserBlocked = useCallback(async (userId) => {
    try {
      const response = await fetch(`${baseUrl}/api/users/blocking/is-blocked/${userId}`, {
        headers: {
          'Authorization': `Bearer ${authToken}`
        }
      });

      if (!response.ok) {
        return false;
      }

      const result = await response.json();
      return result.isBlocked;
    } catch (err) {
      return false;
    }
  }, [authToken, baseUrl]);

  const getBlockedUsersCount = useCallback(async () => {
    try {
      const response = await fetch(`${baseUrl}/api/users/blocking/count`, {
        headers: {
          'Authorization': `Bearer ${authToken}`
        }
      });

      if (!response.ok) {
        return 0;
      }

      const result = await response.json();
      return result.blockedUsersCount;
    } catch (err) {
      return 0;
    }
  }, [authToken, baseUrl]);

  return {
    blockUser,
    unblockUser,
    getBlockedUsers,
    isUserBlocked,
    getBlockedUsersCount,
    loading,
    error
  };
};
```

### React Components

#### Block User Button Component
```jsx
import React, { useState } from 'react';
import { useUserBlocking } from '../hooks/useUserBlocking';

const BlockUserButton = ({ userId, username, isBlocked, onBlockStatusChange }) => {
  const [showDialog, setShowDialog] = useState(false);
  const [reason, setReason] = useState('');
  const { blockUser, unblockUser, loading, error } = useUserBlocking();

  const handleBlock = async () => {
    try {
      await blockUser(userId, reason);
      setShowDialog(false);
      setReason('');
      onBlockStatusChange?.(userId, true);
    } catch (error) {
      // Error handled by hook
    }
  };

  const handleUnblock = async () => {
    try {
      await unblockUser(userId);
      onBlockStatusChange?.(userId, false);
    } catch (error) {
      // Error handled by hook
    }
  };

  if (isBlocked) {
    return (
      <button
        onClick={handleUnblock}
        disabled={loading}
        className="btn btn-secondary"
      >
        {loading ? 'Unblocking...' : 'Unblock User'}
      </button>
    );
  }

  return (
    <>
      <button
        onClick={() => setShowDialog(true)}
        className="btn btn-danger"
        disabled={loading}
      >
        Block User
      </button>

      {showDialog && (
        <div className="modal-overlay">
          <div className="modal">
            <h3>Block {username}?</h3>
            <p>This user will not be able to send you messages.</p>

            <textarea
              placeholder="Reason for blocking (optional)"
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              maxLength={500}
              rows={3}
            />

            <div className="modal-actions">
              <button
                onClick={() => setShowDialog(false)}
                className="btn btn-secondary"
              >
                Cancel
              </button>
              <button
                onClick={handleBlock}
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
    </>
  );
};

export default BlockUserButton;
```

#### Blocked Users List Component
```jsx
import React, { useState, useEffect } from 'react';
import { useUserBlocking } from '../hooks/useUserBlocking';

const BlockedUsersList = () => {
  const [blockedUsers, setBlockedUsers] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');
  const { getBlockedUsers, unblockUser, loading, error } = useUserBlocking();

  useEffect(() => {
    loadBlockedUsers();
  }, []);

  const loadBlockedUsers = async () => {
    try {
      const users = await getBlockedUsers();
      setBlockedUsers(users);
    } catch (error) {
      // Error handled by hook
    }
  };

  const handleUnblock = async (userId) => {
    try {
      await unblockUser(userId);
      setBlockedUsers(prev => prev.filter(user => user.blockedUser.id !== userId));
    } catch (error) {
      // Error handled by hook
    }
  };

  const filteredUsers = blockedUsers.filter(blockedUser =>
    blockedUser.blockedUser.fullName.toLowerCase().includes(searchTerm.toLowerCase()) ||
    blockedUser.blockedUser.username.toLowerCase().includes(searchTerm.toLowerCase())
  );

  if (loading && blockedUsers.length === 0) {
    return <div className="loading">Loading blocked users...</div>;
  }

  return (
    <div className="blocked-users-list">
      <div className="header">
        <h3>Blocked Users ({blockedUsers.length})</h3>

        {blockedUsers.length > 0 && (
          <input
            type="text"
            placeholder="Search blocked users..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="search-input"
          />
        )}
      </div>

      {error && (
        <div className="error-message">
          {error}
        </div>
      )}

      {blockedUsers.length === 0 ? (
        <div className="empty-state">
          <p>No blocked users</p>
          <small>Users you block will appear here</small>
        </div>
      ) : (
        <div className="users-grid">
          {filteredUsers.map((blockedUser) => (
            <div key={blockedUser.id} className="user-card">
              <div className="user-info">
                <img
                  src={blockedUser.blockedUser.profilePicture || '/default-avatar.png'}
                  alt={blockedUser.blockedUser.fullName}
                  className="avatar"
                />
                <div className="user-details">
                  <h4>{blockedUser.blockedUser.fullName}</h4>
                  <p className="username">@{blockedUser.blockedUser.username}</p>
                  <small className="blocked-date">
                    Blocked on {new Date(blockedUser.blockedAt).toLocaleDateString()}
                  </small>
                  {blockedUser.reason && (
                    <p className="block-reason">
                      <strong>Reason:</strong> {blockedUser.reason}
                    </p>
                  )}
                </div>
              </div>
              <button
                onClick={() => handleUnblock(blockedUser.blockedUser.id)}
                disabled={loading}
                className="btn btn-secondary unblock-btn"
              >
                {loading ? 'Unblocking...' : 'Unblock'}
              </button>
            </div>
          ))}
        </div>
      )}

      {searchTerm && filteredUsers.length === 0 && blockedUsers.length > 0 && (
        <div className="no-results">
          <p>No users found matching "{searchTerm}"</p>
        </div>
      )}
    </div>
  );
};

export default BlockedUsersList;
```

#### Block Status Indicator Component
```jsx
import React, { useState, useEffect } from 'react';
import { useUserBlocking } from '../hooks/useUserBlocking';

const BlockStatusIndicator = ({ userId, showText = false }) => {
  const [isBlocked, setIsBlocked] = useState(false);
  const [loading, setLoading] = useState(true);
  const { isUserBlocked } = useUserBlocking();

  useEffect(() => {
    checkBlockStatus();
  }, [userId]);

  const checkBlockStatus = async () => {
    setLoading(true);
    try {
      const blocked = await isUserBlocked(userId);
      setIsBlocked(blocked);
    } catch (error) {
      console.error('Failed to check block status:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return showText ? <span>Checking...</span> : <div className="loading-spinner" />;
  }

  if (!isBlocked) {
    return null; // Don't show anything if user is not blocked
  }

  return (
    <div className="block-status-indicator">
      <span className="block-icon">🚫</span>
      {showText && <span className="block-text">Blocked</span>}
    </div>
  );
};

export default BlockStatusIndicator;
```

## 📱 Flutter Implementation

### User Blocking Service
```dart
import 'dart:convert';
import 'package:http/http.dart' as http;

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

  Future<bool> isUserBlocked(int userId) async {
    try {
      final response = await httpClient.get(
        Uri.parse('$baseUrl/api/users/blocking/is-blocked/$userId'),
        headers: {'Authorization': 'Bearer $authToken'},
      );

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        return data['isBlocked'] ?? false;
      }
      return false;
    } catch (e) {
      return false;
    }
  }

  Future<int> getBlockedUsersCount() async {
    try {
      final response = await httpClient.get(
        Uri.parse('$baseUrl/api/users/blocking/count'),
        headers: {'Authorization': 'Bearer $authToken'},
      );

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        return data['blockedUsersCount'] ?? 0;
      }
      return 0;
    } catch (e) {
      return 0;
    }
  }
}

// Result classes
class BlockUserResult {
  final bool success;
  final BlockedUserResponse? data;
  final String? error;

  BlockUserResult.success(this.data) : success = true, error = null;
  BlockUserResult.error(this.error) : success = false, data = null;
}

class BlockedUserResponse {
  final int id;
  final UserResponse blockedUser;
  final DateTime blockedAt;
  final String? reason;

  BlockedUserResponse({
    required this.id,
    required this.blockedUser,
    required this.blockedAt,
    this.reason,
  });

  factory BlockedUserResponse.fromJson(Map<String, dynamic> json) {
    return BlockedUserResponse(
      id: json['id'],
      blockedUser: UserResponse.fromJson(json['blockedUser']),
      blockedAt: DateTime.parse(json['blockedAt']),
      reason: json['reason'],
    );
  }
}

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

### Flutter Widgets

#### Block User Widget
```dart
import 'package:flutter/material.dart';
import 'package:get_it/get_it.dart';

class BlockUserWidget extends StatefulWidget {
  final int userId;
  final String username;
  final bool isBlocked;
  final VoidCallback? onBlockStatusChanged;

  const BlockUserWidget({
    Key? key,
    required this.userId,
    required this.username,
    required this.isBlocked,
    this.onBlockStatusChanged,
  }) : super(key: key);

  @override
  _BlockUserWidgetState createState() => _BlockUserWidgetState();
}

class _BlockUserWidgetState extends State<BlockUserWidget> {
  bool _isLoading = false;
  final TextEditingController _reasonController = TextEditingController();
  final UserBlockingService _blockingService = GetIt.instance<UserBlockingService>();

  Future<void> _showBlockDialog() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('Block ${widget.username}?'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text('This user will not be able to send you messages.'),
            SizedBox(height: 16),
            TextField(
              controller: _reasonController,
              decoration: InputDecoration(
                labelText: 'Reason (optional)',
                hintText: 'Why are you blocking this user?',
                border: OutlineInputBorder(),
              ),
              maxLength: 500,
              maxLines: 3,
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: Text('Cancel'),
          ),
          ElevatedButton(
            onPressed: () => Navigator.of(context).pop(true),
            style: ElevatedButton.styleFrom(backgroundColor: Colors.red),
            child: Text('Block User'),
          ),
        ],
      ),
    );

    if (confirmed == true) {
      await _blockUser();
    }
  }

  Future<void> _blockUser() async {
    setState(() => _isLoading = true);

    final result = await _blockingService.blockUser(
      widget.userId,
      reason: _reasonController.text.isNotEmpty ? _reasonController.text : null,
    );

    if (result.success) {
      widget.onBlockStatusChanged?.call();
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('User blocked successfully')),
      );
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(result.error ?? 'Failed to block user'),
          backgroundColor: Colors.red,
        ),
      );
    }

    setState(() => _isLoading = false);
    _reasonController.clear();
  }

  Future<void> _unblockUser() async {
    setState(() => _isLoading = true);

    final success = await _blockingService.unblockUser(widget.userId);

    if (success) {
      widget.onBlockStatusChanged?.call();
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('User unblocked successfully')),
      );
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Failed to unblock user'),
          backgroundColor: Colors.red,
        ),
      );
    }

    setState(() => _isLoading = false);
  }

  @override
  Widget build(BuildContext context) {
    if (widget.isBlocked) {
      return ElevatedButton(
        onPressed: _isLoading ? null : _unblockUser,
        style: ElevatedButton.styleFrom(backgroundColor: Colors.grey),
        child: _isLoading
            ? SizedBox(
                width: 16,
                height: 16,
                child: CircularProgressIndicator(strokeWidth: 2),
              )
            : Text('Unblock User'),
      );
    }

    return ElevatedButton.icon(
      onPressed: _isLoading ? null : _showBlockDialog,
      icon: _isLoading
          ? SizedBox(
              width: 16,
              height: 16,
              child: CircularProgressIndicator(strokeWidth: 2),
            )
          : Icon(Icons.block),
      label: Text(_isLoading ? 'Blocking...' : 'Block User'),
      style: ElevatedButton.styleFrom(
        backgroundColor: Colors.red,
        foregroundColor: Colors.white,
      ),
    );
  }

  @override
  void dispose() {
    _reasonController.dispose();
    super.dispose();
  }
}
```

#### Blocked Users List Widget
```dart
class BlockedUsersListWidget extends StatefulWidget {
  @override
  _BlockedUsersListWidgetState createState() => _BlockedUsersListWidgetState();
}

class _BlockedUsersListWidgetState extends State<BlockedUsersListWidget> {
  List<BlockedUserResponse> _blockedUsers = [];
  bool _isLoading = true;
  String _searchQuery = '';
  final UserBlockingService _blockingService = GetIt.instance<UserBlockingService>();

  @override
  void initState() {
    super.initState();
    _loadBlockedUsers();
  }

  Future<void> _loadBlockedUsers() async {
    setState(() => _isLoading = true);

    final users = await _blockingService.getBlockedUsers();

    setState(() {
      _blockedUsers = users;
      _isLoading = false;
    });
  }

  Future<void> _unblockUser(int userId) async {
    final success = await _blockingService.unblockUser(userId);

    if (success) {
      setState(() {
        _blockedUsers.removeWhere((user) => user.blockedUser.id == userId);
      });

      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('User unblocked successfully')),
      );
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Failed to unblock user'),
          backgroundColor: Colors.red,
        ),
      );
    }
  }

  List<BlockedUserResponse> get _filteredUsers {
    if (_searchQuery.isEmpty) return _blockedUsers;

    return _blockedUsers.where((blockedUser) {
      final user = blockedUser.blockedUser;
      return user.fullName.toLowerCase().contains(_searchQuery.toLowerCase()) ||
             user.username.toLowerCase().contains(_searchQuery.toLowerCase());
    }).toList();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Blocked Users (${_blockedUsers.length})'),
      ),
      body: Column(
        children: [
          if (_blockedUsers.isNotEmpty)
            Padding(
              padding: EdgeInsets.all(16),
              child: TextField(
                decoration: InputDecoration(
                  hintText: 'Search blocked users...',
                  prefixIcon: Icon(Icons.search),
                  border: OutlineInputBorder(),
                ),
                onChanged: (value) {
                  setState(() => _searchQuery = value);
                },
              ),
            ),

          Expanded(
            child: _isLoading
                ? Center(child: CircularProgressIndicator())
                : _blockedUsers.isEmpty
                    ? Center(
                        child: Column(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Icon(Icons.block, size: 64, color: Colors.grey),
                            SizedBox(height: 16),
                            Text(
                              'No blocked users',
                              style: Theme.of(context).textTheme.headlineSmall,
                            ),
                            SizedBox(height: 8),
                            Text(
                              'Users you block will appear here',
                              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                                color: Colors.grey,
                              ),
                            ),
                          ],
                        ),
                      )
                    : _filteredUsers.isEmpty
                        ? Center(
                            child: Text('No users found matching "$_searchQuery"'),
                          )
                        : ListView.builder(
                            itemCount: _filteredUsers.length,
                            itemBuilder: (context, index) {
                              final blockedUser = _filteredUsers[index];
                              return _buildUserCard(blockedUser);
                            },
                          ),
          ),
        ],
      ),
    );
  }

  Widget _buildUserCard(BlockedUserResponse blockedUser) {
    final user = blockedUser.blockedUser;

    return Card(
      margin: EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: ListTile(
        leading: CircleAvatar(
          backgroundImage: user.profilePicture != null
              ? NetworkImage(user.profilePicture!)
              : null,
          child: user.profilePicture == null
              ? Text(user.fullName[0].toUpperCase())
              : null,
        ),
        title: Text(user.fullName),
        subtitle: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('@${user.username}'),
            SizedBox(height: 4),
            Text(
              'Blocked on ${blockedUser.blockedAt.day}/${blockedUser.blockedAt.month}/${blockedUser.blockedAt.year}',
              style: TextStyle(fontSize: 12, color: Colors.grey),
            ),
            if (blockedUser.reason != null) ...[
              SizedBox(height: 4),
              Text(
                'Reason: ${blockedUser.reason}',
                style: TextStyle(fontSize: 12, fontStyle: FontStyle.italic),
              ),
            ],
          ],
        ),
        trailing: ElevatedButton(
          onPressed: () => _unblockUser(user.id),
          style: ElevatedButton.styleFrom(backgroundColor: Colors.grey),
          child: Text('Unblock'),
        ),
        isThreeLine: true,
      ),
    );
  }
}
```

## 🎨 UI/UX Guidelines

### User Blocking Interface

#### **Placement Options**
1. **User Profile Screen** - Primary location for block/unblock actions
2. **Chat Interface** - Quick access via user menu or long-press
3. **Settings > Blocked Users** - Management interface for blocked users
4. **Message Context Menu** - Block option in message actions

#### **Visual Design**
- **Block Button**: Red color (#DC3545), clear warning icon (🚫)
- **Unblock Button**: Gray/neutral color (#6C757D), less prominent
- **Confirmation Dialogs**: Clear warning messages with consequences
- **Loading States**: Disable buttons, show progress indicators

#### **User Experience Flow**
```
User Profile → Block User → Confirmation Dialog → Reason (Optional) → Block Action → Success Feedback
```

### Implementation Best Practices

#### **Error Handling**
- Show user-friendly error messages
- Provide retry options for network errors
- Handle offline scenarios gracefully
- Log errors for debugging

#### **Performance**
- Cache blocked user status locally
- Implement pagination for large blocked lists
- Use efficient state management
- Minimize API calls with smart caching

#### **Accessibility**
- Provide screen reader support
- Use semantic HTML elements
- Ensure keyboard navigation
- Maintain proper color contrast

## 🧪 Testing Guidelines

### Unit Testing

#### **Service Layer Tests**
```javascript
describe('UserBlockingService', () => {
  test('should block user successfully', async () => {
    const mockResponse = {
      id: 1,
      blockedUser: { id: 123, username: 'testuser' },
      blockedAt: '2024-01-15T10:30:00',
      reason: 'Test reason'
    };
    fetchMock.mockResponseOnce(JSON.stringify(mockResponse));

    const result = await userBlockingService.blockUser(123, 'Test reason');

    expect(result.success).toBe(true);
    expect(result.data.blockedUser.id).toBe(123);
  });

  test('should handle block error', async () => {
    fetchMock.mockRejectOnce(new Error('Network error'));

    const result = await userBlockingService.blockUser(123);

    expect(result.success).toBe(false);
    expect(result.error).toContain('Network error');
  });

  test('should unblock user successfully', async () => {
    fetchMock.mockResponseOnce('', { status: 204 });

    const result = await userBlockingService.unblockUser(123);

    expect(result.success).toBe(true);
  });

  test('should get blocked users list', async () => {
    const mockUsers = [
      { id: 1, blockedUser: { id: 123, username: 'user1' } },
      { id: 2, blockedUser: { id: 124, username: 'user2' } }
    ];
    fetchMock.mockResponseOnce(JSON.stringify(mockUsers));

    const result = await userBlockingService.getBlockedUsers();

    expect(result.success).toBe(true);
    expect(result.data).toHaveLength(2);
  });
});
```

#### **Component Tests**
```javascript
describe('BlockUserButton', () => {
  test('should show block dialog when clicked', async () => {
    render(<BlockUserButton userId={123} username="testuser" isBlocked={false} />);

    fireEvent.click(screen.getByText('Block User'));

    expect(screen.getByText('Block testuser?')).toBeInTheDocument();
  });

  test('should show unblock button when user is blocked', () => {
    render(<BlockUserButton userId={123} username="testuser" isBlocked={true} />);

    expect(screen.getByText('Unblock User')).toBeInTheDocument();
  });

  test('should call onBlockStatusChange after successful block', async () => {
    const mockCallback = jest.fn();
    fetchMock.mockResponseOnce(JSON.stringify({ id: 1, blockedUser: { id: 123 } }));

    render(<BlockUserButton userId={123} username="testuser" onBlockStatusChange={mockCallback} />);

    fireEvent.click(screen.getByText('Block User'));
    fireEvent.click(screen.getByText('Block User')); // Confirm

    await waitFor(() => {
      expect(mockCallback).toHaveBeenCalledWith(123, true);
    });
  });
});
```

### Integration Testing

#### **End-to-End Flows**
```javascript
describe('User Blocking Flow', () => {
  test('complete block user workflow', async () => {
    // 1. Navigate to user profile
    await navigateToUserProfile(123);

    // 2. Click block user
    await clickBlockUser();

    // 3. Enter reason and confirm
    await enterBlockReason('Inappropriate behavior');
    await confirmBlockAction();

    // 4. Verify user is blocked
    expect(await isUserBlocked(123)).toBe(true);

    // 5. Verify UI updates
    expect(await getBlockButtonText()).toBe('Unblock User');
  });

  test('blocked users list management', async () => {
    // 1. Navigate to blocked users list
    await navigateToBlockedUsersList();

    // 2. Verify blocked user appears
    expect(await getBlockedUsersList()).toContain('testuser');

    // 3. Unblock user
    await unblockUser('testuser');

    // 4. Verify user removed from list
    expect(await getBlockedUsersList()).not.toContain('testuser');
  });
});
```

## 🔧 Implementation Checklist

### Backend Integration
- [ ] Implement UserBlockingService class
- [ ] Add API endpoints configuration
- [ ] Handle authentication headers
- [ ] Implement error handling
- [ ] Add loading states management

### UI Components
- [ ] Block/Unblock button component
- [ ] Block confirmation dialog
- [ ] Blocked users list component
- [ ] Block status indicator
- [ ] Search functionality for blocked users

### User Experience
- [ ] Add block action to user profiles
- [ ] Add block action to chat interface
- [ ] Implement blocked users management screen
- [ ] Add block status checks in messaging
- [ ] Handle blocked user interactions gracefully

### Testing
- [ ] Unit tests for service methods
- [ ] Component tests for UI elements
- [ ] Integration tests for complete flows
- [ ] Error scenario testing
- [ ] Performance testing for large blocked lists

### Cross-Platform
- [ ] Web responsive design
- [ ] Mobile touch-friendly interface
- [ ] Consistent API integration
- [ ] Platform-specific UI patterns

## 🚀 Quick Start Guide

### 1. **Setup Service**
```javascript
const blockingService = new UserBlockingService('http://localhost:8080', authToken);
```

### 2. **Add Block Button**
```jsx
<BlockUserButton
  userId={123}
  username="john_doe"
  isBlocked={false}
  onBlockStatusChange={(userId, isBlocked) => {
    // Update UI state
  }}
/>
```

### 3. **Add Blocked Users List**
```jsx
<BlockedUsersList />
```

### 4. **Check Block Status**
```javascript
const isBlocked = await blockingService.isUserBlocked(123);
```

## 📚 Related Documentation

- [Backend User Blocking Implementation](./USER_BLOCKING_AND_DELETION_GUIDE.md)
- [Account Deletion Frontend Guide](./FRONTEND_ACCOUNT_DELETION_GUIDE.md)
- [API Endpoints Reference](./API_ENDPOINTS_REFERENCE.md)
- [Authentication Guide](./AUTHENTICATION_GUIDE.md)

---

*Last updated: January 2024*
*Version: 1.0*

**The User Blocking system is production-ready with complete backend support!** 🚀