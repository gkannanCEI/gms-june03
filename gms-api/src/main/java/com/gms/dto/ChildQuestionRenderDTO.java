package com.gms.dto;

public class ChildQuestionRenderDTO {
    private String triggerValue;
    private QuestionRenderDTO question;

    public String getTriggerValue() { return triggerValue; }
    public void setTriggerValue(String triggerValue) { this.triggerValue = triggerValue; }
    public QuestionRenderDTO getQuestion() { return question; }
    public void setQuestion(QuestionRenderDTO question) { this.question = question; }
}
