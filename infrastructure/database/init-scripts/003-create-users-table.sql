-- Migration script for User entity
-- Create users table with proper indexes and constraints

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    clerk_id VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255),
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    role VARCHAR(50) NOT NULL CHECK (role IN ('USER', 'EDITOR', 'ADMIN')),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    preferences JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP WITH TIME ZONE
);

-- Create indexes for better performance
CREATE INDEX idx_users_clerk_id ON users(clerk_id);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_is_active ON users(is_active);
CREATE INDEX idx_users_created_at ON users(created_at);
CREATE INDEX idx_users_last_login ON users(last_login);

-- Create composite indexes for common queries
CREATE INDEX idx_users_role_active ON users(role, is_active);
CREATE INDEX idx_users_active_created ON users(is_active, created_at);

-- Add constraints
ALTER TABLE users ADD CONSTRAINT users_email_format_check 
    CHECK (email IS NULL OR email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$');

ALTER TABLE users ADD CONSTRAINT users_clerk_id_not_empty 
    CHECK (LENGTH(TRIM(clerk_id)) > 0);

-- Function to update the updated_at column automatically
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Trigger to automatically update updated_at column
CREATE TRIGGER users_updated_at_trigger
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Add comments for documentation
COMMENT ON TABLE users IS 'Application users integrated with Clerk.js authentication';
COMMENT ON COLUMN users.clerk_id IS 'Clerk.js user ID linking to external authentication';
COMMENT ON COLUMN users.email IS 'Optional user email address, can be synchronized with Clerk primary email';
COMMENT ON COLUMN users.role IS 'Application role: USER, EDITOR, or ADMIN';
COMMENT ON COLUMN users.preferences IS 'JSON preferences and settings for the user';
COMMENT ON COLUMN users.last_login IS 'Timestamp of last successful login';
