# 🔧 JWT Authentication Troubleshooting Guide

## 🚨 Current Issue: 403 Forbidden on Protected Endpoints

You're getting a **403 Forbidden** error when accessing `/api/messages/chatroom/92` even with a valid JWT token. This guide will help you diagnose and fix the issue.

## 🔍 Step-by-Step Debugging Process

### **Step 1: Test Authentication Without Authorization**

First, test if the JWT token is being processed correctly:

```bash
# Test the debug endpoint (no authentication required)
curl -X GET http://localhost:8080/api/debug/auth-info \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Expected Response:**
```json
{
  "authenticated": true,
  "principal": "org.springframework.security.core.userdetails.User [Username=testuser, Password=[PROTECTED], Enabled=true, AccountNonExpired=true, CredentialsNonExpired=true, AccountNonLocked=true, Granted Authorities=[ROLE_USER]]",
  "authorities": "[ROLE_USER]",
  "name": "testuser"
}
```

### **Step 2: Test Role-Based Authorization**

Test if the USER role is working:

```bash
# Test USER role endpoint
curl -X GET http://localhost:8080/api/debug/test-user-role \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Expected Response:**
```json
{
  "message": "USER role test passed!",
  "user": "testuser",
  "authorities": "[ROLE_USER]"
}
```

### **Step 3: Check Application Logs**

With debug logging enabled, check the logs for:

1. **JWT Filter Processing:**
```
JWT Filter: Processing request to /api/messages/chatroom/92
JWT Filter: Extracted JWT token (length: XXX)
JWT Filter: Extracted username: testuser
JWT Filter: Loaded UserDetails for testuser, Authorities: [ROLE_USER]
JWT Filter: Authentication set for user: testuser with authorities: [ROLE_USER]
```

2. **Spring Security Access Decision:**
```
DEBUG o.s.s.access.vote.AffirmativeBased - Voter: org.springframework.security.access.prepost.PreInvocationAuthorizationAdviceVoter@xxx, returned: 1
```

### **Step 4: Verify Database Roles**

Check if users have correct roles in the database:

```sql
SELECT u.username, ur.role 
FROM users u 
JOIN user_roles ur ON u.id = ur.user_id 
WHERE u.username = 'YOUR_USERNAME';
```

**Expected Output:**
```
username | role
---------|-----
testuser | USER
```

## 🛠️ Common Issues and Fixes

### **Issue 1: Token Not Being Extracted**

**Symptoms:**
- Log shows: `JWT Filter: No Bearer token found`
- 403 Forbidden error

**Fix:**
Ensure the Authorization header is correctly formatted:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### **Issue 2: Username Extraction Fails**

**Symptoms:**
- Log shows: `JWT Filter: Could not extract username from token`
- Token appears valid but username is null

**Fix:**
Check if the JWT secret in `application.yml` matches the one used to generate the token.

### **Issue 3: User Not Found in Database**

**Symptoms:**
- Log shows: `UsernameNotFoundException`
- JWT token is valid but user doesn't exist

**Fix:**
Ensure the user exists in the database and has the correct roles.

### **Issue 4: Role Prefix Mismatch**

**Symptoms:**
- Authentication succeeds but authorization fails
- User has authorities but `@PreAuthorize("hasRole('USER')")` fails

**Fix:**
Verify that:
1. Database stores roles as `"USER"` (no prefix)
2. `CustomUserDetailsService` adds `"ROLE_"` prefix
3. `@PreAuthorize` uses `hasRole('USER')` (not `hasRole('ROLE_USER')`)

### **Issue 5: Token Expired**

**Symptoms:**
- Log shows: `JWT Filter: Invalid token for user: username`
- Token was valid before but now fails

**Fix:**
Generate a new token by logging in again.

## 🔧 Enhanced Debugging

### **Enable Maximum Debug Logging**

Add to `application.yml`:
```yaml
logging:
  level:
    "[com.chatapp.security]": TRACE
    "[org.springframework.security]": DEBUG
    "[org.springframework.security.access]": DEBUG
    "[org.springframework.security.web]": DEBUG
```

### **Check JWT Token Content**

Decode your JWT token at [jwt.io](https://jwt.io) to verify:
1. **Header**: Algorithm is HS256
2. **Payload**: Contains correct username and expiration
3. **Signature**: Verify with your JWT secret

### **Test with Postman/Insomnia**

1. **Login Request:**
```
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "testuser",
  "password": "password"
}
```

2. **Copy the `accessToken` from response**

3. **Test Protected Endpoint:**
```
GET http://localhost:8080/api/debug/test-user-role
Authorization: Bearer YOUR_ACCESS_TOKEN
```

## 🎯 Quick Fix Checklist

- [ ] JWT token is properly formatted with "Bearer " prefix
- [ ] Token is not expired (check expiration in JWT payload)
- [ ] User exists in database with correct username
- [ ] User has "USER" role in `user_roles` table
- [ ] `CustomUserDetailsService` adds "ROLE_" prefix correctly
- [ ] JWT secret in config matches the one used to generate token
- [ ] Debug logs show successful authentication and role assignment
- [ ] Spring Security debug logs show successful authorization

## 🚀 Expected Working Flow

1. **Client sends request** with `Authorization: Bearer TOKEN`
2. **JWT Filter extracts token** and validates it
3. **UserDetailsService loads user** with authorities `[ROLE_USER]`
4. **Spring Security sets authentication** in SecurityContext
5. **@PreAuthorize("hasRole('USER')")** matches `ROLE_USER` authority
6. **Request proceeds** to controller method
7. **Response returned** successfully

## 📞 If Still Not Working

If you're still getting 403 errors after following this guide:

1. **Share the debug logs** from the JWT filter
2. **Verify the JWT token payload** at jwt.io
3. **Check the database** for user and role data
4. **Test the debug endpoints** first before the actual endpoints

The issue is likely in one of these areas:
- JWT token format or content
- Database role configuration
- Spring Security role matching
- Token expiration or validation
