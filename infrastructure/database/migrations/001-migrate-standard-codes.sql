-- ================================================================
-- MIGRATION: Update standard_codes to new StandardCodesDto format
-- ================================================================
-- This migration converts the old format:
--   {"ESCO": {"code": "...", "name": "...", "confidence": "..."}, 
--    "ONET": {"code": "...", "name": "...", "confidence": "..."}}
--
-- To the new format:
--   {"esco_ref": {"uri": "http://data.europa.eu/esco/skill/...", "title": "...", "skill_type": "skill"},
--    "onet_ref": {"code": "...", "title": "...", "element_type": "skill"},
--    "global_category": null}
-- ================================================================

-- First, let's see what we have
-- SELECT id, name, standard_codes FROM competencies WHERE standard_codes IS NOT NULL;

-- Update competencies with old ESCO/ONET format to new format
UPDATE competencies
SET standard_codes = jsonb_build_object(
    'global_category', 
    CASE 
        WHEN standard_codes->'BIG_FIVE' IS NOT NULL THEN
            jsonb_build_object(
                'domain', 'big_five',
                'trait', LOWER(standard_codes->'BIG_FIVE'->>'code'),
                'facet', NULL
            )
        ELSE NULL
    END,
    'onet_ref',
    CASE 
        WHEN standard_codes->'ONET' IS NOT NULL THEN
            jsonb_build_object(
                'code', standard_codes->'ONET'->>'code',
                'title', standard_codes->'ONET'->>'name',
                'element_type', 'skill'
            )
        ELSE NULL
    END,
    'esco_ref',
    CASE 
        WHEN standard_codes->'ESCO' IS NOT NULL THEN
            jsonb_build_object(
                'uri', 'http://data.europa.eu/esco/skill/' || REPLACE(standard_codes->'ESCO'->>'code', '.', '-'),
                'title', standard_codes->'ESCO'->>'name',
                'skill_type', 'skill'
            )
        ELSE NULL
    END
)
WHERE standard_codes IS NOT NULL 
  AND (standard_codes ? 'ESCO' OR standard_codes ? 'ONET' OR standard_codes ? 'BIG_FIVE');

-- Verify migration
-- SELECT id, name, standard_codes FROM competencies WHERE standard_codes IS NOT NULL;
