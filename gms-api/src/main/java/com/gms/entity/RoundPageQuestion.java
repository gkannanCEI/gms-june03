package com.gms.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "gms_round_page_question",
       uniqueConstraints = @UniqueConstraint(columnNames = {"round_page_id", "question_id"}))
public class RoundPageQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_page_id", nullable = false)
    @JsonIgnore
    private RoundPage roundPage;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    // Page-level overrides
    @Column(name = "label_override", length = 255)
    private String labelOverride;

    @Column(name = "required_override")
    private Boolean requiredOverride;

    // Round-level overrides
    @Column(name = "round_label_override", length = 255)
    private String roundLabelOverride;

    // Exclusion
    @Column(name = "excluded", nullable = false)
    private boolean excluded = false;

    // Display config
    @Column(name = "readonly", nullable = false)
    private boolean readonly = false;

    @Column(name = "column_span", nullable = false)
    private int columnSpan = 12;

    @Column(name = "label_position", nullable = false, length = 10)
    private String labelPosition = "ABOVE";

    @Column(name = "help_text", length = 500)
    private String helpText;

    @Column(name = "section_group", length = 100)
    private String sectionGroup;

    // Polymorphic min/max constraints (interpretation depends on question type)
    @Column(name = "min_value", length = 100)
    private String minValue;

    @Column(name = "max_value", length = 100)
    private String maxValue;

    // Formula for calculated fields
    @Column(name = "formula", length = 1000)
    private String formula;

    // Visibility rules
    @OneToMany(mappedBy = "roundPageQuestion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<VisibilityRule> visibilityRules = new ArrayList<>();

    // Role visibility
    @OneToMany(mappedBy = "roundPageQuestion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<RoundPageQuestionRole> visibleToRoles = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public RoundPage getRoundPage() { return roundPage; }
    public void setRoundPage(RoundPage roundPage) { this.roundPage = roundPage; }
    public Question getQuestion() { return question; }
    public void setQuestion(Question question) { this.question = question; }
    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
    public String getLabelOverride() { return labelOverride; }
    public void setLabelOverride(String labelOverride) { this.labelOverride = labelOverride; }
    public Boolean getRequiredOverride() { return requiredOverride; }
    public void setRequiredOverride(Boolean requiredOverride) { this.requiredOverride = requiredOverride; }
    public String getRoundLabelOverride() { return roundLabelOverride; }
    public void setRoundLabelOverride(String roundLabelOverride) { this.roundLabelOverride = roundLabelOverride; }
    public boolean isExcluded() { return excluded; }
    public void setExcluded(boolean excluded) { this.excluded = excluded; }
    public boolean isReadonly() { return readonly; }
    public void setReadonly(boolean readonly) { this.readonly = readonly; }
    public int getColumnSpan() { return columnSpan; }
    public void setColumnSpan(int columnSpan) { this.columnSpan = columnSpan; }
    public String getLabelPosition() { return labelPosition; }
    public void setLabelPosition(String labelPosition) { this.labelPosition = labelPosition; }
    public String getHelpText() { return helpText; }
    public void setHelpText(String helpText) { this.helpText = helpText; }
    public String getSectionGroup() { return sectionGroup; }
    public void setSectionGroup(String sectionGroup) { this.sectionGroup = sectionGroup; }
    public String getMinValue() { return minValue; }
    public void setMinValue(String minValue) { this.minValue = minValue; }
    public String getMaxValue() { return maxValue; }
    public void setMaxValue(String maxValue) { this.maxValue = maxValue; }
    public String getFormula() { return formula; }
    public void setFormula(String formula) { this.formula = formula; }
    public List<VisibilityRule> getVisibilityRules() { return visibilityRules; }
    public void setVisibilityRules(List<VisibilityRule> visibilityRules) { this.visibilityRules = visibilityRules; }
    public List<RoundPageQuestionRole> getVisibleToRoles() { return visibleToRoles; }
    public void setVisibleToRoles(List<RoundPageQuestionRole> visibleToRoles) { this.visibleToRoles = visibleToRoles; }

    public String getResolvedLabel() {
        if (roundLabelOverride != null && !roundLabelOverride.isBlank()) {
            return roundLabelOverride;
        }
        if (labelOverride != null && !labelOverride.isBlank()) {
            return labelOverride;
        }
        return question != null ? question.getLabel() : null;
    }

    public boolean getResolvedRequired() {
        if (requiredOverride != null) {
            return requiredOverride;
        }
        return Boolean.TRUE.equals(question != null ? question.getRequired() : Boolean.FALSE);
    }
}
