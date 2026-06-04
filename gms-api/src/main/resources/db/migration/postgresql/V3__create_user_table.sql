-- Local authentication user table (PostgreSQL)

CREATE TABLE gms_user (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(100) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    display_name    VARCHAR(255),
    roles           VARCHAR(200) NOT NULL,
    organization_id BIGINT REFERENCES gms_organization(id),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Seed default users for local development ONLY
-- WARNING: These accounts must be removed or passwords changed in production!
-- In production, users are managed via Azure AD B2C (auth-mode=b2c)
-- Password: password123 (BCrypt hash)
INSERT INTO gms_user (username, password_hash, display_name, roles, active, created_at, updated_at)
VALUES ('admin', '$2a$10$fAkZ.3lrwmmCheSQ8gKLeu9WvoTobm00I7NnhD5pXykdhfY5K0YE.', 'System Admin', 'ADMIN', TRUE, NOW(), NOW());

INSERT INTO gms_user (username, password_hash, display_name, roles, active, created_at, updated_at)
VALUES ('applicant', '$2a$10$fAkZ.3lrwmmCheSQ8gKLeu9WvoTobm00I7NnhD5pXykdhfY5K0YE.', 'Test Applicant', 'APPLICANT', TRUE, NOW(), NOW());
