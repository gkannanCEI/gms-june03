package com.gms.repository;

import com.gms.entity.PageRule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PageRuleRepository extends JpaRepository<PageRule, Long> {
    List<PageRule> findByRoundPageId(Long roundPageId);
}
