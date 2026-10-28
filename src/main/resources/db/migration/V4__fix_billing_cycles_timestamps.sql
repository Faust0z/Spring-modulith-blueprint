-- ============================================================
-- Spring Boot Production Blueprint — V4
-- Fix billing_cycles period_start/period_end from TIMESTAMP to TIMESTAMPTZ
-- ============================================================

ALTER TABLE billing_cycles ALTER COLUMN period_start TYPE TIMESTAMPTZ USING period_start AT TIME ZONE 'UTC';
ALTER TABLE billing_cycles ALTER COLUMN period_end TYPE TIMESTAMPTZ USING period_end AT TIME ZONE 'UTC';