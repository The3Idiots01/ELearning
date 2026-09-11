package com.learnova.elearning.module.course.service;

import com.learnova.elearning.module.course.config.CoursePublishProperties;
import com.learnova.elearning.module.course.dto.response.PublishIssue;
import com.learnova.elearning.module.course.entity.Course;
import com.learnova.elearning.module.course.entity.Assessment;
import com.learnova.elearning.module.course.entity.LearningOutcome;
import com.learnova.elearning.module.course.entity.Lesson;
import com.learnova.elearning.module.course.entity.enums.AssessmentType;
import com.learnova.elearning.module.course.entity.enums.BulletType;
import com.learnova.elearning.module.course.entity.enums.LessonUploadStatus;
import com.learnova.elearning.module.course.entity.enums.PublicationStatus;
import com.learnova.elearning.module.course.repository.CourseBulletRepository;
import com.learnova.elearning.module.course.repository.CourseSectionRepository;
import com.learnova.elearning.module.course.repository.LearningOutcomeRepository;
import com.learnova.elearning.module.course.repository.LessonRepository;
import com.learnova.elearning.module.course.repository.AssessmentRepository;
import lombok.RequiredArgsConstructor;
import com.learnova.elearning.module.quiz.entity.Quiz;
import com.learnova.elearning.module.quiz.repository.QuizRepository;
import com.learnova.elearning.module.quiz.repository.QuizQuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Kiểm điều kiện publish (FR-CA-04 / BR-06). Chạy cùng bộ rule cho cả publish-check
 * (hiển thị checklist) lẫn publish thật (thực thi ở server — NFR-03).
 */
@Component
@RequiredArgsConstructor
public class CoursePublishValidator {

    private static final BigDecimal PRICE_MAX = new BigDecimal("10000000");

    private final CourseBulletRepository bulletRepository;
    private final CourseSectionRepository sectionRepository;
    private final LessonRepository lessonRepository;
    private final CoursePublishProperties props;

    /**
     * Injected separately so existing unit tests that construct this validator
     * with the pre-outcome constructor remain source-compatible.
     */
    private LearningOutcomeRepository outcomeRepository;

    private AssessmentRepository assessmentRepository;
    private QuizRepository quizRepository;
    private QuizQuestionRepository quizQuestionRepository;

    @Autowired
    void setOutcomeRepository(LearningOutcomeRepository outcomeRepository) {
        this.outcomeRepository = outcomeRepository;
    }

    @Autowired
    void setAlignmentRepositories(AssessmentRepository assessmentRepository,
                                   QuizRepository quizRepository,
                                   QuizQuestionRepository quizQuestionRepository) {
        this.assessmentRepository = assessmentRepository;
        this.quizRepository = quizRepository;
        this.quizQuestionRepository = quizQuestionRepository;
    }

    public int minOutcomes() { return props.getMinObjectives(); }

    public List<PublishIssue> validate(Course course) {
        List<PublishIssue> issues = new ArrayList<>();
        Long courseId = course.getId();

        // --- Thông tin landing page ---
        if (isBlank(course.getTitle()) || course.getTitle().length() > 255) {
            issues.add(issue("TITLE_REQUIRED", "title", "Tiêu đề khóa học là bắt buộc (tối đa 255 ký tự)"));
        }
        int descLen = course.getDescription() == null ? 0 : course.getDescription().trim().length();
        if (descLen < props.getMinDescriptionLength()) {
            issues.add(issue("DESCRIPTION_TOO_SHORT", "description",
                    "Mô tả cần tối thiểu " + props.getMinDescriptionLength() + " ký tự (hiện có " + descLen + ")"));
        }
        if (course.getCategory() == null) {
            issues.add(issue("CATEGORY_REQUIRED", "categoryId", "Cần chọn danh mục cho khóa học"));
        }
        if (isBlank(course.getThumbnailKey())) {
            issues.add(issue("THUMBNAIL_REQUIRED", "thumbnail", "Cần tải lên ảnh bìa khóa học"));
        }
        BigDecimal price = course.getPrice();
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0 || price.compareTo(PRICE_MAX) > 0) {
            issues.add(issue("PRICE_INVALID", "price", "Giá phải trong khoảng 0–10.000.000 VND"));
        }

