-- V25: Add unique constraint on session_id in test_results table
-- Prevents duplicate scoring results for the same test session.
-- This is the database-level enforcement of the idempotency guard
-- added in ScoringOrchestrationServiceImpl.calculateAndSaveResult().

ALTER TABLE test_results
    ADD CONSTRAINT uk_test_results_session_id UNIQUE (session_id);
