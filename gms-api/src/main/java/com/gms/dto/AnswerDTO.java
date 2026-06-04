package com.gms.dto;

public class AnswerDTO {
    private Long questionId;
    private String value;
    private String otherText;

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public String getOtherText() { return otherText; }
    public void setOtherText(String otherText) { this.otherText = otherText; }
}
