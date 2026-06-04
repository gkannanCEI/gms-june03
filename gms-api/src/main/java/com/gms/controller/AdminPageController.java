package com.gms.controller;

import com.gms.entity.Page;
import com.gms.entity.PageRule;
import com.gms.entity.PageRuleCondition;
import com.gms.entity.RoundPage;
import com.gms.entity.RoundPageQuestion;
import com.gms.exception.ValidationException;
import com.gms.repository.PageRuleRepository;
import com.gms.repository.RoundPageRepository;
import com.gms.repository.RoundPageQuestionRepository;
import com.gms.service.PageBuilderService;
import com.gms.service.PageService;
import com.gms.dto.PageRenderDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminPageController {

    private final PageService pageService;
    private final RoundPageRepository roundPageRepository;
    private final RoundPageQuestionRepository roundPageQuestionRepository;
    private final PageBuilderService pageBuilderService;
    private final PageRuleRepository pageRuleRepository;

    public AdminPageController(PageService pageService,
                               RoundPageRepository roundPageRepository,
                               RoundPageQuestionRepository roundPageQuestionRepository,
                               PageBuilderService pageBuilderService,
                               PageRuleRepository pageRuleRepository) {
        this.pageService = pageService;
        this.roundPageRepository = roundPageRepository;
        this.roundPageQuestionRepository = roundPageQuestionRepository;
        this.pageBuilderService = pageBuilderService;
        this.pageRuleRepository = pageRuleRepository;
    }

    @PostMapping("/pages")
    public ResponseEntity<Page> createPage(@RequestBody Page page) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pageService.createPage(page));
    }

    @PutMapping("/pages/{id}")
    public ResponseEntity<Page> updatePage(@PathVariable Long id, @RequestBody Page page) {
        return ResponseEntity.ok(pageService.updatePage(id, page));
    }

    @DeleteMapping("/pages/{id}")
    public ResponseEntity<Void> deactivatePage(@PathVariable Long id) {
        pageService.deactivatePage(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pages")
    public ResponseEntity<org.springframework.data.domain.Page<Page>> listPages(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "false") boolean includeInactive,
            Pageable pageable) {
        return ResponseEntity.ok(pageService.listPages(search, includeInactive, pageable));
    }

    /**
     * GAP-18: Single-page GET endpoint for loading a page for editing in the admin UI.
     */
    @GetMapping("/pages/{id}")
    public ResponseEntity<Page> getPage(@PathVariable Long id) {
        return ResponseEntity.ok(pageService.getPage(id));
    }

    // --- Question assignment within round-page context ---

    @PostMapping("/programs/{programId}/rounds/{roundId}/pages/{pageId}/questions")
    public ResponseEntity<Void> assignQuestion(@PathVariable Long programId,
                                               @PathVariable Long roundId,
                                               @PathVariable Long pageId,
                                               @RequestBody RoundPageQuestion config) {
        RoundPage roundPage = getRoundPage(roundId, pageId);
        Long questionId = config.getQuestion() != null ? config.getQuestion().getId() : null;
        if (questionId == null) {
            throw new ValidationException("questionId is required in request body");
        }
        pageService.assignQuestion(roundPage.getId(), questionId, config);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/programs/{programId}/rounds/{roundId}/pages/{pageId}/questions")
    public ResponseEntity<List<RoundPageQuestion>> listQuestionConfigs(@PathVariable Long programId,
                                                                       @PathVariable Long roundId,
                                                                       @PathVariable Long pageId) {
        RoundPage roundPage = getRoundPage(roundId, pageId);
        return ResponseEntity.ok(
            roundPageQuestionRepository.findByRoundPageIdOrderByDisplayOrderAsc(roundPage.getId()));
    }

    @GetMapping("/programs/{programId}/rounds/{roundId}/pages/{pageId}/questions/{questionId}")
    public ResponseEntity<RoundPageQuestion> getQuestionConfig(@PathVariable Long programId,
                                                               @PathVariable Long roundId,
                                                               @PathVariable Long pageId,
                                                               @PathVariable Long questionId) {
        RoundPage roundPage = getRoundPage(roundId, pageId);
        RoundPageQuestion rpq = roundPageQuestionRepository
            .findByRoundPageIdAndQuestionId(roundPage.getId(), questionId)
            .orElseThrow(() -> new ValidationException("Question not assigned to this round-page"));
        return ResponseEntity.ok(rpq);
    }

    @PutMapping("/programs/{programId}/rounds/{roundId}/pages/{pageId}/questions/{questionId}")
    public ResponseEntity<Void> updateQuestionConfig(@PathVariable Long programId,
                                                     @PathVariable Long roundId,
                                                     @PathVariable Long pageId,
                                                     @PathVariable Long questionId,
                                                     @RequestBody RoundPageQuestion updates) {
        RoundPage roundPage = getRoundPage(roundId, pageId);
        pageService.updateRoundPageQuestion(roundPage.getId(), questionId, updates);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/programs/{programId}/rounds/{roundId}/pages/{pageId}/questions/{questionId}")
    public ResponseEntity<Void> removeQuestion(@PathVariable Long programId,
                                               @PathVariable Long roundId,
                                               @PathVariable Long pageId,
                                               @PathVariable Long questionId) {
        RoundPage roundPage = getRoundPage(roundId, pageId);
        pageService.removeQuestion(roundPage.getId(), questionId);
        return ResponseEntity.noContent().build();
    }

    // --- Admin Preview ---

    /**
     * GAP-13: Admin preview passes an empty role list so ALL questions are visible
     * regardless of their visibleToRoles configuration.  An empty visibleToRoles list
     * on a question means "visible to all" (spec §8 AC; PageBuilderService.isVisibleToRoles).
     * Passing List.of("ADMIN") hid APPLICANT-only questions from the admin preview.
     */
    @GetMapping("/programs/{programId}/rounds/{roundId}/pages/{pageId}/preview")
    public ResponseEntity<PageRenderDTO> previewPage(@PathVariable Long programId,
                                                      @PathVariable Long roundId,
                                                      @PathVariable Long pageId) {
        // Empty list → isVisibleToRoles() returns true for every question (no restriction)
        PageRenderDTO page = pageBuilderService.buildPage(roundId, pageId, List.of());
        if (page == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(page);
    }

    private RoundPage getRoundPage(Long roundId, Long pageId) {
        return roundPageRepository.findByProgramRoundIdAndPageId(roundId, pageId)
            .orElseThrow(() -> new ValidationException("Page not assigned to this round"));
    }

    // --- Page Rules (Cross-Field Validation) ---

    @GetMapping("/programs/{programId}/rounds/{roundId}/pages/{pageId}/rules")
    public ResponseEntity<List<PageRule>> getPageRules(@PathVariable Long programId,
                                                       @PathVariable Long roundId,
                                                       @PathVariable Long pageId) {
        RoundPage roundPage = getRoundPage(roundId, pageId);
        return ResponseEntity.ok(pageRuleRepository.findByRoundPageId(roundPage.getId()));
    }

    @PutMapping("/programs/{programId}/rounds/{roundId}/pages/{pageId}/rules")
    public ResponseEntity<Void> savePageRules(@PathVariable Long programId,
                                              @PathVariable Long roundId,
                                              @PathVariable Long pageId,
                                              @RequestBody List<PageRule> rules) {
        RoundPage roundPage = getRoundPage(roundId, pageId);
        // Delete existing rules
        List<PageRule> existing = pageRuleRepository.findByRoundPageId(roundPage.getId());
        pageRuleRepository.deleteAll(existing);
        // Save new rules
        for (PageRule rule : rules) {
            rule.setRoundPage(roundPage);
            if (rule.getConditions() != null) {
                for (PageRuleCondition cond : rule.getConditions()) {
                    cond.setPageRule(rule);
                }
            }
            pageRuleRepository.save(rule);
        }
        return ResponseEntity.ok().build();
    }
}
