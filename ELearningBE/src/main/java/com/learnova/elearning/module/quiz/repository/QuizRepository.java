package com.learnova.elearning.module.quiz.repository;

import com.learnova.elearning.module.quiz.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {

    Optional<Quiz> findByLesson_Id(Long lessonId);

    boolean existsByLesson_Id(Long lessonId);

    Optional<Quiz> findByLesson_IdAndLesson_Section_Course_Id(Long lessonId, Long courseId);
}
