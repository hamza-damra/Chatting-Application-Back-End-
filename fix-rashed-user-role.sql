-- Fix missing role for user 'rashed'
-- This script adds the USER role to the rashed user

-- First, check if the user exists and get their ID
SELECT id, username FROM users WHERE username = 'rashed';

-- Add USER role for the rashed user
-- Replace USER_ID with the actual ID from the query above
INSERT INTO user_roles (user_id, role) 
SELECT id, 'USER' FROM users WHERE username = 'rashed' 
AND NOT EXISTS (
    SELECT 1 FROM user_roles ur WHERE ur.user_id = users.id AND ur.role = 'USER'
);

-- Verify the role was added
SELECT u.id, u.username, ur.role 
FROM users u 
LEFT JOIN user_roles ur ON u.id = ur.user_id 
WHERE u.username = 'rashed';

-- Check all users and their roles for verification
SELECT u.username, ur.role 
FROM users u 
LEFT JOIN user_roles ur ON u.id = ur.user_id 
ORDER BY u.username, ur.role;
