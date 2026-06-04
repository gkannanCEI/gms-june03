package com.gms.controller;

import com.gms.dto.ChildLinkDTO;
import com.gms.dto.QuestionOptionDTO;
import com.gms.entity.Question;
import com.gms.exception.ValidationException;
import com.gms.service.QuestionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/questions")
public class AdminQuestionController {

    private final QuestionService questionService;

    public AdminQuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @PostMapping
    public ResponseEntity<Question> createQuestion(@RequestBody Question question) {
        Question created = questionService.createQuestion(question);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Question> updateQuestion(@PathVariable Long id, @RequestBody Question question) {
        Question updated = questionService.updateQuestion(id, question);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateQuestion(@PathVariable Long id) {
        questionService.deactivateQuestion(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Page<Question>> listQuestions(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "false") boolean includeInactive,
            Pageable pageable) {
        Page<Question> questions = questionService.listQuestions(type, search, includeInactive, pageable);
        return ResponseEntity.ok(questions);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Question> getQuestion(@PathVariable Long id) {
        Question question = questionService.getQuestion(id);
        return ResponseEntity.ok(question);
    }

    // --- Child question links ---

    /**
     * GAP-3/GAP-14: Returns List<ChildLinkDTO> instead of the @JsonIgnore entity collection.
     */
    @GetMapping("/{parentId}/children")
    public ResponseEntity<List<ChildLinkDTO>> listChildren(@PathVariable Long parentId) {
        return ResponseEntity.ok(questionService.listChildren(parentId));
    }

    /**
     * GAP-2: Returns HTTP 201 + ChildLinkDTO instead of raw Map.
     */
    @PostMapping("/{parentId}/children")
    public ResponseEntity<ChildLinkDTO> linkChild(
            @PathVariable Long parentId,
            @RequestBody Map<String, Object> body) {
        Long childId = parseLong(body, "childQuestionId");
        String triggerValue = parseString(body, "triggerValue");
        Integer childDisplayOrder = parseInteger(body, "childDisplayOrder");
        ChildLinkDTO link = questionService.linkChild(parentId, childId, triggerValue, childDisplayOrder);
        return ResponseEntity.status(HttpStatus.CREATED).body(link);
    }

    /**
     * GAP-17: New endpoint — update triggerValue / childDisplayOrder on an existing link.
     * Returns HTTP 200 + ChildLinkDTO.
     */
    @PutMapping("/{parentId}/children/{childId}")
    public ResponseEntity<ChildLinkDTO> updateChildLink(
            @PathVariable Long parentId,
            @PathVariable Long childId,
            @RequestBody Map<String, Object> body) {
        String triggerValue = body.containsKey("triggerValue")
            ? parseString(body, "triggerValue") : null;
        Integer childDisplayOrder = body.containsKey("childDisplayOrder")
            ? parseInteger(body, "childDisplayOrder") : null;
        ChildLinkDTO link = questionService.updateChildLink(parentId, childId, triggerValue, childDisplayOrder);
        return ResponseEntity.ok(link);
    }

    @DeleteMapping("/{parentId}/children/{childId}")
    public ResponseEntity<Void> unlinkChild(@PathVariable Long parentId, @PathVariable Long childId) {
        questionService.unlinkChild(parentId, childId);
        return ResponseEntity.noContent().build();
    }

    // --- Option CRUD ---

    /**
     * GAP-2: Returns HTTP 201 + QuestionOptionDTO (including persisted id) instead of echoing
     * the raw request body.
     */
    @PostMapping("/{id}/options")
    public ResponseEntity<QuestionOptionDTO> addOption(@PathVariable Long id,
                                                        @RequestBody Map<String, Object> body) {
        Question question = questionService.getQuestion(id);
        QuestionOptionDTO created = questionService.addOption(question,
            parseString(body, "optionLabel"),
            parseString(body, "optionValue"),
            body.containsKey("displayOrder") ? parseInteger(body, "displayOrder") : null,
            body.containsKey("isOtherOption")
                ? Boolean.parseBoolean(body.get("isOtherOption").toString()) : null,
            (String) body.get("optionTargetTable"),
            (String) body.get("optionTargetColumn"),
            (String) body.get("lookupId"));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * GAP-2: Returns HTTP 200 + QuestionOptionDTO instead of Void.
     */
    @PutMapping("/{id}/options/{optionId}")
    public ResponseEntity<QuestionOptionDTO> updateOption(@PathVariable Long id,
                                                           @PathVariable Long optionId,
                                                           @RequestBody Map<String, Object> body) {
        QuestionOptionDTO updated = questionService.updateOption(id, optionId,
            (String) body.get("optionLabel"),
            (String) body.get("optionValue"),
            body.containsKey("displayOrder") ? parseInteger(body, "displayOrder") : null,
            body.containsKey("isOtherOption")
                ? Boolean.parseBoolean(body.get("isOtherOption").toString()) : null,
            (String) body.get("optionTargetTable"),
            (String) body.get("optionTargetColumn"),
            (String) body.get("lookupId"));
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}/options/{optionId}")
    public ResponseEntity<Void> removeOption(@PathVariable Long id, @PathVariable Long optionId) {
        questionService.removeOption(id, optionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * GAP-2: Returns HTTP 200 + List<QuestionOptionDTO> instead of Void.
     */
    @PutMapping("/{id}/options/reorder")
    public ResponseEntity<List<QuestionOptionDTO>> reorderOptions(@PathVariable Long id,
                                                                   @RequestBody List<Long> orderedOptionIds) {
        List<QuestionOptionDTO> reordered = questionService.reorderOptions(id, orderedOptionIds);
        return ResponseEntity.ok(reordered);
    }

    // --- Private helpers to parse Map<String,Object> bodies safely ---

    private String parseString(Map<String, Object> body, String key) {
        Object val = body.get(key);
        if (val == null) throw new ValidationException("Missing required field: " + key);
        return val.toString();
    }

    private Long parseLong(Map<String, Object> body, String key) {
        Object val = body.get(key);
        if (val == null) throw new ValidationException("Missing required field: " + key);
        try {
            return Long.valueOf(val.toString());
        } catch (NumberFormatException e) {
            throw new ValidationException("Field '" + key + "' must be a numeric id");
        }
    }

    private Integer parseInteger(Map<String, Object> body, String key) {
        Object val = body.get(key);
        if (val == null) return null;
        try {
            return Integer.valueOf(val.toString());
        } catch (NumberFormatException e) {
            throw new ValidationException("Field '" + key + "' must be an integer");
        }
    }
}
