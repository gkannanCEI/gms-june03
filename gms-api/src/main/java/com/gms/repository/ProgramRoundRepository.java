package com.gms.repository;

import com.gms.entity.ProgramRound;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProgramRoundRepository extends JpaRepository<ProgramRound, Long> {
    List<ProgramRound> findByProgramId(Long programId);
    List<ProgramRound> findByProgramIdAndStatus(Long programId, String status);
}
