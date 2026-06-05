-- V12: Complete fix for gms_round_page_question schema drift.
--
-- Root cause: The table was created before this codebase was baselined into Flyway.
-- It had page_id (FK -> gms_page) instead of round_page_id (FK -> gms_round_page).
-- V10 attempted this fix using DO blocks but DDL inside PL/pgSQL anonymous blocks
-- is silently skipped by PostgreSQL under Flyway's transactional execution model.
-- This migration uses plain DDL which executes unconditionally and reliably.

-- 1. Add round_page_id if missing (no-op if already present from a partial fix)
ALTER TABLE gms_round_page_question ADD COLUMN IF NOT EXISTS round_page_id BIGINT;

-- 2. Drop old FK on page_id
ALTER TABLE gms_round_page_question DROP CONSTRAINT IF EXISTS fkgtf1pkinr80oy0to5l73t5eun;

-- 3. Drop old page_id column
ALTER TABLE gms_round_page_question DROP COLUMN IF EXISTS page_id;

-- 4. Add correct FK to gms_round_page
ALTER TABLE gms_round_page_question DROP CONSTRAINT IF EXISTS fk_rpq_round_page;
ALTER TABLE gms_round_page_question
    ADD CONSTRAINT fk_rpq_round_page
    FOREIGN KEY (round_page_id) REFERENCES gms_round_page(id) ON DELETE CASCADE;

-- 5. Fix column sizes to match entity definitions
ALTER TABLE gms_round_page_question ALTER COLUMN min_value TYPE VARCHAR(100);
ALTER TABLE gms_round_page_question ALTER COLUMN max_value TYPE VARCHAR(100);
ALTER TABLE gms_round_page_question ALTER COLUMN label_override TYPE VARCHAR(255);
ALTER TABLE gms_round_page_question ALTER COLUMN section_group TYPE VARCHAR(100);
ALTER TABLE gms_round_page_question ALTER COLUMN formula TYPE VARCHAR(1000);

-- 6. Unique constraint on (round_page_id, question_id)
ALTER TABLE gms_round_page_question
    DROP CONSTRAINT IF EXISTS gms_round_page_question_round_page_id_question_id_key;
ALTER TABLE gms_round_page_question
    ADD CONSTRAINT gms_round_page_question_round_page_id_question_id_key
    UNIQUE (round_page_id, question_id);
