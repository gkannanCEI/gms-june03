package com.gms.dto;

/**
 * Summary of a single page within a round, as returned to the applicant dashboard.
 * The {@code completed} flag is true when the application has at least one saved
 * answer for a required question on this page.
 */
public record PageSummaryDTO(Long pageId, String pageName, Integer displayOrder, boolean completed) {}
