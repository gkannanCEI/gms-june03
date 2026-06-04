package com.gms.service;

import com.gms.dto.BulkRoundSetupRequest;
import com.gms.entity.*;
import com.gms.exception.ValidationException;
import com.gms.repository.*;
import com.gms.security.SecurityContextProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class BulkRoundSetupService {

    private final ProgramRepository programRepository;
    private final ProgramRoundRepository roundRepository;
    private final PageRepository pageRepository;
    private final QuestionRepository questionRepository;
    private final RoundPageRepository roundPageRepository;
    private final RoundPageQuestionRepository roundPageQuestionRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final SecurityContextProvider securityContextProvider;

    public BulkRoundSetupService(ProgramRepository programRepository,
                                  ProgramRoundRepository roundRepository,
                                  PageRepository pageRepository,
                                  QuestionRepository questionRepository,
                                  RoundPageRepository roundPageRepository,
                                  RoundPageQuestionRepository roundPageQuestionRepository,
                                  StatusHistoryRepository statusHistoryRepository,
                                  SecurityContextProvider securityContextProvider) {
        this.programRepository = programRepository;
        this.roundRepository = roundRepository;
        this.pageRepository = pageRepository;
        this.questionRepository = questionRepository;
        this.roundPageRepository = roundPageRepository;
        this.roundPageQuestionRepository = roundPageQuestionRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.securityContextProvider = securityContextProvider;
    }

    public ProgramRound setupRound(BulkRoundSetupRequest request) {
        if (request.getProgramId() == null) {
            throw new ValidationException("programId is required");
        }
        if (request.getRoundName() == null || request.getRoundName().isBlank()) {
            throw new ValidationException("roundName is required");
        }

        Program program = programRepository.findById(request.getProgramId())
            .orElseThrow(() -> new ValidationException("Program not found: " + request.getProgramId()));

        if ("ARCHIVED".equals(program.getStatus())) {
            throw new ValidationException("Cannot create round under an archived program");
        }

        // Create program round
        ProgramRound round = new ProgramRound();
        round.setProgram(program);
        round.setRoundName(request.getRoundName());
        round.setStartDate(LocalDate.now());
        round.setEndDate(LocalDate.now().plusMonths(3));
        round.setFundsLimit(new BigDecimal("1000.00"));
        round.setStatus("DRAFT");
        round = roundRepository.save(round);

        // Record status history for initial DRAFT creation
        recordStatusChange("PROGRAM_ROUND", round.getId(), null, "DRAFT");

        // Process pages
        if (request.getPages() != null) {
            int pageOrder = 1;
            for (BulkRoundSetupRequest.PageSetup pageSetup : request.getPages()) {
                // Reuse existing page if one with the same name already exists
                java.util.List<Page> existingPages = pageRepository.findByPageNameAndActiveTrue(pageSetup.getPageName());
                Page page;
                if (!existingPages.isEmpty()) {
                    page = existingPages.get(0);
                } else {
                    page = new Page();
                    page.setPageName(pageSetup.getPageName());
                    page.setPageDescription(pageSetup.getPageDescription());
                    page.setActive(true);
                    page = pageRepository.save(page);
                }

                // Create round-page mapping
                RoundPage roundPage = new RoundPage();
                roundPage.setProgramRound(round);
                roundPage.setPage(page);
                roundPage.setDisplayOrder(pageOrder++);
                roundPage = roundPageRepository.save(roundPage);

                // Process questions for this page
                if (pageSetup.getQuestions() != null) {
                    int qOrder = 1;
                    for (BulkRoundSetupRequest.QuestionSetup qSetup : pageSetup.getQuestions()) {
                        Question question = createQuestion(qSetup);
                        mapQuestionToRoundPage(roundPage, question, qSetup, qOrder++);
                    }
                }
            }
        }

        return round;
    }

    private Question createQuestion(BulkRoundSetupRequest.QuestionSetup qSetup) {
        // Reuse existing question if one with the same label and type already exists
        List<Question> existing = questionRepository.findByLabelAndQuestionTypeAndActiveTrue(
            qSetup.getLabel(), qSetup.getQuestionType());
        if (!existing.isEmpty()) {
            return existing.get(0);
        }

        Question question = new Question();
        question.setLabel(qSetup.getLabel());
        question.setQuestionType(qSetup.getQuestionType());
        question.setTargetTable("gms_application_data");
        question.setTargetColumn("value_text");
        question.setRequired(qSetup.isRequired());
        question.setValidationRegex(qSetup.getValidationRegex());
        question.setActive(true);

        // For RADIO_YES_NO with no explicit options, add Yes/No before the first save
        if ("RADIO_YES_NO".equals(qSetup.getQuestionType())
                && (qSetup.getOptions() == null || qSetup.getOptions().isEmpty())) {
            QuestionOption yes = new QuestionOption();
            yes.setQuestion(question);
            yes.setOptionLabel("Yes");
            yes.setOptionValue("Yes");
            yes.setDisplayOrder(1);
            question.getOptions().add(yes);

            QuestionOption no = new QuestionOption();
            no.setQuestion(question);
            no.setOptionLabel("No");
            no.setOptionValue("No");
            no.setDisplayOrder(2);
            question.getOptions().add(no);
        }

        question = questionRepository.save(question);

        // Create options for SELECT_ONE, SELECT_MULTI, or explicit options on RADIO_YES_NO
        if (qSetup.getOptions() != null && !qSetup.getOptions().isEmpty()) {
            int optOrder = 1;
            for (BulkRoundSetupRequest.OptionSetup optSetup : qSetup.getOptions()) {
                QuestionOption opt = new QuestionOption();
                opt.setQuestion(question);
                opt.setOptionLabel(optSetup.getOptionLabel());
                opt.setOptionValue(optSetup.getOptionValue());
                opt.setDisplayOrder(optOrder++);
                opt.setIsOtherOption(optSetup.isIsOtherOption());
                question.getOptions().add(opt);
            }
            question = questionRepository.save(question);
        }

        return question;
    }

    private void mapQuestionToRoundPage(RoundPage roundPage, Question question,
                                         BulkRoundSetupRequest.QuestionSetup qSetup, int displayOrder) {
        RoundPageQuestion rpq = new RoundPageQuestion();
        rpq.setRoundPage(roundPage);
        rpq.setQuestion(question);
        rpq.setDisplayOrder(displayOrder);
        rpq.setExcluded(false);
        rpq.setReadonly(false);
        rpq.setColumnSpan(12);
        rpq.setLabelPosition("ABOVE");
        if (qSetup.getMinValue() != null) rpq.setMinValue(qSetup.getMinValue());
        if (qSetup.getMaxValue() != null) rpq.setMaxValue(qSetup.getMaxValue());
        roundPageQuestionRepository.save(rpq);
    }

    private void recordStatusChange(String entityType, Long entityId, String from, String to) {
        StatusHistory history = new StatusHistory();
        history.setEntityType(entityType);
        history.setEntityId(entityId);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setChangedBy(securityContextProvider.getCurrentUser().userId());
        statusHistoryRepository.save(history);
    }
}
