package com.gms.dto;

import java.util.ArrayList;
import java.util.List;

public class SaveResult {
    private boolean success;
    private List<FieldError> errors = new ArrayList<>();

    public SaveResult(boolean success) {
        this.success = success;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public List<FieldError> getErrors() { return errors; }
    public void addError(Long questionId, String message) {
        errors.add(new FieldError(questionId, message));
    }

    public record FieldError(Long questionId, String message) {}
}
