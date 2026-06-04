-- Create a default organization if none exists and assign it to the applicant user
INSERT INTO gms_organization (name, organization_type, description, active, created_at, updated_at)
SELECT 'Default Nonprofit', 'NONPROFIT', 'Default organization for development', true, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM gms_organization WHERE name = 'Default Nonprofit');

-- Assign the first organization to the applicant user if not already assigned
UPDATE gms_user SET organization_id = (SELECT id FROM gms_organization LIMIT 1)
WHERE username = 'applicant' AND organization_id IS NULL;
