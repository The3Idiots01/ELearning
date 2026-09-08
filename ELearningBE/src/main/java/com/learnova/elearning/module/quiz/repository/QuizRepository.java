package com.learnova.elearning.module.quiz.repository;

import com.learnova.elearning.module.quiz.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import java.util.Optional;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {

    Optional<Quiz> findByAssessment_Id(Long assessmentId);

    boolean existsByAssessment_Id(Long assessmentId);

    Optional<Quiz> findByAssessment_IdAndAssessment_Course_Id(Long assessmentId, Long courseId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM Quiz q WHERE q.assessment.id = :assessmentId AND q.assessment.course.id = :courseId")
    Optional<Quiz> findForUpdateByAssessmentAndCourse(@Param("assessmentId") Long assessmentId,
                                                       @Param("courseId") Long courseId);
}
