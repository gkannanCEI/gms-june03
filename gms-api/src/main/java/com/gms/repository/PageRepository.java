package com.gms.repository;

import com.gms.entity.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PageRepository extends JpaRepository<Page, Long> {
    org.springframework.data.domain.Page<Page> findByActiveTrue(Pageable pageable);

    @Query("SELECT p FROM Page p WHERE p.active = true AND LOWER(p.pageName) LIKE LOWER(CONCAT('%', :search, '%'))")
    org.springframework.data.domain.Page<Page> findByActiveTrueAndPageNameContaining(String search, Pageable pageable);

    java.util.List<Page> findByPageNameAndActiveTrue(String pageName);
}
