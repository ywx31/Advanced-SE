ALTER TABLE users
    ADD COLUMN avatar_url VARCHAR(500) NULL AFTER display_name,
    ADD COLUMN avatar_object_key VARCHAR(500) NULL AFTER avatar_url;
