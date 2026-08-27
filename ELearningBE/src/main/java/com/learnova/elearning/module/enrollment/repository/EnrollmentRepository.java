package com.learnova.elearning.module.enrollment.repository;

import com.learnova.elearning.module.enrollment.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    boolean existsByStudent_IdAndCourse_Id(Long studentId, Long courseId);

    Optional<Enrollment> findByStudent_IdAndCourse_Id(Long studentId, Long courseId);

    java.util.List<Enrollment> findByStudent_IdOrderByEnrolledAtDesc(Long studentId);

    boolean existsByCourse_Id(Long courseId);
}
