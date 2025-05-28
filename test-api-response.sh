#!/bin/bash

# Test script to verify the ChatRoom API response format
# This script tests that the lastMessageSender field is returned as a string

echo "=== Testing ChatRoom API Response Format ==="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "$1$2${NC}"
}

# Check if server is running
echo "Checking if server is running on localhost:8080..."
if ! curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
    print_status $RED "❌ Server is not running on localhost:8080"
    echo "Please start the Spring Boot application first:"
    echo "  ./mvnw spring-boot:run"
    exit 1
fi

print_status $GREEN "✅ Server is running"
echo ""

# Test authentication endpoint first
echo "Testing authentication..."
AUTH_RESPONSE=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }')

if [ $? -ne 0 ]; then
    print_status $RED "❌ Failed to connect to authentication endpoint"
    exit 1
fi

# Extract token (assuming the response contains an accessToken field)
TOKEN=$(echo "$AUTH_RESPONSE" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
    print_status $YELLOW "⚠️  Could not extract token from auth response"
    echo "Auth response: $AUTH_RESPONSE"
    echo ""
    echo "Trying to test without authentication..."
    TOKEN=""
fi

# Test the chatrooms endpoint
echo "Testing /api/chatrooms endpoint..."

if [ -n "$TOKEN" ]; then
    RESPONSE=$(curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/chatrooms)
else
    RESPONSE=$(curl -s http://localhost:8080/api/chatrooms)
fi

if [ $? -ne 0 ]; then
    print_status $RED "❌ Failed to call /api/chatrooms endpoint"
    exit 1
fi

echo "Response received:"
echo "$RESPONSE" | jq . 2>/dev/null || echo "$RESPONSE"
echo ""

# Check if response contains the expected flat fields
echo "Checking for flat fields in response..."

# Check for lastMessageContent
if echo "$RESPONSE" | grep -q '"lastMessageContent"'; then
    print_status $GREEN "✅ lastMessageContent field found"
else
    print_status $YELLOW "⚠️  lastMessageContent field not found (may be null if no messages)"
fi

# Check for lastMessageSender
if echo "$RESPONSE" | grep -q '"lastMessageSender"'; then
    print_status $GREEN "✅ lastMessageSender field found"
    
    # Check if it's a string (not an object)
    SENDER_VALUE=$(echo "$RESPONSE" | grep -o '"lastMessageSender":"[^"]*"' | head -1)
    if [ -n "$SENDER_VALUE" ]; then
        print_status $GREEN "✅ lastMessageSender is a string: $SENDER_VALUE"
    else
        # Check if it's an object (which would be wrong)
        if echo "$RESPONSE" | grep -q '"lastMessageSender":{'; then
            print_status $RED "❌ lastMessageSender is an object (should be string)"
        else
            print_status $YELLOW "⚠️  lastMessageSender is null (no messages in chat rooms)"
        fi
    fi
else
    print_status $YELLOW "⚠️  lastMessageSender field not found (may be null if no messages)"
fi

# Check for lastMessageTime
if echo "$RESPONSE" | grep -q '"lastMessageTime"'; then
    print_status $GREEN "✅ lastMessageTime field found"
else
    print_status $YELLOW "⚠️  lastMessageTime field not found (may be null if no messages)"
fi

echo ""
print_status $GREEN "=== Test completed ==="

# Summary
echo ""
echo "Summary:"
echo "- The API should now return flat fields for Flutter compatibility"
echo "- lastMessageSender should be a string (user's full name), not an object"
echo "- This fixes the Flutter type casting error"
