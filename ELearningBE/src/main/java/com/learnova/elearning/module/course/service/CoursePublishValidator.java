package com.learnova.elearning.module.course.service;

import com.learnova.elearning.module.course.config.CoursePublishProperties;
import com.learnova.elearning.module.course.dto.response.PublishIssue;
import com.learnova.elearning.module.course.entity.Course;
import com.learnova.elearning.module.course.entity.Lesson;
import com.learnova.elearning.module.course.entity.enums.BulletType;
import com.learnova.elearning.module.course.entity.enums.LessonContentType;
import com.learnova.elearning.module.course.entity.enums.LessonUploadStatus;
import com.learnova.elearning.module.course.repository.CourseBulletRepository;
import com.learnova.elearning.module.course.repository.CourseSectionRepository;
import com.learnova.elearning.module.course.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
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
        long objectives = bulletRepository.countByCourse_IdAndBulletType(courseId, BulletType.LEARNING_OBJECTIVE);
        if (objectives < props.getMinObjectives()) {
            issues.add(issue("OBJECTIVES_NOT_ENOUGH", "learningObjectives",
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
                String code = lesson.getContentType() == LessonContentType.QUIZ
                        ? "QUIZ_INCOMPLETE" : "LESSON_CONTENT_INCOMPLETE";
                issues.add(issue(code, "lesson:" + lesson.getId(),
                        "Bài học \"" + lesson.getTitle() + "\" chưa có nội dung hoàn chỉnh"));
            }
        }

        return issues;
    }

    /**
     * "Có nội dung thật": VIDEO/FILE đã upload xong (READY); ARTICLE có nội dung text;
     * QUIZ hiện chưa hoàn chỉnh được (module quiz thuộc Sprint 2 — sẽ thay bằng kiểm
     * tra số câu hỏi/đáp án khi có bảng quizzes).
     */
    private boolean isLessonComplete(Lesson lesson) {
        return switch (lesson.getContentType()) {
            case VIDEO, FILE -> lesson.getUploadStatus() == LessonUploadStatus.READY;
            case ARTICLE -> !isBlank(lesson.getContentText());
            case QUIZ -> false;
        };
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private PublishIssue issue(String code, String field, String message) {
        return PublishIssue.builder().code(code).field(field).message(message).build();
    }
}
