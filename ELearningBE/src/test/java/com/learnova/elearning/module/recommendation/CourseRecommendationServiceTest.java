package com.learnova.elearning.module.recommendation;

import com.learnova.elearning.integration.ai.AiProperties;
import com.learnova.elearning.integration.storage.StorageProperties;
import com.learnova.elearning.integration.storage.StorageService;
import com.learnova.elearning.module.category.entity.Category;
import com.learnova.elearning.module.course.entity.Course;
import com.learnova.elearning.module.course.entity.enums.CourseLevel;
import com.learnova.elearning.module.course.entity.enums.CourseStatus;
import com.learnova.elearning.module.course.repository.CourseRepository;
import com.learnova.elearning.module.enrollment.entity.Enrollment;
import com.learnova.elearning.module.enrollment.repository.EnrollmentRepository;
import com.learnova.elearning.module.recommendation.dto.request.AiRecommendationRequest;
import com.learnova.elearning.module.recommendation.dto.response.AiRecommendationResponse;
import com.learnova.elearning.module.recommendation.dto.response.ContinuousRecommendationResponse;
import com.learnova.elearning.module.recommendation.engine.AiRecommendationEngine;
import com.learnova.elearning.module.recommendation.service.impl.CourseRecommendationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseRecommendationServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private AiRecommendationEngine aiRecommendationEngine;

    @Mock
    private StorageService storageService;

    @Mock
    private StorageProperties storageProperties;

    @Mock
    private AiProperties aiProperties;

    @Mock
    private com.learnova.elearning.module.recommendation.repository.UserAiRecommendationRepository userAiRecommendationRepository;

    @Mock
    private com.learnova.elearning.module.user.repository.UserRepository userRepository;

    @InjectMocks
    private CourseRecommendationServiceImpl recommendationService;

    private Category itCategory;
    private List<Course> mockCourses;

    @BeforeEach
    void setUp() {
        lenient().when(storageProperties.getDownloadTtl()).thenReturn(Duration.ofMinutes(15));
        lenient().when(aiProperties.getModel()).thenReturn("gemini-3.5-flash-lite");

        itCategory = Category.builder()
                .id(10L)
                .name("Công nghệ thông tin")
                .slug("it")
                .build();

        mockCourses = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            mockCourses.add(Course.builder()
                    .id((long) i)
                    .title("Course " + i)
                    .subtitle("Subtitle " + i)
                    .slug("course-" + i)
                    .category(itCategory)
                    .lecturer(com.learnova.elearning.module.user.entity.User.builder()
                            .id(i == 1 ? 50L : (long) (100 + i))
                            .build())
                    .level(i % 2 == 0 ? CourseLevel.INTERMEDIATE : CourseLevel.BEGINNER)
                    .status(CourseStatus.PUBLISHED)
                    .price(BigDecimal.valueOf(100000))
                    .ratingAvg(BigDecimal.valueOf(4.0 + (i * 0.1)))
                    .ratingCount(10 + i)
                    .totalStudents(50 + i * 10)
                    .build());
        }
    }

    @Test
    void testContinuousRecommendations_ReturnsTop5() {
        when(courseRepository.findByStatus(CourseStatus.PUBLISHED)).thenReturn(mockCourses);

        ContinuousRecommendationResponse response =
                recommendationService.getContinuousRecommendations(null, null, null);

        assertNotNull(response);
        assertEquals(5, response.getItems().size());
        assertEquals("POPULAR_DISCOVERY", response.getStrategy());
        assertEquals(5, response.getTotal());

        // Đảm bảo được xếp hạng điểm số giảm dần
        assertTrue(response.getItems().get(0).getMatchScore()
                .compareTo(response.getItems().get(4).getMatchScore()) >= 0);
    }

    @Test
    void testContinuousRecommendations_ExcludesAuthoredCourses() {
        when(courseRepository.findByStatus(CourseStatus.PUBLISHED)).thenReturn(mockCourses);
        when(enrollmentRepository.findByStudent_IdOrderByEnrolledAtDesc(50L))
                .thenReturn(java.util.Collections.emptyList());

        // learnerId = 50L (là tác giả của Course 1)
        ContinuousRecommendationResponse response =
                recommendationService.getContinuousRecommendations(null, null, 50L);

        assertNotNull(response);
        boolean containsAuthored = response.getItems().stream()
                .anyMatch(item -> item.getCourse().getId().equals(1L));
        assertFalse(containsAuthored, "Khóa học do chính người dùng tạo ra không được phép xuất hiện trong đề xuất");
    }

    @Test
    void testContinuousRecommendations_ExcludesEnrolledCourses() {
        when(courseRepository.findByStatus(CourseStatus.PUBLISHED)).thenReturn(mockCourses);

        Course enrolledCourse = mockCourses.get(0); // id = 1
        Enrollment enrollment = Enrollment.builder()
                .id(100L)
                .course(enrolledCourse)
                .build();
        when(enrollmentRepository.findByStudent_IdOrderByEnrolledAtDesc(99L))
                .thenReturn(List.of(enrollment));

        ContinuousRecommendationResponse response =
                recommendationService.getContinuousRecommendations(null, null, 99L);

        assertNotNull(response);
        assertEquals(5, response.getItems().size());
        // Khóa học id = 1 không được nằm trong danh sách đề xuất
        boolean containsEnrolled = response.getItems().stream()
                .anyMatch(item -> item.getCourse().getId().equals(1L));
        assertFalse(containsEnrolled, "Khóa học đã mua không được xuất hiện trong danh sách đề xuất");
    }

    @Test
    void testAiRecommendations_Success() {
        when(courseRepository.findByStatus(CourseStatus.PUBLISHED)).thenReturn(mockCourses);

        AiRecommendationEngine.RawAiRecommendationResult rawResult =
                new AiRecommendationEngine.RawAiRecommendationResult();
        rawResult.setSummaryAdvice("Lộ trình học tập xuất sắc từ AI");

        AiRecommendationEngine.RawAiCourseItem item1 = new AiRecommendationEngine.RawAiCourseItem();
        item1.setCourseId(2L);
        item1.setScore(BigDecimal.valueOf(0.96));
        item1.setReason("Lý do AI 1");

        AiRecommendationEngine.RawAiCourseItem item2 = new AiRecommendationEngine.RawAiCourseItem();
        item2.setCourseId(4L);
        item2.setScore(BigDecimal.valueOf(0.89));
        item2.setReason("Lý do AI 2");

        AiRecommendationEngine.RawAiCourseItem item3 = new AiRecommendationEngine.RawAiCourseItem();
        item3.setCourseId(6L);
        item3.setScore(BigDecimal.valueOf(0.85));
        item3.setReason("Lý do AI 3");

        rawResult.setRecommendations(List.of(item1, item2, item3));

        when(aiRecommendationEngine.getRecommendations(any(), anyList(), anyList()))
                .thenReturn(rawResult);

        AiRecommendationRequest request = AiRecommendationRequest.builder()
                .goal("Học Spring Boot Backend")
                .build();

        AiRecommendationResponse response = recommendationService.getAiRecommendations(request, null);

        assertNotNull(response);
        assertFalse(response.isFallback());
        assertEquals("gemini-3.5-flash-lite", response.getModelUsed());
        assertEquals(3, response.getItems().size());
        assertEquals("Lý do AI 1", response.getItems().get(0).getRecommendationReason());
        assertEquals("GEMINI_AI", response.getItems().get(0).getRecommendationType());
    }

    @Test
    void testAiRecommendations_FallbackWhenAiFails() {
        when(courseRepository.findByStatus(CourseStatus.PUBLISHED)).thenReturn(mockCourses);

        // Mô phỏng AI gặp lỗi / timeout / rate limit -> engine trả về null
        when(aiRecommendationEngine.getRecommendations(any(), anyList(), anyList()))
                .thenReturn(null);

        AiRecommendationRequest request = AiRecommendationRequest.builder()
                .goal("Học Spring Boot Backend")
                .build();

        AiRecommendationResponse response = recommendationService.getAiRecommendations(request, null);

        assertNotNull(response);
        assertTrue(response.isFallback(), "Hệ thống phải tự động fallback khi AI lỗi");
        assertEquals("system-logic-fallback", response.getModelUsed());
        assertEquals(3, response.getItems().size(), "Fallback phải trả về 3 khóa học");
        assertEquals("SYSTEM_FALLBACK", response.getItems().get(0).getRecommendationType());
    }

    @Test
    void testAiRecommendations_PersistAndReplaceUserRecommendation() {
        when(courseRepository.findByStatus(CourseStatus.PUBLISHED)).thenReturn(mockCourses);
        when(aiProperties.getModel()).thenReturn("gemini-3.5-flash-lite");

        AiRecommendationEngine.RawAiRecommendationResult rawResult = new AiRecommendationEngine.RawAiRecommendationResult();
        rawResult.setSummaryAdvice("Lộ trình cho học viên 1");
        AiRecommendationEngine.RawAiCourseItem item1 = new AiRecommendationEngine.RawAiCourseItem();
        item1.setCourseId(1L);
        item1.setScore(BigDecimal.valueOf(0.95));
        item1.setReason("Phù hợp mục tiêu");
        rawResult.setRecommendations(List.of(item1));

        when(aiRecommendationEngine.getRecommendations(any(), anyList(), anyList()))
                .thenReturn(rawResult);

        com.learnova.elearning.module.user.entity.User mockUser = com.learnova.elearning.module.user.entity.User.builder()
                .id(100L)
                .fullName("Test Student")
                .email("student@test.com")
                .build();
        when(userRepository.findById(100L)).thenReturn(java.util.Optional.of(mockUser));
        when(courseRepository.findById(1L)).thenReturn(java.util.Optional.of(mockCourses.get(0)));

        AiRecommendationRequest request = AiRecommendationRequest.builder()
                .goal("Học Spring Boot")
                .build();

        AiRecommendationResponse response = recommendationService.getAiRecommendations(request, 100L);

        assertNotNull(response);
        // Verify delete previous recommendations for this user was executed
        verify(userAiRecommendationRepository, times(1)).deleteByUserId(100L);
        verify(userAiRecommendationRepository, times(1)).flush();
        // Verify save new recommendation was executed
        verify(userAiRecommendationRepository, times(1)).save(any());
        assertNotNull(response.getCreatedAt());
        assertEquals("Học Spring Boot", response.getGoal());
    }

    @Test
    void testGetLatestAiRecommendation() {
        com.learnova.elearning.module.user.entity.User mockUser = com.learnova.elearning.module.user.entity.User.builder()
                .id(100L)
                .fullName("Test Student")
                .build();

        com.learnova.elearning.module.recommendation.entity.UserAiRecommendation savedRec =
                com.learnova.elearning.module.recommendation.entity.UserAiRecommendation.builder()
                        .id(1L)
                        .user(mockUser)
                        .goal("Mục tiêu cũ đã lưu")
                        .adviceSummary("Lời khuyên cũ đã lưu")
                        .createdAt(java.time.Instant.now())
                        .build();

        com.learnova.elearning.module.recommendation.entity.UserAiRecommendationItem item =
                com.learnova.elearning.module.recommendation.entity.UserAiRecommendationItem.builder()
                        .id(10L)
                        .recommendation(savedRec)
                        .course(mockCourses.get(0))
                        .matchScore(95)
                        .reason("Lý do cũ")
                        .rankOrder(1)
                        .build();
        savedRec.addItem(item);

        when(userAiRecommendationRepository.findByUserId(100L)).thenReturn(java.util.Optional.of(savedRec));

        AiRecommendationResponse response = recommendationService.getLatestAiRecommendation(100L);

        assertNotNull(response);
        assertEquals("Mục tiêu cũ đã lưu", response.getGoal());
        assertEquals("Lời khuyên cũ đã lưu", response.getSummaryAdvice());
        assertEquals(1, response.getItems().size());
        assertEquals("SAVED_AI_RECOMMENDATION", response.getItems().get(0).getRecommendationType());
    }
}
