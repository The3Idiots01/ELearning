package com.learnova.elearning.module.course.repository;

import com.learnova.elearning.module.course.entity.LearningOutcome;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface LearningOutcomeRepository extends JpaRepository<LearningOutcome, Long> {

    Optional<LearningOutcome> findByIdAndCourse_Id(Long id, Long courseId);

    List<LearningOutcome> findByCourse_IdOrderByPositionAsc(Long courseId);

    List<LearningOutcome> findByCourse_IdAndIdIn(Long courseId, Collection<Long> ids);

    long countByCourse_Id(Long courseId);

    @Query(value = "SELECT COUNT(*) FROM lesson_outcomes lo JOIN lessons l ON l.id = lo.lesson_id "
            + "WHERE lo.outcome_id = :outcomeId AND l.deleted_at IS NULL", nativeQuery = true)
    long countActiveLessonReferences(@Param("outcomeId") Long outcomeId);

    @Query(value = "SELECT COUNT(*) FROM assessment_outcomes ao JOIN assessments a ON a.id = ao.assessment_id "
            + "WHERE ao.outcome_id = :outcomeId AND a.deleted_at IS NULL", nativeQuery = true)
    long countActiveAssessmentReferences(@Param("outcomeId") Long outcomeId);

}
