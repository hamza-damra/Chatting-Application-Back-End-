-- Fix existing user roles that have incorrect ROLE_ prefix
-- This script removes the ROLE_ prefix from existing roles in the database

-- Update ROLE_USER to USER
UPDATE user_roles SET role = 'USER' WHERE role = 'ROLE_USER';

-- Update ROLE_ADMIN to ADMIN  
UPDATE user_roles SET role = 'ADMIN' WHERE role = 'ROLE_ADMIN';

-- Verify the changes
SELECT u.username, ur.role 
FROM users u 
JOIN user_roles ur ON u.id = ur.user_id 
ORDER BY u.username, ur.role;
