package com.learnova.elearning.module.review.repository;

import com.learnova.elearning.module.review.entity.CourseReview;
import com.learnova.elearning.module.review.entity.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseReviewRepository extends JpaRepository<CourseReview, Long> {

    Optional<CourseReview> findByCourse_IdAndUser_IdAndDeletedAtIsNull(Long courseId, Long userId);

    Optional<CourseReview> findByCourse_IdAndUser_Id(Long courseId, Long userId);

    Page<CourseReview> findByCourse_IdAndDeletedAtIsNullAndStatus(
            Long courseId,
            ReviewStatus status,
            Pageable pageable
    );

    Page<CourseReview> findByCourse_IdAndRatingAndDeletedAtIsNullAndStatus(
            Long courseId,
            Integer rating,
            ReviewStatus status,
            Pageable pageable
    );

    Page<CourseReview> findByCourse_IdAndDeletedAtIsNull(
            Long courseId,
            Pageable pageable
    );

    Page<CourseReview> findByCourse_IdAndRatingAndDeletedAtIsNull(
            Long courseId,
            Integer rating,
            Pageable pageable
    );

    @Query("""
        SELECT AVG(r.rating)
        FROM CourseReview r
        WHERE r.course.id = :courseId
          AND r.deletedAt IS NULL
          AND r.status = com.learnova.elearning.module.review.entity.ReviewStatus.APPROVED
    """)
    Double calculateAverageRating(@Param("courseId") Long courseId);

    @Query("""
        SELECT COUNT(r)
        FROM CourseReview r
        WHERE r.course.id = :courseId
          AND r.deletedAt IS NULL
          AND r.status = com.learnova.elearning.module.review.entity.ReviewStatus.APPROVED
    """)
    Long countApprovedReviews(@Param("courseId") Long courseId);

    @Query("""
        SELECT r.rating, COUNT(r)
        FROM CourseReview r
        WHERE r.course.id = :courseId
          AND r.deletedAt IS NULL
          AND r.status = com.learnova.elearning.module.review.entity.ReviewStatus.APPROVED
        GROUP BY r.rating
    """)
    List<Object[]> countReviewsGroupByRating(@Param("courseId") Long courseId);
}
