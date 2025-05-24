# 📤 Complete Postman File Upload Testing Guide

## 🎯 **OVERVIEW**

This guide provides step-by-step instructions for testing file uploads using Postman with your chat application backend.

## 🔧 **PREREQUISITES**

- Postman installed on your computer
- Valid JWT token (obtained from login)
- Image files ready for testing
- Backend server running on `http://abusaker.zapto.org:8080`

## 📋 **STEP-BY-STEP POSTMAN SETUP**

### **Step 1: Get Authentication Token**

#### **1.1 Create Login Request**
- **Method**: `POST`
- **URL**: `http://abusaker.zapto.org:8080/api/auth/login`
- **Headers**:
  ```
  Content-Type: application/json
  ```
- **Body** (raw JSON):
  ```json
  {
    "username": "safinafi",
    "password": "your_password"
  }
  ```

#### **1.2 Execute Login & Copy Token**
1. Click **Send**
2. Copy the `accessToken` from the response
3. Save it for use in file upload requests

**Expected Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJzYWZpbmFmaSIsImlhdCI6MTc0ODA3Nzc3NiwiZXhwIjoxNzQ4MTY0MTc2fQ._O6Nbq5e79PdG2BRfon9VpYc7ppxlMeDA4ideQd0N9E",
  "tokenType": "Bearer",
  "expiresIn": 86400
}
```

### **Step 2: Setup File Upload Request**

#### **2.1 Create New Request**
- **Method**: `POST`
- **URL**: `http://abusaker.zapto.org:8080/api/files/upload`

#### **2.2 Configure Headers**
- **Key**: `Authorization`
- **Value**: `Bearer YOUR_ACCESS_TOKEN_HERE`

**Example:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJzYWZpbmFmaSIsImlhdCI6MTc0ODA3Nzc3NiwiZXhwIjoxNzQ4MTY0MTc2fQ._O6Nbq5e79PdG2BRfon9VpYc7ppxlMeDA4ideQd0N9E
```

#### **2.3 Configure Body (form-data)**
1. Select **Body** tab
2. Choose **form-data** option
3. Add the following fields:

**Field 1 - File:**
- **Key**: `file`
- **Type**: `File` (select from dropdown)
- **Value**: Click **Select Files** and choose your image

**Field 2 - Chat Room ID:**
- **Key**: `chatRoomId`
- **Type**: `Text`
- **Value**: `94` (or your actual chat room ID)

### **Step 3: Execute File Upload**

#### **3.1 Send Request**
1. Click **Send** button
2. Wait for response (may take a few seconds for large files)

#### **3.2 Expected Success Response**
```json
{
  "id": 17,
  "fileName": "test-image.jpg",
  "contentType": "image/jpeg",
  "fileUrl": "http://abusaker.zapto.org:8080/api/files/download/20250524-143022-test-image.jpg-a1b2c3d4.jpg",
  "fileSize": 245760,
  "uploadedAt": "2025-05-24T14:30:22.123456",
  "storageLocation": "images",
  "isDuplicate": false
}
```

#### **3.3 Copy File URL**
- Copy the `fileUrl` value from the response
- This URL will be used in WebSocket messages

## 🔍 **TESTING DIFFERENT FILE TYPES**

### **Image Files**
```
Supported: .jpg, .jpeg, .png, .gif
Max Size: 10MB
Content-Type: image/jpeg, image/png, image/gif
```

### **Document Files**
```
Supported: .pdf, .txt, .doc, .docx
Max Size: 10MB
Content-Type: application/pdf, text/plain, etc.
```

### **Example Test Files**
1. **Small Image**: `test-small.jpg` (< 1MB)
2. **Large Image**: `test-large.png` (5-10MB)
3. **PDF Document**: `test-document.pdf`
4. **Text File**: `test-file.txt`

## 🚨 **COMMON ERRORS & SOLUTIONS**

### **Error 1: 401 Unauthorized**
```json
{
  "error": "Unauthorized",
  "message": "JWT token is missing or invalid"
}
```
**Solution**: Check Authorization header format and token validity

### **Error 2: 403 Forbidden**
```json
{
  "error": "Access Denied",
  "message": "You don't have permission to access this resource"
}
```
**Solution**: Ensure user has proper role (USER or ADMIN)

### **Error 3: 400 Bad Request**
```json
{
  "error": "Bad Request",
  "message": "File is required"
}
```
**Solution**: Ensure file is selected in form-data

### **Error 4: 413 Payload Too Large**
```json
{
  "error": "Payload Too Large",
  "message": "File size exceeds maximum limit"
}
```
**Solution**: Use smaller file (< 10MB)

## 📊 **POSTMAN COLLECTION SETUP**

### **Create Collection**
1. Click **New** → **Collection**
2. Name: "Chat App File Upload Tests"
3. Add description: "Testing file upload functionality"

### **Add Requests to Collection**
1. **Login Request**
2. **File Upload Request**
3. **File Download Test**
4. **Debug Endpoints**

### **Collection Variables**
Set up variables for reuse:
- `baseUrl`: `http://abusaker.zapto.org:8080`
- `authToken`: `{{accessToken}}` (from login response)
- `chatRoomId`: `94`

