package com.gms.exception;

import java.util.List;
import java.util.Map;

public class ConflictException extends RuntimeException {
    private final List<Map<String, Object>> affectedResources;

    public ConflictException(String message, List<Map<String, Object>> affectedResources) {
        super(message);
        this.affectedResources = affectedResources;
    }

    public List<Map<String, Object>> getAffectedResources() {
        return affectedResources;
    }
}
