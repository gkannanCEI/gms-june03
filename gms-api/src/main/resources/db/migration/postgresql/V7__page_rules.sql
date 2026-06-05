-- Page rules: cross-field validation at page level
CREATE TABLE IF NOT EXISTS gms_page_rule (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    round_page_id BIGINT NOT NULL REFERENCES gms_round_page(id) ON DELETE CASCADE,
    logic VARCHAR(5) DEFAULT 'AND',
    error_message VARCHAR(500),
    created_at TIMESTAMP DEFAULT NOW() NOT NULL
);

CREATE TABLE IF NOT EXISTS gms_page_rule_condition (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    page_rule_id BIGINT NOT NULL REFERENCES gms_page_rule(id) ON DELETE CASCADE,
    question_id BIGINT NOT NULL REFERENCES gms_question(id),
    operator VARCHAR(30) NOT NULL,
    value VARCHAR(255),
    value2 VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_page_rule_rp ON gms_page_rule(round_page_id);
CREATE INDEX IF NOT EXISTS idx_page_rule_condition_rule ON gms_page_rule_condition(page_rule_id);
