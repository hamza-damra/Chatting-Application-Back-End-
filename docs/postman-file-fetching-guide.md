# 📥 Complete Postman File Fetching Guide

## 🎯 **OVERVIEW**

This guide provides step-by-step instructions for fetching/downloading files using Postman with your chat application backend.

## 🔧 **PREREQUISITES**

- Postman installed on your computer
- Valid JWT token (obtained from login)
- Knowledge of file URLs or filenames to fetch
- Backend server running on `http://abusaker.zapto.org:8080`

## 📋 **AVAILABLE FILE ENDPOINTS**

### **1. Download File by Filename**
```
GET /api/files/download/{filename}
```

### **2. Get File Upload Status**
```
GET /api/files/status
```

### **3. Get File Debug Information**
```
GET /api/debug/file-upload-info
```

## 📥 **STEP-BY-STEP FILE FETCHING**

### **Step 1: Get Authentication Token**

First, obtain a valid JWT token (if you don't have one):

#### **1.1 Login Request**
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

#### **1.2 Copy Access Token**
From the response, copy the `accessToken` value for use in file requests.

### **Step 2: Download File by Filename**

#### **2.1 Setup Download Request**
- **Method**: `GET`
- **URL**: `http://abusaker.zapto.org:8080/api/files/download/{filename}`
- **Replace `{filename}`** with actual filename

**Example URLs:**
```
http://abusaker.zapto.org:8080/api/files/download/20250524-143022-image.jpg-a1b2c3d4.jpg
http://abusaker.zapto.org:8080/api/files/download/20250524-150000-document.pdf-b2c3d4e5.pdf
```

#### **2.2 Configure Headers (Optional)**
For most file downloads, no authentication is required, but if needed:
- **Key**: `Authorization`
- **Value**: `Bearer YOUR_ACCESS_TOKEN`

#### **2.3 Send Request**
1. Click **Send** button
2. File should download or display in Postman

### **Step 3: Handle Different File Types**

#### **3.1 Image Files (.jpg, .png, .gif)**
- **Expected Response**: Binary image data
- **Content-Type**: `image/jpeg`, `image/png`, `image/gif`
- **Postman Display**: Image preview in response body

#### **3.2 Document Files (.pdf, .txt, .doc)**
- **Expected Response**: Binary document data
- **Content-Type**: `application/pdf`, `text/plain`, etc.
- **Postman Display**: PDF viewer or text content

#### **3.3 Audio Files (.mp3, .wav)**
- **Expected Response**: Binary audio data
- **Content-Type**: `audio/mpeg`, `audio/wav`
- **Postman Display**: Audio player controls

#### **3.4 Video Files (.mp4, .mov)**
- **Expected Response**: Binary video data
- **Content-Type**: `video/mp4`, `video/mpeg`
- **Postman Display**: Video player controls

## 🔍 **FINDING FILE URLS**

### **Method 1: From Upload Response**
When you upload a file, the response contains the download URL:

```json
{
  "fileUrl": "http://abusaker.zapto.org:8080/api/files/download/20250524-143022-image.jpg-a1b2c3d4.jpg",
  "downloadUrl": "/api/files/download/20250524-143022-image.jpg-a1b2c3d4.jpg"
}
```

### **Method 2: From Chat Messages**
File URLs appear in chat message content:

```json
{
  "content": "http://abusaker.zapto.org:8080/api/files/download/20250524-143022-image.jpg-a1b2c3d4.jpg",
  "contentType": "image/jpeg"
}
```

### **Method 3: From Database Query**
Query the database for file metadata:

```sql
SELECT fileName, filePath FROM file_metadata WHERE uploadedBy_id = 8;
```

## 🧪 **TESTING DIFFERENT SCENARIOS**

### **Test 1: Valid File Download**
```
GET http://abusaker.zapto.org:8080/api/files/download/20250524-143022-image.jpg-a1b2c3d4.jpg
```
**Expected**: 200 OK with file content

### **Test 2: Non-existent File**
```
GET http://abusaker.zapto.org:8080/api/files/download/nonexistent-file.jpg
```
**Expected**: 404 Not Found

### **Test 3: Invalid Filename Format**
```
GET http://abusaker.zapto.org:8080/api/files/download/invalid-filename
```
**Expected**: 404 Not Found

### **Test 4: Large File Download**
```
GET http://abusaker.zapto.org:8080/api/files/download/large-video.mp4
```
**Expected**: 200 OK (may take longer to load)

## 📊 **RESPONSE ANALYSIS**

### **✅ Successful Download (200 OK)**
```
Status: 200 OK
Content-Type: image/jpeg (or appropriate type)
Content-Length: 245760
Content-Disposition: inline; filename="image.jpg"

[Binary file data]
```

### **❌ File Not Found (404)**
```
Status: 404 Not Found
Content-Type: application/json

{
  "error": "File not found",
  "message": "The requested file does not exist"
}
```

### **❌ Server Error (500)**
```
Status: 500 Internal Server Error
Content-Type: application/json

{
  "error": "Internal Server Error",
  "message": "Error accessing file"
}
```

## 🔧 **POSTMAN COLLECTION SETUP**

### **Create File Fetching Collection**
1. **New Collection**: "File Fetching Tests"
2. **Add Variables**:
   ```
   baseUrl: http://abusaker.zapto.org:8080
   authToken: {{accessToken}}
   sampleFilename: 20250524-143022-image.jpg-a1b2c3d4.jpg
   ```

### **Collection Requests**
1. **Login** → Get auth token
2. **Download Sample File** → Test basic download
3. **Download Non-existent File** → Test error handling
4. **Get File Status** → Check system status

## 📝 **POSTMAN SCRIPTS**

### **Pre-request Script (for authenticated requests)**
```javascript
// Auto-set authorization header if needed
if (pm.collectionVariables.get('authToken')) {
    pm.request.headers.add({
        key: 'Authorization',
        value: 'Bearer ' + pm.collectionVariables.get('authToken')
    });
}
```

### **Test Script (for file downloads)**
```javascript
// Test successful download
pm.test("File download successful", function () {
    pm.response.to.have.status(200);
});

pm.test("Response has correct content type", function () {
    const contentType = pm.response.headers.get('Content-Type');
    pm.expect(contentType).to.exist;
});

pm.test("Response has file content", function () {
    pm.expect(pm.response.responseSize).to.be.above(0);
});

// Log file information
console.log("Content-Type:", pm.response.headers.get('Content-Type'));
console.log("Content-Length:", pm.response.headers.get('Content-Length'));
console.log("File Size:", pm.response.responseSize + " bytes");
```

## 🎯 **COMMON USE CASES**

### **1. Verify File Upload**
After uploading a file, immediately test the download URL to ensure it works.

### **2. Debug Broken Images**
If images aren't displaying in the chat app, test the URLs directly in Postman.

### **3. Check File Accessibility**
Verify that files are properly accessible and not corrupted.

### **4. Test File Permissions**
Ensure files can be accessed without authentication (if intended).

## 🚨 **TROUBLESHOOTING**

### **File Not Found (404)**
- ✅ Check filename spelling and format
- ✅ Verify file was actually uploaded
- ✅ Check database for file metadata
- ✅ Ensure file exists on filesystem

### **Access Denied (403)**
- ✅ Add Authorization header if required
- ✅ Check JWT token validity
- ✅ Verify user permissions

### **Server Error (500)**
- ✅ Check server logs for detailed errors
- ✅ Verify file system permissions
- ✅ Check disk space availability

### **Slow Downloads**
- ✅ Check file size (large files take longer)
- ✅ Verify network connection
- ✅ Check server performance

## 📱 **INTEGRATION TESTING**

### **End-to-End Workflow**
1. **Upload file** via POST `/api/files/upload`
2. **Get file URL** from upload response
3. **Download file** via GET `/api/files/download/{filename}`
4. **Verify file integrity** (size, content type)

### **Chat Integration Test**
1. **Upload file** and get URL
2. **Send WebSocket message** with file URL
3. **Download file** using URL from message
4. **Verify file displays** correctly

## ✅ **VERIFICATION CHECKLIST**

- [ ] Can download recently uploaded files
- [ ] Different file types download correctly
- [ ] File content matches original upload
- [ ] Proper content types are returned
- [ ] Error handling works for invalid files
- [ ] Large files download without issues
- [ ] File URLs work in browser and Postman

## 🎉 **CONCLUSION**

This guide provides comprehensive instructions for fetching files using Postman. Use these techniques to:

- ✅ **Test file download functionality**
- ✅ **Debug file access issues**
- ✅ **Verify file integrity**
- ✅ **Validate file URLs**
- ✅ **Troubleshoot upload/download problems**

**Your file fetching system should work perfectly with these testing methods!** 🚀

## 📸 **POSTMAN SCREENSHOTS GUIDE**

### **File Download Request Setup**
```
Method: GET
URL: http://abusaker.zapto.org:8080/api/files/download/filename.jpg
Headers Tab: (Usually no headers needed for public files)
```

### **Success Response Indicators**
- ✅ **Status**: 200 OK (green)
- ✅ **Response Time**: < 5 seconds (depending on file size)
- ✅ **Response Size**: > 0 bytes
- ✅ **Content-Type**: Matches file type (image/jpeg, application/pdf, etc.)
- ✅ **Body**: Shows file preview or binary data

### **File Type Response Examples**

#### **Image File Response**
```
Status: 200 OK
Content-Type: image/jpeg
Content-Length: 245760
Body: [Image preview displayed in Postman]
```

#### **PDF File Response**
```
Status: 200 OK
Content-Type: application/pdf
Content-Length: 1048576
Body: [PDF viewer or download option]
```

#### **Text File Response**
```
Status: 200 OK
Content-Type: text/plain
Content-Length: 1024
Body: [Text content displayed]
```

## 🔗 **QUICK REFERENCE URLS**

### **Common File Download Patterns**
```
Images:
GET /api/files/download/20250524-143022-photo.jpg-a1b2c3d4.jpg
GET /api/files/download/20250524-143022-screenshot.png-b2c3d4e5.png

Documents:
GET /api/files/download/20250524-143022-report.pdf-c3d4e5f6.pdf
GET /api/files/download/20250524-143022-notes.txt-d4e5f6g7.txt

Media:
GET /api/files/download/20250524-143022-song.mp3-e5f6g7h8.mp3
GET /api/files/download/20250524-143022-video.mp4-f6g7h8i9.mp4
```

### **System Endpoints**
```
File Status:
GET /api/files/status

Debug Info:
GET /api/debug/file-upload-info

Authentication:
POST /api/auth/login
```

## 🎯 **QUICK TEST COMMANDS**

### **Test File Download (cURL)**
```bash
# Download image file
curl -X GET "http://abusaker.zapto.org:8080/api/files/download/20250524-143022-image.jpg-a1b2c3d4.jpg" \
  -o downloaded-image.jpg

# Download with headers
curl -X GET "http://abusaker.zapto.org:8080/api/files/download/filename.pdf" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -o downloaded-file.pdf

# Check file info only (headers)
curl -I "http://abusaker.zapto.org:8080/api/files/download/filename.jpg"
```

### **Browser Testing**
Simply paste the file URL in your browser:
```
http://abusaker.zapto.org:8080/api/files/download/20250524-143022-image.jpg-a1b2c3d4.jpg
```

## 📋 **TESTING CHECKLIST**

### **Basic Functionality**
- [ ] Can access file download endpoint
- [ ] Files download with correct content type
- [ ] File content matches original upload
- [ ] Different file types work (images, PDFs, etc.)

### **Error Handling**
- [ ] Non-existent files return 404
- [ ] Invalid filenames return 404
- [ ] Server errors return 500 with proper message

### **Performance**
- [ ] Small files (< 1MB) download quickly
- [ ] Large files (5-10MB) download successfully
- [ ] Multiple concurrent downloads work

### **Integration**
- [ ] URLs from upload response work
- [ ] URLs in chat messages work
- [ ] Files accessible from different clients

---

**Created**: January 2025
**Status**: ✅ COMPLETE
**Next Action**: Test file downloads using the provided methods
