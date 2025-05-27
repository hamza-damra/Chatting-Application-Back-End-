-- Ensure profile_picture column exists in users table
-- This script will add the column if it doesn't exist

-- Check if the column exists and add it if it doesn't
SET @column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'users'
    AND COLUMN_NAME = 'profile_picture'
);

-- Add the column if it doesn't exist
SET @sql = IF(@column_exists = 0,
    'ALTER TABLE users ADD COLUMN profile_picture VARCHAR(500) NULL COMMENT "URL path to user profile image"',
    'SELECT "Column profile_picture already exists" as message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Verify the column was created/exists
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
AND TABLE_NAME = 'users'
AND COLUMN_NAME = 'profile_picture';
