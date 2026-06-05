-- V13: Add path column to gms_page table

ALTER TABLE gms_page ADD COLUMN IF NOT EXISTS path VARCHAR(500);
