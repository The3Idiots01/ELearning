package com.learnova.elearning.module.enrollment.repository;

import com.learnova.elearning.module.enrollment.entity.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {

    boolean existsByEnrollment_IdAndLesson_Id(Long enrollmentId, Long lessonId);

    long countByEnrollment_Id(Long enrollmentId);
    
    List<LessonProgress> findByEnrollment_Id(Long enrollmentId);
}
