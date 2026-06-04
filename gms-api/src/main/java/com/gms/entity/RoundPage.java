package com.gms.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "gms_round_page")
public class RoundPage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_round_id", nullable = false)
    @JsonIgnore
    private ProgramRound programRound;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "page_id", nullable = false)
    private Page page;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "page_name_override", length = 200)
    private String pageNameOverride;

    @Column(name = "page_description_override", length = 1000)
    private String pageDescriptionOverride;

    @OneToMany(mappedBy = "roundPage", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    @JsonIgnore
    private List<RoundPageQuestion> roundPageQuestions = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ProgramRound getProgramRound() { return programRound; }
    public void setProgramRound(ProgramRound programRound) { this.programRound = programRound; }
    public Page getPage() { return page; }
    public void setPage(Page page) { this.page = page; }
    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
    public String getPageNameOverride() { return pageNameOverride; }
    public void setPageNameOverride(String pageNameOverride) { this.pageNameOverride = pageNameOverride; }
    public String getPageDescriptionOverride() { return pageDescriptionOverride; }
    public void setPageDescriptionOverride(String pageDescriptionOverride) { this.pageDescriptionOverride = pageDescriptionOverride; }
    public List<RoundPageQuestion> getRoundPageQuestions() { return roundPageQuestions; }
    public void setRoundPageQuestions(List<RoundPageQuestion> roundPageQuestions) { this.roundPageQuestions = roundPageQuestions; }
}
