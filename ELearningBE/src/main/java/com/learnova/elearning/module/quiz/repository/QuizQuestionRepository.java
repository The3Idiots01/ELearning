package com.learnova.elearning.module.quiz.repository;

import com.learnova.elearning.module.quiz.entity.QuizQuestion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {

    List<QuizQuestion> findByQuiz_IdOrderByPositionAsc(Long quizId);

    Optional<QuizQuestion> findByIdAndQuiz_Id(Long id, Long quizId);

    int countByQuiz_Id(Long quizId);

    @EntityGraph(attributePaths = {
            "quiz",
            "quiz.assessment",
            "quiz.assessment.section",
            "quiz.assessment.outcomes",
            "outcome"
    })
    @Query("SELECT question FROM QuizQuestion question "
            + "JOIN question.quiz quiz "
            + "JOIN quiz.assessment assessment "
            + "LEFT JOIN assessment.section section "
            + "WHERE assessment.course.id = :courseId "
            + "ORDER BY CASE WHEN assessment.section IS NULL THEN 0 ELSE 1 END, "
            + "section.position, assessment.position, assessment.id, "
            + "question.position, question.id")
    List<QuizQuestion> findCurriculumReferencesByCourseId(@Param("courseId") Long courseId);

    @Query("SELECT DISTINCT q.outcome.id FROM QuizQuestion q "
            + "WHERE q.quiz.assessment.id = :assessmentId AND q.outcome IS NOT NULL")
    Set<Long> findUsedOutcomeIdsByAssessmentId(@Param("assessmentId") Long assessmentId);
}
