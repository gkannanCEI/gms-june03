package com.gms.service;

import com.gms.dto.AnswerDTO;
import com.gms.dto.SaveResult;
import com.gms.entity.Question;
import com.gms.entity.QuestionOption;
import com.gms.entity.RoundPageQuestion;
import com.gms.repository.QuestionRepository;
import com.gms.repository.RoundPageQuestionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ApplicationDataService {

    private static final Pattern CURRENCY_PATTERN = Pattern.compile("^\\d{1,12}(\\.\\d{1,2})?$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9\\-()\\s]{7,15}$");
    private static final Pattern ZIP_PATTERN = Pattern.compile("^\\d{5}(-\\d{4})?$");

    private final QuestionRepository questionRepository;
    private final DynamicDataRepository dynamicDataRepository;
    private final RoundPageQuestionRepository roundPageQuestionRepository;
    private final ObjectMapper objectMapper;

    public ApplicationDataService(QuestionRepository questionRepository,
                                  DynamicDataRepository dynamicDataRepository,
                                  RoundPageQuestionRepository roundPageQuestionRepository,
                                  ObjectMapper objectMapper) {
        this.questionRepository = questionRepository;
        this.dynamicDataRepository = dynamicDataRepository;
        this.roundPageQuestionRepository = roundPageQuestionRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SaveResult saveAnswers(Long applicationId, Long roundPageId, List<AnswerDTO> answers) {
        SaveResult result = new SaveResult(true);

        // Phase 1: Validate all answers
        Map<Long, Question> questionMap = new HashMap<>();
        Map<Long, RoundPageQuestion> rpqMap = new HashMap<>();
        for (AnswerDTO answer : answers) {
            Optional<Question> qOpt = questionRepository.findById(answer.getQuestionId());
            if (qOpt.isEmpty() || !qOpt.get().isActive()) {
                result.addError(answer.getQuestionId(), "Unknown or inactive question");
                continue;
            }
            Question q = qOpt.get();
            questionMap.put(q.getId(), q);

            // Look up RoundPageQuestion for min/max constraints
            if (roundPageId != null) {
                roundPageQuestionRepository.findByRoundPageIdAndQuestionId(roundPageId, q.getId())
                    .ifPresent(rpq -> rpqMap.put(q.getId(), rpq));
            }

            validateAnswer(answer, q, rpqMap.get(q.getId()), result);
        }

        if (!result.getErrors().isEmpty()) {
            result.setSuccess(false);
            return result;
        }

        // Phase 2: Persist all answers
        for (AnswerDTO answer : answers) {
            Question q = questionMap.get(answer.getQuestionId());
            if (q == null || "LABEL".equals(q.getQuestionType())) continue;

            String valueToStore = resolveValueToStore(answer, q);
            String targetTable = q.getTargetTable();
            String targetColumn = q.getTargetColumn();

            // Check per-option routing for SELECT_ONE
            if ("SELECT_ONE".equals(q.getQuestionType()) && answer.getValue() != null) {
                QuestionOption routedOption = findRoutedOption(q, answer.getValue());
                if (routedOption != null) {
                    targetTable = routedOption.getOptionTargetTable();
                    targetColumn = routedOption.getOptionTargetColumn();
                    valueToStore = routedOption.getLookupId();
                }
            }

            if ("gms_application_data".equalsIgnoreCase(targetTable)) {
                dynamicDataRepository.upsertSingleTable(applicationId, q.getId(), valueToStore);
            } else {
                dynamicDataRepository.upsertDomainTable(targetTable, targetColumn, applicationId, valueToStore);
            }
        }

        // Phase 3: Post-save verification
        for (AnswerDTO answer : answers) {
            Question q = questionMap.get(answer.getQuestionId());
            if (q == null || "LABEL".equals(q.getQuestionType())) continue;

            String expectedValue = resolveValueToStore(answer, q);
            String targetTable = q.getTargetTable();
            String targetColumn = q.getTargetColumn();

            if ("SELECT_ONE".equals(q.getQuestionType()) && answer.getValue() != null) {
                QuestionOption routedOption = findRoutedOption(q, answer.getValue());
                if (routedOption != null) {
                    targetTable = routedOption.getOptionTargetTable();
                    targetColumn = routedOption.getOptionTargetColumn();
                    expectedValue = routedOption.getLookupId();
                }
            }

            String storedValue;
            if ("gms_application_data".equalsIgnoreCase(targetTable)) {
                storedValue = dynamicDataRepository.readSingleTable(applicationId, q.getId());
            } else {
                storedValue = dynamicDataRepository.readDomainTable(targetTable, targetColumn, applicationId);
            }

            if (storedValue == null) {
                result.addError(q.getId(), "Post-save verification failed: no row found");
                result.setSuccess(false);
            } else if (!Objects.equals(expectedValue, storedValue)) {
                result.addError(q.getId(), "Post-save verification failed: value mismatch");
                result.setSuccess(false);
            }
        }

        if (!result.isSuccess()) {
            throw new RuntimeException("Post-save verification failed, triggering rollback");
        }

        return result;
    }

    /**
     * Loads saved answers for all supplied question IDs.
     *
     * Each question may store its answer in one of two places:
     *   1. gms_application_data (EAV single-table) — when targetTable = 'gms_application_data'
     *   2. A domain table (e.g. gms_applicant_profile) — when targetTable is something else.
     *
     * We look up the Question entity to determine the correct table and column,
     * then read from the appropriate store.  Questions not found or of type LABEL
     * are silently skipped.
     */
    @Transactional(readOnly = true)
    public List<AnswerDTO> loadAnswers(Long applicationId, List<Long> questionIds) {
        List<AnswerDTO> answers = new ArrayList<>();
        for (Long qId : questionIds) {
            Optional<Question> qOpt = questionRepository.findById(qId);
            if (qOpt.isEmpty()) continue;
            Question q = qOpt.get();
            if ("LABEL".equals(q.getQuestionType())) continue;

            String value;
            String targetTable  = q.getTargetTable();
            String targetColumn = q.getTargetColumn();

            if (targetTable == null || "gms_application_data".equalsIgnoreCase(targetTable)) {
                // EAV single-table — key is (application_id, question_id)
                value = dynamicDataRepository.readSingleTable(applicationId, qId);
            } else {
                // Domain table — key is application_id; column is the answer field
                value = dynamicDataRepository.readDomainTable(targetTable, targetColumn, applicationId);
            }

            if (value != null) {
                AnswerDTO dto = new AnswerDTO();
                dto.setQuestionId(qId);
                dto.setValue(value);
                answers.add(dto);
            }
        }
        return answers;
    }

    private void validateAnswer(AnswerDTO answer, Question q, RoundPageQuestion rpq, SaveResult result) {
        String value = answer.getValue();
        String type = q.getQuestionType();
        String minVal = rpq != null ? rpq.getMinValue() : null;
        String maxVal = rpq != null ? rpq.getMaxValue() : null;

        // Required check
        if (Boolean.TRUE.equals(q.getRequired()) && (value == null || value.isBlank())) {
            result.addError(q.getId(), "This field is required");
            return;
        }

        if (value == null || value.isBlank()) return; // optional and empty is fine

        switch (type) {
            case "TEXT", "TEXT_AREA" -> {
                if (q.getValidationRegex() != null) {
                    int flags = "TEXT_AREA".equals(type) ? Pattern.DOTALL : 0;
                    if (!Pattern.compile(q.getValidationRegex(), flags).matcher(value).matches()) {
                        result.addError(q.getId(), "Value does not match the required format");
                    }
                }
                // min/max = character length
                if (minVal != null) {
                    try {
                        if (value.length() < Integer.parseInt(minVal)) {
                            result.addError(q.getId(), "Minimum length is " + minVal + " characters");
                        }
                    } catch (NumberFormatException e) {
                        result.addError(q.getId(), "Invalid minValue configuration: " + minVal);
                    }
                }
                if (maxVal != null) {
                    try {
                        if (value.length() > Integer.parseInt(maxVal)) {
                            result.addError(q.getId(), "Maximum length is " + maxVal + " characters");
                        }
                    } catch (NumberFormatException e) {
                        result.addError(q.getId(), "Invalid maxValue configuration: " + maxVal);
                    }
                }
            }
            case "DATE" -> {
                try {
                    LocalDate date = LocalDate.parse(value);
                    if (minVal != null && date.isBefore(LocalDate.parse(minVal))) {
                        result.addError(q.getId(), "Date must be on or after " + minVal);
                    }
                    if (maxVal != null && date.isAfter(LocalDate.parse(maxVal))) {
                        result.addError(q.getId(), "Date must be on or before " + maxVal);
                    }
                } catch (DateTimeParseException e) {
                    result.addError(q.getId(), "Invalid date format (expected yyyy-MM-dd)");
                }
            }
            case "DECIMAL", "CURRENCY" -> {
                try {
                    BigDecimal num = new BigDecimal(value);
                    if (minVal != null && num.compareTo(new BigDecimal(minVal)) < 0) {
                        result.addError(q.getId(), "Value must be at least " + minVal);
                    }
                    if (maxVal != null && num.compareTo(new BigDecimal(maxVal)) > 0) {
                        result.addError(q.getId(), "Value must be at most " + maxVal);
                    }
                } catch (NumberFormatException e) {
                    result.addError(q.getId(), "Invalid numeric value");
                }
                if ("CURRENCY".equals(type) && !CURRENCY_PATTERN.matcher(value).matches()) {
                    result.addError(q.getId(), "Invalid currency format");
                }
            }
            case "WHOLE_NUMBER" -> {
                try {
                    long num = Long.parseLong(value);
                    if (minVal != null && num < Long.parseLong(minVal)) {
                        result.addError(q.getId(), "Value must be at least " + minVal);
                    }
                    if (maxVal != null && num > Long.parseLong(maxVal)) {
                        result.addError(q.getId(), "Value must be at most " + maxVal);
                    }
                } catch (NumberFormatException e) {
                    result.addError(q.getId(), "Invalid whole number");
                }
            }
            case "PHONE" -> {
                if (!PHONE_PATTERN.matcher(value).matches()) {
                    result.addError(q.getId(), "Invalid phone number format");
                }
                if (minVal != null) {
                    try {
                        if (value.length() < Integer.parseInt(minVal)) {
                            result.addError(q.getId(), "Minimum length is " + minVal);
                        }
                    } catch (NumberFormatException e) {
                        result.addError(q.getId(), "Invalid minValue configuration: " + minVal);
                    }
                }
                if (maxVal != null) {
                    try {
                        if (value.length() > Integer.parseInt(maxVal)) {
                            result.addError(q.getId(), "Maximum length is " + maxVal);
                        }
                    } catch (NumberFormatException e) {
                        result.addError(q.getId(), "Invalid maxValue configuration: " + maxVal);
                    }
                }
            }
            case "ZIP_CODE" -> {
                if (!ZIP_PATTERN.matcher(value).matches()) {
                    result.addError(q.getId(), "Invalid ZIP code format");
                }
            }
            case "CHECKBOX" -> {
                if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                    result.addError(q.getId(), "Value must be true or false");
                }
            }
            case "RADIO_YES_NO" -> {
                if (!value.equalsIgnoreCase("Yes") && !value.equalsIgnoreCase("No")) {
                    result.addError(q.getId(), "Value must be Yes or No");
                }
            }
            case "SELECT_ONE" -> {
                Set<String> validOptions = q.getOptions().stream()
                    .map(QuestionOption::getOptionValue).collect(Collectors.toSet());
                if (!validOptions.contains(value)) {
                    result.addError(q.getId(), "Selected value is not a valid option");
                }
            }
            case "SELECT_MULTI" -> validateSelectMulti(answer, q, result);
            case "LABEL" -> {} // accept and ignore
            case "ATTACHMENT" -> {} // file validation handled by FileStorageService
        }
    }

    private void validateSelectMulti(AnswerDTO answer, Question q, SaveResult result) {
        try {
            List<String> values = objectMapper.readValue(answer.getValue(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
            Set<String> validOptions = q.getOptions().stream()
                .map(QuestionOption::getOptionValue).collect(Collectors.toSet());
            boolean hasOtherOption = q.getOptions().stream()
                .anyMatch(o -> Boolean.TRUE.equals(o.getIsOtherOption()));

            for (String v : values) {
                if (v.startsWith("OTHER:")) {
                    if (!hasOtherOption) {
                        result.addError(q.getId(), "Other option not allowed for this question");
                    } else if (v.length() <= 6) {
                        result.addError(q.getId(), "Other text is required when Other is selected");
                    }
                } else if (!validOptions.contains(v)) {
                    result.addError(q.getId(), "Selected value '" + v + "' is not a valid option");
                }
            }
        } catch (JsonProcessingException e) {
            result.addError(q.getId(), "Invalid multi-select format (expected JSON array)");
        }
    }

    private String resolveValueToStore(AnswerDTO answer, Question q) {
        if ("SELECT_MULTI".equals(q.getQuestionType())) {
            return answer.getValue(); // already JSON array
        }
        return answer.getValue();
    }

    private QuestionOption findRoutedOption(Question q, String selectedValue) {
        if (q.getOptions() == null) return null;
        return q.getOptions().stream()
            .filter(o -> o.getOptionValue().equals(selectedValue))
            .filter(o -> o.getOptionTargetTable() != null && o.getOptionTargetColumn() != null && o.getLookupId() != null)
            .findFirst()
            .orElse(null);
    }
}
