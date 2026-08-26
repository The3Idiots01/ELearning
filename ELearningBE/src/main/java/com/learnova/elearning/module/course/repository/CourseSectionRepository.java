package com.learnova.elearning.module.course.repository;

import com.learnova.elearning.module.course.entity.CourseSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseSectionRepository extends JpaRepository<CourseSection, Long> {

    /** Kiểm section thuộc đúng course (chống thao tác chéo course). */
    Optional<CourseSection> findByIdAndCourse_Id(Long id, Long courseId);

    List<CourseSection> findByCourse_IdOrderByPositionAsc(Long courseId);

    long countByCourse_Id(Long courseId);
}
