# 🔧 Video Upload NonUniqueResultException Fix

## 🚨 **ISSUE IDENTIFIED**

**Problem**: Video file uploads failing intermittently with error:
```
Exception: An error occurred while uploading the file: query did not return a unique result: 2; 
nested exception is javax.persistence.NonUniqueResultException: query did not return a unique result: 2
```

**Root Cause**: Multiple files with the same MD5 hash exist in the database, causing `findByFileHash()` to return multiple results when it expects only one.

## 🔍 **TECHNICAL ANALYSIS**

### **The Problem:**
1. **Database State**: Multiple `FileMetadata` entries exist with identical `fileHash` values
2. **Query Expectation**: `findByFileHash()` returns `Optional<FileMetadata>` (expects 0 or 1 result)
3. **Actual Result**: Query finds 2+ records with same hash
4. **JPA Behavior**: Throws `NonUniqueResultException` when multiple results found for single-result query

### **Why This Happens:**
- **Duplicate File Uploads**: Same file uploaded multiple times
- **Hash Collisions**: Different files with same MD5 hash (rare but possible)
- **Race Conditions**: Concurrent uploads of same file
- **Database Inconsistency**: Duplicate detection logic failed previously

## ✅ **COMPLETE FIX APPLIED**

### **1. Updated FileMetadataService.registerFile()**

**Before (Problematic Code):**
```java
// This could throw NonUniqueResultException
Optional<FileMetadata> existingFile = fileMetadataRepository.findByFileHash(fileHash);
if (existingFile.isPresent()) {
    // Handle duplicate...
    .originalFileId(existingFile.get().getId())
}
```

**After (Fixed Code):**
```java
// This safely handles multiple results
List<FileMetadata> existingFiles = fileMetadataRepository.findAllByFileHash(fileHash);
if (!existingFiles.isEmpty()) {
    // Use the first existing file as the original
    FileMetadata originalFile = existingFiles.get(0);
    // Handle duplicate...
    .originalFileId(originalFile.getId())
}
```

### **2. Updated FileMetadataService.scanFilesystem()**

**Before:**
```java
Optional<FileMetadata> existingFile = fileMetadataRepository.findByFileHash(fileHash);
if (existingFile.isEmpty()) {
```

**After:**
```java
List<FileMetadata> existingFiles = fileMetadataRepository.findAllByFileHash(fileHash);
if (existingFiles.isEmpty()) {
```

## 🎯 **BENEFITS OF THE FIX**

### **✅ Immediate Benefits:**
1. **No More Crashes**: `NonUniqueResultException` eliminated
2. **Consistent Behavior**: File uploads work reliably
3. **Proper Duplicate Handling**: Multiple files with same hash handled correctly
4. **Graceful Degradation**: Uses first found file as original for duplicates

### **✅ Long-term Benefits:**
1. **Scalability**: Handles large numbers of duplicate files
2. **Data Integrity**: Maintains proper relationships between duplicates
3. **Performance**: Avoids query failures and retries
4. **Robustness**: Resilient to database inconsistencies

## 🧪 **TESTING THE FIX**

### **Test 1: Upload Same Video Multiple Times**
```bash
# Upload the same video file 3 times
curl -X POST http://abusaker.zapto.org:8080/api/files/upload \
  -H "Authorization: Bearer JWT_TOKEN" \
  -F "file=@video.mp4" \
  -F "chatRoomId=95"
```

**Expected Result:**
- ✅ First upload: Creates original file entry
- ✅ Second upload: Creates duplicate entry with `isDuplicate=true`
- ✅ Third upload: Creates another duplicate entry
- ✅ No `NonUniqueResultException` thrown

### **Test 2: Concurrent Video Uploads**
```bash
# Upload same file simultaneously from multiple terminals
curl -X POST ... & curl -X POST ... & curl -X POST ...
```

**Expected Result:**
- ✅ All uploads succeed
- ✅ Proper duplicate relationships maintained
- ✅ No database constraint violations

### **Test 3: Large Video Files**
```bash
# Upload large video files (5-10MB)
curl -X POST http://abusaker.zapto.org:8080/api/files/upload \
  -H "Authorization: Bearer JWT_TOKEN" \
  -F "file=@large-video.mp4"
```

**Expected Result:**
- ✅ Upload completes successfully
- ✅ File metadata registered correctly
- ✅ Download URL works

## 🔍 **DATABASE CLEANUP (OPTIONAL)**

If you want to clean up existing duplicate entries:

