# 🚨 URGENT: Role Prefix Fix Required

## 🎯 **EXACT PROBLEM IDENTIFIED**

From your logs, I can see the user `safinafi` has **`[ROLE_ROLE_USER]`** authority instead of **`[ROLE_USER]`**.

**Root Cause:**
- User has `ROLE_USER` stored in database (incorrect)
- `CustomUserDetailsService` adds `ROLE_` prefix
- Result: `ROLE_ROLE_USER` (double prefix)
- `@PreAuthorize("hasRole('USER')")` expects `ROLE_USER`
- **Authorization fails** ❌

## 🔧 **IMMEDIATE FIX STEPS**

### **Step 1: Fix Database Roles**

Run this SQL command on your database:

```sql
UPDATE user_roles SET role = 'USER' WHERE role = 'ROLE_USER';
UPDATE user_roles SET role = 'ADMIN' WHERE role = 'ROLE_ADMIN';
```

### **Step 2: Restart Application**

After updating the database, restart your Spring Boot application.

### **Step 3: Verify Fix**

Check the logs after restart. You should see:
```
JWT Filter: Loaded UserDetails for safinafi, Authorities: [ROLE_USER]
```

Instead of:
```
JWT Filter: Loaded UserDetails for safinafi, Authorities: [ROLE_ROLE_USER]
```

## 📋 **VERIFICATION COMMANDS**

### **Check Database Roles:**
```sql
SELECT u.username, ur.role 
FROM users u 
JOIN user_roles ur ON u.id = ur.user_id 
WHERE u.username = 'safinafi';
```

**Expected Output:**
```
username | role
---------|-----
safinafi | USER
```

### **Test API After Fix:**
```bash
curl -X GET http://abusaker.zapto.org:8080/api/chatrooms \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Expected:** ✅ 200 OK with chat rooms data

## 🎯 **WHY THIS HAPPENED**

1. **Initial Registration:** User was created with `ROLE_USER` in database
2. **Our Fix:** We updated `AuthService` to store `USER` (without prefix)
3. **Existing Users:** Still had old `ROLE_USER` format
4. **Double Prefix:** `CustomUserDetailsService` added another `ROLE_`

## 🚀 **AUTOMATED FIX**

I've created migration scripts that will run automatically:

- **`fix-user-roles.sql`** - Manual SQL script
- **`src/main/resources/db/fix-role-prefixes.sql`** - Auto-migration
- **Updated `application.yml`** - Includes the migration

## ⚡ **QUICK TEST**

After applying the fix, test with this command:

```bash
# Test the debug endpoint
curl -X GET http://abusaker.zapto.org:8080/api/debug/auth-info \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJzYWZpbmFmaSIsImlhdCI6MTc0ODA3Nzc3NiwiZXhwIjoxNzQ4MTY0MTc2fQ._O6Nbq5e79PdG2BRfon9VpYc7ppxlMeDA4ideQd0N9E"
```

**Expected Response:**
```json
{
  "authenticated": true,
  "authorities": "[ROLE_USER]",
  "name": "safinafi"
}
```

## 🎉 **EXPECTED RESULT**

After the fix:
- ✅ Authentication works
- ✅ Authorization works  
- ✅ `/api/chatrooms` returns 200 OK
- ✅ All protected endpoints accessible
- ✅ Flutter app can fetch chat rooms

## 🔄 **IF STILL NOT WORKING**

If you still get 403 after the database fix:

1. **Clear application cache/restart**
2. **Generate new JWT token** (login again)
3. **Check logs** for the new authority format
4. **Verify database** was actually updated

The fix should resolve the issue immediately! 🎯
