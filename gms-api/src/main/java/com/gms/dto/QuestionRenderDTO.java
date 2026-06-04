package com.gms.dto;

import java.util.List;

public class QuestionRenderDTO {
    private Long questionId;
    private String questionType;
    private String label;
    private boolean required;
    private String validationRegex;
    private String minValue;
    private String maxValue;
    private String allowedFileTypes;
    private Integer maxFileSizeMb;
    private Integer displayOrder;
    private QuestionDisplayConfig displayConfig;
    private List<QuestionOptionDTO> options;
    private List<ChildQuestionRenderDTO> childQuestions;
    private List<VisibilityRuleDTO> visibilityRules;
    private String formula;

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }
    public String getQuestionType() { return questionType; }
    public void setQuestionType(String questionType) { this.questionType = questionType; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
    public String getValidationRegex() { return validationRegex; }
    public void setValidationRegex(String validationRegex) { this.validationRegex = validationRegex; }
    public String getMinValue() { return minValue; }
    public void setMinValue(String minValue) { this.minValue = minValue; }
    public String getMaxValue() { return maxValue; }
    public void setMaxValue(String maxValue) { this.maxValue = maxValue; }
    public String getAllowedFileTypes() { return allowedFileTypes; }
    public void setAllowedFileTypes(String allowedFileTypes) { this.allowedFileTypes = allowedFileTypes; }
    public Integer getMaxFileSizeMb() { return maxFileSizeMb; }
    public void setMaxFileSizeMb(Integer maxFileSizeMb) { this.maxFileSizeMb = maxFileSizeMb; }
    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
    public QuestionDisplayConfig getDisplayConfig() { return displayConfig; }
    public void setDisplayConfig(QuestionDisplayConfig displayConfig) { this.displayConfig = displayConfig; }
    public List<QuestionOptionDTO> getOptions() { return options; }
    public void setOptions(List<QuestionOptionDTO> options) { this.options = options; }
    public List<ChildQuestionRenderDTO> getChildQuestions() { return childQuestions; }
    public void setChildQuestions(List<ChildQuestionRenderDTO> childQuestions) { this.childQuestions = childQuestions; }
    public List<VisibilityRuleDTO> getVisibilityRules() { return visibilityRules; }
    public void setVisibilityRules(List<VisibilityRuleDTO> visibilityRules) { this.visibilityRules = visibilityRules; }
    public String getFormula() { return formula; }
    public void setFormula(String formula) { this.formula = formula; }
}
