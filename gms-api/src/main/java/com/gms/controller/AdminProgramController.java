package com.gms.controller;

import com.gms.dto.BulkRoundSetupRequest;
import com.gms.entity.*;
import com.gms.repository.PageRuleRepository;
import com.gms.repository.RoundPageQuestionRepository;
import com.gms.repository.RoundPageRepository;
import com.gms.service.BulkRoundSetupService;
import com.gms.service.ProgramRoundService;
import com.gms.service.ProgramService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin/programs")
public class AdminProgramController {

    private final ProgramService programService;
    private final ProgramRoundService roundService;
    private final BulkRoundSetupService bulkRoundSetupService;
    private final RoundPageQuestionRepository roundPageQuestionRepository;
    private final RoundPageRepository roundPageRepository;
    private final PageRuleRepository pageRuleRepository;

    public AdminProgramController(ProgramService programService,
                                ProgramRoundService roundService,
                                BulkRoundSetupService bulkRoundSetupService,
                                RoundPageQuestionRepository roundPageQuestionRepository,
                                RoundPageRepository roundPageRepository,
                                PageRuleRepository pageRuleRepository) {
        this.programService = programService;
        this.roundService = roundService;
        this.bulkRoundSetupService = bulkRoundSetupService;
        this.roundPageQuestionRepository = roundPageQuestionRepository;
        this.roundPageRepository = roundPageRepository;
        this.pageRuleRepository = pageRuleRepository;
    }

