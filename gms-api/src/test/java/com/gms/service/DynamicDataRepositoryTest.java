package com.gms.service;

import com.gms.config.DynamicDataConfig;
import com.gms.exception.DynamicSqlSecurityException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for DynamicDataRepository allowlist enforcement.
 * Verifies that invalid table/column identifiers are rejected with a security exception.
 */
class DynamicDataRepositoryTest {

    private DynamicDataRepository repository;
    private JdbcTemplate jdbcTemplate;
    private AllowlistService allowlistService;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);

        // Configure allowlist with only gms_application_data.value_text
        DynamicDataConfig config = new DynamicDataConfig();
        DynamicDataConfig.TargetEntry entry = new DynamicDataConfig.TargetEntry();
        entry.setTable("gms_application_data");
        entry.setColumns(List.of("value_text"));
        config.setAllowedTargets(List.of(entry));

        allowlistService = new AllowlistService(config);
        repository = new DynamicDataRepository(jdbcTemplate, allowlistService);
    }

    @Test
    void upsertDomainTable_validTarget_doesNotThrow() {
        // gms_application_data.value_text is in the allowlist
        // This should not throw (it will fail on JDBC since we're mocking, but no security exception)
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any())).thenReturn(0);
        assertDoesNotThrow(() -> repository.upsertSingleTable(1L, 1L, "test value"));
    }

    @Test
    void upsertDomainTable_invalidTable_throwsSecurityException() {
        assertThrows(DynamicSqlSecurityException.class, () ->
            repository.upsertDomainTable("malicious_table", "col", 1L, "value"));
    }

    @Test
    void upsertDomainTable_invalidColumn_throwsSecurityException() {
        assertThrows(DynamicSqlSecurityException.class, () ->
            repository.upsertDomainTable("gms_application_data", "malicious_column", 1L, "value"));
    }

    @Test
    void upsertDomainTable_sqlInjectionInTableName_throwsSecurityException() {
        assertThrows(DynamicSqlSecurityException.class, () ->
            repository.upsertDomainTable("gms_application_data; DROP TABLE users;--", "value_text", 1L, "value"));
    }

    @Test
    void upsertDomainTable_sqlInjectionInColumnName_throwsSecurityException() {
        assertThrows(DynamicSqlSecurityException.class, () ->
            repository.upsertDomainTable("gms_application_data", "value_text; DROP TABLE users;--", 1L, "value"));
    }

    @Test
    void readDomainTable_invalidTarget_throwsSecurityException() {
        assertThrows(DynamicSqlSecurityException.class, () ->
            repository.readDomainTable("nonexistent_table", "col", 1L));
    }

    @Test
    void allowlistService_caseInsensitive() {
        assertTrue(allowlistService.isPermitted("GMS_APPLICATION_DATA", "VALUE_TEXT"));
        assertTrue(allowlistService.isPermitted("gms_application_data", "value_text"));
        assertTrue(allowlistService.isPermitted("Gms_Application_Data", "Value_Text"));
    }

    @Test
    void allowlistService_rejectsUnknown() {
        assertFalse(allowlistService.isPermitted("unknown_table", "value_text"));
        assertFalse(allowlistService.isPermitted("gms_application_data", "unknown_column"));
        assertFalse(allowlistService.isPermitted(null, "value_text"));
        assertFalse(allowlistService.isPermitted("gms_application_data", null));
    }
}
