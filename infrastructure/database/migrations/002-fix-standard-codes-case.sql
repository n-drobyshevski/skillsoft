-- Migration: Convert standard_codes JSONB from camelCase to snake_case
-- This migration fixes the JSON format stored in standard_codes column
-- from Java camelCase (globalCategory, onetRef, escoRef, bigFive)
-- to snake_case (global_category, onet_ref, esco_ref, big_five)
--
-- Run this after deploying the HibernateJsonConfig.java fix

-- Update global_category structure (convert from camelCase keys to snake_case)
UPDATE competencies
SET standard_codes = (
    SELECT jsonb_strip_nulls(jsonb_build_object(
        'global_category', CASE 
            WHEN standard_codes->'globalCategory' IS NOT NULL THEN
                jsonb_strip_nulls(jsonb_build_object(
                    'big_five', COALESCE(
                        standard_codes->'globalCategory'->>'bigFive',
                        standard_codes->'globalCategory'->>'big_five',
                        UPPER(standard_codes->'globalCategory'->>'trait')
                    ),
                    'dimension', COALESCE(
                        standard_codes->'globalCategory'->>'dimension',
                        standard_codes->'globalCategory'->>'facet'
                    ),
                    'domain', standard_codes->'globalCategory'->>'domain',
                    'trait', standard_codes->'globalCategory'->>'trait',
                    'facet', standard_codes->'globalCategory'->>'facet'
                ))
            WHEN standard_codes->'global_category' IS NOT NULL THEN
                jsonb_strip_nulls(jsonb_build_object(
                    'big_five', COALESCE(
                        standard_codes->'global_category'->>'bigFive',
                        standard_codes->'global_category'->>'big_five',
                        UPPER(standard_codes->'global_category'->>'trait')
                    ),
                    'dimension', COALESCE(
                        standard_codes->'global_category'->>'dimension',
                        standard_codes->'global_category'->>'facet'
                    ),
                    'domain', standard_codes->'global_category'->>'domain',
                    'trait', standard_codes->'global_category'->>'trait',
                    'facet', standard_codes->'global_category'->>'facet'
                ))
            ELSE NULL
        END,
        'onet_ref', CASE
            WHEN standard_codes->'onetRef' IS NOT NULL THEN
                jsonb_strip_nulls(jsonb_build_object(
                    'code', COALESCE(standard_codes->'onetRef'->>'code', standard_codes->'onet_ref'->>'code'),
                    'title', COALESCE(standard_codes->'onetRef'->>'title', standard_codes->'onetRef'->>'name', standard_codes->'onet_ref'->>'title'),
                    'element_type', COALESCE(standard_codes->'onetRef'->>'elementType', standard_codes->'onetRef'->>'element_type', standard_codes->'onet_ref'->>'element_type')
                ))
            WHEN standard_codes->'onet_ref' IS NOT NULL THEN
                jsonb_strip_nulls(jsonb_build_object(
                    'code', standard_codes->'onet_ref'->>'code',
                    'title', COALESCE(standard_codes->'onet_ref'->>'title', standard_codes->'onet_ref'->>'name'),
                    'element_type', COALESCE(standard_codes->'onet_ref'->>'elementType', standard_codes->'onet_ref'->>'element_type')
                ))
            ELSE NULL
        END,
        'esco_ref', CASE
            WHEN standard_codes->'escoRef' IS NOT NULL THEN
                jsonb_strip_nulls(jsonb_build_object(
                    'uri', COALESCE(standard_codes->'escoRef'->>'uri', standard_codes->'esco_ref'->>'uri'),
                    'title', COALESCE(standard_codes->'escoRef'->>'title', standard_codes->'escoRef'->>'label', standard_codes->'esco_ref'->>'title'),
                    'skill_type', COALESCE(standard_codes->'escoRef'->>'skillType', standard_codes->'escoRef'->>'skill_type', standard_codes->'esco_ref'->>'skill_type')
                ))
            WHEN standard_codes->'esco_ref' IS NOT NULL THEN
                jsonb_strip_nulls(jsonb_build_object(
                    'uri', standard_codes->'esco_ref'->>'uri',
                    'title', COALESCE(standard_codes->'esco_ref'->>'title', standard_codes->'esco_ref'->>'label'),
                    'skill_type', COALESCE(standard_codes->'esco_ref'->>'skillType', standard_codes->'esco_ref'->>'skill_type')
                ))
            ELSE NULL
        END
    ))
)
WHERE standard_codes IS NOT NULL;

-- Verify the migration
SELECT 
    id,
    name,
    standard_codes
FROM competencies
WHERE standard_codes IS NOT NULL
LIMIT 10;
