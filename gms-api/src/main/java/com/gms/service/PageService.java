package com.gms.service;

import com.gms.entity.*;
import com.gms.entity.Page;
import com.gms.exception.ConflictException;
import com.gms.exception.ValidationException;
import com.gms.repository.*;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class PageService {

    private final PageRepository pageRepository;
    private final RoundPageRepository roundPageRepository;
    private final RoundPageQuestionRepository roundPageQuestionRepository;
    private final QuestionRepository questionRepository;

    public PageService(PageRepository pageRepository,
                       RoundPageRepository roundPageRepository,
                       RoundPageQuestionRepository roundPageQuestionRepository,
                       QuestionRepository questionRepository) {
        this.pageRepository = pageRepository;
        this.roundPageRepository = roundPageRepository;
        this.roundPageQuestionRepository = roundPageQuestionRepository;
        this.questionRepository = questionRepository;
    }

    public Page createPage(Page page) {
        if (page.getPageName() == null || page.getPageName().isBlank()) {
            throw new ValidationException("pageName is required");
        }
        if (page.getPageName().length() > 200) {
            throw new ValidationException("pageName must be at most 200 characters");
        }
        if (page.getPath() != null && page.getPath().length() > 500) {
            throw new ValidationException("path must be at most 500 characters");
        }
        page.setActive(true);
        return pageRepository.save(page);
    }

    public Page updatePage(Long id, Page updates) {
        Page existing = pageRepository.findById(id)
            .orElseThrow(() -> new ValidationException("Page not found: " + id));
        if (updates.getPageName() != null) existing.setPageName(updates.getPageName());
        if (updates.getPageDescription() != null) existing.setPageDescription(updates.getPageDescription());
        if (updates.getPath() != null) existing.setPath(updates.getPath());
        return pageRepository.save(existing);
    }

    @Transactional(readOnly = true)
    public Page getPage(Long id) {
        return pageRepository.findById(id)
            .orElseThrow(() -> new ValidationException("Page not found: " + id));
    }

    /**
     * GAP-6: Replaced roundPageRepository.findAll() full-table scan with a
     * targeted findByPageId() query so only rows for this page are loaded,
     * then filters in-memory only on those rows (O(assignments) not O(all rows)).
     */
    public void deactivatePage(Long id) {
        Page page = pageRepository.findById(id)
            .orElseThrow(() -> new ValidationException("Page not found: " + id));

        if (roundPageRepository.existsByPageIdAndProgramRound_Status(id, "ACTIVE")) {
            List<RoundPage> activeAssignments = roundPageRepository.findByPageId(id).stream()
                .filter(rp -> "ACTIVE".equals(rp.getProgramRound().getStatus()))
                .toList();
            List<Map<String, Object>> affected = activeAssignments.stream()
                .map(rp -> Map.<String, Object>of(
                    "type", "ProgramRound",
                    "id", rp.getProgramRound().getId(),
                    "name", rp.getProgramRound().getRoundName()))
                .collect(Collectors.toList());
            throw new ConflictException("Page is assigned to active rounds", affected);
        }
        page.setActive(false);
        pageRepository.save(page);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Page> listPages(String search, boolean includeInactive, Pageable pageable) {
        if (includeInactive) {
            return pageRepository.findAll(pageable);
        }
        if (search != null && !search.isBlank()) {
            return pageRepository.findByActiveTrueAndPageNameContaining(search, pageable);
        }
        return pageRepository.findByActiveTrue(pageable);
    }

    public RoundPageQuestion assignQuestion(Long roundPageId, Long questionId, RoundPageQuestion config) {
        RoundPage roundPage = roundPageRepository.findById(roundPageId)
            .orElseThrow(() -> new ValidationException("RoundPage not found: " + roundPageId));
        Question question = questionRepository.findById(questionId)
            .orElseThrow(() -> new ValidationException("Question not found: " + questionId));

        if (roundPageQuestionRepository.existsByRoundPageIdAndQuestionId(roundPageId, questionId)) {
            throw new ValidationException("Question already assigned to this round-page");
        }

        config.setRoundPage(roundPage);
        config.setQuestion(question);
        if (config.getDisplayOrder() == null) config.setDisplayOrder(1);
        return roundPageQuestionRepository.save(config);
    }

    /**
     * GAP-5: Now also updates visibleToRoles when the updates body contains role entries.
     * The existing RoundPageQuestionRole records are replaced entirely (orphanRemoval = true
     * handles cleanup) when a new list is provided.
     */
    public RoundPageQuestion updateRoundPageQuestion(Long roundPageId, Long questionId, RoundPageQuestion updates) {
        RoundPageQuestion existing = roundPageQuestionRepository.findByRoundPageIdAndQuestionId(roundPageId, questionId)
            .orElseThrow(() -> new ValidationException("Question not assigned to this round-page"));

        if (updates.getDisplayOrder() != null) existing.setDisplayOrder(updates.getDisplayOrder());
        if (updates.getLabelOverride() != null) existing.setLabelOverride(updates.getLabelOverride());
        if (updates.getRequiredOverride() != null) existing.setRequiredOverride(updates.getRequiredOverride());
        if (updates.getRoundLabelOverride() != null) existing.setRoundLabelOverride(updates.getRoundLabelOverride());
        existing.setExcluded(updates.isExcluded());
        existing.setReadonly(updates.isReadonly());
        existing.setColumnSpan(updates.getColumnSpan());
        if (updates.getLabelPosition() != null) existing.setLabelPosition(updates.getLabelPosition());
        if (updates.getHelpText() != null) existing.setHelpText(updates.getHelpText());
        if (updates.getSectionGroup() != null) existing.setSectionGroup(updates.getSectionGroup());
        if (updates.getMinValue() != null) existing.setMinValue(updates.getMinValue());
        if (updates.getMaxValue() != null) existing.setMaxValue(updates.getMaxValue());

        // GAP-5: Update visibleToRoles — replace existing role records with the incoming set.
        // orphanRemoval = true on the collection will delete the removed RoundPageQuestionRole rows.
        if (updates.getVisibleToRoles() != null) {
            existing.getVisibleToRoles().clear();
            for (RoundPageQuestionRole incoming : updates.getVisibleToRoles()) {
                RoundPageQuestionRole role = new RoundPageQuestionRole();
                role.setRoundPageQuestion(existing);
                role.setRoleName(incoming.getRoleName());
                existing.getVisibleToRoles().add(role);
            }
        }

        // Update visibility rules — replace existing with incoming set.
        if (updates.getVisibilityRules() != null) {
            existing.getVisibilityRules().clear();
            for (VisibilityRule incoming : updates.getVisibilityRules()) {
                VisibilityRule rule = new VisibilityRule();
                rule.setRoundPageQuestion(existing);
                rule.setTriggerQuestionId(incoming.getTriggerQuestionId());
                rule.setOperator(incoming.getOperator());
                rule.setValue(incoming.getValue());
                rule.setLogic(incoming.getLogic());
                existing.getVisibilityRules().add(rule);
            }
        }

        // Update formula for calculated fields.
        if (updates.getFormula() != null) {
            existing.setFormula(updates.getFormula().isBlank() ? null : updates.getFormula());
        }

        return roundPageQuestionRepository.save(existing);
    }

    public void removeQuestion(Long roundPageId, Long questionId) {
        RoundPageQuestion rpq = roundPageQuestionRepository.findByRoundPageIdAndQuestionId(roundPageId, questionId)
            .orElseThrow(() -> new ValidationException("Question not assigned to this round-page"));
        roundPageQuestionRepository.delete(rpq);
    }
}