    @PostMapping
    public ResponseEntity<Program> createProgram(@RequestBody Program program) {
        return ResponseEntity.status(HttpStatus.CREATED).body(programService.createProgram(program));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Program> updateProgram(@PathVariable Long id, @RequestBody Program program) {
        return ResponseEntity.ok(programService.updateProgram(id, program));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> archiveProgram(@PathVariable Long id) {
        programService.archiveProgram(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Page<Program>> listPrograms(Pageable pageable) {
        return ResponseEntity.ok(programService.listPrograms(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Program> getProgram(@PathVariable Long id) {
        return ResponseEntity.ok(programService.getProgram(id));
    }

    // --- Rounds ---

    @PostMapping("/{programId}/rounds")
    public ResponseEntity<ProgramRound> createRound(@PathVariable Long programId, @RequestBody ProgramRound round) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roundService.createRound(programId, round));
    }

    @PutMapping("/{programId}/rounds/{roundId}")
    public ResponseEntity<ProgramRound> updateRound(@PathVariable Long programId,
                                                     @PathVariable Long roundId,
                                                     @RequestBody ProgramRound round) {
        return ResponseEntity.ok(roundService.updateRound(roundId, round));
    }

    /**
     * GAP-4: Soft-delete (archive) a round regardless of its current status.
     * Using roundService.archiveRound() which sets the status field directly
     * instead of going through the lifecycle transitionStatus() which requires
     * CLOSED -> ARCHIVED ordering and would fail for DRAFT/ACTIVE rounds.
     */
    @DeleteMapping("/{programId}/rounds/{roundId}")
    public ResponseEntity<Void> archiveRound(@PathVariable Long programId, @PathVariable Long roundId) {
        roundService.archiveRound(roundId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{programId}/rounds/{roundId}")
    public ResponseEntity<ProgramRound> getRound(@PathVariable Long programId,
                                                  @PathVariable Long roundId) {
        return ResponseEntity.ok(roundService.getRound(roundId));
    }

    @GetMapping("/{programId}/rounds")
    public ResponseEntity<List<ProgramRound>> listRounds(@PathVariable Long programId) {
        return ResponseEntity.ok(roundService.listRounds(programId));
    }

    @PutMapping("/{programId}/rounds/{roundId}/status")
    public ResponseEntity<ProgramRound> updateRoundStatus(@PathVariable Long programId,
                                                           @PathVariable Long roundId,
                                                           @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(roundService.transitionStatus(roundId, body.get("status")));
    }

    // --- Round Pages ---

    @PostMapping("/{programId}/rounds/{roundId}/pages")
    public ResponseEntity<Void> assignPage(@PathVariable Long programId,
                                           @PathVariable Long roundId,
                                           @RequestBody Map<String, Object> body) {
        if (body.get("pageId") == null) {
            throw new com.gms.exception.ValidationException("pageId is required");
        }
        Long pageId = Long.valueOf(body.get("pageId").toString());
        Integer displayOrder = body.containsKey("displayOrder")
            ? Integer.valueOf(body.get("displayOrder").toString()) : null;
        roundService.assignPage(roundId, pageId, displayOrder);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{programId}/rounds/{roundId}/pages")
    public ResponseEntity<List<RoundPage>> listRoundPages(@PathVariable Long programId,
                                                           @PathVariable Long roundId) {
        return ResponseEntity.ok(roundService.listRoundPages(roundId));
    }

    @DeleteMapping("/{programId}/rounds/{roundId}/pages/{pageId}")
    public ResponseEntity<Void> removePage(@PathVariable Long programId,
                                           @PathVariable Long roundId,
                                           @PathVariable Long pageId) {
        roundService.removePage(roundId, pageId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{programId}/rounds/{roundId}/pages/{pageId}")
    public ResponseEntity<Void> updateRoundPage(@PathVariable Long programId,
                                                @PathVariable Long roundId,
                                                @PathVariable Long pageId,
                                                @RequestBody Map<String, Object> body) {
        String nameOverride = body.containsKey("pageNameOverride")
            ? body.get("pageNameOverride").toString() : null;
        String descOverride = body.containsKey("pageDescriptionOverride")
            ? body.get("pageDescriptionOverride").toString() : null;
        Integer displayOrder = body.containsKey("displayOrder")
            ? Integer.valueOf(body.get("displayOrder").toString()) : null;
        roundService.updateRoundPage(roundId, pageId, nameOverride, descOverride, displayOrder);
        return ResponseEntity.ok().build();
    }

    // --- Bulk Round Setup ---

    @PostMapping("/bulk-round-setup")
    public ResponseEntity<ProgramRound> bulkRoundSetup(@RequestBody BulkRoundSetupRequest request) {
        ProgramRound round = bulkRoundSetupService.setupRound(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(round);
    }

    // --- Import from external form JSON (sections/fields format) ---

    @SuppressWarnings("unchecked")
    @PostMapping("/import-form")
    public ResponseEntity<ProgramRound> importForm(@RequestBody Map<String, Object> body) {
        Long programId = body.containsKey("programId") ? Long.valueOf(body.get("programId").toString()) : 1L;
        String roundName = "round_" + java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        List<Map<String, Object>> sections = (List<Map<String, Object>>) body.get("sections");
        if (sections == null || sections.isEmpty()) {
            throw new com.gms.exception.ValidationException("Request must contain 'sections' array");
        }

        BulkRoundSetupRequest request = new BulkRoundSetupRequest();
        request.setProgramId(programId);
        request.setRoundName(roundName);

        // Track externalFieldId → field data for post-processing visibility/page rules
        List<String> fieldExternalIds = new java.util.ArrayList<>();

        List<BulkRoundSetupRequest.PageSetup> pages = new java.util.ArrayList<>();
        for (Map<String, Object> section : sections) {
            BulkRoundSetupRequest.PageSetup page = new BulkRoundSetupRequest.PageSetup();
            page.setPageName((String) section.getOrDefault("title", "Untitled Page"));
            page.setPageDescription(null);

            List<Map<String, Object>> fields = (List<Map<String, Object>>) section.get("fields");
            List<BulkRoundSetupRequest.QuestionSetup> questions = new java.util.ArrayList<>();
            if (fields != null) {
                for (Map<String, Object> field : fields) {
                    String fieldId = (String) field.get("id");
                    fieldExternalIds.add(fieldId);

                    BulkRoundSetupRequest.QuestionSetup q = new BulkRoundSetupRequest.QuestionSetup();
                    q.setLabel((String) field.getOrDefault("label", ""));
                    q.setQuestionType(mapFieldType((String) field.get("type")));
                    q.setRequired(Boolean.TRUE.equals(field.get("required")));

                    // Validation constraints
                    Map<String, Object> validation = (Map<String, Object>) field.get("validation");
                    if (validation != null) {
                        if (validation.containsKey("min")) q.setMinValue(validation.get("min").toString());
                        if (validation.containsKey("max")) q.setMaxValue(validation.get("max").toString());
                        if (validation.containsKey("minLength")) q.setMinValue(validation.get("minLength").toString());
                        if (validation.containsKey("maxLength")) q.setMaxValue(validation.get("maxLength").toString());
                    }

                    // Options
                    List<String> opts = (List<String>) field.get("options");
                    if (opts != null && !opts.isEmpty()) {
                        List<BulkRoundSetupRequest.OptionSetup> optionSetups = new java.util.ArrayList<>();
                        for (String opt : opts) {
                            BulkRoundSetupRequest.OptionSetup os = new BulkRoundSetupRequest.OptionSetup();
                            os.setOptionLabel(opt);
                            os.setOptionValue(opt.toLowerCase().replaceAll("[^a-z0-9]+", "_"));
                            optionSetups.add(os);
                        }
                        q.setOptions(optionSetups);
                    }
                    questions.add(q);
                }
            }
            page.setQuestions(questions);
            pages.add(page);
        }
        request.setPages(pages);

        ProgramRound round = bulkRoundSetupService.setupRound(request);

        // Post-processing: set externalId on questions and process visibility/page rules
        // Retrieve round pages in order and build the externalId → questionId mapping
        List<RoundPage> roundPages = roundPageRepository.findByProgramRoundIdOrderByDisplayOrderAsc(round.getId());

        // Build mapping: externalFieldId → RoundPageQuestion
        Map<String, RoundPageQuestion> externalIdToRpq = new HashMap<>();
        Map<String, Long> externalIdToQuestionId = new HashMap<>();
        int fieldIndex = 0;

        for (RoundPage rp : roundPages) {
            List<RoundPageQuestion> rpqs = roundPageQuestionRepository.findByRoundPageIdOrderByDisplayOrderAsc(rp.getId());
            for (RoundPageQuestion rpq : rpqs) {
                if (fieldIndex < fieldExternalIds.size()) {
                    String extId = fieldExternalIds.get(fieldIndex);
                    if (extId != null) {
                        // Set externalId on the question entity
                        rpq.getQuestion().setExternalId(extId);
                        externalIdToRpq.put(extId, rpq);
                        externalIdToQuestionId.put(extId, rpq.getQuestion().getId());
                    }
                    fieldIndex++;
                }
            }
        }

        // Process visibility rules and page rules per section
        int sectionIndex = 0;
        for (Map<String, Object> section : sections) {
            List<Map<String, Object>> fields = (List<Map<String, Object>>) section.get("fields");
            if (fields != null) {
                for (Map<String, Object> field : fields) {
                    String fieldId = (String) field.get("id");
                    if (fieldId == null) continue;

                    List<Map<String, Object>> visibilityRules = (List<Map<String, Object>>) field.get("visibilityRules");
                    if (visibilityRules != null && !visibilityRules.isEmpty()) {
                        RoundPageQuestion rpq = externalIdToRpq.get(fieldId);
                        if (rpq != null) {
                            for (Map<String, Object> ruleData : visibilityRules) {
                                String triggerFieldId = (String) ruleData.get("field");
                                Long triggerQuestionId = externalIdToQuestionId.get(triggerFieldId);
                                if (triggerQuestionId == null) continue;

                                VisibilityRule rule = new VisibilityRule();
                                rule.setRoundPageQuestion(rpq);
                                rule.setTriggerQuestionId(triggerQuestionId);
                                rule.setOperator(mapOperator((String) ruleData.get("operator")));
                                rule.setValue(ruleData.get("value") != null ? ruleData.get("value").toString() : null);
                                rule.setLogic(ruleData.containsKey("logic") ? (String) ruleData.get("logic") : "AND");
                                rpq.getVisibilityRules().add(rule);
                            }
                            roundPageQuestionRepository.save(rpq);
                        }
                    }
                }
            }

            // Process page rules for this section
            List<Map<String, Object>> pageRulesData = (List<Map<String, Object>>) section.get("pageRules");
            if (pageRulesData != null && !pageRulesData.isEmpty() && sectionIndex < roundPages.size()) {
                RoundPage roundPage = roundPages.get(sectionIndex);
                for (Map<String, Object> prData : pageRulesData) {
                    PageRule pageRule = new PageRule();
                    pageRule.setRoundPage(roundPage);
                    pageRule.setLogic(prData.containsKey("logic") ? (String) prData.get("logic") : "AND");
                    pageRule.setErrorMessage((String) prData.get("errorMessage"));
                    pageRule = pageRuleRepository.save(pageRule);

                    List<Map<String, Object>> conditions = (List<Map<String, Object>>) prData.get("conditions");
                    if (conditions != null) {
                        for (Map<String, Object> condData : conditions) {
                            String condFieldId = (String) condData.get("field");
                            Long condQuestionId = externalIdToQuestionId.get(condFieldId);
                            if (condQuestionId == null) continue;

                            PageRuleCondition condition = new PageRuleCondition();
                            condition.setPageRule(pageRule);
                            condition.setQuestionId(condQuestionId);
                            condition.setOperator(mapOperator((String) condData.get("operator")));
                            condition.setValue(condData.get("value") != null ? condData.get("value").toString() : null);
                            condition.setValue2(condData.get("value2") != null ? condData.get("value2").toString() : null);
                            pageRule.getConditions().add(condition);
                        }
                        pageRuleRepository.save(pageRule);
                    }
                }
            }
            sectionIndex++;
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(round);
    }

    private String mapFieldType(String externalType) {
        if (externalType == null) return "TEXT";
        return switch (externalType.toLowerCase()) {
            case "text" -> "TEXT";
            case "textarea" -> "TEXT_AREA";
            case "number" -> "WHOLE_NUMBER";
            case "email" -> "TEXT";
            case "date" -> "DATE";
            case "select" -> "SELECT_ONE";
            case "radio" -> "RADIO_YES_NO";
            case "checkbox" -> "SELECT_MULTI";
            default -> "TEXT";
        };
    }

    private String mapOperator(String externalOperator) {
        if (externalOperator == null) return "EQUALS";
        return switch (externalOperator.toLowerCase()) {
            case "is_not_empty" -> "IS_NOT_EMPTY";
            case "is_empty" -> "IS_EMPTY";
            case "equals" -> "EQUALS";
            case "not_equals" -> "NOT_EQUALS";
            case "less_than" -> "LESS_THAN";
            case "greater_than" -> "GREATER_THAN";
            case "contains" -> "CONTAINS";
            default -> externalOperator.toUpperCase();
        };
    }
}
