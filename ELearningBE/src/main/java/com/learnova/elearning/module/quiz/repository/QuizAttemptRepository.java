package com.learnova.elearning.module.quiz.repository;

import com.learnova.elearning.module.quiz.entity.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    List<QuizAttempt> findByQuiz_IdAndLearner_IdOrderBySubmittedAtDesc(Long quizId, Long learnerId);

    int countByQuiz_IdAndLearner_Id(Long quizId, Long learnerId);

    int countByQuiz_IdAndLearner_IdAndSubmittedAtGreaterThanEqual(Long quizId, Long learnerId, java.time.Instant submittedAt);

    boolean existsByQuiz_IdAndLearner_IdAndIsPassedTrue(Long quizId, Long learnerId);
}
