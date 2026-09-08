package com.learnova.elearning.module.qa.repository;

import com.learnova.elearning.module.qa.entity.CourseAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseAnswerRepository extends JpaRepository<CourseAnswer, Long> {

    List<CourseAnswer> findByQuestion_IdOrderByCreatedAtAsc(Long questionId);

    long countByQuestion_Id(Long questionId);
}
