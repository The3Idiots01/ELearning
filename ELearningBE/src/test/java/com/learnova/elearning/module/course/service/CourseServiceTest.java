package com.learnova.elearning.module.course.service;

import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import com.learnova.elearning.integration.storage.StorageKeyFactory;
import com.learnova.elearning.integration.storage.StorageProperties;
import com.learnova.elearning.integration.storage.StorageService;
import com.learnova.elearning.module.category.service.CategoryService;
import com.learnova.elearning.module.course.dto.request.CreateCourseRequest;
import com.learnova.elearning.module.course.dto.request.UpdatePriceRequest;
import com.learnova.elearning.module.course.dto.request.UpdateThumbnailRequest;
import com.learnova.elearning.module.course.entity.Course;
import com.learnova.elearning.module.course.entity.enums.CourseStatus;
import com.learnova.elearning.module.course.repository.CourseBulletRepository;
import com.learnova.elearning.module.course.repository.CourseRepository;
import com.learnova.elearning.module.enrollment.repository.EnrollmentRepository;
import com.learnova.elearning.module.user.entity.User;
import com.learnova.elearning.module.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock private CourseRepository courseRepository;
    @Mock private CourseBulletRepository bulletRepository;
    @Mock private CategoryService categoryService;
    @Mock private CourseOwnershipGuard ownershipGuard;
    @Mock private UserRepository userRepository;
    @Mock private StorageService storageService;
    @Mock private StorageKeyFactory keyFactory;
    @Mock private StorageProperties storageProperties;
    @Mock private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private CourseService courseService;

    private Course editableCourse() {
        return Course.builder().id(1L).title("Cũ").status(CourseStatus.DRAFT)
                .price(BigDecimal.ZERO).build();
    }

    @Test
    @DisplayName("Create: slug bị trùng -> thêm hậu tố -2")
    void create_slugCollision_appendsSuffix() {
        CreateCourseRequest req = new CreateCourseRequest();
        req.setTitle("Java Cơ Bản");

        when(userRepository.getReferenceById(10L)).thenReturn(User.builder().id(10L).build());
        when(courseRepository.existsBySlugRaw("java-co-ban")).thenReturn(true);
        when(courseRepository.existsBySlugRaw("java-co-ban-2")).thenReturn(false);
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bulletRepository.findByCourse_IdOrderByBulletTypeAscPositionAsc(any())).thenReturn(List.of());

        courseService.create(req, 10L);

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(captor.capture());
        assertThat(captor.getValue().getSlug()).isEqualTo("java-co-ban-2");
        assertThat(captor.getValue().getStatus()).isEqualTo(CourseStatus.DRAFT);
    }

    @Test
    @DisplayName("UpdatePrice: giá vượt 10 triệu -> COURSE_PRICE_OUT_OF_RANGE")
    void updatePrice_outOfRange() {
        when(ownershipGuard.requireEditableCourse(1L, 10L)).thenReturn(editableCourse());
        UpdatePriceRequest req = new UpdatePriceRequest();
        req.setPrice(new BigDecimal("10000001"));

        assertThatThrownBy(() -> courseService.updatePrice(1L, req, 10L))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COURSE_PRICE_OUT_OF_RANGE);

        verify(courseRepository, never()).save(any());
    }

    @Test
    @DisplayName("UpdatePrice: giá hợp lệ -> lưu giá mới")
    void updatePrice_valid() {
        Course course = editableCourse();
        when(ownershipGuard.requireEditableCourse(1L, 10L)).thenReturn(course);
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bulletRepository.findByCourse_IdOrderByBulletTypeAscPositionAsc(any())).thenReturn(List.of());

        UpdatePriceRequest req = new UpdatePriceRequest();
        req.setPrice(new BigDecimal("499000"));
        courseService.updatePrice(1L, req, 10L);

        assertThat(course.getPrice()).isEqualByComparingTo("499000.00");
    }

    @Test
    @DisplayName("UpdateThumbnail: key không thuộc course -> UPLOAD_METADATA_MISMATCH")
    void updateThumbnail_wrongPrefix() {
        when(ownershipGuard.requireEditableCourse(1L, 10L)).thenReturn(editableCourse());
        when(keyFactory.coursePrefix(1L)).thenReturn("courses/1/");

        UpdateThumbnailRequest req = new UpdateThumbnailRequest();
        req.setStorageKey("courses/2/thumbnail/hack.png");

        assertThatThrownBy(() -> courseService.updateThumbnail(1L, req, 10L))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.UPLOAD_METADATA_MISMATCH);
    }

    @Test
    @DisplayName("Delete: course đã từng publish -> chặn (COURSE_HAS_ENROLLMENTS)")
    void delete_publishedCourse_blocked() {
        Course published = Course.builder().id(1L).status(CourseStatus.PUBLISHED)
                .publishedAt(Instant.now()).build();
        when(ownershipGuard.requireOwnedCourse(1L, 10L)).thenReturn(published);
        when(enrollmentRepository.existsByCourse_Id(1L)).thenReturn(true);

        assertThatThrownBy(() -> courseService.delete(1L, 10L))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COURSE_HAS_ENROLLMENTS);

        verify(courseRepository, never()).save(any());
        verify(storageService, never()).deleteByPrefix(anyString());
    }

    @Test
    @DisplayName("getPublicDetail: course chưa publish -> COURSE_NOT_FOUND")
    void getPublicDetail_unpublished() {
        when(courseRepository.findByIdAndStatus(1L, CourseStatus.PUBLISHED)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.getPublicDetail(1L))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COURSE_NOT_FOUND);
    }

    @Test
    @DisplayName("getPublicDetail: course đã publish -> trả về detail")
    void getPublicDetail_published() {
        Course published = Course.builder().id(1L).status(CourseStatus.PUBLISHED).title("Java")
                .publishedAt(Instant.now()).build();
        when(courseRepository.findByIdAndStatus(1L, CourseStatus.PUBLISHED)).thenReturn(Optional.of(published));
        when(bulletRepository.findByCourse_IdOrderByBulletTypeAscPositionAsc(any())).thenReturn(List.of());

        var res = courseService.getPublicDetail(1L);
        assertThat(res.getTitle()).isEqualTo("Java");
        assertThat(res.getStatus()).isEqualTo(CourseStatus.PUBLISHED);
    }
}
