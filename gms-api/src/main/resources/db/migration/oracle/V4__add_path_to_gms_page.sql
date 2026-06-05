-- V4: Add path column to gms_page table

BEGIN
  EXECUTE IMMEDIATE 'ALTER TABLE gms_page ADD path VARCHAR2(500)';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -1430 THEN
      RAISE;
    END IF;
END;
/
