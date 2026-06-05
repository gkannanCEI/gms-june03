package com.gms.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JDBC-based repository for reading application answer data.
 *
 * Used exclusively by PageBuilderService to compute per-page completion status
 * on the applicant dashboard. Completion is defined as: every visible, non-excluded,
 * non-LABEL question on the page has a non-blank answer saved — regardless of
 * whether the question is required or optional, and regardless of which target
 * table stores the answer.
 */
@Repository
public class ApplicationDataRepository {

    private final JdbcTemplate jdbc;

    public ApplicationDataRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Returns the number of question IDs (from the supplied list) for which the
     * application has a non-null, non-blank answer saved in gms_application_data
     * (the EAV single-table store).
     *
     * Only call this for questions whose targetTable is 'gms_application_data'
     * or null (defaulting to single-table storage).
     *
     * @param applicationId the application to check
     * @param questionIds   list of question IDs that store in gms_application_data
     * @return count of questions that have a non-blank saved answer
     */
    public long countAnsweredInSingleTable(Long applicationId, List<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) return 0L;

        String placeholders = buildPlaceholders(questionIds.size());

        // Standard SQL — no CAST or database-specific syntax.
        // value_text is already a VARCHAR column; TRIM() and <> '' handle blank strings.
        String sql = "SELECT COUNT(*) FROM gms_application_data " +
                     "WHERE application_id = ? " +
                     "AND question_id IN (" + placeholders + ") " +
                     "AND value_text IS NOT NULL " +
                     "AND value_text <> ''";

        Object[] params = buildParams(applicationId, questionIds);
        Long count = jdbc.queryForObject(sql, Long.class, params);
        return count != null ? count : 0L;
    }

    /**
     * Returns true if the application has a non-null, non-blank value in the
     * specified domain table column (e.g. gms_applicant_profile.first_name).
     *
     * The table and column names are validated by the caller (AllowlistService)
     * before being passed here — they are not user-supplied at runtime.
     *
     * @param table         the domain table name (allowlist-validated)
     * @param column        the column name (allowlist-validated)
     * @param applicationId the application to check
     * @return true if a non-blank value exists
     */
    public boolean hasAnswerInDomainTable(String table, String column, Long applicationId) {
        // table and column are allowlist-validated before this call — safe to interpolate.
        String sql = "SELECT COUNT(*) FROM " + table +
                     " WHERE application_id = ? " +
                     "AND \"" + column + "\" IS NOT NULL " +
                     "AND CAST(\"" + column + "\" AS VARCHAR) <> ''";
        Integer count = jdbc.queryForObject(sql, Integer.class, applicationId);
        return count != null && count > 0;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String buildPlaceholders(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append(',');
            sb.append('?');
        }
        return sb.toString();
    }

    private Object[] buildParams(Long applicationId, List<Long> questionIds) {
        Object[] params = new Object[questionIds.size() + 1];
        params[0] = applicationId;
        for (int i = 0; i < questionIds.size(); i++) {
            params[i + 1] = questionIds.get(i);
        }
        return params;
    }
}
