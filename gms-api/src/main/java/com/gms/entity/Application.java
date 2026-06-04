package com.gms.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;

@Entity
@Table(name = "gms_application",
       uniqueConstraints = @UniqueConstraint(columnNames = {"program_round_id", "user_id"}))
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_round_id", nullable = false)
    @JsonIgnore
    private ProgramRound programRound;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    @JsonIgnore
    private Organization organization;

    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "DRAFT";

    @Column(name = "eligibility_warning", nullable = false)
    private boolean eligibilityWarning = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

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
    public ProgramRound getProgramRound() { return programRound; }
    public void setProgramRound(ProgramRound programRound) { this.programRound = programRound; }
    @jakarta.persistence.Transient
    @com.fasterxml.jackson.annotation.JsonProperty("programRoundId")
    public Long getProgramRoundIdValue() { return programRound != null ? programRound.getId() : null; }
    public Organization getOrganization() { return organization; }
    public void setOrganization(Organization organization) { this.organization = organization; }
    @jakarta.persistence.Transient
    @com.fasterxml.jackson.annotation.JsonProperty("organizationId")
    public Long getOrganizationIdValue() { return organization != null ? organization.getId() : null; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isEligibilityWarning() { return eligibilityWarning; }
    public void setEligibilityWarning(boolean eligibilityWarning) { this.eligibilityWarning = eligibilityWarning; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
