-- Local authentication user table
-- Only used when gms.security.auth-mode=local

CREATE TABLE gms_user (
    id              NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username        VARCHAR2(100) NOT NULL UNIQUE,
    password_hash   VARCHAR2(255) NOT NULL,
    display_name    VARCHAR2(255),
    roles           VARCHAR2(200) NOT NULL,
    organization_id NUMBER REFERENCES gms_organization(id),
    active          NUMBER(1) DEFAULT 1 NOT NULL,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NOT NULL
);

-- Seed default admin and applicant users for local development
-- Passwords are BCrypt hashes of "password123"
INSERT INTO gms_user (username, password_hash, display_name, roles, active, created_at, updated_at)
VALUES ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'System Admin', 'ADMIN', 1, SYSTIMESTAMP, SYSTIMESTAMP);

INSERT INTO gms_user (username, password_hash, display_name, roles, active, created_at, updated_at)
VALUES ('applicant', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Test Applicant', 'APPLICANT', 1, SYSTIMESTAMP, SYSTIMESTAMP);
