package com.gms.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "gms_program_round")
public class ProgramRound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false)
    @JsonIgnore
    private Program program;

    @Column(name = "round_name", nullable = false, length = 255)
    private String roundName;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "funds_limit", precision = 18, scale = 2)
    private BigDecimal fundsLimit;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "DRAFT";

    @Column(name = "eligible_organization_types", length = 500)
    private String eligibleOrganizationTypes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "programRound", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<RoundPage> roundPages = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Program getProgram() { return program; }
    public void setProgram(Program program) { this.program = program; }
    public String getRoundName() { return roundName; }
    public void setRoundName(String roundName) { this.roundName = roundName; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public BigDecimal getFundsLimit() { return fundsLimit; }
    public void setFundsLimit(BigDecimal fundsLimit) { this.fundsLimit = fundsLimit; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getEligibleOrganizationTypes() { return eligibleOrganizationTypes; }
    public void setEligibleOrganizationTypes(String eligibleOrganizationTypes) { this.eligibleOrganizationTypes = eligibleOrganizationTypes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<RoundPage> getRoundPages() { return roundPages; }
    public void setRoundPages(List<RoundPage> roundPages) { this.roundPages = roundPages; }
}
