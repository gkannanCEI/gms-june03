package com.gms.repository;

import com.gms.entity.Program;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProgramRepository extends JpaRepository<Program, Long> {
    Page<Program> findAll(Pageable pageable);
    List<Program> findByStatus(String status);
}
