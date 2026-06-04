-- External ID for import tracking
ALTER TABLE gms_question ADD COLUMN external_id VARCHAR(100);

-- Formula for calculated fields
ALTER TABLE gms_round_page_question ADD COLUMN formula VARCHAR(1000);
