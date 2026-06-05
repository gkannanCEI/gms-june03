-- Visibility rules: conditional show/hide of questions based on other question values
CREATE TABLE IF NOT EXISTS gms_visibility_rule (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    round_page_question_id BIGINT NOT NULL REFERENCES gms_round_page_question(id) ON DELETE CASCADE,
    trigger_question_id BIGINT NOT NULL REFERENCES gms_question(id),
    operator VARCHAR(30) NOT NULL,
    value VARCHAR(255),
    logic VARCHAR(5) DEFAULT 'AND',
    created_at TIMESTAMP DEFAULT NOW() NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_visibility_rule_rpq ON gms_visibility_rule(round_page_question_id);
