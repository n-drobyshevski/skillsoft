-- Migration: Add context_scope column to behavioral_indicators
-- Purpose: Smart Assessment Two-Tier Scoping System
-- Author: AI Assistant
-- Date: 2025-12-10

-- Step 1: Add column as nullable to allow migration of existing data
ALTER TABLE behavioral_indicators 
ADD COLUMN IF NOT EXISTS context_scope VARCHAR(255);

-- Step 2: Update existing rows to default value (UNIVERSAL)
-- This ensures backward compatibility - all existing indicators are context-neutral
UPDATE behavioral_indicators 
SET context_scope = 'UNIVERSAL' 
WHERE context_scope IS NULL;

-- Step 3: Add check constraint to enforce valid enum values
ALTER TABLE behavioral_indicators
ADD CONSTRAINT check_context_scope 
CHECK (context_scope IN ('UNIVERSAL', 'PROFESSIONAL', 'TECHNICAL', 'MANAGERIAL'));

-- Note: Column remains nullable in schema to allow safe future migrations
-- Application code ensures non-null values through default initialization
