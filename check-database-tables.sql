-- Check database tables

-- Check users table
SELECT * FROM users;

-- Check chat_rooms table
SELECT * FROM chat_rooms;

-- Check user_chatrooms table (join table)
SELECT * FROM user_chatrooms;

-- Check messages table
SELECT * FROM messages;

-- Check message_statuses table
SELECT * FROM message_statuses;

-- Check user_roles table
SELECT * FROM user_roles;

-- Check if test user exists
SELECT * FROM users WHERE username = 'websocket_test_user';

-- Check if test user is in any chat rooms
SELECT cr.* 
FROM chat_rooms cr
JOIN user_chatrooms uc ON cr.id = uc.chatroom_id
JOIN users u ON u.id = uc.user_id
WHERE u.username = 'websocket_test_user';

-- Check messages sent by test user
SELECT m.* 
FROM messages m
JOIN users u ON m.sender_id = u.id
WHERE u.username = 'websocket_test_user'
ORDER BY m.sent_at DESC;

-- Check message statuses for test user
SELECT ms.* 
FROM message_statuses ms
JOIN users u ON ms.user_id = u.id
WHERE u.username = 'websocket_test_user'
ORDER BY ms.updated_at DESC;
