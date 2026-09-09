package com.learnova.elearning.module.qa.repository;

import com.learnova.elearning.module.qa.entity.CourseQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseQuestionRepository extends JpaRepository<CourseQuestion, Long> {

    @Query("SELECT DISTINCT q FROM CourseQuestion q " +
           "JOIN FETCH q.user " +
           "JOIN FETCH q.lesson " +
           "LEFT JOIN FETCH q.answers a " +
           "LEFT JOIN FETCH a.user " +
           "WHERE q.course.id = :courseId AND q.lesson.id = :lessonId " +
           "ORDER BY q.createdAt DESC")
    List<CourseQuestion> findByCourseIdAndLessonIdWithDetails(
            @Param("courseId") Long courseId,
            @Param("lessonId") Long lessonId
    );

    @Query("SELECT DISTINCT q FROM CourseQuestion q " +
           "JOIN FETCH q.user " +
           "JOIN FETCH q.lesson " +
           "LEFT JOIN FETCH q.answers a " +
           "LEFT JOIN FETCH a.user " +
           "WHERE q.course.id = :courseId " +
           "ORDER BY q.createdAt DESC")
    List<CourseQuestion> findByCourseIdWithDetails(
            @Param("courseId") Long courseId
    );

    long countByCourse_Id(Long courseId);

    long countByLesson_Id(Long lessonId);
}
