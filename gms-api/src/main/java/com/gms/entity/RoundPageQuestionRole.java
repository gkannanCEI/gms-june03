package com.gms.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "gms_round_page_question_role")
public class RoundPageQuestionRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_page_question_id", nullable = false)
    @JsonIgnore
    private RoundPageQuestion roundPageQuestion;

    @Column(name = "role_name", nullable = false, length = 100)
    private String roleName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public RoundPageQuestion getRoundPageQuestion() { return roundPageQuestion; }
    public void setRoundPageQuestion(RoundPageQuestion roundPageQuestion) { this.roundPageQuestion = roundPageQuestion; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
}
