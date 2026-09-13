package com.learnova.elearning.module.recommendation.service.impl;

import com.learnova.elearning.integration.ai.AiProperties;
import com.learnova.elearning.integration.storage.StorageProperties;
import com.learnova.elearning.integration.storage.StorageService;
import com.learnova.elearning.module.category.entity.Category;
import com.learnova.elearning.module.course.dto.response.CourseSummaryResponse;
import com.learnova.elearning.module.course.entity.Course;
import com.learnova.elearning.module.course.entity.enums.CourseLevel;
import com.learnova.elearning.module.course.entity.enums.CourseStatus;
import com.learnova.elearning.module.course.mapper.CourseMapper;
import com.learnova.elearning.module.course.repository.CourseRepository;
import com.learnova.elearning.module.enrollment.entity.Enrollment;
import com.learnova.elearning.module.enrollment.repository.EnrollmentRepository;
import com.learnova.elearning.module.recommendation.dto.request.AiRecommendationRequest;
import com.learnova.elearning.module.recommendation.dto.response.AiRecommendationResponse;
import com.learnova.elearning.module.recommendation.dto.response.ContinuousRecommendationResponse;
import com.learnova.elearning.module.recommendation.dto.response.RecommendedCourseResponse;
import com.learnova.elearning.module.recommendation.engine.AiRecommendationEngine;
import com.learnova.elearning.module.recommendation.entity.UserAiRecommendation;
import com.learnova.elearning.module.recommendation.entity.UserAiRecommendationItem;
import com.learnova.elearning.module.recommendation.repository.UserAiRecommendationRepository;
import com.learnova.elearning.module.recommendation.service.CourseRecommendationService;
import com.learnova.elearning.module.user.entity.User;
import com.learnova.elearning.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseRecommendationServiceImpl implements CourseRecommendationService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AiRecommendationEngine aiRecommendationEngine;
    private final UserAiRecommendationRepository userAiRecommendationRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final StorageProperties storageProperties;
    private final AiProperties aiProperties;

    @Override
    @Transactional(readOnly = true)
    public ContinuousRecommendationResponse getContinuousRecommendations(Long courseId, Long categoryId, Long learnerId) {
        List<Course> allPublished = courseRepository.findByStatus(CourseStatus.PUBLISHED);
        if (allPublished.isEmpty()) {
            return ContinuousRecommendationResponse.builder()
                    .items(Collections.emptyList())
                    .strategy("NONE")
                    .total(0)
                    .build();
        }

        // Lấy danh sách ID các khóa học học viên đã đăng ký để loại trừ
        Set<Long> enrolledCourseIds = getEnrolledCourseIds(learnerId);

        // Khóa học tham chiếu (nếu đang xem trang chi tiết một khóa học)
        Course currentCourse = courseId != null ? findCourseSafely(courseId, allPublished) : null;

        // Lọc bỏ chính khóa học đang xem, các khóa học người dùng đã mua, và khóa học do chính người dùng tạo
        List<Course> candidates = allPublished.stream()
                .filter(c -> currentCourse == null || !c.getId().equals(currentCourse.getId()))
                .filter(c -> !enrolledCourseIds.contains(c.getId()))
                .filter(c -> !isCreatedByLearner(c, learnerId))
                .toList();

        if (candidates.isEmpty()) {
            return ContinuousRecommendationResponse.builder()
                    .items(Collections.emptyList())
                    .strategy("NONE")
                    .total(0)
                    .build();
        }

        String strategy;
        Map<Course, Double> scoredCourses = new HashMap<>();
        Map<Course, String> reasons = new HashMap<>();

        if (currentCourse != null) {
            // Chiến lược 1: Gợi ý liên quan đến khóa học đang xem
            strategy = "RELATED_COURSE";
            Long refCatId = currentCourse.getCategory() != null ? currentCourse.getCategory().getId() : null;
            CourseLevel refLevel = currentCourse.getLevel();

            for (Course c : candidates) {
                double score = 0.0;
                Long cCatId = c.getCategory() != null ? c.getCategory().getId() : null;
                boolean sameCategory = refCatId != null && refCatId.equals(cCatId);

                if (sameCategory) {
                    score += 45.0;
                }
                if (refLevel != null && c.getLevel() != null) {
                    if (refLevel == c.getLevel()) {
                        score += 20.0;
                    } else if (isNextLevel(refLevel, c.getLevel())) {
                        score += 25.0; // Bước tiến tiếp theo
                    }
                }

                // Điểm uy tín chất lượng
                score += (c.getRatingAvg() != null ? c.getRatingAvg().doubleValue() : 0.0) * 5.0; // max 25
                score += Math.min(10.0, (c.getTotalStudents() != null ? c.getTotalStudents() : 0) / 5.0);

                scoredCourses.put(c, score);
                if (sameCategory) {
                    reasons.put(c, "Cùng chủ đề " + (c.getCategory() != null ? c.getCategory().getName() : "")
                            + " và phù hợp để học tiếp theo.");
                } else {
                    reasons.put(c, "Khóa học nổi bật được học viên đánh giá cao.");
                }
            }
        } else if (learnerId != null && !enrolledCourseIds.isEmpty()) {
            // Chiến lược 2: Cá nhân hóa theo lịch sử học viên đã đăng ký
            strategy = "LEARNER_PERSONALIZED";
            Set<Long> learnerFavoriteCatIds = getLearnerFavoriteCategoryIds(learnerId);

            for (Course c : candidates) {
                double score = 0.0;
                Long cCatId = c.getCategory() != null ? c.getCategory().getId() : null;
                boolean matchInterest = cCatId != null && learnerFavoriteCatIds.contains(cCatId);

                if (matchInterest) {
                    score += 40.0;
                }
                score += (c.getRatingAvg() != null ? c.getRatingAvg().doubleValue() : 0.0) * 7.0; // max 35
                score += Math.min(25.0, (c.getTotalStudents() != null ? c.getTotalStudents() : 0) / 2.0);

                scoredCourses.put(c, score);
                if (matchInterest) {
                    reasons.put(c, "Phù hợp với lĩnh vực bạn đang tích cực theo học.");
                } else {
                    reasons.put(c, "Khóa học chất lượng cao được cộng đồng sinh viên đánh giá tốt.");
                }
            }
        } else if (categoryId != null) {
            // Chiến lược 3: Xu hướng theo danh mục cụ thể
            strategy = "CATEGORY_TRENDING";
            for (Course c : candidates) {
                double score = 0.0;
                Long cCatId = c.getCategory() != null ? c.getCategory().getId() : null;
                if (categoryId.equals(cCatId)) {
                    score += 50.0;
                }
                score += (c.getRatingAvg() != null ? c.getRatingAvg().doubleValue() : 0.0) * 6.0;
                score += Math.min(20.0, (c.getTotalStudents() != null ? c.getTotalStudents() : 0) / 3.0);

                scoredCourses.put(c, score);
                reasons.put(c, "Khóa học tiêu biểu trong danh mục bạn đang quan tâm.");
            }
        } else {
            // Chiến lược 4: Khóa học phổ biến & chất lượng nhất toàn hệ thống
            strategy = "POPULAR_DISCOVERY";
            for (Course c : candidates) {
                double score = 0.0;
                score += (c.getRatingAvg() != null ? c.getRatingAvg().doubleValue() : 0.0) * 10.0; // max 50
                score += Math.min(30.0, (c.getTotalStudents() != null ? c.getTotalStudents() : 0));
                score += Math.min(20.0, (c.getRatingCount() != null ? c.getRatingCount() : 0) * 2.0);

                scoredCourses.put(c, score);
                reasons.put(c, "Khóa học được đánh giá cao nhất và có nhiều học viên theo học.");
            }
        }

        // Lấy Top 5 khóa học có điểm số cao nhất
        List<Course> top5 = candidates.stream()
                .sorted((c1, c2) -> Double.compare(scoredCourses.getOrDefault(c2, 0.0), scoredCourses.getOrDefault(c1, 0.0)))
                .limit(5)
                .toList();

        List<RecommendedCourseResponse> items = top5.stream()
                .map(c -> {
                    double rawScore = scoredCourses.getOrDefault(c, 50.0);
                    // Chuẩn hóa điểm số trong khoảng [0.60, 0.99]
                    double normalized = Math.min(0.99, Math.max(0.60, rawScore / 100.0));
                    BigDecimal matchScore = BigDecimal.valueOf(normalized).setScale(2, RoundingMode.HALF_UP);

                    return RecommendedCourseResponse.builder()
                            .course(CourseMapper.toSummary(c, signThumbnail(c)))
                            .matchScore(matchScore)
                            .recommendationReason(reasons.getOrDefault(c, "Đề xuất phù hợp từ hệ thống Learnova."))
                            .recommendationType("SYSTEM_RULE")
                            .build();
                })
                .toList();

        return ContinuousRecommendationResponse.builder()
                .items(items)
                .strategy(strategy)
                .total(items.size())
                .build();
    }

    @Override
    @Transactional
    public AiRecommendationResponse getAiRecommendations(AiRecommendationRequest request, Long learnerId) {
        List<Course> allPublished = courseRepository.findByStatus(CourseStatus.PUBLISHED);
        if (allPublished.isEmpty()) {
            return AiRecommendationResponse.builder()
                    .items(Collections.emptyList())
                    .modelUsed(aiProperties.getModel())
                    .fallback(false)
                    .summaryAdvice("Hiện chưa có khóa học nào sẵn sàng để đề xuất.")
                    .build();
        }

        Set<Long> enrolledIds = getEnrolledCourseIds(learnerId);
        List<String> enrolledTitles = getEnrolledCourseTitles(learnerId);

        // Lọc ứng viên: loại bỏ khóa học đã mua và khóa học do chính người dùng tạo
        List<Course> candidates = allPublished.stream()
                .filter(c -> !enrolledIds.contains(c.getId()))
                .filter(c -> !isCreatedByLearner(c, learnerId))
                .filter(c -> request == null || request.getTargetCategoryId() == null
                        || (c.getCategory() != null && request.getTargetCategoryId().equals(c.getCategory().getId())))
                .toList();

        if (candidates.isEmpty()) {
            candidates = allPublished.stream()
                    .filter(c -> !enrolledIds.contains(c.getId()))
                    .filter(c -> !isCreatedByLearner(c, learnerId))
                    .toList();
        }

        // Chuyển đổi thành context tóm tắt để gửi sang AI Engine (tiết kiệm token & giảm latency)
        List<AiRecommendationEngine.CoursePromptContext> promptContexts = candidates.stream()
                .map(c -> AiRecommendationEngine.CoursePromptContext.builder()
                        .id(c.getId())
                        .title(c.getTitle())
                        .headline(c.getSubtitle())
                        .category(c.getCategory() != null ? c.getCategory().getName() : "Chung")
                        .level(c.getLevel() != null ? c.getLevel().name() : "All Levels")
                        .ratingAvg(c.getRatingAvg())
                        .totalStudents(c.getTotalStudents())
                        .build())
                .toList();

        // 1. Thử gọi AI Engine
        AiRecommendationEngine.RawAiRecommendationResult aiResult =
                aiRecommendationEngine.getRecommendations(request, promptContexts, enrolledTitles);

        Map<Long, Course> courseMap = candidates.stream()
                .collect(Collectors.toMap(Course::getId, c -> c, (a, b) -> a));

        AiRecommendationResponse response = null;

        if (aiResult != null && aiResult.getRecommendations() != null && !aiResult.getRecommendations().isEmpty()) {
            List<RecommendedCourseResponse> items = new ArrayList<>();

            for (AiRecommendationEngine.RawAiCourseItem rec : aiResult.getRecommendations()) {
                Course matchedCourse = courseMap.get(rec.getCourseId());
                if (matchedCourse != null) {
                    BigDecimal score = rec.getScore() != null
                            ? rec.getScore().setScale(2, RoundingMode.HALF_UP)
                            : BigDecimal.valueOf(0.90);

                    items.add(RecommendedCourseResponse.builder()
                            .course(CourseMapper.toSummary(matchedCourse, signThumbnail(matchedCourse)))
                            .matchScore(score)
                            .recommendationReason(rec.getReason())
                            .recommendationType("GEMINI_AI")
                            .build());
                }
                if (items.size() >= 3) {
                    break;
                }
            }

            if (!items.isEmpty()) {
                response = AiRecommendationResponse.builder()
                        .items(items)
                        .modelUsed(aiProperties.getModel() != null ? aiProperties.getModel() : "gemini-3.5-flash-lite")
                        .fallback(false)
                        .summaryAdvice(aiResult.getSummaryAdvice() != null
                                ? aiResult.getSummaryAdvice()
                                : "Lộ trình học tập được AI tối ưu hóa riêng theo mục tiêu của bạn.")
                        .build();
            }
        }

        if (response == null) {
            // 2. Fallback an toàn sang logic hệ thống (Top 3) khi AI bận / hết quota / lỗi
            log.info("Applying graceful system fallback for AI course recommendation (learnerId={})", learnerId);
            ContinuousRecommendationResponse systemRec = getContinuousRecommendations(null,
                    request != null ? request.getTargetCategoryId() : null, learnerId);

            List<RecommendedCourseResponse> fallbackItems = systemRec.getItems().stream()
                    .limit(3)
                    .map(item -> RecommendedCourseResponse.builder()
                            .course(item.getCourse())
                            .matchScore(item.getMatchScore())
                            .recommendationReason(item.getRecommendationReason() + " (Đề xuất tự động từ hệ thống)")
                            .recommendationType("SYSTEM_FALLBACK")
                            .build())
                    .toList();

            response = AiRecommendationResponse.builder()
                    .items(fallbackItems)
                    .modelUsed("system-logic-fallback")
                    .fallback(true)
                    .summaryAdvice("Hệ thống tự động lựa chọn các khóa học uy tín, có lượt đánh giá tích cực nhất phù hợp với bạn.")
                    .build();
        }

        // Lưu đề xuất AI vào DB cho người dùng (ghi đè và xóa đề xuất cũ nếu có)
        if (learnerId != null && !response.getItems().isEmpty()) {
            saveUserRecommendation(learnerId, request, response);
        }

        return response;
    }

    @Override
    @Transactional
    public AiRecommendationResponse getQuickAiRecommendations(Long learnerId) {
        if (learnerId == null) {
            return getAiRecommendations(null, null);
        }

        List<Enrollment> enrollments = enrollmentRepository.findByStudent_IdOrderByEnrolledAtDesc(learnerId);
        List<String> interests = new ArrayList<>();
        CourseLevel maxLevel = CourseLevel.BEGINNER;

        for (Enrollment e : enrollments) {
            Course c = e.getCourse();
            if (c != null) {
                if (c.getCategory() != null && !interests.contains(c.getCategory().getName())) {
                    interests.add(c.getCategory().getName());
                }
                if (c.getLevel() == CourseLevel.ADVANCED) {
                    maxLevel = CourseLevel.ADVANCED;
                } else if (c.getLevel() == CourseLevel.INTERMEDIATE && maxLevel != CourseLevel.ADVANCED) {
                    maxLevel = CourseLevel.INTERMEDIATE;
                }
            }
        }

        AiRecommendationRequest request = AiRecommendationRequest.builder()
                .goal("Nâng cao và mở rộng kỹ năng chuyên môn từ các khóa học hiện tại")
                .interests(interests)
                .preferredLevel(maxLevel)
                .build();

        return getAiRecommendations(request, learnerId);
    }

    @Override
    @Transactional(readOnly = true)
    public AiRecommendationResponse getLatestAiRecommendation(Long learnerId) {
        if (learnerId == null) {
            return null;
        }

        Optional<UserAiRecommendation> opt = userAiRecommendationRepository.findByUserId(learnerId);
        if (opt.isEmpty()) {
            return null;
        }

        UserAiRecommendation rec = opt.get();
        List<RecommendedCourseResponse> items = new ArrayList<>();

        for (UserAiRecommendationItem item : rec.getItems()) {
            Course c = item.getCourse();
            if (c != null && c.getStatus() == CourseStatus.PUBLISHED) {
                BigDecimal score = BigDecimal.valueOf(item.getMatchScore())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                items.add(RecommendedCourseResponse.builder()
                        .course(CourseMapper.toSummary(c, signThumbnail(c)))
                        .matchScore(score)
                        .recommendationReason(item.getReason())
                        .recommendationType("SAVED_AI_RECOMMENDATION")
                        .build());
            }
        }

        if (items.isEmpty()) {
            return null;
        }

        return AiRecommendationResponse.builder()
                .items(items)
                .modelUsed("gemini-3.5-flash-lite")
                .fallback(false)
                .summaryAdvice(rec.getAdviceSummary())
                .goal(rec.getGoal())
                .createdAt(rec.getCreatedAt())
                .build();
    }

    private void saveUserRecommendation(Long learnerId, AiRecommendationRequest request, AiRecommendationResponse response) {
        try {
            userAiRecommendationRepository.deleteByUserId(learnerId);
            userAiRecommendationRepository.flush();

            User user = userRepository.findById(learnerId).orElse(null);
            if (user == null) {
                return;
            }

            String goal = request != null && request.getGoal() != null && !request.getGoal().isBlank()
                    ? request.getGoal()
                    : "Tư vấn lộ trình học tập tối ưu với AI";
            String preferredLevel = request != null && request.getPreferredLevel() != null
                    ? request.getPreferredLevel().name()
                    : null;
            String interests = (request != null && request.getInterests() != null && !request.getInterests().isEmpty())
                    ? String.join(", ", request.getInterests())
                    : null;

            UserAiRecommendation rec = UserAiRecommendation.builder()
                    .user(user)
                    .goal(goal)
                    .preferredLevel(preferredLevel)
                    .interests(interests)
                    .adviceSummary(response.getSummaryAdvice())
                    .build();

            int rank = 1;
            for (RecommendedCourseResponse item : response.getItems()) {
                if (item.getCourse() != null && item.getCourse().getId() != null) {
                    Course course = courseRepository.findById(item.getCourse().getId()).orElse(null);
                    if (course != null) {
                        int scoreInt = item.getMatchScore() != null
                                ? (int) Math.round(item.getMatchScore().doubleValue() * 100)
                                : 90;
                        UserAiRecommendationItem recItem = UserAiRecommendationItem.builder()
                                .recommendation(rec)
                                .course(course)
                                .matchScore(scoreInt)
                                .reason(item.getRecommendationReason())
                                .rankOrder(rank++)
                                .build();
                        rec.addItem(recItem);
                    }
                }
            }

            userAiRecommendationRepository.save(rec);
            response.setGoal(goal);
            response.setCreatedAt(Instant.now());
        } catch (Exception e) {
            log.error("Failed to persist user AI recommendation for learnerId={}: {}", learnerId, e.getMessage(), e);
        }
    }

    private Set<Long> getEnrolledCourseIds(Long learnerId) {
        if (learnerId == null) {
            return Collections.emptySet();
        }
        return enrollmentRepository.findByStudent_IdOrderByEnrolledAtDesc(learnerId).stream()
                .map(e -> e.getCourse() != null ? e.getCourse().getId() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private List<String> getEnrolledCourseTitles(Long learnerId) {
        if (learnerId == null) {
            return Collections.emptyList();
        }
        return enrollmentRepository.findByStudent_IdOrderByEnrolledAtDesc(learnerId).stream()
                .map(e -> e.getCourse() != null ? e.getCourse().getTitle() : null)
                .filter(Objects::nonNull)
                .toList();
    }

    private Set<Long> getLearnerFavoriteCategoryIds(Long learnerId) {
        if (learnerId == null) {
            return Collections.emptySet();
        }
        return enrollmentRepository.findByStudent_IdOrderByEnrolledAtDesc(learnerId).stream()
                .map(e -> e.getCourse() != null && e.getCourse().getCategory() != null ? e.getCourse().getCategory().getId() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Course findCourseSafely(Long courseId, List<Course> courses) {
        return courses.stream()
                .filter(c -> c.getId().equals(courseId))
                .findFirst()
                .orElse(null);
    }

    private boolean isNextLevel(CourseLevel current, CourseLevel candidate) {
        if (current == CourseLevel.BEGINNER && candidate == CourseLevel.INTERMEDIATE) {
            return true;
        }
        return current == CourseLevel.INTERMEDIATE && candidate == CourseLevel.ADVANCED;
    }

    private boolean isCreatedByLearner(Course course, Long learnerId) {
        if (learnerId == null || course == null || course.getLecturer() == null) {
            return false;
        }
        return learnerId.equals(course.getLecturer().getId());
    }

    private String signThumbnail(Course course) {
        if (course.getThumbnailKey() == null) {
            return null;
        }
        try {
            return storageService.presignDownload(course.getThumbnailKey(), storageProperties.getDownloadTtl());
        } catch (Exception e) {
            return null;
        }
    }
}
