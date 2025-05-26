-- Create notifications table
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_id BIGINT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    data TEXT,
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    is_delivered BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    delivered_at TIMESTAMP NULL,
    read_at TIMESTAMP NULL,
    expires_at TIMESTAMP NULL,
    related_message_id BIGINT NULL,
    related_chatroom_id BIGINT NULL,
    triggered_by_user_id BIGINT NULL,

    -- Foreign key constraints
    CONSTRAINT fk_notification_recipient
        FOREIGN KEY (recipient_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_notification_related_message
        FOREIGN KEY (related_message_id) REFERENCES messages(id) ON DELETE SET NULL,
    CONSTRAINT fk_notification_related_chatroom
        FOREIGN KEY (related_chatroom_id) REFERENCES chat_rooms(id) ON DELETE SET NULL,
    CONSTRAINT fk_notification_triggered_by_user
        FOREIGN KEY (triggered_by_user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Create indexes for notifications table
CREATE INDEX IF NOT EXISTS idx_notification_recipient ON notifications(recipient_id);
CREATE INDEX IF NOT EXISTS idx_notification_type ON notifications(notification_type);
CREATE INDEX IF NOT EXISTS idx_notification_read ON notifications(is_read);
CREATE INDEX IF NOT EXISTS idx_notification_created ON notifications(created_at);
CREATE INDEX IF NOT EXISTS idx_notification_delivered ON notifications(is_delivered);
CREATE INDEX IF NOT EXISTS idx_notification_expires ON notifications(expires_at);
CREATE INDEX IF NOT EXISTS idx_notification_priority ON notifications(priority);

-- Create notification_preferences table
CREATE TABLE IF NOT EXISTS notification_preferences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    push_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    new_message_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    private_message_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    group_message_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    mention_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    chat_room_invite_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    file_sharing_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    system_announcement_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    do_not_disturb BOOLEAN NOT NULL DEFAULT FALSE,
    dnd_start_time VARCHAR(5) NULL,
    dnd_end_time VARCHAR(5) NULL,
    sound_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    vibration_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    show_preview BOOLEAN NOT NULL DEFAULT TRUE,
    max_offline_notifications INT NOT NULL DEFAULT 100,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- Foreign key constraint
    CONSTRAINT fk_notification_prefs_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create index for notification_preferences table
CREATE INDEX IF NOT EXISTS idx_notification_prefs_user ON notification_preferences(user_id);

-- Add some sample notification types as comments for reference
/*
Notification Types:
- NEW_MESSAGE: Standard new message notification
- PRIVATE_MESSAGE: New private message
- GROUP_MESSAGE: New group message
- MENTION: User was mentioned
- CHAT_ROOM_INVITE: Invited to a chat room
- CHAT_ROOM_ADDED: Added to a chat room
- CHAT_ROOM_REMOVED: Removed from a chat room
- USER_JOINED: User joined a chat room
- USER_LEFT: User left a chat room
- FILE_SHARED: File was shared
- SYSTEM_ANNOUNCEMENT: System-wide announcement
- FRIEND_REQUEST: Friend request (future feature)
- FRIEND_ACCEPTED: Friend request accepted (future feature)

Priority Levels:
- LOW: Non-urgent notifications
- NORMAL: Standard notifications
- HIGH: Important notifications
- URGENT: Critical notifications that should be delivered immediately
*/
