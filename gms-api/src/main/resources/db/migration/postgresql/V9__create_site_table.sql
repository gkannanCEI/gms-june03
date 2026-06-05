CREATE TABLE IF NOT EXISTS site (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL REFERENCES gms_application(id),
    "openDate" VARCHAR(255),
    "ADDRESS-1" VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
