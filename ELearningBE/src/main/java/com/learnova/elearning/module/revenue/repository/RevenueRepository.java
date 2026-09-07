package com.learnova.elearning.module.revenue.repository;

import com.learnova.elearning.module.revenue.repository.CourseRevenueProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.learnova.elearning.module.course.entity.Course;

import java.util.List;

@Repository
public interface RevenueRepository extends JpaRepository<Course, Long> {

    @Query(value = """
            SELECT c.id AS course_id,
                   c.title AS course_title,
                   COALESCE(SUM(po.amount), 0) AS revenue
            FROM courses c
            LEFT JOIN payment_orders po
                   ON po.course_id = c.id
                  AND po.status = 'PAID'
            WHERE c.lecturer_id = :lecturerId
              AND c.deleted_at IS NULL
            GROUP BY c.id, c.title
            ORDER BY COALESCE(SUM(po.amount), 0) DESC, c.id ASC
            """, nativeQuery = true)
    List<CourseRevenueProjection> findCourseRevenue(@Param("lecturerId") Long lecturerId);
}
