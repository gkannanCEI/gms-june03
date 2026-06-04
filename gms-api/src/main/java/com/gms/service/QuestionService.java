package com.gms.service;

import com.gms.dto.ChildLinkDTO;
import com.gms.dto.QuestionOptionDTO;
import com.gms.entity.Question;
import com.gms.entity.QuestionOption;
import com.gms.exception.ValidationException;
import com.gms.repository.QuestionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@Transactional
public class QuestionService {

    private static final Set<String> OPTION_TYPES = Set.of("SELECT_ONE", "SELECT_MULTI");
    private static final int MAX_CHILDREN = 50;

    private final QuestionRepository questionRepository;
    private final AllowlistService allowlistService;

    public QuestionService(QuestionRepository questionRepository, AllowlistService allowlistService) {
        this.questionRepository = questionRepository;
        this.allowlistService = allowlistService;
    }

    public Question createQuestion(Question question) {
        validateQuestion(question);
        if ("RADIO_YES_NO".equals(question.getQuestionType())) {
            injectYesNoOptions(question);
        }
        return questionRepository.save(question);
    }

    public Question updateQuestion(Long id, Question updates) {
        Question existing = questionRepository.findById(id)
            .orElseThrow(() -> new ValidationException("Question not found: " + id));
        existing.setLabel(updates.getLabel());
        existing.setQuestionType(updates.getQuestionType());
        existing.setTargetTable(updates.getTargetTable());
        existing.setTargetColumn(updates.getTargetColumn());
        existing.setRequired(updates.getRequired());
        existing.setValidationRegex(updates.getValidationRegex());
        existing.setAllowedFileTypes(updates.getAllowedFileTypes());
        existing.setMaxFileSizeMb(updates.getMaxFileSizeMb());
        // NOTE: minDate/maxDate were removed from gms_question in V4 migration.
        //       They now live on gms_round_page_question as polymorphic minValue/maxValue.
        validateQuestion(existing);
        if ("RADIO_YES_NO".equals(existing.getQuestionType())) {
            injectYesNoOptions(existing);
        }
        return questionRepository.save(existing);
    }

    public void deactivateQuestion(Long id) {
        Question question = questionRepository.findById(id)
            .orElseThrow(() -> new ValidationException("Question not found: " + id));
        question.setActive(false);
        // Remove parent/child links
        if (question.getParentQuestion() != null) {
            question.setParentQuestion(null);
            question.setTriggerValue(null);
            question.setChildDisplayOrder(null);
        }
        List<Question> children = questionRepository.findByParentQuestionId(id);
        for (Question child : children) {
            child.setParentQuestion(null);
            child.setTriggerValue(null);
            child.setChildDisplayOrder(null);
        }
        questionRepository.save(question);
        questionRepository.saveAll(children);
    }

    @Transactional(readOnly = true)
    public Page<Question> listQuestions(String type, String search, boolean includeInactive, Pageable pageable) {
        String safeType = (type != null && !type.isBlank()) ? type : null;
        String safeSearch = (search != null && !search.isBlank()) ? "%" + search.toLowerCase() + "%" : null;
        if (includeInactive) {
            return questionRepository.findFilteredIncludeInactive(safeType, safeSearch, pageable);
        }
        return questionRepository.findFiltered(safeType, safeSearch, pageable);
    }

    @Transactional(readOnly = true)
    public Question getQuestion(Long id) {
        return questionRepository.findById(id)
            .orElseThrow(() -> new ValidationException("Question not found: " + id));
    }

    /**
     * Returns all direct children of a parent question as ChildLinkDTOs.
     * Uses a targeted query rather than the @JsonIgnore entity collection.
     */
    @Transactional(readOnly = true)
    public List<ChildLinkDTO> listChildren(Long parentId) {
        // Ensure parent exists
        getQuestion(parentId);
        return questionRepository.findByParentQuestionId(parentId).stream()
            .map(child -> new ChildLinkDTO(
                parentId,
                child.getId(),
                child.getTriggerValue(),
                child.getChildDisplayOrder()))
            .toList();
    }

    public ChildLinkDTO linkChild(Long parentId, Long childId, String triggerValue, Integer childDisplayOrder) {
        Question parent = getQuestion(parentId);
        if (!"RADIO_YES_NO".equals(parent.getQuestionType())) {
            throw new ValidationException("Only RADIO_YES_NO questions may be parents");
        }
        Question child = getQuestion(childId);
        if (child.getParentQuestion() != null) {
            throw new ValidationException("Question is already a child of another parent");
        }
        if (childId.equals(parentId)) {
            throw new ValidationException("A question cannot be its own parent");
        }
        if (wouldCreateCycle(parentId, childId)) {
            throw new ValidationException("Circular parent-child relationship detected");
        }
        long childCount = questionRepository.countByParentQuestionId(parentId);
        if (childCount >= MAX_CHILDREN) {
            throw new ValidationException("Parent already has maximum " + MAX_CHILDREN + " children");
        }
        String normalizedTrigger = normalizeTriggerValue(triggerValue);
        child.setParentQuestion(parent);
        child.setTriggerValue(normalizedTrigger);
        child.setChildDisplayOrder(childDisplayOrder);
        questionRepository.save(child);
        return new ChildLinkDTO(parentId, childId, normalizedTrigger, childDisplayOrder);
    }

