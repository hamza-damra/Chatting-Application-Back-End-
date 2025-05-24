-- Database migration to fix role prefix issues
-- This script ensures all roles are stored without the ROLE_ prefix

-- Fix existing roles that have incorrect ROLE_ prefix
UPDATE user_roles SET role = 'USER' WHERE role = 'ROLE_USER';
UPDATE user_roles SET role = 'ADMIN' WHERE role = 'ROLE_ADMIN';

-- Add any other role fixes as needed
-- UPDATE user_roles SET role = 'MODERATOR' WHERE role = 'ROLE_MODERATOR';
