# Setting Up a Postman Collection for Chat Application Testing

This guide will help you create a structured Postman collection to test the Chat Application API efficiently.

## Table of Contents

1. [Creating the Collection](#creating-the-collection)
2. [Setting Up Environment Variables](#setting-up-environment-variables)
3. [Organizing Requests](#organizing-requests)
4. [Using Tests for Automation](#using-tests-for-automation)
5. [Exporting and Sharing](#exporting-and-sharing)

## Creating the Collection

1. Open Postman and click on "Collections" in the sidebar
2. Click the "+" button to create a new collection
3. Name it "Chat Application API"
4. Add a description (optional)
5. Click "Create"

## Setting Up Environment Variables

1. Click on "Environments" in the sidebar
2. Click the "+" button to create a new environment
3. Name it "Chat Application Local"
4. Add the following variables:
   - `baseUrl`: `http://localhost:8080`
   - `token`: (leave empty for now)
   - `refreshToken`: (leave empty for now)
   - `userId`: (leave empty for now)
   - `chatRoomId`: (leave empty for now)
5. Click "Save"
6. Select the environment from the dropdown in the top-right corner

## Organizing Requests

Organize your requests into folders for better management:

### 1. Authentication Folder

Create a folder named "Authentication" and add the following requests:

#### Register
- Method: POST
- URL: `{{baseUrl}}/api/auth/register`
- Headers: `Content-Type: application/json`
- Body:
  ```json
  {
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123",
    "fullName": "Test User"
  }
  ```

#### Login
- Method: POST
- URL: `{{baseUrl}}/api/auth/login`
- Headers: `Content-Type: application/json`
- Body:
  ```json
  {
    "usernameOrEmail": "testuser",
    "password": "password123"
  }
  ```
  > Note: You can use either a username or an email address in the `usernameOrEmail` field.
- Tests:
  ```javascript
  // Save the token to environment
  var jsonData = pm.response.json();
  pm.environment.set("token", jsonData.accessToken);
  pm.environment.set("refreshToken", jsonData.refreshToken);
  pm.environment.set("userId", jsonData.user.id);
  ```

#### Refresh Token
- Method: POST
- URL: `{{baseUrl}}/api/auth/refresh`
- Headers:
  - `Content-Type: application/json`
  - `Authorization: Bearer {{token}}`
- Body:
  ```json
  {
    "refreshToken": "{{refreshToken}}"
  }
  ```
- Tests:
  ```javascript
  var jsonData = pm.response.json();
  pm.environment.set("token", jsonData.accessToken);
  pm.environment.set("refreshToken", jsonData.refreshToken);
  ```

#### Logout
- Method: POST
- URL: `{{baseUrl}}/api/auth/logout`
- Headers: `Authorization: Bearer {{token}}`

### 2. Users Folder

Create a folder named "Users" and add the following requests:

#### Get Current User
- Method: GET
- URL: `{{baseUrl}}/api/users/me`
- Headers: `Authorization: Bearer {{token}}`

#### Update Profile
- Method: PUT
- URL: `{{baseUrl}}/api/users/me`
- Headers:
  - `Content-Type: application/json`
  - `Authorization: Bearer {{token}}`
- Body:
  ```json
  {
    "fullName": "Updated Name",
    "profilePicture": "https://example.com/avatar.jpg"
  }
  ```

#### Get All Users
- Method: GET
- URL: `{{baseUrl}}/api/users`
- Headers: `Authorization: Bearer {{token}}`

### 3. Chat Rooms Folder

Create a folder named "Chat Rooms" and add the following requests:

#### Create Chat Room
- Method: POST
- URL: `{{baseUrl}}/api/chatrooms`
- Headers:
  - `Content-Type: application/json`
  - `Authorization: Bearer {{token}}`
- Body:
  ```json
  {
    "name": "Test Chat Room",
    "isPrivate": false,
    "participantIds": []
  }
  ```
- Tests:
  ```javascript
  var jsonData = pm.response.json();
  pm.environment.set("chatRoomId", jsonData.id);
  ```

#### Get All Chat Rooms
- Method: GET
- URL: `{{baseUrl}}/api/chatrooms`
- Headers: `Authorization: Bearer {{token}}`

#### Get Chat Room by ID
- Method: GET
- URL: `{{baseUrl}}/api/chatrooms/{{chatRoomId}}`
- Headers: `Authorization: Bearer {{token}}`

### 4. Messages Folder

Create a folder named "Messages" and add the following requests:

#### Send Message
- Method: POST
- URL: `{{baseUrl}}/api/messages`
- Headers:
  - `Content-Type: application/json`
  - `Authorization: Bearer {{token}}`
- Body:
  ```json
  {
    "chatRoomId": {{chatRoomId}},
    "content": "Hello, this is a test message!",
    "contentType": "TEXT"
  }
  ```

#### Get Messages for Chat Room
- Method: GET
- URL: `{{baseUrl}}/api/messages/chatroom/{{chatRoomId}}?page=0&size=20`
- Headers: `Authorization: Bearer {{token}}`

## Using Tests for Automation

Add test scripts to automate your testing workflow:

### Pre-request Script for Authentication

Add this script to requests that require authentication:

```javascript
// Check if token exists and is valid
if (!pm.environment.get("token")) {
    console.log("No token found, logging in...");

    // Create a login request
    const loginRequest = {
        url: pm.environment.get("baseUrl") + '/api/auth/login',
        method: 'POST',
        header: {
            'Content-Type': 'application/json'
        },
        body: {
            mode: 'raw',
            raw: JSON.stringify({
                username: "testuser",
                password: "password123"
            })
        }
    };

    // Send the login request
    pm.sendRequest(loginRequest, function (err, res) {
        if (err) {
            console.error(err);
        } else {
            const jsonData = res.json();
            pm.environment.set("token", jsonData.accessToken);
            pm.environment.set("refreshToken", jsonData.refreshToken);
            pm.environment.set("userId", jsonData.user.id);
        }
    });
}
```

## Exporting and Sharing

To export your collection:

1. Click on the three dots (...) next to your collection name
2. Select "Export"
3. Choose "Collection v2.1" format
4. Save the JSON file

To export your environment:

1. Click on the three dots (...) next to your environment name
2. Select "Export"
3. Save the JSON file

You can share these files with your team members, who can then import them into their Postman.