## 🔄 **COMPLETE TESTING WORKFLOW**

### **Workflow 1: Basic File Upload Test**
1. **Login** → Get token
2. **Upload file** → Get file URL
3. **Verify file** → Check if accessible

### **Workflow 2: Integration Test**
1. **Login** → Get token
2. **Upload file** → Get file URL
3. **Send WebSocket message** with file URL
4. **Verify in chat** → Check if image displays

### **Workflow 3: Error Testing**
1. **Test without token** → Should get 401
2. **Test with invalid token** → Should get 401
3. **Test with large file** → Should get 413
4. **Test without file** → Should get 400

## 📝 **POSTMAN SCRIPTS**

### **Pre-request Script (for file upload)**
```javascript
// Auto-set authorization header from collection variable
pm.request.headers.add({
    key: 'Authorization',
    value: 'Bearer ' + pm.collectionVariables.get('authToken')
});
```

### **Test Script (for file upload)**
```javascript
// Test successful upload
pm.test("File upload successful", function () {
    pm.response.to.have.status(200);
});

pm.test("Response contains file URL", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData).to.have.property('fileUrl');

    // Save file URL for later use
    pm.collectionVariables.set('lastUploadedFileUrl', jsonData.fileUrl);
});

pm.test("File URL is valid", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.fileUrl).to.include('http://');
});
```

## 🎯 **EXPECTED RESULTS**

### **Successful Upload Response**
- ✅ Status: 200 OK
- ✅ Contains `fileUrl` field
- ✅ File accessible via download URL
- ✅ Proper content type detected

### **File URL Format**
```
http://abusaker.zapto.org:8080/api/files/download/YYYYMMDD-HHMMSS-filename-hash.ext
```

**Example:**
```
http://abusaker.zapto.org:8080/api/files/download/20250524-143022-test-image.jpg-a1b2c3d4.jpg
```

## 🔗 **NEXT STEPS AFTER UPLOAD**

1. **Copy the file URL** from the response
2. **Use in WebSocket message**:
   ```json
   {
     "content": "http://abusaker.zapto.org:8080/api/files/download/20250524-143022-test-image.jpg-a1b2c3d4.jpg",
     "contentType": "image/jpeg",
     "attachmentUrl": "http://abusaker.zapto.org:8080/api/files/download/20250524-143022-test-image.jpg-a1b2c3d4.jpg"
   }
   ```
3. **Verify image displays** in chat application

## 📞 **TROUBLESHOOTING**

### **If Upload Fails:**
1. Check server logs for detailed errors
2. Verify JWT token is not expired
3. Ensure file size is within limits
4. Check file type is supported

### **If File URL Doesn't Work:**
1. Test URL directly in browser
2. Check file permissions on server
3. Verify download endpoint is accessible

This guide ensures you can properly test file uploads and integrate them with your chat application! 🎉

## 📸 **POSTMAN SCREENSHOTS GUIDE**

### **Login Request Setup**
```
Method: POST
URL: http://abusaker.zapto.org:8080/api/auth/login
Headers Tab:
  Content-Type: application/json
Body Tab:
  Select: raw → JSON
  Content: {"username":"safinafi","password":"your_password"}
```

### **File Upload Request Setup**
```
Method: POST
URL: http://abusaker.zapto.org:8080/api/files/upload
Headers Tab:
  Authorization: Bearer YOUR_TOKEN_HERE
Body Tab:
  Select: form-data
  Row 1: Key=file, Type=File, Value=[Select Files]
  Row 2: Key=chatRoomId, Type=Text, Value=94
```

### **Success Indicators**
- ✅ Status: 200 OK (green)
- ✅ Response time: < 5 seconds
- ✅ Response size: > 0 bytes
- ✅ JSON response with fileUrl field

## 🎯 **QUICK TEST CHECKLIST**

- [ ] Login request returns access token
- [ ] File upload request has Authorization header
- [ ] File is selected in form-data
- [ ] chatRoomId is provided
- [ ] Response status is 200 OK
- [ ] Response contains valid fileUrl
- [ ] File URL is accessible in browser
- [ ] Ready to use URL in WebSocket messages

## 🔧 **POSTMAN ENVIRONMENT SETUP**

Create environment variables for easier testing:

```
Variable Name: baseUrl
Initial Value: http://abusaker.zapto.org:8080
Current Value: http://abusaker.zapto.org:8080

Variable Name: authToken
Initial Value:
Current Value: [Will be set from login response]

Variable Name: chatRoomId
Initial Value: 94
Current Value: 94
```

Use variables in requests:
- URL: `{{baseUrl}}/api/files/upload`
- Authorization: `Bearer {{authToken}}`
- chatRoomId: `{{chatRoomId}}`
