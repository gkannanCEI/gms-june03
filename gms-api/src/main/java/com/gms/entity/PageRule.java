package com.gms.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "gms_page_rule")
public class PageRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_page_id", nullable = false)
    @JsonIgnore
    private RoundPage roundPage;

    @Column(name = "logic", length = 5)
    private String logic = "AND";

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "pageRule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PageRuleCondition> conditions = new ArrayList<>();

    @PrePersist
    protected void onCreate() { createdAt = Instant.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public RoundPage getRoundPage() { return roundPage; }
    public void setRoundPage(RoundPage roundPage) { this.roundPage = roundPage; }
    public String getLogic() { return logic; }
    public void setLogic(String logic) { this.logic = logic; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
    public List<PageRuleCondition> getConditions() { return conditions; }
    public void setConditions(List<PageRuleCondition> conditions) { this.conditions = conditions; }
}
