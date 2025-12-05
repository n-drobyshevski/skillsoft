-- Migration script to add Clerk fields to users table
-- These fields sync additional user data from Clerk.js

-- Add new columns for Clerk user data
ALTER TABLE users ADD COLUMN IF NOT EXISTS username VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS image_url VARCHAR(500);
ALTER TABLE users ADD COLUMN IF NOT EXISTS has_image BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS banned BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS locked BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS clerk_created_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_sign_in_at TIMESTAMP WITH TIME ZONE;

-- Add indexes for the new columns
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_banned ON users(banned);
CREATE INDEX IF NOT EXISTS idx_users_locked ON users(locked);
CREATE INDEX IF NOT EXISTS idx_users_last_sign_in_at ON users(last_sign_in_at);

-- Update composite index for access control queries
CREATE INDEX IF NOT EXISTS idx_users_access ON users(is_active, banned, locked);

-- Add comments for documentation
COMMENT ON COLUMN users.username IS 'Username from Clerk';
COMMENT ON COLUMN users.image_url IS 'Profile image URL from Clerk';
COMMENT ON COLUMN users.has_image IS 'Whether user has a custom profile image';
COMMENT ON COLUMN users.banned IS 'Whether user is banned in Clerk';
COMMENT ON COLUMN users.locked IS 'Whether user is locked in Clerk';
COMMENT ON COLUMN users.clerk_created_at IS 'When the user was created in Clerk';
COMMENT ON COLUMN users.last_sign_in_at IS 'When the user last signed in via Clerk';
