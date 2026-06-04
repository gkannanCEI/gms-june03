package com.gms.dto;

/**
 * DTO returned from option CRUD endpoints.
 * Includes the persisted id so callers can reference the option immediately after creation.
 */
public record QuestionOptionDTO(
    Long id,
    String optionLabel,
    String optionValue,
    Integer displayOrder,
    Boolean isOtherOption,
    String optionTargetTable,
    String optionTargetColumn,
    String lookupId
) {
    /** Convenience constructor for cases where routing fields are not relevant. */
    public QuestionOptionDTO(Long id, String optionLabel, String optionValue,
                              Integer displayOrder, Boolean isOtherOption) {
        this(id, optionLabel, optionValue, displayOrder, isOtherOption, null, null, null);
    }
}
