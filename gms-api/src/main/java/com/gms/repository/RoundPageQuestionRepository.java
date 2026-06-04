package com.gms.repository;

import com.gms.entity.RoundPageQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RoundPageQuestionRepository extends JpaRepository<RoundPageQuestion, Long> {
    List<RoundPageQuestion> findByRoundPageIdOrderByDisplayOrderAsc(Long roundPageId);
    Optional<RoundPageQuestion> findByRoundPageIdAndQuestionId(Long roundPageId, Long questionId);
    boolean existsByRoundPageIdAndQuestionId(Long roundPageId, Long questionId);
}
