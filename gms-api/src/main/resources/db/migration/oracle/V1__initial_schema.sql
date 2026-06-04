-- Grant Management System - Initial Schema

CREATE TABLE gms_program (
    id              NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    program_name    VARCHAR2(255) NOT NULL,
    description     VARCHAR2(2000),
    goal            VARCHAR2(2000),
    total_budget    NUMBER(18,2),
    status          VARCHAR2(20) NOT NULL,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NOT NULL
);

CREATE TABLE gms_program_round (
    id                          NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    program_id                  NUMBER NOT NULL REFERENCES gms_program(id),
    round_name                  VARCHAR2(255) NOT NULL,
    start_date                  DATE NOT NULL,
    end_date                    DATE NOT NULL,
    funds_limit                 NUMBER(18,2),
    status                      VARCHAR2(20) NOT NULL,
    eligible_organization_types VARCHAR2(500),
    created_at                  TIMESTAMP NOT NULL,
    updated_at                  TIMESTAMP NOT NULL
);

CREATE TABLE gms_page (
    id               NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    page_name        VARCHAR2(200) NOT NULL,
    page_description VARCHAR2(1000),
    active           NUMBER(1) NOT NULL,
    created_at       TIMESTAMP NOT NULL,
    updated_at       TIMESTAMP NOT NULL
);

CREATE TABLE gms_round_page (
    id                        NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    program_round_id          NUMBER NOT NULL REFERENCES gms_program_round(id),
    page_id                   NUMBER NOT NULL REFERENCES gms_page(id),
    display_order             NUMBER NOT NULL,
    page_name_override        VARCHAR2(200),
    page_description_override VARCHAR2(1000)
);

CREATE TABLE gms_question (
    id                  NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    question_type       VARCHAR2(50) NOT NULL,
    label               VARCHAR2(255) NOT NULL,
    target_table        VARCHAR2(100),
    target_column       VARCHAR2(100),
    required            NUMBER(1),
    validation_regex    VARCHAR2(500),
    allowed_file_types  VARCHAR2(500),
    max_file_size_mb    NUMBER,
    min_date            VARCHAR2(10),
    max_date            VARCHAR2(10),
    parent_question_id  NUMBER REFERENCES gms_question(id),
    trigger_value       VARCHAR2(100),
    child_display_order NUMBER,
    active              NUMBER(1) NOT NULL,
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP NOT NULL
);

CREATE TABLE gms_question_option (
    id                   NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    question_id          NUMBER NOT NULL REFERENCES gms_question(id),
    option_label         VARCHAR2(255) NOT NULL,
    option_value         VARCHAR2(100) NOT NULL,
    display_order        NUMBER NOT NULL,
    is_other_option      NUMBER(1),
    option_target_table  VARCHAR2(100),
    option_target_column VARCHAR2(100),
    lookup_id            VARCHAR2(100)
);
