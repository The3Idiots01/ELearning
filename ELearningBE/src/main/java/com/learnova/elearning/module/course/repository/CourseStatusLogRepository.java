package com.learnova.elearning.module.course.repository;

import com.learnova.elearning.module.course.entity.CourseStatusLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseStatusLogRepository extends JpaRepository<CourseStatusLog, Long> {

    /** JOIN FETCH actor để tránh N+1 khi hiển thị tên người thực hiện. */
    @Query("""
            SELECT l FROM CourseStatusLog l
            JOIN FETCH l.actor
            WHERE l.course.id = :courseId
            ORDER BY l.createdAt DESC
            """)
    List<CourseStatusLog> findByCourseIdWithActor(@Param("courseId") Long courseId);
}