### **1. Find Duplicate Files**
```sql
SELECT fileHash, COUNT(*) as count 
FROM file_metadata 
GROUP BY fileHash 
HAVING COUNT(*) > 1;
```

### **2. Mark Duplicates Properly**
```sql
-- Update duplicate entries to reference the first occurrence as original
UPDATE file_metadata fm1 
SET isDuplicate = true, 
    originalFileId = (
        SELECT MIN(id) 
        FROM file_metadata fm2 
        WHERE fm2.fileHash = fm1.fileHash
    )
WHERE fm1.id NOT IN (
    SELECT MIN(id) 
    FROM file_metadata fm3 
    GROUP BY fm3.fileHash
);
```

### **3. Verify Cleanup**
```sql
SELECT 
    fileHash, 
    COUNT(*) as total_files,
    SUM(CASE WHEN isDuplicate = false THEN 1 ELSE 0 END) as originals,
    SUM(CASE WHEN isDuplicate = true THEN 1 ELSE 0 END) as duplicates
FROM file_metadata 
GROUP BY fileHash 
HAVING COUNT(*) > 1;
```

## 📊 **MONITORING & PREVENTION**

### **1. Add Logging for Duplicate Detection**
The fix includes enhanced logging:
```
[registerFile] Checking for duplicates with hash: abc123...
[registerFile] Duplicate file detected: /path/to/file (hash: abc123...)
[registerFile] Created duplicate file metadata: fileName=video.mp4, ...
```

### **2. Monitor Duplicate Rates**
```sql
-- Check duplicate percentage
SELECT 
    COUNT(*) as total_files,
    SUM(CASE WHEN isDuplicate = true THEN 1 ELSE 0 END) as duplicates,
    ROUND(100.0 * SUM(CASE WHEN isDuplicate = true THEN 1 ELSE 0 END) / COUNT(*), 2) as duplicate_percentage
FROM file_metadata;
```

### **3. Regular Cleanup Job**
Consider adding a scheduled task to clean up old duplicate files:
```java
@Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
public void cleanupDuplicateFiles() {
    int removed = fileMetadataService.removeDuplicateFiles();
    log.info("Cleanup job removed {} duplicate files", removed);
}
```

## 🚨 **TROUBLESHOOTING**

### **If Issue Persists:**

1. **Check Database State**
   ```sql
   SELECT fileHash, COUNT(*) FROM file_metadata GROUP BY fileHash HAVING COUNT(*) > 1;
   ```

2. **Verify Repository Method**
   ```java
   // Ensure this method exists in FileMetadataRepository
   List<FileMetadata> findAllByFileHash(String fileHash);
   ```

3. **Check Application Logs**
   ```
   Look for: "[registerFile] Checking for duplicates with hash:"
   Should see: List<FileMetadata> instead of Optional<FileMetadata>
   ```

4. **Test with Simple File**
   ```bash
   # Try uploading a small text file first
   echo "test" > test.txt
   curl -X POST ... -F "file=@test.txt"
   ```

## ✅ **VERIFICATION CHECKLIST**

After applying the fix:

- [ ] `FileMetadataService.registerFile()` uses `findAllByFileHash()`
- [ ] `FileMetadataService.scanFilesystem()` uses `findAllByFileHash()`
- [ ] Video uploads work consistently
- [ ] No `NonUniqueResultException` in logs
- [ ] Duplicate files are properly marked
- [ ] File download URLs work correctly
- [ ] Database integrity maintained

## 🎉 **EXPECTED RESULTS**

### **✅ Before Fix (Broken):**
```
Exception: NonUniqueResultException: query did not return a unique result: 2
File upload failed
User sees error message
```

### **✅ After Fix (Working):**
```
[registerFile] Duplicate file detected: video.mp4 (hash: abc123...)
[registerFile] Created duplicate file metadata: fileName=video.mp4-xyz.mp4
File upload successful
User gets download URL
```

## 🔄 **DEPLOYMENT STEPS**

1. **Apply the code changes** (already done)
2. **Restart the application** to load the updated service
3. **Test video uploads** to verify the fix
4. **Monitor logs** for successful duplicate handling
5. **Optional**: Run database cleanup if needed

## 📞 **SUPPORT**

If you encounter any issues after applying this fix:

1. **Check application logs** for detailed error messages
2. **Verify database state** using the provided SQL queries
3. **Test with different file types** to isolate the issue
4. **Monitor system resources** during large file uploads

---

**Fix Date**: January 2025  
**Status**: ✅ COMPLETE  
**Impact**: Eliminates NonUniqueResultException for video uploads  
**Next Action**: Test video uploads to verify the fix works
