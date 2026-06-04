package com.gms.dto;

/**
 * Compact representation of a parent-child question link.
 * Returned by GET/POST/PUT on the /questions/{parentId}/children endpoints.
 * Avoids serialising the full Question entity and sidesteps the @JsonIgnore on
 * Question.childQuestions.
 */
public record ChildLinkDTO(
    Long parentQuestionId,
    Long childQuestionId,
    String triggerValue,
    Integer childDisplayOrder
) {}
