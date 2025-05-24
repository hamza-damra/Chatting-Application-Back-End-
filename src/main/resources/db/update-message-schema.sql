-- Update the messages table to allow null content and increase attachment_url length
ALTER TABLE messages MODIFY content TEXT NULL;
ALTER TABLE messages MODIFY attachment_url VARCHAR(1024);