    /**
     * Updates triggerValue and/or childDisplayOrder for an existing parent-child link.
     */
    public ChildLinkDTO updateChildLink(Long parentId, Long childId, String triggerValue, Integer childDisplayOrder) {
        Question child = getQuestion(childId);
        if (child.getParentQuestion() == null || !child.getParentQuestion().getId().equals(parentId)) {
            throw new ValidationException("Question " + childId + " is not a child of " + parentId);
        }
        if (triggerValue != null) {
            child.setTriggerValue(normalizeTriggerValue(triggerValue));
        }
        if (childDisplayOrder != null) {
            child.setChildDisplayOrder(childDisplayOrder);
        }
        questionRepository.save(child);
        return new ChildLinkDTO(parentId, childId, child.getTriggerValue(), child.getChildDisplayOrder());
    }

    public void unlinkChild(Long parentId, Long childId) {
        Question child = getQuestion(childId);
        if (child.getParentQuestion() == null || !child.getParentQuestion().getId().equals(parentId)) {
            throw new ValidationException("Question " + childId + " is not a child of " + parentId);
        }
        child.setParentQuestion(null);
        child.setTriggerValue(null);
        child.setChildDisplayOrder(null);
        questionRepository.save(child);
    }

    // --- Option CRUD ---

    public QuestionOptionDTO addOption(Question question, String optionLabel, String optionValue,
                                       Integer displayOrder, Boolean isOtherOption,
                                       String optionTargetTable, String optionTargetColumn, String lookupId) {
        if (!OPTION_TYPES.contains(question.getQuestionType())) {
            throw new ValidationException("Question type " + question.getQuestionType() + " does not support options");
        }
        if (optionLabel == null || optionLabel.isBlank()) {
            throw new ValidationException("optionLabel is required");
        }
        if (optionValue == null || optionValue.isBlank()) {
            throw new ValidationException("optionValue is required");
        }
        boolean duplicate = question.getOptions().stream()
            .anyMatch(o -> o.getOptionValue().equals(optionValue));
        if (duplicate) {
            throw new ValidationException("optionValue already exists on this question: " + optionValue);
        }
        validateOptionRouting(optionTargetTable, optionTargetColumn, lookupId);
        if (Boolean.TRUE.equals(isOtherOption) && !"SELECT_MULTI".equals(question.getQuestionType())) {
            throw new ValidationException("isOtherOption is only valid for SELECT_MULTI questions");
        }

        QuestionOption option = new QuestionOption();
        option.setQuestion(question);
        option.setOptionLabel(optionLabel);
        option.setOptionValue(optionValue);
        option.setDisplayOrder(displayOrder != null ? displayOrder : 1);
        option.setIsOtherOption(isOtherOption);
        option.setOptionTargetTable(optionTargetTable);
        option.setOptionTargetColumn(optionTargetColumn);
        option.setLookupId(lookupId);
        question.getOptions().add(option);
        Question saved = questionRepository.save(question);

        // Return the persisted option (with its generated id) as DTO
        QuestionOption persisted = saved.getOptions().stream()
            .filter(o -> o.getOptionValue().equals(optionValue))
            .findFirst()
            .orElse(option);
        return toOptionDTO(persisted);
    }

    public QuestionOptionDTO updateOption(Long questionId, Long optionId, String optionLabel, String optionValue,
                                          Integer displayOrder, Boolean isOtherOption,
                                          String optionTargetTable, String optionTargetColumn, String lookupId) {
        Question question = getQuestion(questionId);
        QuestionOption option = question.getOptions().stream()
            .filter(o -> o.getId().equals(optionId))
            .findFirst()
            .orElseThrow(() -> new ValidationException("Option not found: " + optionId));

        if (optionLabel != null) option.setOptionLabel(optionLabel);
        if (optionValue != null) {
            boolean duplicate = question.getOptions().stream()
                .anyMatch(o -> !o.getId().equals(optionId) && o.getOptionValue().equals(optionValue));
            if (duplicate) {
                throw new ValidationException("optionValue already exists: " + optionValue);
            }
            option.setOptionValue(optionValue);
        }
        if (displayOrder != null) option.setDisplayOrder(displayOrder);
        if (isOtherOption != null) option.setIsOtherOption(isOtherOption);
        if (optionTargetTable != null || optionTargetColumn != null || lookupId != null) {
            validateOptionRouting(optionTargetTable, optionTargetColumn, lookupId);
            option.setOptionTargetTable(optionTargetTable);
            option.setOptionTargetColumn(optionTargetColumn);
            option.setLookupId(lookupId);
        }
        questionRepository.save(question);
        return toOptionDTO(option);
    }

