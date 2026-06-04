package com.gms.repository;

import com.gms.entity.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    Page<Question> findByActiveTrue(Pageable pageable);

    @Query("SELECT q FROM Question q WHERE q.active = true " +
           "AND (:type IS NULL OR q.questionType = :type) " +
           "AND (:search IS NULL OR LOWER(q.label) LIKE :search)")
    Page<Question> findFiltered(@Param("type") String type,
                                @Param("search") String search,
                                Pageable pageable);

    @Query("SELECT q FROM Question q WHERE " +
           "(:type IS NULL OR q.questionType = :type) " +
           "AND (:search IS NULL OR LOWER(q.label) LIKE :search)")
    Page<Question> findFilteredIncludeInactive(@Param("type") String type,
                                               @Param("search") String search,
                                               Pageable pageable);

    List<Question> findByParentQuestionId(Long parentId);

    long countByParentQuestionId(Long parentId);

    List<Question> findByLabelAndQuestionTypeAndActiveTrue(String label, String questionType);
}
