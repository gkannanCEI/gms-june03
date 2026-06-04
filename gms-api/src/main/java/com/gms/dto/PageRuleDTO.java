package com.gms.dto;

import java.util.List;

public class PageRuleDTO {
    private Long id;
    private String logic;
    private String errorMessage;
    private List<PageRuleConditionDTO> conditions;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getLogic() { return logic; }
    public void setLogic(String logic) { this.logic = logic; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public List<PageRuleConditionDTO> getConditions() { return conditions; }
    public void setConditions(List<PageRuleConditionDTO> conditions) { this.conditions = conditions; }

    public static class PageRuleConditionDTO {
        private Long questionId;
        private String operator;
        private String value;
        private String value2;

        public PageRuleConditionDTO() {}
        public PageRuleConditionDTO(Long questionId, String operator, String value, String value2) {
            this.questionId = questionId;
            this.operator = operator;
            this.value = value;
            this.value2 = value2;
        }

        public Long getQuestionId() { return questionId; }
        public void setQuestionId(Long questionId) { this.questionId = questionId; }
        public String getOperator() { return operator; }
        public void setOperator(String operator) { this.operator = operator; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public String getValue2() { return value2; }
        public void setValue2(String value2) { this.value2 = value2; }
    }
}
