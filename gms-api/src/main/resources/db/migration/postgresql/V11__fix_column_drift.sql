-- Fix column drift between DB state and entity definitions

-- gms_visibility_rule: missing created_at, column size mismatches
ALTER TABLE gms_visibility_rule ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT NOW() NOT NULL;
ALTER TABLE gms_visibility_rule ALTER COLUMN logic TYPE VARCHAR(5);
ALTER TABLE gms_visibility_rule ALTER COLUMN operator TYPE VARCHAR(30);
ALTER TABLE gms_visibility_rule ALTER COLUMN value TYPE VARCHAR(255);

-- gms_question: missing help_text, label/allowed_file_types size mismatches
ALTER TABLE gms_question ADD COLUMN IF NOT EXISTS help_text VARCHAR(1000);
ALTER TABLE gms_question ALTER COLUMN label TYPE VARCHAR(255);
ALTER TABLE gms_question ALTER COLUMN allowed_file_types TYPE VARCHAR(500);
