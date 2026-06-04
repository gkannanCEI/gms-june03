package com.gms.repository;

import com.gms.entity.RoundPage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RoundPageRepository extends JpaRepository<RoundPage, Long> {
    List<RoundPage> findByProgramRoundIdOrderByDisplayOrderAsc(Long roundId);
    Optional<RoundPage> findByProgramRoundIdAndPageId(Long roundId, Long pageId);
    boolean existsByPageIdAndProgramRound_Status(Long pageId, String status);

    /**
     * GAP-6: Targeted query replacing the findAll() full-table scan in deactivatePage().
     * Returns only the RoundPage rows referencing this specific page.
     */
    List<RoundPage> findByPageId(Long pageId);
}
