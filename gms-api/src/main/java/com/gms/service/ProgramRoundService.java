package com.gms.service;

import com.gms.entity.*;
import com.gms.exception.ValidationException;
import com.gms.repository.*;
import com.gms.security.SecurityContextProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class ProgramRoundService {

    private static final Map<String, String> VALID_TRANSITIONS = Map.of(
        "DRAFT", "ACTIVE",
        "ACTIVE", "CLOSED",
        "CLOSED", "ARCHIVED"
    );

    private final ProgramRoundRepository roundRepository;
    private final ProgramRepository programRepository;
    private final RoundPageRepository roundPageRepository;
    private final RoundPageQuestionRepository roundPageQuestionRepository;
    private final PageRepository pageRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final SecurityContextProvider securityContextProvider;

    public ProgramRoundService(ProgramRoundRepository roundRepository,
                               ProgramRepository programRepository,
                               RoundPageRepository roundPageRepository,
                               RoundPageQuestionRepository roundPageQuestionRepository,
                               PageRepository pageRepository,
                               StatusHistoryRepository statusHistoryRepository,
                               SecurityContextProvider securityContextProvider) {
        this.roundRepository = roundRepository;
        this.programRepository = programRepository;
        this.roundPageRepository = roundPageRepository;
        this.roundPageQuestionRepository = roundPageQuestionRepository;
        this.pageRepository = pageRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.securityContextProvider = securityContextProvider;
    }

    public ProgramRound createRound(Long programId, ProgramRound round) {
        Program program = programRepository.findById(programId)
            .orElseThrow(() -> new ValidationException("Program not found: " + programId));
        if ("ARCHIVED".equals(program.getStatus())) {
            throw new ValidationException("Cannot create round under archived program");
        }
        validateRound(round);
        round.setProgram(program);
        round.setStatus("DRAFT");
        ProgramRound saved = roundRepository.save(round);
        recordStatusChange("PROGRAM_ROUND", saved.getId(), null, "DRAFT");
        return saved;
    }

    public ProgramRound updateRound(Long roundId, ProgramRound updates) {
        ProgramRound existing = roundRepository.findById(roundId)
            .orElseThrow(() -> new ValidationException("Round not found: " + roundId));
        existing.setRoundName(updates.getRoundName());
        existing.setStartDate(updates.getStartDate());
        existing.setEndDate(updates.getEndDate());
        existing.setFundsLimit(updates.getFundsLimit());
        existing.setEligibleOrganizationTypes(updates.getEligibleOrganizationTypes());
        validateRound(existing);
        return roundRepository.save(existing);
    }

    public ProgramRound transitionStatus(Long roundId, String newStatus) {
        ProgramRound round = roundRepository.findById(roundId)
            .orElseThrow(() -> new ValidationException("Round not found: " + roundId));
        String currentStatus = round.getStatus();
        String allowedNext = VALID_TRANSITIONS.get(currentStatus);
        if (allowedNext == null || !allowedNext.equals(newStatus)) {
            throw new ValidationException("Invalid transition: " + currentStatus + " -> " + newStatus);
        }
        round.setStatus(newStatus);
        roundRepository.save(round);
        recordStatusChange("PROGRAM_ROUND", roundId, currentStatus, newStatus);
        return round;
    }

    /**
     * GAP-4: Soft-delete (archive) a round by directly setting status to ARCHIVED
     * regardless of its current lifecycle position.  This is used by the admin
     * DELETE endpoint which acts as a force-archive / soft-delete operation.
     * The strict lifecycle transition (DRAFT→ACTIVE→CLOSED→ARCHIVED) is preserved
     * in {@link #transitionStatus} for the PUT /status endpoint.
     */
    public void archiveRound(Long roundId) {
        ProgramRound round = roundRepository.findById(roundId)
            .orElseThrow(() -> new ValidationException("Round not found: " + roundId));
        String previousStatus = round.getStatus();
        if ("ARCHIVED".equals(previousStatus)) {
            return; // idempotent
        }
        round.setStatus("ARCHIVED");
        roundRepository.save(round);
        recordStatusChange("PROGRAM_ROUND", roundId, previousStatus, "ARCHIVED");
    }

    public void assignPage(Long roundId, Long pageId, Integer displayOrder) {
        ProgramRound round = roundRepository.findById(roundId)
            .orElseThrow(() -> new ValidationException("Round not found: " + roundId));
        com.gms.entity.Page page = pageRepository.findById(pageId)
            .orElseThrow(() -> new ValidationException("Page not found: " + pageId));
        if (!page.isActive()) {
            throw new ValidationException("Page is inactive: " + pageId);
        }
        if (roundPageRepository.findByProgramRoundIdAndPageId(roundId, pageId).isPresent()) {
            throw new ValidationException("Page already assigned to this round");
        }
        RoundPage roundPage = new RoundPage();
        roundPage.setProgramRound(round);
        roundPage.setPage(page);
        roundPage.setDisplayOrder(displayOrder != null ? displayOrder : 1);
        roundPageRepository.save(roundPage);
    }

    /**
     * GAP-15: Removing a page from a round deletes the RoundPage AND all its
     * RoundPageQuestion records via the CascadeType.ALL + orphanRemoval = true
     * relationship on RoundPage.roundPageQuestions.  JPA will cascade the delete
     * automatically when the RoundPage entity is deleted, provided the collection
     * was loaded — we call roundPageRepository.delete() which triggers that cascade.
     */
    public void removePage(Long roundId, Long pageId) {
        RoundPage roundPage = roundPageRepository.findByProgramRoundIdAndPageId(roundId, pageId)
            .orElseThrow(() -> new ValidationException("Page not assigned to this round"));
        // Cascade delete removes all RoundPageQuestion rows for this RoundPage
        // because RoundPage.roundPageQuestions has CascadeType.ALL + orphanRemoval = true
        roundPageRepository.delete(roundPage);
    }

    public void updateRoundPage(Long roundId, Long pageId, String pageNameOverride,
                                String pageDescOverride, Integer displayOrder) {
        RoundPage roundPage = roundPageRepository.findByProgramRoundIdAndPageId(roundId, pageId)
            .orElseThrow(() -> new ValidationException("Page not assigned to this round"));
        if (pageNameOverride != null) roundPage.setPageNameOverride(pageNameOverride.isBlank() ? null : pageNameOverride);
        if (pageDescOverride != null) roundPage.setPageDescriptionOverride(pageDescOverride.isBlank() ? null : pageDescOverride);
        if (displayOrder != null) roundPage.setDisplayOrder(displayOrder);
        roundPageRepository.save(roundPage);
    }

    @Transactional(readOnly = true)
    public List<ProgramRound> listRounds(Long programId) {
        return roundRepository.findByProgramId(programId);
    }

    @Transactional(readOnly = true)
    public ProgramRound getRound(Long roundId) {
        return roundRepository.findById(roundId)
            .orElseThrow(() -> new ValidationException("Round not found: " + roundId));
    }

    @Transactional(readOnly = true)
    public List<RoundPage> listRoundPages(Long roundId) {
        return roundPageRepository.findByProgramRoundIdOrderByDisplayOrderAsc(roundId);
    }

    private void validateRound(ProgramRound round) {
        if (round.getRoundName() == null || round.getRoundName().isBlank()) {
            throw new ValidationException("roundName is required");
        }
        if (round.getStartDate() == null) throw new ValidationException("startDate is required");
        if (round.getEndDate() == null) throw new ValidationException("endDate is required");
        if (round.getStartDate().isAfter(round.getEndDate())) {
            throw new ValidationException("startDate must not be after endDate");
        }
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
