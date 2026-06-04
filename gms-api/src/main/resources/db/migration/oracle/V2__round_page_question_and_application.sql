-- Round-Page-Question configuration

CREATE TABLE gms_round_page_question (
    id                      NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    round_page_id           NUMBER NOT NULL REFERENCES gms_round_page(id) ON DELETE CASCADE,
    question_id             NUMBER NOT NULL REFERENCES gms_question(id),
    display_order           NUMBER NOT NULL,
    label_override          VARCHAR2(255),
    required_override       NUMBER(1),
    round_label_override    VARCHAR2(255),
    excluded                NUMBER(1) DEFAULT 0 NOT NULL,
    readonly                NUMBER(1) DEFAULT 0 NOT NULL,
    column_span             NUMBER DEFAULT 12 NOT NULL,
    label_position          VARCHAR2(10) DEFAULT 'ABOVE' NOT NULL,
    help_text               VARCHAR2(500),
    section_group           VARCHAR2(100),
    CONSTRAINT uq_rpq_round_page_question UNIQUE (round_page_id, question_id)
);

CREATE TABLE gms_round_page_question_role (
    id                      NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    round_page_question_id  NUMBER NOT NULL REFERENCES gms_round_page_question(id) ON DELETE CASCADE,
    role_name               VARCHAR2(100) NOT NULL
);

-- Organization

CREATE TABLE gms_organization (
    id                NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name              VARCHAR2(255) NOT NULL UNIQUE,
    organization_type VARCHAR2(20) NOT NULL,
    description       VARCHAR2(2000),
    active            NUMBER(1) DEFAULT 1 NOT NULL,
    created_at        TIMESTAMP NOT NULL,
    updated_at        TIMESTAMP NOT NULL
);

-- Application lifecycle

CREATE TABLE gms_application (
    id                    NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    program_round_id      NUMBER NOT NULL REFERENCES gms_program_round(id),
    organization_id       NUMBER NOT NULL REFERENCES gms_organization(id),
    user_id               VARCHAR2(255) NOT NULL,
    status                VARCHAR2(20) NOT NULL,
    eligibility_warning   NUMBER(1) DEFAULT 0 NOT NULL,
    created_at            TIMESTAMP NOT NULL,
    updated_at            TIMESTAMP NOT NULL,
    CONSTRAINT uq_app_round_user UNIQUE (program_round_id, user_id)
);

-- Application data (reference single-table pattern)

CREATE TABLE gms_application_data (
    id              NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    application_id  NUMBER NOT NULL REFERENCES gms_application(id),
    question_id     NUMBER NOT NULL REFERENCES gms_question(id),
    value_text      VARCHAR2(4000),
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NOT NULL,
    CONSTRAINT uq_app_data_app_question UNIQUE (application_id, question_id)
);

-- File attachments

CREATE TABLE gms_file_attachment (
    id                VARCHAR2(36) PRIMARY KEY,
    application_id    NUMBER NOT NULL REFERENCES gms_application(id),
    question_id       NUMBER NOT NULL REFERENCES gms_question(id),
    original_filename VARCHAR2(500) NOT NULL,
    file_extension    VARCHAR2(20) NOT NULL,
    file_size_bytes   NUMBER NOT NULL,
    blob_path         VARCHAR2(1000) NOT NULL,
    scan_status       VARCHAR2(20) NOT NULL,
    uploaded_at       TIMESTAMP NOT NULL,
    scanned_at        TIMESTAMP
);

-- Status history

CREATE TABLE gms_status_history (
    id          NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    entity_type VARCHAR2(50) NOT NULL,
    entity_id   NUMBER NOT NULL,
    from_status VARCHAR2(20),
    to_status   VARCHAR2(20) NOT NULL,
    changed_by  VARCHAR2(255) NOT NULL,
    changed_at  TIMESTAMP NOT NULL,
    reason      VARCHAR2(500)
);
