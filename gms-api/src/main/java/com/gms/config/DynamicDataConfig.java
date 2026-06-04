package com.gms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "gms.dynamic-data")
public class DynamicDataConfig {

    private List<TargetEntry> allowedTargets;

    public List<TargetEntry> getAllowedTargets() { return allowedTargets; }
    public void setAllowedTargets(List<TargetEntry> allowedTargets) { this.allowedTargets = allowedTargets; }

    public static class TargetEntry {
        private String table;
        private List<String> columns;

        public String getTable() { return table; }
        public void setTable(String table) { this.table = table; }
        public List<String> getColumns() { return columns; }
        public void setColumns(List<String> columns) { this.columns = columns; }
    }
}
