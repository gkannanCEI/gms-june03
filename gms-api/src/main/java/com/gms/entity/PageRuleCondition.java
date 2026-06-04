package com.gms.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "gms_page_rule_condition")
public class PageRuleCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_rule_id", nullable = false)
    @JsonIgnore
    private PageRule pageRule;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "operator", nullable = false, length = 30)
    private String operator;

    @Column(name = "value", length = 255)
    private String value;

    @Column(name = "value2", length = 255)
    private String value2;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public PageRule getPageRule() { return pageRule; }
    public void setPageRule(PageRule pageRule) { this.pageRule = pageRule; }
    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public String getValue2() { return value2; }
    public void setValue2(String value2) { this.value2 = value2; }
}
