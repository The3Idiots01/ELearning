package com.learnova.elearning.module.course.repository;

import com.learnova.elearning.module.course.entity.Course;
import com.learnova.elearning.module.course.entity.enums.CourseLevel;
import com.learnova.elearning.module.course.entity.enums.CourseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * @SQLRestriction trên Course đã tự lọc deleted_at IS NULL cho mọi truy vấn.
 * Các method có lecturerId nhằm kiểm quyền sở hữu ngay ở tầng query (chống IDOR).
 */
@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findByIdAndLecturer_Id(Long id, Long lecturerId);

    /**
     * Danh sách course của instructor, lọc tùy chọn theo status và keyword (title).
     * LEFT JOIN FETCH category để tránh N+1 khi map summary (category là to-one nên
     * vẫn phân trang được ở DB).
     * <p>
     * CAST(:keyword AS String) là bắt buộc: khi keyword null, PostgreSQL không suy
     * được kiểu tham số nên hiểu thành bytea → "function lower(bytea) does not exist".
     * CAST giúp Hibernate bind tham số kèm kiểu rõ ràng.
     */
    @Query(value = """
            SELECT c FROM Course c
            LEFT JOIN FETCH c.category
            WHERE c.lecturer.id = :lecturerId
              AND (:status IS NULL OR c.status = :status)
              AND (CAST(:keyword AS String) IS NULL
                   OR LOWER(c.title) LIKE LOWER(CONCAT('%', CAST(:keyword AS String), '%')))
            """,
            countQuery = """
            SELECT COUNT(c) FROM Course c
            WHERE c.lecturer.id = :lecturerId
              AND (:status IS NULL OR c.status = :status)
              AND (CAST(:keyword AS String) IS NULL
                   OR LOWER(c.title) LIKE LOWER(CONCAT('%', CAST(:keyword AS String), '%')))
            """)
    Page<Course> search(@Param("lecturerId") Long lecturerId,
                         @Param("status") CourseStatus status,
                         @Param("keyword") String keyword,
                         Pageable pageable);

    /**
     * Kiểm slug tồn tại — dùng native để KHÔNG bị @SQLRestriction lọc mất course
     * đã soft-delete (slug vẫn chiếm chỗ trong unique index của DB).
     */
    @Query(value = "SELECT EXISTS(SELECT 1 FROM courses WHERE slug = :slug)", nativeQuery = true)
    boolean existsBySlugRaw(@Param("slug") String slug);

    @Query(value = """
            SELECT c FROM Course c
            LEFT JOIN FETCH c.category
            WHERE c.status = 'PUBLISHED'
              AND (:categoryId IS NULL OR c.category.id = :categoryId)
              AND (:level IS NULL OR c.level = :level)
              AND (CAST(:keyword AS String) IS NULL
                   OR LOWER(c.title) LIKE LOWER(CONCAT('%', CAST(:keyword AS String), '%'))
                   OR LOWER(c.subtitle) LIKE LOWER(CONCAT('%', CAST(:keyword AS String), '%')))
            """,
            countQuery = """
            SELECT COUNT(c) FROM Course c
            WHERE c.status = 'PUBLISHED'
              AND (:categoryId IS NULL OR c.category.id = :categoryId)
              AND (:level IS NULL OR c.level = :level)
              AND (CAST(:keyword AS String) IS NULL
                   OR LOWER(c.title) LIKE LOWER(CONCAT('%', CAST(:keyword AS String), '%'))
                   OR LOWER(c.subtitle) LIKE LOWER(CONCAT('%', CAST(:keyword AS String), '%')))
            """)
    Page<Course> searchPublic(@Param("categoryId") Long categoryId,
                             @Param("level") CourseLevel level,
                             @Param("keyword") String keyword,
                             Pageable pageable);

    Optional<Course> findByIdAndStatus(Long id, CourseStatus status);

    Optional<Course> findBySlugAndStatus(String slug, CourseStatus status);
}
