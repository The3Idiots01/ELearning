package com.learnova.elearning.module.course.service;

import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import com.learnova.elearning.module.course.dto.request.UnpublishRequest;
import com.learnova.elearning.module.course.dto.response.CourseResponse;
import com.learnova.elearning.module.course.dto.response.PublishIssue;
import com.learnova.elearning.module.course.entity.Course;
import com.learnova.elearning.module.course.entity.CourseStatusLog;
import com.learnova.elearning.module.course.entity.enums.CourseStatus;
import com.learnova.elearning.module.course.exception.CourseNotReadyException;
import com.learnova.elearning.module.course.repository.CourseRepository;
import com.learnova.elearning.module.course.repository.CourseStatusLogRepository;
import com.learnova.elearning.module.user.entity.User;
import com.learnova.elearning.module.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CoursePublishServiceTest {

    @Mock private CourseOwnershipGuard ownershipGuard;
    @Mock private CourseRepository courseRepository;
    @Mock private CourseStatusLogRepository statusLogRepository;
    @Mock private CoursePublishValidator publishValidator;
    @Mock private CourseService courseService;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private CoursePublishService publishService;

    private Course course(CourseStatus status, Instant publishedAt) {
        return Course.builder().id(1L).status(status).publishedAt(publishedAt).build();
    }

    @Test
    @DisplayName("Publish: DRAFT + đủ điều kiện -> PUBLISHED, set published_at, ghi log")
    void publish_fromDraft_success() {
        Course course = course(CourseStatus.DRAFT, null);
        when(ownershipGuard.requireOwnedCourse(1L, 10L)).thenReturn(course);
        when(publishValidator.validate(course)).thenReturn(List.of());
        when(userRepository.getReferenceById(10L)).thenReturn(User.builder().id(10L).build());
        when(courseService.getDetail(1L, 10L)).thenReturn(CourseResponse.builder().id(1L).build());

        publishService.publish(1L, 10L);

        assertThat(course.getStatus()).isEqualTo(CourseStatus.PUBLISHED);
        assertThat(course.getPublishedAt()).isNotNull();

        ArgumentCaptor<CourseStatusLog> logCaptor = ArgumentCaptor.forClass(CourseStatusLog.class);
        verify(statusLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getFromStatus()).isEqualTo(CourseStatus.DRAFT);
        assertThat(logCaptor.getValue().getToStatus()).isEqualTo(CourseStatus.PUBLISHED);
    }

    @Test
    @DisplayName("Publish: chưa đủ điều kiện -> CourseNotReadyException, không lưu")
    void publish_notReady_throws() {
        Course course = course(CourseStatus.DRAFT, null);
        when(ownershipGuard.requireOwnedCourse(1L, 10L)).thenReturn(course);
        when(publishValidator.validate(course)).thenReturn(List.of(
                PublishIssue.builder().code("NO_SECTION").build()));

        assertThatThrownBy(() -> publishService.publish(1L, 10L))
                .isInstanceOf(CourseNotReadyException.class);

        verify(courseRepository, never()).save(any());
        verify(statusLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("Publish: publish lại từ UNPUBLISHED không ghi đè published_at cũ")
    void publish_republish_keepsOriginalPublishedAt() {
        Instant original = Instant.parse("2026-01-01T00:00:00Z");
        Course course = course(CourseStatus.UNPUBLISHED, original);
        when(ownershipGuard.requireOwnedCourse(1L, 10L)).thenReturn(course);
        when(publishValidator.validate(course)).thenReturn(List.of());
        when(userRepository.getReferenceById(10L)).thenReturn(User.builder().id(10L).build());
        when(courseService.getDetail(1L, 10L)).thenReturn(CourseResponse.builder().id(1L).build());

        publishService.publish(1L, 10L);

        assertThat(course.getPublishedAt()).isEqualTo(original);
    }

    @Test
    @DisplayName("Publish: từ PUBLISHED -> INVALID_STATUS_TRANSITION")
    void publish_alreadyPublished_invalid() {
        when(ownershipGuard.requireOwnedCourse(1L, 10L)).thenReturn(course(CourseStatus.PUBLISHED, Instant.now()));

        assertThatThrownBy(() -> publishService.publish(1L, 10L))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COURSE_INVALID_STATUS_TRANSITION);
    }

    @Test
    @DisplayName("Publish: course bị SUSPENDED -> COURSE_LOCKED_BY_ADMIN")
    void publish_suspended_locked() {
        when(ownershipGuard.requireOwnedCourse(1L, 10L)).thenReturn(course(CourseStatus.SUSPENDED, null));

        assertThatThrownBy(() -> publishService.publish(1L, 10L))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COURSE_LOCKED_BY_ADMIN);
    }

    @Test
    @DisplayName("Unpublish: từ PUBLISHED -> UNPUBLISHED, ghi log kèm reason")
    void unpublish_success() {
        Course course = course(CourseStatus.PUBLISHED, Instant.now());
        when(ownershipGuard.requireOwnedCourse(1L, 10L)).thenReturn(course);
        when(userRepository.getReferenceById(10L)).thenReturn(User.builder().id(10L).build());
        when(courseService.getDetail(1L, 10L)).thenReturn(CourseResponse.builder().id(1L).build());

        UnpublishRequest req = new UnpublishRequest();
        req.setReason("Cập nhật nội dung");
        publishService.unpublish(1L, req, 10L);

        assertThat(course.getStatus()).isEqualTo(CourseStatus.UNPUBLISHED);
        ArgumentCaptor<CourseStatusLog> logCaptor = ArgumentCaptor.forClass(CourseStatusLog.class);
        verify(statusLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getComment()).isEqualTo("Cập nhật nội dung");
    }

    @Test
    @DisplayName("Unpublish: từ DRAFT -> INVALID_STATUS_TRANSITION")
    void unpublish_fromDraft_invalid() {
        when(ownershipGuard.requireOwnedCourse(1L, 10L)).thenReturn(course(CourseStatus.DRAFT, null));

        assertThatThrownBy(() -> publishService.unpublish(1L, null, 10L))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COURSE_INVALID_STATUS_TRANSITION);

        verify(statusLogRepository, never()).save(any());
    }
}
