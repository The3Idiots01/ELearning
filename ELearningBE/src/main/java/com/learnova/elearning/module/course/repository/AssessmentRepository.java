package com.learnova.elearning.module.course.repository;

import com.learnova.elearning.module.course.entity.Assessment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssessmentRepository extends JpaRepository<Assessment, Long> {

    @EntityGraph(attributePaths = {"section", "outcomes"})
    Optional<Assessment> findWithDetailsByIdAndCourse_Id(Long id, Long courseId);

    Optional<Assessment> findByIdAndCourse_Id(Long id, Long courseId);

    @EntityGraph(attributePaths = {"section", "outcomes"})
    List<Assessment> findByCourse_Id(Long courseId);

    @EntityGraph(attributePaths = {"section", "outcomes"})
    List<Assessment> findBySection_IdOrderByPositionAscIdAsc(Long sectionId);

    long countBySection_Id(Long sectionId);
}
