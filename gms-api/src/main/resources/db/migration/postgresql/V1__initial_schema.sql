-- Grant Management System - Initial Schema (PostgreSQL)

CREATE TABLE gms_program (
    id              BIGSERIAL PRIMARY KEY,
    program_name    VARCHAR(255) NOT NULL,
    description     VARCHAR(2000),
    goal            VARCHAR(2000),
    total_budget    NUMERIC(18,2),
    status          VARCHAR(20) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE gms_program_round (
    id                          BIGSERIAL PRIMARY KEY,
    program_id                  BIGINT NOT NULL REFERENCES gms_program(id),
    round_name                  VARCHAR(255) NOT NULL,
    start_date                  DATE NOT NULL,
    end_date                    DATE NOT NULL,
    funds_limit                 NUMERIC(18,2),
    status                      VARCHAR(20) NOT NULL,
    eligible_organization_types VARCHAR(500),
    created_at                  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE gms_page (
    id               BIGSERIAL PRIMARY KEY,
    page_name        VARCHAR(200) NOT NULL,
    page_description VARCHAR(1000),
    active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE gms_round_page (
    id                        BIGSERIAL PRIMARY KEY,
    program_round_id          BIGINT NOT NULL REFERENCES gms_program_round(id),
    page_id                   BIGINT NOT NULL REFERENCES gms_page(id),
    display_order             INTEGER NOT NULL,
    page_name_override        VARCHAR(200),
    page_description_override VARCHAR(1000)
);

CREATE TABLE gms_question (
    id                  BIGSERIAL PRIMARY KEY,
    question_type       VARCHAR(50) NOT NULL,
    label               VARCHAR(255) NOT NULL,
    target_table        VARCHAR(100),
    target_column       VARCHAR(100),
    required            BOOLEAN,
    validation_regex    VARCHAR(500),
    allowed_file_types  VARCHAR(500),
    max_file_size_mb    INTEGER,
    min_date            VARCHAR(10),
    max_date            VARCHAR(10),
    parent_question_id  BIGINT REFERENCES gms_question(id),
    trigger_value       VARCHAR(100),
    child_display_order INTEGER,
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE gms_question_option (
    id                   BIGSERIAL PRIMARY KEY,
    question_id          BIGINT NOT NULL REFERENCES gms_question(id),
    option_label         VARCHAR(255) NOT NULL,
    option_value         VARCHAR(100) NOT NULL,
    display_order        INTEGER NOT NULL,
    is_other_option      BOOLEAN,
    option_target_table  VARCHAR(100),
    option_target_column VARCHAR(100),
    lookup_id            VARCHAR(100)
);
