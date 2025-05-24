-- This script will be executed when the application starts
-- It can be used to initialize the database with some data

-- Create admin user if not exists (password is 'admin' encoded with BCrypt)
INSERT IGNORE INTO users (id, username, email, password, full_name, is_online, last_seen, created_at)
VALUES (1, 'admin', 'admin@chatapp.com', '$2a$10$OwuE0FQqgTWgx5DZqGrWAOZcB1u7GhE2qRMcyO1AYEYqGfzOeFkHK', 'Admin User', false, NOW(), NOW());

-- Add roles for admin user (without ROLE_ prefix - it's added by CustomUserDetailsService)
INSERT IGNORE INTO user_roles (user_id, role)
VALUES (1, 'ADMIN');

INSERT IGNORE INTO user_roles (user_id, role)
VALUES (1, 'USER');

-- Create a general chat room if not exists
INSERT IGNORE INTO chat_rooms (id, name, is_private, created_at, updated_at, creator_id)
VALUES (1, 'General', false, NOW(), NOW(), 1);

-- Add admin user to the general chat room
INSERT IGNORE INTO user_chatrooms (user_id, chatroom_id)
VALUES (1, 1);
