-- Fix gms_round_page_question: replace stale page_id FK with round_page_id FK
-- The table was originally created with page_id (referencing gms_page directly)
-- but the schema requires round_page_id (referencing gms_round_page).

DO $$
BEGIN
    -- Step 1: Add round_page_id column if it doesn't exist
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'gms_round_page_question' AND column_name = 'round_page_id'
    ) THEN
        ALTER TABLE gms_round_page_question ADD COLUMN round_page_id BIGINT;
    END IF;

    -- Step 2: Drop old page_id FK constraint if present
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints tc
        JOIN information_schema.key_column_usage kcu
          ON tc.constraint_name = kcu.constraint_name
        WHERE tc.table_name = 'gms_round_page_question'
          AND tc.constraint_type = 'FOREIGN KEY'
          AND kcu.column_name = 'page_id'
    ) THEN
        ALTER TABLE gms_round_page_question
            DROP CONSTRAINT fkgtf1pkinr80oy0to5l73t5eun;
    END IF;

    -- Step 3: Drop the old page_id column if it exists
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'gms_round_page_question' AND column_name = 'page_id'
    ) THEN
        ALTER TABLE gms_round_page_question DROP COLUMN page_id;
    END IF;
END
$$;

-- Step 4: Add NOT NULL constraint and FK to gms_round_page (only if not already there)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints tc
        JOIN information_schema.key_column_usage kcu
          ON tc.constraint_name = kcu.constraint_name
        WHERE tc.table_name = 'gms_round_page_question'
          AND tc.constraint_type = 'FOREIGN KEY'
          AND kcu.column_name = 'round_page_id'
    ) THEN
        ALTER TABLE gms_round_page_question
            ADD CONSTRAINT fk_rpq_round_page
            FOREIGN KEY (round_page_id) REFERENCES gms_round_page(id) ON DELETE CASCADE;
    END IF;
END
$$;

-- Step 5: Fix column lengths that differ from entity expectations
ALTER TABLE gms_round_page_question ALTER COLUMN min_value TYPE VARCHAR(100);
ALTER TABLE gms_round_page_question ALTER COLUMN max_value TYPE VARCHAR(100);
ALTER TABLE gms_round_page_question ALTER COLUMN label_override TYPE VARCHAR(255);
ALTER TABLE gms_round_page_question ALTER COLUMN section_group TYPE VARCHAR(100);
ALTER TABLE gms_round_page_question ALTER COLUMN formula TYPE VARCHAR(1000);

-- Step 6: Create unique constraint if not present
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'gms_round_page_question'
          AND constraint_type = 'UNIQUE'
          AND constraint_name = 'gms_round_page_question_round_page_id_question_id_key'
    ) THEN
        ALTER TABLE gms_round_page_question
            ADD CONSTRAINT gms_round_page_question_round_page_id_question_id_key
            UNIQUE (round_page_id, question_id);
    END IF;
END
$$;
