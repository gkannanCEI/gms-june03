-- Move min/max constraints from gms_question to gms_round_page_question
-- Renamed to min_value/max_value with polymorphic interpretation based on question type

ALTER TABLE gms_round_page_question ADD COLUMN IF NOT EXISTS min_value VARCHAR(100);
ALTER TABLE gms_round_page_question ADD COLUMN IF NOT EXISTS max_value VARCHAR(100);

-- Remove min_date/max_date from gms_question (no longer needed here)
ALTER TABLE gms_question DROP COLUMN IF EXISTS min_date;
ALTER TABLE gms_question DROP COLUMN IF EXISTS max_date;
