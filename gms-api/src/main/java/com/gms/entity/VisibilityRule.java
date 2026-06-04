package com.gms.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "gms_visibility_rule")
public class VisibilityRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_page_question_id", nullable = false)
    @JsonIgnore
    private RoundPageQuestion roundPageQuestion;

    @Column(name = "trigger_question_id", nullable = false)
    private Long triggerQuestionId;

    @Column(name = "operator", nullable = false, length = 30)
    private String operator;

    @Column(name = "value", length = 255)
    private String value;

    @Column(name = "logic", length = 5)
    private String logic = "AND";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() { createdAt = Instant.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public RoundPageQuestion getRoundPageQuestion() { return roundPageQuestion; }
    public void setRoundPageQuestion(RoundPageQuestion rpq) { this.roundPageQuestion = rpq; }
    public Long getTriggerQuestionId() { return triggerQuestionId; }
    public void setTriggerQuestionId(Long triggerQuestionId) { this.triggerQuestionId = triggerQuestionId; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public String getLogic() { return logic; }
    public void setLogic(String logic) { this.logic = logic; }
    public Instant getCreatedAt() { return createdAt; }
}
