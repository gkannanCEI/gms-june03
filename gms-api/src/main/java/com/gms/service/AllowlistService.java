package com.gms.service;

import com.gms.config.DynamicDataConfig;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AllowlistService {

    private final Map<String, Set<String>> allowedTargets;

    public AllowlistService(DynamicDataConfig config) {
        this.allowedTargets = new HashMap<>();
        if (config.getAllowedTargets() != null) {
            for (DynamicDataConfig.TargetEntry entry : config.getAllowedTargets()) {
                String table = entry.getTable().toUpperCase();
                Set<String> columns = entry.getColumns().stream()
                    .map(String::toUpperCase)
                    .collect(Collectors.toSet());
                allowedTargets.put(table, columns);
            }
        }
    }

    public boolean isPermitted(String table, String column) {
        if (table == null || column == null) return false;
        Set<String> columns = allowedTargets.get(table.toUpperCase());
        return columns != null && columns.contains(column.toUpperCase());
    }
}