        // --- Ba khối mô tả ---
        long objectives = outcomeRepository == null
                ? bulletRepository.countByCourse_IdAndBulletType(courseId, BulletType.LEARNING_OBJECTIVE)
                : outcomeRepository.countByCourse_Id(courseId);
        if (objectives < props.getMinObjectives()) {
            issues.add(issue("OUTCOMES_NOT_ENOUGH", "outcomes",
                    "Cần ít nhất " + props.getMinObjectives() + " mục học viên sẽ học được (hiện có " + objectives + ")"));
        }
        long requirements = bulletRepository.countByCourse_IdAndBulletType(courseId, BulletType.REQUIREMENT);
        if (requirements < props.getMinRequirements()) {
            issues.add(issue("REQUIREMENTS_NOT_ENOUGH", "requirements",
                    "Cần ít nhất " + props.getMinRequirements() + " yêu cầu/điều kiện tiên quyết"));
        }
        long audiences = bulletRepository.countByCourse_IdAndBulletType(courseId, BulletType.TARGET_AUDIENCE);
        if (audiences < props.getMinAudiences()) {
            issues.add(issue("AUDIENCE_NOT_ENOUGH", "targetAudiences",
                    "Cần ít nhất " + props.getMinAudiences() + " đối tượng khóa học hướng tới"));
        }

        // --- Nội dung ---
        long sectionCount = sectionRepository.countByCourse_Id(courseId);
        if (sectionCount == 0) {
            issues.add(issue("NO_SECTION", "curriculum", "Khóa học cần ít nhất 1 chương"));
        }

        List<Lesson> lessons = lessonRepository.findBySection_Course_Id(courseId);
        boolean anyComplete = lessons.stream().anyMatch(this::isLessonComplete);
        if (!anyComplete) {
            issues.add(issue("NO_LESSON_WITH_CONTENT", "curriculum",
                    "Khóa học cần ít nhất 1 bài học có nội dung"));
        }
        for (Lesson lesson : lessons) {
            if (!isLessonComplete(lesson)) {
                issues.add(issue("LESSON_CONTENT_INCOMPLETE", "lesson:" + lesson.getId(),
                        "Bài học \"" + lesson.getTitle() + "\" chưa có nội dung hoàn chỉnh",
                        lesson.getId(), curriculumPath(courseId, "lesson-" + lesson.getId())));
            }
            if (lesson.getPendingUploadStatus() != null
                    && lesson.getPendingUploadStatus() != LessonUploadStatus.READY) {
                issues.add(issue("PENDING_VIDEO_NOT_READY", "lesson:" + lesson.getId(),
                        "Video thay thế của bài học chưa xử lý xong", lesson.getId(),
                        curriculumPath(courseId, "lesson-" + lesson.getId())));
            }
        }

        validateAlignment(courseId, lessons, null, issues);

