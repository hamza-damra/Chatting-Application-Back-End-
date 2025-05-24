#!/bin/bash

# JWT Authentication Test Script
# This script tests the authentication and authorization flow

BASE_URL="http://localhost:8080"
USERNAME="admin"
PASSWORD="admin"

echo "🔐 JWT Authentication Test Script"
echo "================================="

# Step 1: Login and get JWT token
echo "📝 Step 1: Logging in..."
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$USERNAME\",\"password\":\"$PASSWORD\"}")

echo "Login Response: $LOGIN_RESPONSE"

# Extract access token
ACCESS_TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

if [ -z "$ACCESS_TOKEN" ]; then
    echo "❌ Failed to get access token. Check login credentials."
    exit 1
fi

echo "✅ Access token obtained (length: ${#ACCESS_TOKEN})"
echo "Token preview: ${ACCESS_TOKEN:0:50}..."

# Step 2: Test authentication info (no authorization required)
echo ""
echo "🔍 Step 2: Testing authentication info..."
AUTH_INFO_RESPONSE=$(curl -s -X GET "$BASE_URL/api/debug/auth-info" \
  -H "Authorization: Bearer $ACCESS_TOKEN")

echo "Auth Info Response: $AUTH_INFO_RESPONSE"

# Step 3: Test USER role authorization
echo ""
echo "👤 Step 3: Testing USER role authorization..."
USER_ROLE_RESPONSE=$(curl -s -X GET "$BASE_URL/api/debug/test-user-role" \
  -H "Authorization: Bearer $ACCESS_TOKEN")

echo "USER Role Response: $USER_ROLE_RESPONSE"

# Step 4: Test ADMIN role authorization (if user has admin role)
echo ""
echo "👑 Step 4: Testing ADMIN role authorization..."
ADMIN_ROLE_RESPONSE=$(curl -s -X GET "$BASE_URL/api/debug/test-admin-role" \
  -H "Authorization: Bearer $ACCESS_TOKEN")

echo "ADMIN Role Response: $ADMIN_ROLE_RESPONSE"

# Step 5: Test actual chat rooms endpoint
echo ""
echo "💬 Step 5: Testing chat rooms endpoint..."
CHATROOMS_RESPONSE=$(curl -s -X GET "$BASE_URL/api/chatrooms" \
  -H "Authorization: Bearer $ACCESS_TOKEN")

echo "Chat Rooms Response: $CHATROOMS_RESPONSE"

# Step 6: Test messages endpoint
echo ""
echo "📨 Step 6: Testing messages endpoint..."
MESSAGES_RESPONSE=$(curl -s -X GET "$BASE_URL/api/messages/chatroom/1?page=0&size=20" \
  -H "Authorization: Bearer $ACCESS_TOKEN")

echo "Messages Response: $MESSAGES_RESPONSE"

echo ""
echo "🎯 Test Summary:"
echo "==============="

# Check if responses contain error indicators
if echo "$AUTH_INFO_RESPONSE" | grep -q "authenticated.*true"; then
    echo "✅ Authentication: WORKING"
else
    echo "❌ Authentication: FAILED"
fi

if echo "$USER_ROLE_RESPONSE" | grep -q "USER role test passed"; then
    echo "✅ USER Role Authorization: WORKING"
else
    echo "❌ USER Role Authorization: FAILED"
fi

if echo "$CHATROOMS_RESPONSE" | grep -q "Forbidden\|403\|error"; then
    echo "❌ Chat Rooms Endpoint: FAILED (403 Forbidden)"
else
    echo "✅ Chat Rooms Endpoint: WORKING"
fi

if echo "$MESSAGES_RESPONSE" | grep -q "Forbidden\|403\|error"; then
    echo "❌ Messages Endpoint: FAILED (403 Forbidden)"
else
    echo "✅ Messages Endpoint: WORKING"
fi

echo ""
echo "💡 If any tests failed, check the application logs for detailed error messages."
echo "🔧 Run with debug logging enabled to see JWT filter processing details."
