# Profile Image Upload Troubleshooting Guide

## Issue: Profile Picture Not Saved to Database

If the profile image upload appears to work but the `profile_picture` column in the `users` table remains empty, follow this troubleshooting guide.

## 🔍 Diagnostic Steps

### 1. Check Database Schema

First, verify that the `profile_picture` column exists in the `users` table:

```sql
-- Connect to your MySQL database and run:
DESCRIBE users;

-- Or check specifically for the profile_picture column:
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'chatapp'
AND TABLE_NAME = 'users'
AND COLUMN_NAME = 'profile_picture';
```

### 2. Test with Debug Endpoint

Use the debug endpoint to test profile picture updates without file upload:

```bash
# Test setting a profile picture URL directly
curl -X POST "http://localhost:8080/api/users/me/profile-picture-debug" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d "url=/api/files/download/test-image.jpg"
```

### 3. Check Application Logs

Look for these log messages in your application logs:

```
PROFILE_IMAGE: Setting profile picture URL: /api/files/download/filename.jpg for user ID: 1
PROFILE_IMAGE: Profile image added successfully for user: username with URL: /api/files/download/filename.jpg
PROFILE_IMAGE: Verification - User username profile picture in DB: /api/files/download/filename.jpg
```

## 🛠️ Solutions

### Solution 1: Database Schema Fix

If the `profile_picture` column doesn't exist, create it manually:

```sql
-- Add the profile_picture column if it doesn't exist
ALTER TABLE users ADD COLUMN profile_picture VARCHAR(500) NULL 
COMMENT 'URL path to user profile image';
```

### Solution 2: Restart Application

The application includes an automatic migration script. Restart the application to ensure it runs:

1. Stop the application
2. Start the application
3. Check logs for migration execution

### Solution 3: Manual Database Update

If the automatic solutions don't work, update the database manually:

```sql
-- Test updating a user's profile picture directly
UPDATE users 
SET profile_picture = '/api/files/download/test-image.jpg' 
WHERE id = 1;

-- Verify the update
SELECT id, username, profile_picture FROM users WHERE id = 1;
```

### Solution 4: Check Transaction Configuration

Ensure that the transaction is properly committed. The updated code uses `saveAndFlush()` to force immediate database updates.

## 🧪 Testing Steps

### 1. Test Database Connection

```bash
# Test that you can connect to the database
mysql -u root -p chatapp

# Check if the users table exists
SHOW TABLES LIKE 'users';
```

### 2. Test Profile Image Upload

```bash
# Upload a profile image
curl -X POST "http://localhost:8080/api/users/me/profile-image" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@test-image.jpg"
```

### 3. Verify Database Update

```sql
-- Check if the profile_picture was updated
SELECT id, username, email, profile_picture, updated_at 
FROM users 
WHERE profile_picture IS NOT NULL;
```

## 🔧 Code Changes Made

### Enhanced Logging

Added detailed logging to track the profile picture update process:

```java
log.info("PROFILE_IMAGE: Setting profile picture URL: {} for user ID: {}", 
         profileImageUrl, currentUser.getId());
log.info("PROFILE_IMAGE: Profile image added successfully for user: {} with URL: {}", 
         updatedUser.getUsername(), updatedUser.getProfilePicture());
```

### Database Flush

Changed from `save()` to `saveAndFlush()` to ensure immediate database updates:

```java
// Old code
User updatedUser = userRepository.save(currentUser);

// New code
User updatedUser = userRepository.saveAndFlush(currentUser);
```

### Verification Query

Added verification to confirm the database update:

```java
User verifyUser = userRepository.findById(updatedUser.getId()).orElse(null);
if (verifyUser != null) {
    log.info("PROFILE_IMAGE: Verification - User {} profile picture in DB: {}", 
             verifyUser.getUsername(), verifyUser.getProfilePicture());
}
```

## 📋 Checklist

- [ ] Database `users` table has `profile_picture` column
- [ ] Application logs show profile picture URL being set
- [ ] Application logs show successful database save
- [ ] Verification query shows the profile picture in database
- [ ] API response includes the profile picture URL
- [ ] Frontend can display the profile image

## 🚨 Common Issues

### Issue 1: Column Doesn't Exist
**Symptoms:** Database error about unknown column
**Solution:** Run the migration script or create column manually

### Issue 2: Transaction Not Committed
**Symptoms:** Logs show success but database not updated
**Solution:** Use `saveAndFlush()` instead of `save()`

### Issue 3: Wrong Database Connection
**Symptoms:** Updates appear successful but wrong database
**Solution:** Verify `application.yml` database configuration

### Issue 4: Caching Issues
**Symptoms:** Old data returned even after update
**Solution:** Clear JPA cache or restart application

## 📞 Support Commands

### Check Database Configuration
```bash
# Check which database the application is connecting to
grep -A 5 "datasource:" src/main/resources/application.yml
```

### Check Table Structure
```sql
-- Get complete table structure
SHOW CREATE TABLE users;
```

### Check Recent Updates
```sql
-- Check for recent profile picture updates
SELECT id, username, profile_picture, 
       CASE WHEN profile_picture IS NOT NULL THEN 'HAS_IMAGE' ELSE 'NO_IMAGE' END as status
FROM users 
ORDER BY id;
```

## 🔄 Next Steps

1. **Test the debug endpoint** to isolate the issue
2. **Check application logs** for detailed error information
3. **Verify database schema** and column existence
4. **Test with manual SQL updates** to confirm database connectivity
5. **Contact support** if issues persist with full log output
