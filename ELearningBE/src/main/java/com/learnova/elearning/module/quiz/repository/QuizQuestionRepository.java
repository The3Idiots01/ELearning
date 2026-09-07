package com.learnova.elearning.module.quiz.repository;

import com.learnova.elearning.module.quiz.entity.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {

    List<QuizQuestion> findByQuiz_IdOrderByPositionAsc(Long quizId);

    Optional<QuizQuestion> findByIdAndQuiz_Id(Long id, Long quizId);

    int countByQuiz_Id(Long quizId);
}
