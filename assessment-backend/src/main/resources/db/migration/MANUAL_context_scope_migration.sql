-- Manual Migration Script for context_scope column
-- Run this script BEFORE starting the application if you have existing data
-- This ensures all existing behavioral_indicators have a valid context_scope value

-- Update all NULL context_scope values to UNIVERSAL (backward compatibility)
UPDATE behavioral_indicators 
SET context_scope = 'UNIVERSAL' 
WHERE context_scope IS NULL;

-- Verify the migration
SELECT COUNT(*) as total_indicators,
       COUNT(context_scope) as with_context_scope,
       COUNT(*) - COUNT(context_scope) as still_null
FROM behavioral_indicators;

-- Expected result: still_null should be 0
