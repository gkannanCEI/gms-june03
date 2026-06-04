package com.gms.service;

import com.gms.entity.Program;
import com.gms.entity.ProgramRound;
import com.gms.entity.StatusHistory;
import com.gms.exception.ValidationException;
import com.gms.repository.ProgramRepository;
import com.gms.repository.ProgramRoundRepository;
import com.gms.repository.StatusHistoryRepository;
import com.gms.security.SecurityContextProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class ProgramService {

    private final ProgramRepository programRepository;
    private final ProgramRoundRepository programRoundRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final SecurityContextProvider securityContextProvider;

    public ProgramService(ProgramRepository programRepository,
                          ProgramRoundRepository programRoundRepository,
                          StatusHistoryRepository statusHistoryRepository,
                          SecurityContextProvider securityContextProvider) {
        this.programRepository = programRepository;
        this.programRoundRepository = programRoundRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.securityContextProvider = securityContextProvider;
    }

    public Program createProgram(Program program) {
        if (program.getProgramName() == null || program.getProgramName().isBlank()) {
            throw new ValidationException("programName is required");
        }
        if (program.getTotalBudget() != null && program.getTotalBudget().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Total budget must be a positive value");
        }
        program.setStatus("ACTIVE");
        Program saved = programRepository.save(program);
        recordStatusChange("PROGRAM", saved.getId(), null, "ACTIVE");
        return saved;
    }

    public Program updateProgram(Long id, Program updates) {
        Program existing = programRepository.findById(id)
            .orElseThrow(() -> new ValidationException("Program not found: " + id));
        existing.setProgramName(updates.getProgramName());
        existing.setDescription(updates.getDescription());
        existing.setGoal(updates.getGoal());
        existing.setTotalBudget(updates.getTotalBudget());
        return programRepository.save(existing);
    }

    public void archiveProgram(Long id) {
        Program program = programRepository.findById(id)
            .orElseThrow(() -> new ValidationException("Program not found: " + id));
        if ("ARCHIVED".equals(program.getStatus())) {
            throw new ValidationException("Program is already archived");
        }
        String oldStatus = program.getStatus();
        program.setStatus("ARCHIVED");
        programRepository.save(program);
        recordStatusChange("PROGRAM", id, oldStatus, "ARCHIVED");
    }

    @Transactional(readOnly = true)
    public Program getProgram(Long id) {
        return programRepository.findById(id)
            .orElseThrow(() -> new ValidationException("Program not found: " + id));
    }

    @Transactional(readOnly = true)
    public Page<Program> listPrograms(Pageable pageable) {
        return programRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listActivePrograms() {
        List<Program> activePrograms = programRepository.findByStatus("ACTIVE");
        List<Map<String, Object>> result = new ArrayList<>();

        for (Program program : activePrograms) {
            List<ProgramRound> activeRounds = programRoundRepository
                .findByProgramIdAndStatus(program.getId(), "ACTIVE");
            if (activeRounds.isEmpty()) {
                continue;
            }

            Map<String, Object> programMap = new HashMap<>();
            programMap.put("id", program.getId());
            programMap.put("programName", program.getProgramName());
            programMap.put("description", program.getDescription());
            programMap.put("totalBudget", program.getTotalBudget());
            programMap.put("goal", program.getGoal());

            List<Map<String, Object>> roundsList = new ArrayList<>();
            for (ProgramRound round : activeRounds) {
                Map<String, Object> roundMap = new HashMap<>();
                roundMap.put("id", round.getId());
                roundMap.put("roundName", round.getRoundName());
                roundMap.put("status", round.getStatus());
                roundMap.put("startDate", round.getStartDate());
                roundMap.put("endDate", round.getEndDate());
                roundsList.add(roundMap);
            }
            programMap.put("rounds", roundsList);
            result.add(programMap);
        }

        return result;
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
