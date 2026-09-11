package com.learnova.elearning.module.review;

import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import com.learnova.elearning.module.course.entity.Course;
import com.learnova.elearning.module.course.repository.CourseRepository;
import com.learnova.elearning.module.enrollment.entity.Enrollment;
import com.learnova.elearning.module.enrollment.repository.EnrollmentRepository;
import com.learnova.elearning.module.review.dto.request.CreateOrUpdateReviewRequest;
import com.learnova.elearning.module.review.dto.response.CourseReviewEligibilityResponse;
import com.learnova.elearning.module.review.dto.response.CourseReviewResponse;
import com.learnova.elearning.module.review.entity.CourseReview;
import com.learnova.elearning.module.review.moderation.ContentModerationService;
import com.learnova.elearning.module.review.repository.CourseReviewRepository;
import com.learnova.elearning.module.review.service.impl.CourseReviewServiceImpl;
import com.learnova.elearning.module.user.entity.User;
import com.learnova.elearning.module.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseReviewServiceTest {

    @Mock
    private CourseReviewRepository reviewRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private ContentModerationService contentModerationService;

    private CourseReviewServiceImpl reviewService;

    private Course sampleCourse;
    private User sampleUser;
    private Enrollment sampleEnrollment;

    @BeforeEach
    void setUp() {
        reviewService = new CourseReviewServiceImpl(
                reviewRepository,
                courseRepository,
                userRepository,
                enrollmentRepository,
                contentModerationService
        );

        sampleCourse = Course.builder().id(1L).title("Java Pro").build();
        sampleUser = User.builder().id(100L).fullName("Nguyen Van A").build();
        sampleEnrollment = Enrollment.builder()
                .id(10L)
                .course(sampleCourse)
                .student(sampleUser)
                .progress(BigDecimal.valueOf(10.0)) // 10%
                .build();
    }

    @Test
    @DisplayName("Submit review fails when student progress is below 20%")
    void submitReview_fails_whenProgressBelow20Percent() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(sampleCourse));
        when(enrollmentRepository.findByStudent_IdAndCourse_Id(100L, 1L)).thenReturn(Optional.of(sampleEnrollment));
        when(reviewRepository.findByCourse_IdAndUser_Id(1L, 100L)).thenReturn(Optional.empty());

        CreateOrUpdateReviewRequest request = new CreateOrUpdateReviewRequest();
        request.setRating(5);
        request.setComment("Khóa học rất hay!");

        assertThatThrownBy(() -> reviewService.submitReview(1L, 100L, request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.REVIEW_PROGRESS_INSUFFICIENT);

        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("Submit review succeeds when student progress is >= 20%")
    void submitReview_succeeds_whenProgressAbove20Percent() {
        sampleEnrollment.setProgress(BigDecimal.valueOf(25.0)); // 25%

        when(courseRepository.findById(1L)).thenReturn(Optional.of(sampleCourse));
        when(enrollmentRepository.findByStudent_IdAndCourse_Id(100L, 1L)).thenReturn(Optional.of(sampleEnrollment));
        when(reviewRepository.findByCourse_IdAndUser_Id(1L, 100L)).thenReturn(Optional.empty());
        when(userRepository.findById(100L)).thenReturn(Optional.of(sampleUser));

        CourseReview savedReview = CourseReview.builder()
                .id(99L)
                .course(sampleCourse)
                .user(sampleUser)
                .rating(5)
                .comment("Khóa học rất hay!")
                .build();
        when(reviewRepository.save(any(CourseReview.class))).thenReturn(savedReview);

        CreateOrUpdateReviewRequest request = new CreateOrUpdateReviewRequest();
        request.setRating(5);
        request.setComment("Khóa học rất hay!");

        CourseReviewResponse response = reviewService.submitReview(1L, 100L, request);

        assertThat(response).isNotNull();
        assertThat(response.getRating()).isEqualTo(5);
        verify(contentModerationService).validateContent("Khóa học rất hay!");
        verify(reviewRepository).save(any(CourseReview.class));
    }

    @Test
    @DisplayName("Check review eligibility correctly reflects student progress")
    void getReviewEligibility_reflectsProgressRequirement() {
        // Case 1: Progress < 20%
        when(enrollmentRepository.findByStudent_IdAndCourse_Id(100L, 1L)).thenReturn(Optional.of(sampleEnrollment));
        when(reviewRepository.findByCourse_IdAndUser_IdAndDeletedAtIsNull(1L, 100L)).thenReturn(Optional.empty());

        CourseReviewEligibilityResponse response1 = reviewService.getReviewEligibility(1L, 100L);
        assertThat(response1.getIsEnrolled()).isTrue();
        assertThat(response1.getCanReview()).isFalse();
        assertThat(response1.getCurrentProgress()).isEqualTo(10.0);
        assertThat(response1.getRequiredProgress()).isEqualTo(20.0);

        // Case 2: Progress >= 20%
        sampleEnrollment.setProgress(BigDecimal.valueOf(50.0));
        CourseReviewEligibilityResponse response2 = reviewService.getReviewEligibility(1L, 100L);
        assertThat(response2.getIsEnrolled()).isTrue();
        assertThat(response2.getCanReview()).isTrue();
        assertThat(response2.getCurrentProgress()).isEqualTo(50.0);
    }
}
