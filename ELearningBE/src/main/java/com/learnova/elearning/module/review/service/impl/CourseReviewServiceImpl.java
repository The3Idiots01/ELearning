package com.learnova.elearning.module.review.service.impl;

import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import com.learnova.elearning.module.course.entity.Course;
import com.learnova.elearning.module.course.repository.CourseRepository;
import com.learnova.elearning.module.enrollment.entity.Enrollment;
import com.learnova.elearning.module.enrollment.repository.EnrollmentRepository;
import com.learnova.elearning.module.review.dto.request.CreateOrUpdateReviewRequest;
import com.learnova.elearning.module.review.dto.response.CourseReviewEligibilityResponse;
import com.learnova.elearning.module.review.dto.response.CourseReviewResponse;
import com.learnova.elearning.module.review.dto.response.CourseReviewSummaryResponse;
import com.learnova.elearning.module.review.dto.response.InstructorReviewResponse;
import com.learnova.elearning.module.review.entity.CourseReview;
import com.learnova.elearning.module.review.entity.ReviewStatus;
import com.learnova.elearning.module.review.moderation.ContentModerationService;
import com.learnova.elearning.module.review.repository.CourseReviewRepository;
import com.learnova.elearning.module.review.service.CourseReviewService;
import com.learnova.elearning.module.user.entity.User;
import com.learnova.elearning.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseReviewServiceImpl implements CourseReviewService {

    private static final BigDecimal MIN_PROGRESS_FOR_REVIEW = BigDecimal.valueOf(20.0);

    private final CourseReviewRepository reviewRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ContentModerationService contentModerationService;

    @Override
    @Transactional
    public CourseReviewResponse submitReview(Long courseId, Long userId, CreateOrUpdateReviewRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));

        // 1. Kiểm tra học viên đã đăng ký khóa học chưa
        Enrollment enrollment = enrollmentRepository.findByStudent_IdAndCourse_Id(userId, courseId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_ENROLLED));

        // 2. Tìm review đã tồn tại (kể cả đã từng soft-delete)
        Optional<CourseReview> existingOpt = reviewRepository.findByCourse_IdAndUser_Id(courseId, userId);
        boolean isBrandNew = existingOpt.isEmpty() || existingOpt.get().getDeletedAt() != null;

        // Nếu là đánh giá mới: yêu cầu tiến độ học tập tối thiểu 20%
        if (isBrandNew) {
            BigDecimal currentProgress = enrollment.getProgress() != null ? enrollment.getProgress() : BigDecimal.ZERO;
            if (currentProgress.compareTo(MIN_PROGRESS_FOR_REVIEW) < 0) {
                throw new AppException(ErrorCode.REVIEW_PROGRESS_INSUFFICIENT);
            }
        }

        // 3. Kiểm duyệt nội dung (Offline Aho-Corasick + AI Gemini)
        contentModerationService.validateContent(request.getComment());
        CourseReview review;

        if (existingOpt.isPresent()) {
            review = existingOpt.get();
            review.setRating(request.getRating());
            review.setComment(request.getComment() != null ? request.getComment().trim() : null);
            review.setStatus(ReviewStatus.APPROVED);
            review.setDeletedAt(null);
            review.setUpdatedAt(Instant.now());
        } else {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

            review = CourseReview.builder()
                    .course(course)
                    .user(user)
                    .rating(request.getRating())
                    .comment(request.getComment() != null ? request.getComment().trim() : null)
                    .status(ReviewStatus.APPROVED)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
        }

        review = reviewRepository.save(review);

        // 4. Tính toán lại điểm số trung bình và số lượng review của khóa học
        recalculateCourseRating(courseId);

        return toReviewResponse(review, enrollment.getProgress() != null ? enrollment.getProgress().doubleValue() : 0.0);
    }

    @Override
    @Transactional
    public void deleteReview(Long courseId, Long userId) {
        CourseReview review = reviewRepository.findByCourse_IdAndUser_IdAndDeletedAtIsNull(courseId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        review.setDeletedAt(Instant.now());
        reviewRepository.save(review);

        // Tính lại điểm sau khi xóa
        recalculateCourseRating(courseId);
        log.info("Student {} successfully deleted review for course {}", userId, courseId);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseReviewResponse getMyReview(Long courseId, Long userId) {
        return reviewRepository.findByCourse_IdAndUser_IdAndDeletedAtIsNull(courseId, userId)
                .map(review -> {
                    Double progress = enrollmentRepository.findByStudent_IdAndCourse_Id(userId, courseId)
                            .map(e -> e.getProgress() != null ? e.getProgress().doubleValue() : 0.0)
                            .orElse(0.0);
                    return toReviewResponse(review, progress);
                })
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseReviewEligibilityResponse getReviewEligibility(Long courseId, Long userId) {
        Optional<Enrollment> enrollmentOpt = enrollmentRepository.findByStudent_IdAndCourse_Id(userId, courseId);
        if (enrollmentOpt.isEmpty()) {
            return CourseReviewEligibilityResponse.builder()
                    .isEnrolled(false)
                    .currentProgress(0.0)
                    .requiredProgress(20.0)
                    .canReview(false)
                    .review(null)
                    .build();
        }

        Enrollment enrollment = enrollmentOpt.get();
        double currentProgress = enrollment.getProgress() != null ? enrollment.getProgress().doubleValue() : 0.0;
        Optional<CourseReview> reviewOpt = reviewRepository.findByCourse_IdAndUser_IdAndDeletedAtIsNull(courseId, userId);
        boolean hasExistingReview = reviewOpt.isPresent();
        boolean canReview = hasExistingReview || currentProgress >= 20.0;
        CourseReviewResponse reviewResp = reviewOpt.map(r -> toReviewResponse(r, currentProgress)).orElse(null);

        return CourseReviewEligibilityResponse.builder()
                .isEnrolled(true)
                .currentProgress(currentProgress)
                .requiredProgress(20.0)
                .canReview(canReview)
                .review(reviewResp)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseReviewResponse> getCourseReviews(Long courseId, Integer ratingFilter, Pageable pageable) {
        Page<CourseReview> reviewPage;
        if (ratingFilter != null && ratingFilter >= 1 && ratingFilter <= 5) {
            reviewPage = reviewRepository.findByCourse_IdAndRatingAndDeletedAtIsNullAndStatus(
                    courseId, ratingFilter, ReviewStatus.APPROVED, pageable);
        } else {
            reviewPage = reviewRepository.findByCourse_IdAndDeletedAtIsNullAndStatus(
                    courseId, ReviewStatus.APPROVED, pageable);
        }

        return reviewPage.map(r -> toReviewResponse(r, null));
    }

    @Override
    @Transactional(readOnly = true)
    public CourseReviewSummaryResponse getCourseReviewSummary(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));

        List<Object[]> starCounts = reviewRepository.countReviewsGroupByRating(courseId);
        Map<Integer, Long> ratingCounts = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            ratingCounts.put(i, 0L);
        }

        long total = 0L;
        for (Object[] row : starCounts) {
            Integer rating = ((Number) row[0]).intValue();
            Long count = ((Number) row[1]).longValue();
            ratingCounts.put(rating, count);
            total += count;
        }

        Map<Integer, Double> ratingPercentages = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            if (total > 0) {
                double pct = Math.round((ratingCounts.get(i) * 100.0 / total) * 10.0) / 10.0;
                ratingPercentages.put(i, pct);
            } else {
                ratingPercentages.put(i, 0.0);
            }
        }

        return CourseReviewSummaryResponse.builder()
                .courseId(courseId)
                .ratingAvg(course.getRatingAvg())
                .totalReviews(total)
                .ratingCounts(ratingCounts)
                .ratingPercentages(ratingPercentages)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InstructorReviewResponse> getInstructorReviews(Long courseId, Long lecturerId, Integer ratingFilter, Pageable pageable) {
        // Kiểm tra quyền sở hữu khóa học của giảng viên
        courseRepository.findByIdAndLecturer_Id(courseId, lecturerId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_ACCESS_DENIED));

        Page<CourseReview> reviewPage;
        if (ratingFilter != null && ratingFilter >= 1 && ratingFilter <= 5) {
            reviewPage = reviewRepository.findByCourse_IdAndRatingAndDeletedAtIsNull(courseId, ratingFilter, pageable);
        } else {
            reviewPage = reviewRepository.findByCourse_IdAndDeletedAtIsNull(courseId, pageable);
        }

        return reviewPage.map(r -> {
            Double progress = enrollmentRepository.findByStudent_IdAndCourse_Id(r.getUser().getId(), courseId)
                    .map(e -> e.getProgress() != null ? e.getProgress().doubleValue() : 0.0)
                    .orElse(0.0);

            return InstructorReviewResponse.builder()
                    .id(r.getId())
                    .courseId(courseId)
                    .userId(r.getUser().getId())
                    .studentName(r.getUser().getFullName())
                    .studentEmail(r.getUser().getEmail())
                    .studentAvatarUrl(r.getUser().getAvatarUrl())
                    .rating(r.getRating())
                    .comment(r.getComment())
                    .progressPercent(progress)
                    .createdAt(r.getCreatedAt())
                    .updatedAt(r.getUpdatedAt())
                    .isEdited(r.getUpdatedAt() != null && r.getCreatedAt() != null && r.getUpdatedAt().isAfter(r.getCreatedAt()))
                    .build();
        });
    }

    private void recalculateCourseRating(Long courseId) {
        Double avg = reviewRepository.calculateAverageRating(courseId);
        Long count = reviewRepository.countApprovedReviews(courseId);

        Course course = courseRepository.findById(courseId).orElse(null);
        if (course != null) {
            BigDecimal roundedAvg = (avg != null)
                    ? BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            course.setRatingAvg(roundedAvg);
            course.setRatingCount(count != null ? count.intValue() : 0);
            courseRepository.save(course);
            log.info("Course {} rating recalculated: avg={}, count={}", courseId, roundedAvg, course.getRatingCount());
        }
    }

    private CourseReviewResponse toReviewResponse(CourseReview review, Double progress) {
        return CourseReviewResponse.builder()
                .id(review.getId())
                .courseId(review.getCourse().getId())
                .userId(review.getUser().getId())
                .userName(review.getUser().getFullName())
                .userAvatarUrl(review.getUser().getAvatarUrl())
                .rating(review.getRating())
                .comment(review.getComment())
                .progressPercent(progress)
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .isEdited(review.getUpdatedAt() != null && review.getCreatedAt() != null && review.getUpdatedAt().isAfter(review.getCreatedAt()))
                .build();
    }
}
