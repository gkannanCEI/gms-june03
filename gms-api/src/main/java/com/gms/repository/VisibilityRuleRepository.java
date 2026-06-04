package com.gms.repository;

import com.gms.entity.VisibilityRule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VisibilityRuleRepository extends JpaRepository<VisibilityRule, Long> {
    List<VisibilityRule> findByRoundPageQuestionId(Long roundPageQuestionId);
    void deleteByRoundPageQuestionId(Long roundPageQuestionId);
}
