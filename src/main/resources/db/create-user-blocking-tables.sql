-- Create blocked_users table for user blocking functionality
CREATE TABLE IF NOT EXISTS blocked_users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    blocker_id BIGINT NOT NULL,
    blocked_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reason VARCHAR(500),
    
    -- Foreign key constraints
    CONSTRAINT fk_blocked_users_blocker FOREIGN KEY (blocker_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_blocked_users_blocked FOREIGN KEY (blocked_id) REFERENCES users(id) ON DELETE CASCADE,
    
    -- Unique constraint to prevent duplicate blocks
    CONSTRAINT uk_blocked_users_blocker_blocked UNIQUE (blocker_id, blocked_id),
    
    -- Check constraint to prevent self-blocking
    CONSTRAINT chk_blocked_users_no_self_block CHECK (blocker_id != blocked_id)
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_blocked_users_blocker ON blocked_users(blocker_id);
CREATE INDEX IF NOT EXISTS idx_blocked_users_blocked ON blocked_users(blocked_id);
CREATE INDEX IF NOT EXISTS idx_blocked_users_created ON blocked_users(created_at);

-- Add comment to the table
ALTER TABLE blocked_users COMMENT = 'Stores user blocking relationships for the chat application';

-- Insert some sample data for testing (optional)
-- Uncomment the following lines if you want to add test data
/*
INSERT IGNORE INTO blocked_users (blocker_id, blocked_id, reason) VALUES 
(1, 2, 'Spam messages'),
(2, 3, 'Inappropriate behavior');
*/
