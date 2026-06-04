package com.gms.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "gms_question_option")
public class QuestionOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    @JsonIgnore
    private Question question;

    @Column(name = "option_label", nullable = false, length = 255)
    private String optionLabel;

    @Column(name = "option_value", nullable = false, length = 100)
    private String optionValue;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "is_other_option")
    private Boolean isOtherOption;

    @Column(name = "option_target_table", length = 100)
    private String optionTargetTable;

    @Column(name = "option_target_column", length = 100)
    private String optionTargetColumn;

    @Column(name = "lookup_id", length = 100)
    private String lookupId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Question getQuestion() { return question; }
    public void setQuestion(Question question) { this.question = question; }
    public String getOptionLabel() { return optionLabel; }
    public void setOptionLabel(String optionLabel) { this.optionLabel = optionLabel; }
    public String getOptionValue() { return optionValue; }
    public void setOptionValue(String optionValue) { this.optionValue = optionValue; }
    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
    public Boolean getIsOtherOption() { return isOtherOption; }
    public void setIsOtherOption(Boolean isOtherOption) { this.isOtherOption = isOtherOption; }
    public String getOptionTargetTable() { return optionTargetTable; }
    public void setOptionTargetTable(String optionTargetTable) { this.optionTargetTable = optionTargetTable; }
    public String getOptionTargetColumn() { return optionTargetColumn; }
    public void setOptionTargetColumn(String optionTargetColumn) { this.optionTargetColumn = optionTargetColumn; }
    public String getLookupId() { return lookupId; }
    public void setLookupId(String lookupId) { this.lookupId = lookupId; }
}
