-- Round-Page-Question configuration (PostgreSQL)

CREATE TABLE IF NOT EXISTS gms_round_page_question (
    id                      BIGSERIAL PRIMARY KEY,
    round_page_id           BIGINT NOT NULL REFERENCES gms_round_page(id) ON DELETE CASCADE,
    question_id             BIGINT NOT NULL REFERENCES gms_question(id),
    display_order           INTEGER NOT NULL,
    label_override          VARCHAR(255),
    required_override       BOOLEAN,
    round_label_override    VARCHAR(255),
    round_required_override BOOLEAN,
    excluded                BOOLEAN NOT NULL DEFAULT FALSE,
    readonly                BOOLEAN NOT NULL DEFAULT FALSE,
    column_span             INTEGER NOT NULL DEFAULT 12,
    label_position          VARCHAR(10) NOT NULL DEFAULT 'ABOVE',
    help_text               VARCHAR(500),
    section_group           VARCHAR(100),
    UNIQUE (round_page_id, question_id)
);

CREATE TABLE IF NOT EXISTS gms_round_page_question_role (
    id                      BIGSERIAL PRIMARY KEY,
    round_page_question_id  BIGINT NOT NULL REFERENCES gms_round_page_question(id) ON DELETE CASCADE,
    role_name               VARCHAR(100) NOT NULL
);

-- Organization

CREATE TABLE IF NOT EXISTS gms_organization (
    id                BIGSERIAL PRIMARY KEY,
    name              VARCHAR(255) NOT NULL UNIQUE,
    organization_type VARCHAR(20) NOT NULL,
    description       VARCHAR(2000),
    active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Application lifecycle

CREATE TABLE IF NOT EXISTS gms_application (
    id                    BIGSERIAL PRIMARY KEY,
    program_round_id      BIGINT NOT NULL REFERENCES gms_program_round(id),
    organization_id       BIGINT NOT NULL REFERENCES gms_organization(id),
    user_id               VARCHAR(255) NOT NULL,
    status                VARCHAR(20) NOT NULL,
    eligibility_warning   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (program_round_id, user_id)
);

-- Application data (reference single-table pattern)

CREATE TABLE IF NOT EXISTS gms_application_data (
    id              BIGSERIAL PRIMARY KEY,
    application_id  BIGINT NOT NULL REFERENCES gms_application(id),
    question_id     BIGINT NOT NULL REFERENCES gms_question(id),
    value_text      VARCHAR(4000),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (application_id, question_id)
);

-- File attachments

CREATE TABLE IF NOT EXISTS gms_file_attachment (
    id                VARCHAR(36) PRIMARY KEY,
    application_id    BIGINT NOT NULL REFERENCES gms_application(id),
    question_id       BIGINT NOT NULL REFERENCES gms_question(id),
    original_filename VARCHAR(500) NOT NULL,
    file_extension    VARCHAR(20) NOT NULL,
    file_size_bytes   BIGINT NOT NULL,
    blob_path         VARCHAR(1000) NOT NULL,
    scan_status       VARCHAR(20) NOT NULL,
    uploaded_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    scanned_at        TIMESTAMP
);

-- Status history

CREATE TABLE IF NOT EXISTS gms_status_history (
    id          BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL,
    entity_id   BIGINT NOT NULL,
    from_status VARCHAR(20),
    to_status   VARCHAR(20) NOT NULL,
    changed_by  VARCHAR(255) NOT NULL,
    changed_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    reason      VARCHAR(500)
);
