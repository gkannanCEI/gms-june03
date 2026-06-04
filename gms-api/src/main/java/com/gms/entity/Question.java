package com.gms.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "gms_question")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_type", nullable = false, length = 50)
    private String questionType;

    @Column(name = "label", nullable = false, length = 255)
    private String label;

    @Column(name = "target_table", length = 100)
    private String targetTable;

    @Column(name = "target_column", length = 100)
    private String targetColumn;

    @Column(name = "required")
    private Boolean required;

    @Column(name = "validation_regex", length = 500)
    private String validationRegex;

    @Column(name = "allowed_file_types", length = 500)
    private String allowedFileTypes;

    @Column(name = "max_file_size_mb")
    private Integer maxFileSizeMb;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_question_id")
    @JsonIgnore
    private Question parentQuestion;

    @Column(name = "trigger_value", length = 100)
    private String triggerValue;

    @Column(name = "child_display_order")
    private Integer childDisplayOrder;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "external_id", length = 100)
    private String externalId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<QuestionOption> options = new ArrayList<>();

    @OneToMany(mappedBy = "parentQuestion")
    @OrderBy("childDisplayOrder ASC")
    @JsonIgnore
    private List<Question> childQuestions = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getQuestionType() { return questionType; }
    public void setQuestionType(String questionType) { this.questionType = questionType; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getTargetTable() { return targetTable; }
    public void setTargetTable(String targetTable) { this.targetTable = targetTable; }
    public String getTargetColumn() { return targetColumn; }
    public void setTargetColumn(String targetColumn) { this.targetColumn = targetColumn; }
    public Boolean getRequired() { return required; }
    public void setRequired(Boolean required) { this.required = required; }
    public String getValidationRegex() { return validationRegex; }
    public void setValidationRegex(String validationRegex) { this.validationRegex = validationRegex; }
    public String getAllowedFileTypes() { return allowedFileTypes; }
    public void setAllowedFileTypes(String allowedFileTypes) { this.allowedFileTypes = allowedFileTypes; }
    public Integer getMaxFileSizeMb() { return maxFileSizeMb; }
    public void setMaxFileSizeMb(Integer maxFileSizeMb) { this.maxFileSizeMb = maxFileSizeMb; }
    public Question getParentQuestion() { return parentQuestion; }
    public void setParentQuestion(Question parentQuestion) { this.parentQuestion = parentQuestion; }
    public String getTriggerValue() { return triggerValue; }
    public void setTriggerValue(String triggerValue) { this.triggerValue = triggerValue; }
    public Integer getChildDisplayOrder() { return childDisplayOrder; }
    public void setChildDisplayOrder(Integer childDisplayOrder) { this.childDisplayOrder = childDisplayOrder; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<QuestionOption> getOptions() { return options; }
    public void setOptions(List<QuestionOption> options) { this.options = options; }
    public List<Question> getChildQuestions() { return childQuestions; }
    public void setChildQuestions(List<Question> childQuestions) { this.childQuestions = childQuestions; }
}
