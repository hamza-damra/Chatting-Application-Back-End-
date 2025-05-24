-- Check if the test user exists
SELECT * FROM users WHERE username = 'websocket_test_user';

-- Check if any chat rooms exist
SELECT * FROM chat_rooms;

-- Check if the test user is a participant in any chat rooms
SELECT * FROM user_chatrooms WHERE user_id IN (SELECT id FROM users WHERE username = 'websocket_test_user');

-- Check if any messages exist
SELECT * FROM messages ORDER BY sent_at DESC LIMIT 10;

-- Check message statuses
SELECT * FROM message_statuses ORDER BY created_at DESC LIMIT 10;

-- Check user roles
SELECT * FROM user_roles WHERE user_id IN (SELECT id FROM users WHERE username = 'websocket_test_user');
