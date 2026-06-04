package com.gms.repository;

import com.gms.entity.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    Optional<Application> findByProgramRoundIdAndUserId(Long roundId, String userId);

    /**
     * CR-01: Fetch application with programRound eagerly in one query to avoid
     * LazyInitializationException when resolveRoundPageId() calls
     * app.getProgramRound().getId() outside the original transaction boundary.
     */
    @Query("SELECT a FROM Application a JOIN FETCH a.programRound WHERE a.id = :id")
    Optional<Application> findByIdWithRound(@Param("id") Long id);

    @Query("SELECT a FROM Application a WHERE a.programRound.id = :roundId " +
           "AND (:status IS NULL OR a.status = :status) " +
           "AND (:eligibilityWarning IS NULL OR a.eligibilityWarning = :eligibilityWarning)")
    Page<Application> findByRoundFiltered(@Param("roundId") Long roundId,
                                          @Param("status") String status,
                                          @Param("eligibilityWarning") Boolean eligibilityWarning,
                                          Pageable pageable);
}
