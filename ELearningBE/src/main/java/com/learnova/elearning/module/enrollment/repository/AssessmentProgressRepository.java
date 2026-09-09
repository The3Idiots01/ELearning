package com.learnova.elearning.module.enrollment.repository;

import com.learnova.elearning.module.enrollment.entity.AssessmentProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssessmentProgressRepository extends JpaRepository<AssessmentProgress, Long> {

    List<AssessmentProgress> findByEnrollment_Id(Long enrollmentId);

    boolean existsByEnrollment_IdAndAssessment_Id(Long enrollmentId, Long assessmentId);

    long countByEnrollment_IdAndAssessment_Id(Long enrollmentId, Long assessmentId);

    /** Atomic and idempotent even if two passing submissions race. */
    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO assessment_progress (
                enrollment_id, assessment_id, completed_at,
                completion_source, created_at, updated_at
            ) VALUES (
                :enrollmentId, :assessmentId, now(), 'QUIZ', now(), now()
            )
            ON CONFLICT (enrollment_id, assessment_id) DO NOTHING
            """, nativeQuery = true)
    int markCompletedByQuiz(@Param("enrollmentId") Long enrollmentId,
                            @Param("assessmentId") Long assessmentId);
}
