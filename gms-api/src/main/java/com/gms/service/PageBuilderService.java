package com.gms.service;

import com.gms.dto.*;
import com.gms.entity.*;
import com.gms.repository.ApplicationDataRepository;
import com.gms.repository.RoundPageQuestionRepository;
import com.gms.repository.RoundPageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PageBuilderService {

    private final RoundPageRepository roundPageRepository;
    private final RoundPageQuestionRepository roundPageQuestionRepository;
    private final com.gms.repository.PageRuleRepository pageRuleRepository;
    private final ApplicationDataRepository applicationDataRepository;
    private final AllowlistService allowlistService;

    public PageBuilderService(RoundPageRepository roundPageRepository,
                              RoundPageQuestionRepository roundPageQuestionRepository,
                              com.gms.repository.PageRuleRepository pageRuleRepository,
                              ApplicationDataRepository applicationDataRepository,
                              AllowlistService allowlistService) {
        this.roundPageRepository = roundPageRepository;
        this.roundPageQuestionRepository = roundPageQuestionRepository;
        this.pageRuleRepository = pageRuleRepository;
        this.applicationDataRepository = applicationDataRepository;
        this.allowlistService = allowlistService;
    }

    public PageRenderDTO buildPage(Long roundId, Long pageId, List<String> userRoles) {
        RoundPage roundPage = roundPageRepository.findByProgramRoundIdAndPageId(roundId, pageId)
            .orElse(null);
        if (roundPage == null) return null;

        String resolvedPageName = roundPage.getPageNameOverride() != null && !roundPage.getPageNameOverride().isBlank()
            ? roundPage.getPageNameOverride() : roundPage.getPage().getPageName();
        String resolvedPageDesc = roundPage.getPageDescriptionOverride() != null && !roundPage.getPageDescriptionOverride().isBlank()
            ? roundPage.getPageDescriptionOverride() : roundPage.getPage().getPageDescription();

        List<RoundPageQuestion> allQuestions = roundPageQuestionRepository
            .findByRoundPageIdOrderByDisplayOrderAsc(roundPage.getId());

        List<QuestionRenderDTO> renderedQuestions = new ArrayList<>();
        for (RoundPageQuestion rpq : allQuestions) {
            if (rpq.isExcluded()) continue;
            if (!isVisibleToRoles(rpq, userRoles)) continue;
            Question q = rpq.getQuestion();
            if (q.getParentQuestion() != null) continue; // children handled by parent

            QuestionRenderDTO dto = buildQuestionRenderDTO(rpq, allQuestions, userRoles);
            renderedQuestions.add(dto);
        }

        PageRenderDTO pageRender = new PageRenderDTO();
        pageRender.setProgramId(roundPage.getProgramRound().getProgram().getId());
        pageRender.setRoundId(roundPage.getProgramRound().getId());
        pageRender.setPageId(roundPage.getPage().getId());
        pageRender.setPageName(resolvedPageName);
        pageRender.setPageDescription(resolvedPageDesc);
        pageRender.setPath(roundPage.getPage().getPath());
        pageRender.setQuestions(renderedQuestions);

        // Page rules
        List<com.gms.entity.PageRule> pageRules = pageRuleRepository.findByRoundPageId(roundPage.getId());
        if (!pageRules.isEmpty()) {
            pageRender.setPageRules(pageRules.stream().map(pr -> {
                com.gms.dto.PageRuleDTO dto2 = new com.gms.dto.PageRuleDTO();
                dto2.setId(pr.getId());
                dto2.setLogic(pr.getLogic());
                dto2.setErrorMessage(pr.getErrorMessage());
                dto2.setConditions(pr.getConditions().stream()
                    .map(c -> new com.gms.dto.PageRuleDTO.PageRuleConditionDTO(
                        c.getQuestionId(), c.getOperator(), c.getValue(), c.getValue2()))
                    .collect(Collectors.toList()));
                return dto2;
            }).collect(Collectors.toList()));
        }

        return pageRender;
    }

    /**
     * Returns the ordered list of pages visible to the current user for a round.
     *
     * When {@code appId} is supplied (non-null), each page's {@code completed} flag
     * is computed as follows:
     *   - Collect all visible, non-excluded, non-LABEL questions on the page.
     *   - For each question, check whether the application has a non-blank saved answer
     *     in the correct store (gms_application_data OR the question's domain table).
     *   - The page is complete when EVERY such question has a saved answer.
     *   - Pages with no answerable questions are considered complete by default.
     *
     * Questions in gms_application_data are batched into a single COUNT query.
     * Questions in domain tables are checked individually (one query per unique table/column).
     */
    public List<PageSummaryDTO> getPagesForRound(Long roundId, List<String> userRoles, Long appId) {
        List<RoundPage> roundPages = roundPageRepository.findByProgramRoundIdOrderByDisplayOrderAsc(roundId);
        List<PageSummaryDTO> summaries = new ArrayList<>();

        for (RoundPage rp : roundPages) {
            List<RoundPageQuestion> questions = roundPageQuestionRepository
                .findByRoundPageIdOrderByDisplayOrderAsc(rp.getId());

            // Only include pages that have at least one visible, answerable question
            List<RoundPageQuestion> answerableQuestions = questions.stream()
                .filter(rpq -> !rpq.isExcluded()
                    && isVisibleToRoles(rpq, userRoles)
                    && !"LABEL".equals(rpq.getQuestion().getQuestionType()))
                .collect(Collectors.toList());

            if (answerableQuestions.isEmpty()) continue;

            String name = rp.getPageNameOverride() != null && !rp.getPageNameOverride().isBlank()
                ? rp.getPageNameOverride() : rp.getPage().getPageName();

            boolean completed = false;
            if (appId != null) {
                completed = isPageComplete(appId, answerableQuestions);
            }

            String path = rp.getPage() != null ? rp.getPage().getPath() : null;

            summaries.add(new PageSummaryDTO(rp.getPage().getId(), name, path, rp.getDisplayOrder(), completed));
        }
        return summaries;
    }

    /**
     * Returns true when every answerable question on the page has a saved non-blank
     * answer for the given application.
     *
     * Strategy:
     *   1. Separate questions into two buckets:
     *      a. Single-table (targetTable is null or 'gms_application_data')
     *      b. Domain-table (any other targetTable)
     *   2. Batch-check the single-table bucket in one SQL COUNT query.
     *   3. Check each domain-table question individually (one query per question,
     *      since each may point to a different table/column).
     *   4. All questions must have answers for the page to be complete.
     */
    private boolean isPageComplete(Long appId, List<RoundPageQuestion> answerableQuestions) {
        List<Long> singleTableIds = new ArrayList<>();
        List<RoundPageQuestion> domainTableQuestions = new ArrayList<>();

        for (RoundPageQuestion rpq : answerableQuestions) {
            String targetTable = rpq.getQuestion().getTargetTable();
            if (targetTable == null || "gms_application_data".equalsIgnoreCase(targetTable)) {
                singleTableIds.add(rpq.getQuestion().getId());
            } else {
                domainTableQuestions.add(rpq);
            }
        }

        // Check single-table answers in one batch query
        if (!singleTableIds.isEmpty()) {
            long answered = applicationDataRepository.countAnsweredInSingleTable(appId, singleTableIds);
            if (answered < singleTableIds.size()) {
                return false; // At least one single-table question is unanswered
            }
        }

        // Check domain-table answers individually
        for (RoundPageQuestion rpq : domainTableQuestions) {
            String table  = rpq.getQuestion().getTargetTable();
            String column = rpq.getQuestion().getTargetColumn();
            if (table == null || column == null) continue; // misconfigured — skip
            if (!allowlistService.isPermitted(table, column)) continue; // not in allowlist — skip checking

            if (!applicationDataRepository.hasAnswerInDomainTable(table, column, appId)) {
                return false; // This domain-table question is unanswered
            }
        }

        return true; // All questions have saved answers
    }

    private QuestionRenderDTO buildQuestionRenderDTO(RoundPageQuestion rpq,
                                                      List<RoundPageQuestion> allQuestions,
                                                      List<String> userRoles) {
        Question q = rpq.getQuestion();
        QuestionRenderDTO dto = new QuestionRenderDTO();
        dto.setQuestionId(q.getId());
        dto.setQuestionType(q.getQuestionType());
        dto.setLabel(rpq.getResolvedLabel());
        dto.setRequired(rpq.getResolvedRequired());
        dto.setValidationRegex(q.getValidationRegex());
        dto.setMinValue(rpq.getMinValue());
        dto.setMaxValue(rpq.getMaxValue());
        dto.setAllowedFileTypes(q.getAllowedFileTypes());
        dto.setMaxFileSizeMb(q.getMaxFileSizeMb());
        dto.setDisplayOrder(rpq.getDisplayOrder());

        // Display config
        QuestionDisplayConfig displayConfig = new QuestionDisplayConfig();
        displayConfig.setReadonly(rpq.isReadonly());
        displayConfig.setColumnSpan(rpq.getColumnSpan());
        displayConfig.setLabelPosition(rpq.getLabelPosition());
        displayConfig.setHelpText(rpq.getHelpText());
        displayConfig.setSectionGroup(rpq.getSectionGroup());
        dto.setDisplayConfig(displayConfig);

        // Formula (calculated fields)
        if (rpq.getFormula() != null && !rpq.getFormula().isBlank()) {
            dto.setFormula(rpq.getFormula());
        }

        // Visibility rules
        if (rpq.getVisibilityRules() != null && !rpq.getVisibilityRules().isEmpty()) {
            dto.setVisibilityRules(rpq.getVisibilityRules().stream()
                .map(vr -> new com.gms.dto.VisibilityRuleDTO(
                    vr.getId(), vr.getTriggerQuestionId(), vr.getOperator(), vr.getValue(), vr.getLogic()))
                .collect(Collectors.toList()));
        }

        // Options
        if (q.getOptions() != null && !q.getOptions().isEmpty()) {
            dto.setOptions(q.getOptions().stream()
                .map(opt -> new QuestionOptionDTO(opt.getId(), opt.getOptionLabel(),
                    opt.getOptionValue(), opt.getDisplayOrder(), opt.getIsOtherOption()))
                .collect(Collectors.toList()));
        }

        // Child questions
        if ("RADIO_YES_NO".equals(q.getQuestionType())) {
            List<ChildQuestionRenderDTO> children = new ArrayList<>();
            for (RoundPageQuestion childRpq : allQuestions) {
                Question childQ = childRpq.getQuestion();
                if (childQ.getParentQuestion() != null && childQ.getParentQuestion().getId().equals(q.getId())) {
                    if (childRpq.isExcluded()) continue;
                    if (!isVisibleToRoles(childRpq, userRoles)) continue;
                    ChildQuestionRenderDTO childDto = new ChildQuestionRenderDTO();
                    childDto.setTriggerValue(childQ.getTriggerValue());
                    childDto.setQuestion(buildQuestionRenderDTO(childRpq, allQuestions, userRoles));
                    children.add(childDto);
                }
            }
            if (!children.isEmpty()) dto.setChildQuestions(children);
        }

        return dto;
    }

    private boolean isVisibleToRoles(RoundPageQuestion rpq, List<String> userRoles) {
        List<RoundPageQuestionRole> visibleTo = rpq.getVisibleToRoles();
        if (visibleTo == null || visibleTo.isEmpty()) return true;
        if (userRoles == null || userRoles.isEmpty()) return false;
        Set<String> allowedRoles = visibleTo.stream()
            .map(RoundPageQuestionRole::getRoleName)
            .collect(Collectors.toSet());
        return userRoles.stream().anyMatch(allowedRoles::contains);
    }
}