    public void removeOption(Long questionId, Long optionId) {
        Question question = getQuestion(questionId);
        question.getOptions().removeIf(o -> o.getId().equals(optionId));
        questionRepository.save(question);
    }

    /**
     * Reorders options by position in the supplied orderedOptionIds list.
     * GAP-8: removed the dead first loop that used indexOf incorrectly.
     */
    public List<QuestionOptionDTO> reorderOptions(Long questionId, List<Long> orderedOptionIds) {
        Question question = getQuestion(questionId);
        for (int i = 0; i < orderedOptionIds.size(); i++) {
            final int order = i + 1;
            final Long optId = orderedOptionIds.get(i);
            question.getOptions().stream()
                .filter(o -> o.getId().equals(optId))
                .findFirst()
                .ifPresent(o -> o.setDisplayOrder(order));
        }
        questionRepository.save(question);
        return question.getOptions().stream()
            .sorted((a, b) -> a.getDisplayOrder() - b.getDisplayOrder())
            .map(this::toOptionDTO)
            .toList();
    }

    // --- Helpers ---

    private QuestionOptionDTO toOptionDTO(QuestionOption o) {
        return new QuestionOptionDTO(
            o.getId(),
            o.getOptionLabel(),
            o.getOptionValue(),
            o.getDisplayOrder(),
            o.getIsOtherOption()
        );
    }

    private void validateOptionRouting(String table, String column, String lookupId) {
        boolean hasTable = table != null && !table.isBlank();
        boolean hasColumn = column != null && !column.isBlank();
        boolean hasLookup = lookupId != null && !lookupId.isBlank();
        if (hasTable || hasColumn || hasLookup) {
            if (!(hasTable && hasColumn && hasLookup)) {
                throw new ValidationException("Per-option routing requires all three fields: targetTable, targetColumn, lookupId");
            }
            if (!allowlistService.isPermitted(table, column)) {
                throw new ValidationException("Option targetTable/targetColumn not in allowlist: " + table + "." + column);
            }
        }
    }

    private void validateQuestion(Question question) {
        if (question.getQuestionType() == null || question.getQuestionType().isBlank()) {
            throw new ValidationException("questionType is required");
        }
        if (question.getLabel() == null || question.getLabel().isBlank()) {
            throw new ValidationException("label is required");
        }
        if (question.getLabel().length() > 255) {
            throw new ValidationException("label must be at most 255 characters");
        }
        if (!"LABEL".equals(question.getQuestionType())) {
            if (question.getTargetTable() == null || question.getTargetTable().isBlank()) {
                throw new ValidationException("targetTable is required for type " + question.getQuestionType());
            }
            if (question.getTargetColumn() == null || question.getTargetColumn().isBlank()) {
                throw new ValidationException("targetColumn is required for type " + question.getQuestionType());
            }
            if (!allowlistService.isPermitted(question.getTargetTable(), question.getTargetColumn())) {
                throw new ValidationException("targetTable/targetColumn not in allowlist: "
                    + question.getTargetTable() + "." + question.getTargetColumn());
            }
        }
        if (OPTION_TYPES.contains(question.getQuestionType())) {
            if (question.getOptions() == null || question.getOptions().isEmpty()) {
                throw new ValidationException("At least one option is required for " + question.getQuestionType());
            }
        }
    }

    private void injectYesNoOptions(Question question) {
        question.getOptions().clear();
        QuestionOption yes = new QuestionOption();
        yes.setQuestion(question);
        yes.setOptionLabel("Yes");
        yes.setOptionValue("Yes");
        yes.setDisplayOrder(1);
        QuestionOption no = new QuestionOption();
        no.setQuestion(question);
        no.setOptionLabel("No");
        no.setOptionValue("No");
        no.setDisplayOrder(2);
        question.getOptions().add(yes);
        question.getOptions().add(no);
    }

    private String normalizeTriggerValue(String triggerValue) {
        if (triggerValue == null) throw new ValidationException("triggerValue is required");
        String normalized = triggerValue.trim();
        if (normalized.equalsIgnoreCase("Yes")) return "Yes";
        if (normalized.equalsIgnoreCase("No")) return "No";
        throw new ValidationException("triggerValue must be 'Yes' or 'No'");
    }

    private boolean wouldCreateCycle(Long parentId, Long childId) {
        Question current = questionRepository.findById(parentId).orElse(null);
        while (current != null && current.getParentQuestion() != null) {
            if (current.getParentQuestion().getId().equals(childId)) {
                return true;
            }
            current = current.getParentQuestion();
        }
        return hasDescendant(childId, parentId);
    }

    private boolean hasDescendant(Long questionId, Long targetId) {
        List<Question> children = questionRepository.findByParentQuestionId(questionId);
        for (Question child : children) {
            if (child.getId().equals(targetId)) return true;
            if (hasDescendant(child.getId(), targetId)) return true;
        }
        return false;
    }
}
