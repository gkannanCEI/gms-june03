package com.gms.dto;

import java.util.List;

public class PageRenderDTO {
    private Long programId;
    private Long roundId;
    private Long pageId;
    private String pageName;
    private String pageDescription;
    private String path;
    private List<QuestionRenderDTO> questions;
    private List<PageRuleDTO> pageRules;

    public Long getProgramId() { return programId; }
    public void setProgramId(Long programId) { this.programId = programId; }
    public Long getRoundId() { return roundId; }
    public void setRoundId(Long roundId) { this.roundId = roundId; }
    public Long getPageId() { return pageId; }
    public void setPageId(Long pageId) { this.pageId = pageId; }
    public String getPageName() { return pageName; }
    public void setPageName(String pageName) { this.pageName = pageName; }
    public String getPageDescription() { return pageDescription; }
    public void setPageDescription(String pageDescription) { this.pageDescription = pageDescription; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public List<QuestionRenderDTO> getQuestions() { return questions; }
    public void setQuestions(List<QuestionRenderDTO> questions) { this.questions = questions; }
    public List<PageRuleDTO> getPageRules() { return pageRules; }
    public void setPageRules(List<PageRuleDTO> pageRules) { this.pageRules = pageRules; }
}
