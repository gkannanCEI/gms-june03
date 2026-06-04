package com.gms.dto;

public class VisibilityRuleDTO {
    private Long id;
    private Long triggerQuestionId;
    private String operator;
    private String value;
    private String logic;

    public VisibilityRuleDTO() {}

    public VisibilityRuleDTO(Long id, Long triggerQuestionId, String operator, String value, String logic) {
        this.id = id;
        this.triggerQuestionId = triggerQuestionId;
        this.operator = operator;
        this.value = value;
        this.logic = logic;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTriggerQuestionId() { return triggerQuestionId; }
    public void setTriggerQuestionId(Long triggerQuestionId) { this.triggerQuestionId = triggerQuestionId; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public String getLogic() { return logic; }
    public void setLogic(String logic) { this.logic = logic; }
}
