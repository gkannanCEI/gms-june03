package com.gms.service;

import com.gms.exception.DynamicSqlSecurityException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Database-agnostic dynamic data repository.
 * Uses standard SQL INSERT/UPDATE with existence check instead of database-specific
 * MERGE (Oracle) or ON CONFLICT (PostgreSQL) syntax.
 */
@Repository
public class DynamicDataRepository {

    private final JdbcTemplate jdbcTemplate;
    private final AllowlistService allowlistService;

    @Value("${spring.profiles.active:postgresql}")
    private String activeProfile;

    public DynamicDataRepository(JdbcTemplate jdbcTemplate, AllowlistService allowlistService) {
        this.jdbcTemplate = jdbcTemplate;
        this.allowlistService = allowlistService;
    }

    /**
     * Upsert for the single-table (EAV) pattern: gms_application_data.
     * Key: (application_id, question_id)
     */
    public void upsertSingleTable(Long applicationId, Long questionId, String value) {
        validateTarget("gms_application_data", "value_text");
        Timestamp now = Timestamp.from(Instant.now());

        if (existsSingleTable(applicationId, questionId)) {
            String sql = "UPDATE gms_application_data SET value_text = ?, updated_at = ? " +
                         "WHERE application_id = ? AND question_id = ?";
            jdbcTemplate.update(sql, value, now, applicationId, questionId);
        } else {
            String sql = "INSERT INTO gms_application_data (application_id, question_id, value_text, created_at, updated_at) " +
                         "VALUES (?, ?, ?, ?, ?)";
            jdbcTemplate.update(sql, applicationId, questionId, value, now, now);
        }
    }

    /**
     * Upsert for the multi-table (domain) pattern.
     * Key: application_id (one row per application per table)
     */
    public void upsertDomainTable(String table, String column, Long applicationId, String value) {
        validateTarget(table, column);
        Timestamp now = Timestamp.from(Instant.now());

        if (existsDomainTable(table, applicationId)) {
            String sql = "UPDATE " + table + " SET \"" + column + "\" = ?, updated_at = ? " +
                         "WHERE application_id = ?";
            jdbcTemplate.update(sql, value, now, applicationId);
        } else {
            String sql = "INSERT INTO " + table + " (application_id, \"" + column + "\", created_at, updated_at) " +
                         "VALUES (?, ?, ?, ?)";
            jdbcTemplate.update(sql, applicationId, value, now, now);
        }
    }

    /**
     * Read back a value from the single-table pattern for post-save verification.
     */
    public String readSingleTable(Long applicationId, Long questionId) {
        String sql = "SELECT value_text FROM gms_application_data WHERE application_id = ? AND question_id = ?";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, applicationId, questionId);
        if (rows.isEmpty()) return null;
        Object val = rows.get(0).get("value_text");
        return val != null ? val.toString() : null;
    }

    /**
     * Read back a value from a domain table for post-save verification.
     */
    public String readDomainTable(String table, String column, Long applicationId) {
        validateTarget(table, column);
        String sql = "SELECT \"" + column + "\" FROM " + table + " WHERE application_id = ?";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, applicationId);
        if (rows.isEmpty()) return null;
        Object val = rows.get(0).get(column);
        // PostgreSQL returns lowercase column names; Oracle returns uppercase
        if (val == null) val = rows.get(0).get(column.toUpperCase());
        if (val == null) val = rows.get(0).get(column.toLowerCase());
        return val != null ? val.toString() : null;
    }

    private boolean existsSingleTable(Long applicationId, Long questionId) {
        String sql = "SELECT COUNT(*) FROM gms_application_data WHERE application_id = ? AND question_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, applicationId, questionId);
        return count != null && count > 0;
    }

    private boolean existsDomainTable(String table, Long applicationId) {
        validateTarget(table, "application_id"); // application_id is always allowed implicitly
        String sql = "SELECT COUNT(*) FROM " + table + " WHERE application_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, applicationId);
        return count != null && count > 0;
    }

    private void validateTarget(String table, String column) {
        // application_id is always a valid column (it's the lookup key, not a data column)
        if ("application_id".equalsIgnoreCase(column)) return;
        if (!allowlistService.isPermitted(table, column)) {
            throw new DynamicSqlSecurityException("Target not in allowlist: " + table + "." + column);
        }
    }
}
