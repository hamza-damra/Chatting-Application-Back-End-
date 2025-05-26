#!/bin/bash

# Chat Room API Test Script
# This script tests the chat room creation API to verify that the isPrivate field is working correctly

echo "🧪 Chat Room API Test Script"
echo "=============================="

# Configuration
BASE_URL="http://localhost:8080"
API_ENDPOINT="$BASE_URL/api/chatrooms"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

# Function to get JWT token (you'll need to implement this based on your auth system)
get_jwt_token() {
    echo "📝 Please provide your JWT token:"
    echo "   You can get this by:"
    echo "   1. Logging in through the web interface"
    echo "   2. Using the /api/auth/login endpoint"
    echo "   3. Checking your browser's developer tools"
    echo ""
    read -p "Enter JWT token: " JWT_TOKEN
    echo ""
}

# Function to test chat room creation
test_chat_room_creation() {
    local test_name=$1
    local json_data=$2
    local expected_private=$3
    
    print_status $BLUE "🔍 Testing: $test_name"
    
    response=$(curl -s -w "\n%{http_code}" -X POST "$API_ENDPOINT" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $JWT_TOKEN" \
        -d "$json_data")
    
    # Extract HTTP status code (last line)
    http_code=$(echo "$response" | tail -n1)
    # Extract response body (all lines except last)
    response_body=$(echo "$response" | head -n -1)
    
    if [ "$http_code" = "201" ]; then
        print_status $GREEN "✅ HTTP Status: $http_code (Created)"
        
        # Parse the isPrivate field from the response
        is_private=$(echo "$response_body" | grep -o '"isPrivate":[^,}]*' | cut -d':' -f2 | tr -d ' ')
        
        if [ "$is_private" = "$expected_private" ]; then
            print_status $GREEN "✅ isPrivate field: $is_private (Expected: $expected_private)"
            print_status $GREEN "✅ Test PASSED: $test_name"
        else
            print_status $RED "❌ isPrivate field: $is_private (Expected: $expected_private)"
            print_status $RED "❌ Test FAILED: $test_name"
        fi
        
        # Extract and display room details
        room_id=$(echo "$response_body" | grep -o '"id":[^,}]*' | cut -d':' -f2 | tr -d ' ')
        room_name=$(echo "$response_body" | grep -o '"name":"[^"]*"' | cut -d':' -f2 | tr -d '"')
        
        echo "   📋 Room Details:"
        echo "      ID: $room_id"
        echo "      Name: $room_name"
        echo "      Private: $is_private"
        
    else
        print_status $RED "❌ HTTP Status: $http_code"
        print_status $RED "❌ Test FAILED: $test_name"
        echo "   Response: $response_body"
    fi
    
    echo ""
}

# Main execution
main() {
    print_status $YELLOW "Starting Chat Room API Tests..."
    echo ""
    
    # Check if server is running
    if ! curl -s "$BASE_URL" > /dev/null; then
        print_status $RED "❌ Server is not running at $BASE_URL"
        print_status $YELLOW "Please start the application first with: ./mvnw spring-boot:run"
        exit 1
    fi
    
    print_status $GREEN "✅ Server is running at $BASE_URL"
    echo ""
    
    # Get JWT token
    get_jwt_token
    
    if [ -z "$JWT_TOKEN" ]; then
        print_status $RED "❌ JWT token is required"
        exit 1
    fi
    
    # Test 1: Create private room (isPrivate: true)
    test_chat_room_creation \
        "Private Room Creation" \
        '{"name":"Private Test Room","isPrivate":true}' \
        "true"
    
    # Test 2: Create public room (isPrivate: false)
    test_chat_room_creation \
        "Public Room Creation" \
        '{"name":"Public Test Room","isPrivate":false}' \
        "false"
    
    # Test 3: Create room without isPrivate field (should default to false)
    test_chat_room_creation \
        "Default Room Creation" \
        '{"name":"Default Test Room"}' \
        "false"
    
    print_status $YELLOW "🏁 All tests completed!"
    echo ""
    print_status $BLUE "💡 Tips:"
    echo "   - Check application logs for detailed debugging information"
    echo "   - Verify database records with: SELECT id, name, is_private FROM chat_rooms;"
    echo "   - Run unit tests with: ./mvnw test -Dtest=ChatRoomRequestTest,ChatRoomResponseTest"
}

# Run the main function
main