        return issues;
    }

    public List<PublishIssue> validateLiveSnapshot(Course course) {
        Long courseId = course.getId();
        List<PublishIssue> issues = new ArrayList<>();
        if (isBlank(course.getTitle())) issues.add(issue("TITLE_REQUIRED", "title", "Tiêu đề khóa học là bắt buộc"));
        if (course.getCategory() == null) issues.add(issue("CATEGORY_REQUIRED", "categoryId", "Cần chọn danh mục cho khóa học"));
        if (isBlank(course.getThumbnailKey())) issues.add(issue("THUMBNAIL_REQUIRED", "thumbnail", "Cần tải lên ảnh bìa khóa học"));
        BigDecimal price = course.getPrice();
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0 || price.compareTo(PRICE_MAX) > 0) {
            issues.add(issue("PRICE_INVALID", "price", "Giá phải trong khoảng 0–10.000.000 VND"));
        }
        long requirements = bulletRepository.countByCourse_IdAndBulletType(courseId, BulletType.REQUIREMENT);
        long audiences = bulletRepository.countByCourse_IdAndBulletType(courseId, BulletType.TARGET_AUDIENCE);
        if (requirements < props.getMinRequirements()) issues.add(issue("REQUIREMENTS_NOT_ENOUGH", "requirements", "Thiếu yêu cầu/điều kiện tiên quyết"));
        if (audiences < props.getMinAudiences()) issues.add(issue("AUDIENCE_NOT_ENOUGH", "targetAudiences", "Thiếu đối tượng khóa học"));
        if (outcomeRepository != null && outcomeRepository.countByCourse_Id(courseId) < props.getMinObjectives()) {
            issues.add(issue("OUTCOMES_NOT_ENOUGH", "outcomes", "Thiếu kết quả đầu ra tối thiểu"));
        }
        int descriptionLength = course.getDescription() == null ? 0 : course.getDescription().trim().length();
        if (descriptionLength < props.getMinDescriptionLength()) {
            issues.add(issue("DESCRIPTION_TOO_SHORT", "description", "Mô tả khóa học chưa đủ dài"));
        }
        List<Lesson> publishedLessons = lessonRepository.findBySection_Course_Id(courseId).stream()
                .filter(l -> l.getPublicationStatus() == PublicationStatus.PUBLISHED).toList();
        List<Assessment> publishedAssessments = assessmentRepository == null ? List.of()
                : assessmentRepository.findByCourse_Id(courseId).stream()
                    .filter(a -> a.getPublicationStatus() == PublicationStatus.PUBLISHED).toList();
        if (publishedLessons.isEmpty() && publishedAssessments.isEmpty()) {
            issues.add(issue("NO_PUBLISHED_CONTENT", "curriculum", "Khóa học cần ít nhất một nội dung đã xuất bản"));
        }
        for (Lesson lesson : publishedLessons) {
            if (!isLessonComplete(lesson)) {
                issues.add(issue("LESSON_CONTENT_INCOMPLETE", "lesson:" + lesson.getId(),
                        "Nội dung live của bài học không còn hợp lệ", lesson.getId(),
                        curriculumPath(courseId, "lesson-" + lesson.getId())));
            }
        }
        validateAlignment(courseId, publishedLessons, publishedAssessments, issues);
        return issues;
    }

    private void validateAlignment(Long courseId, List<Lesson> lessons,
                                   List<Assessment> requestedAssessments,
                                   List<PublishIssue> issues) {
        if (outcomeRepository == null || assessmentRepository == null) {
            return;
        }

        List<LearningOutcome> outcomes = outcomeRepository.findByCourse_IdOrderByPositionAsc(courseId);
        List<Assessment> assessments = requestedAssessments != null
                ? requestedAssessments : assessmentRepository.findByCourse_Id(courseId);

        for (LearningOutcome outcome : outcomes) {
            boolean usedByLesson = lessons.stream().anyMatch(lesson -> hasOutcome(lesson.getOutcomes(), outcome.getId()));
            boolean usedByAssessment = assessments.stream().anyMatch(assessment -> hasOutcome(assessment.getOutcomes(), outcome.getId()));
            if (!usedByLesson) {
                issues.add(issue("OUTCOME_WITHOUT_LESSON", "outcome:" + outcome.getId(),
                        "Kết quả đầu ra chưa được gắn với bài học", outcome.getId(), curriculumPath(courseId, "outcome-" + outcome.getId())));
            }
            if (!usedByAssessment) {
                issues.add(issue("OUTCOME_WITHOUT_ASSESSMENT", "outcome:" + outcome.getId(),
                        "Kết quả đầu ra chưa được gắn với đánh giá", outcome.getId(), curriculumPath(courseId, "outcome-" + outcome.getId())));
            }
        }

        for (Lesson lesson : lessons) {
            if (lesson.getOutcomes() == null || lesson.getOutcomes().isEmpty()) {
                issues.add(issue("LESSON_WITHOUT_OUTCOME", "lesson:" + lesson.getId(),
                        "Bài học chưa được gắn với kết quả đầu ra", lesson.getId(), curriculumPath(courseId, "lesson-" + lesson.getId())));
            }
        }

        for (Assessment assessment : assessments) {
            String path = curriculumPath(courseId, "assessment-" + assessment.getId());
            if (assessment.getOutcomes() == null || assessment.getOutcomes().isEmpty()) {
                issues.add(issue("ASSESSMENT_WITHOUT_OUTCOME", "assessment:" + assessment.getId(),
                        "Đánh giá chưa được gắn với kết quả đầu ra", assessment.getId(), path));
            }
            if (assessment.getSection() == null) {
                issues.add(issue("ASSESSMENT_NOT_PLACED", "assessment:" + assessment.getId(),
                        "Đánh giá chưa được đặt vào chương học", assessment.getId(), path));
            }
            if (assessment.getType() == AssessmentType.QUIZ) {
                validateQuiz(courseId, assessment, path, issues);
            }
        }
    }

    private void validateQuiz(Long courseId, Assessment assessment, String path, List<PublishIssue> issues) {
        if (quizRepository == null || quizQuestionRepository == null) {
            return;
        }
        Quiz quiz = quizRepository.findByAssessment_IdAndAssessment_Course_Id(assessment.getId(), courseId).orElse(null);
        if (quiz == null || quiz.getPassingScore() == null
                || quiz.getPassingScore().signum() < 0
                || quiz.getPassingScore().compareTo(BigDecimal.valueOf(100)) > 0
                || (quiz.getMaxAttempts() != null && quiz.getMaxAttempts() < 1)) {
            issues.add(issue("QUIZ_CONFIG_INCOMPLETE", "assessment:" + assessment.getId(),
                    "Quiz chưa có cấu hình hợp lệ (điểm đạt 0–100 và số lần làm phải từ 1)", assessment.getId(), path));
            return;
        }

        if (quizQuestionRepository.findByQuiz_IdOrderByPositionAsc(quiz.getId()).isEmpty()) {
            issues.add(issue("QUIZ_WITHOUT_QUESTION", "assessment:" + assessment.getId(),
                    "Quiz cần ít nhất 1 câu hỏi", assessment.getId(), path));
            return;
        }
    }

    /**
     * "Có nội dung thật": VIDEO/FILE đã upload xong (READY); ARTICLE có nội dung text.
     */
    private boolean isLessonComplete(Lesson lesson) {
        if (lesson.getContentType() == null) {
            return false;
        }
        return switch (lesson.getContentType()) {
            case VIDEO, FILE -> lesson.getUploadStatus() == LessonUploadStatus.READY;
            case ARTICLE -> !isBlank(lesson.getContentText());
            case QUIZ -> true;
        };
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean hasOutcome(java.util.Set<LearningOutcome> outcomes, Long outcomeId) {
        return outcomes != null && outcomes.stream().anyMatch(outcome -> outcomeId != null && outcomeId.equals(outcome.getId()));
    }

    private String curriculumPath(Long courseId, String anchor) {
        return "/instructor/courses/" + courseId + "/curriculum#" + anchor;
    }

    private PublishIssue issue(String code, String field, String message) {
        return issue(code, field, message, null, null);
    }

    private PublishIssue issue(String code, String field, String message, Long recordId, String path) {
        return PublishIssue.builder().code(code).field(field).message(message).recordId(recordId).path(path).build();
    }
}
