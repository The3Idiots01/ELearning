package com.learnova.elearning.module.course.repository;

import com.learnova.elearning.module.course.entity.LessonResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface LessonResourceRepository extends JpaRepository<LessonResource, Long> {

    Optional<LessonResource> findByIdAndLesson_Id(Long id, Long lessonId);

    List<LessonResource> findByLesson_IdOrderByPositionAsc(Long lessonId);

    List<LessonResource> findByLesson_IdInOrderByPositionAsc(Collection<Long> lessonIds);

    long countByLesson_Id(Long lessonId);
}
