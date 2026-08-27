package com.learnova.elearning.module.course.service;

import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import com.learnova.elearning.integration.storage.StorageProperties;
import com.learnova.elearning.integration.storage.StorageService;
import com.learnova.elearning.module.course.dto.request.CreateLessonRequest;
import com.learnova.elearning.module.course.dto.request.CreateSectionRequest;
import com.learnova.elearning.module.course.dto.request.UpdateLessonRequest;
import com.learnova.elearning.module.course.dto.request.UpdateSectionRequest;
import com.learnova.elearning.module.course.dto.response.CurriculumResponse;
import com.learnova.elearning.module.course.dto.response.LessonResourceResponse;
import com.learnova.elearning.module.course.dto.response.LessonResponse;
import com.learnova.elearning.module.course.dto.response.SectionResponse;
import com.learnova.elearning.module.course.entity.Course;
import com.learnova.elearning.module.course.entity.CourseSection;
import com.learnova.elearning.module.course.entity.Lesson;
import com.learnova.elearning.module.course.entity.LessonResource;
import com.learnova.elearning.module.course.entity.enums.LessonContentType;
import com.learnova.elearning.module.course.entity.enums.LessonUploadStatus;
import com.learnova.elearning.module.course.entity.enums.CourseStatus;
import com.learnova.elearning.module.course.mapper.CurriculumMapper;
import com.learnova.elearning.module.course.repository.CourseRepository;
import com.learnova.elearning.module.course.repository.CourseSectionRepository;
import com.learnova.elearning.module.course.repository.LessonRepository;
import com.learnova.elearning.module.course.repository.LessonResourceRepository;
import com.learnova.elearning.module.enrollment.entity.Enrollment;
import com.learnova.elearning.module.enrollment.repository.EnrollmentRepository;
import com.learnova.elearning.module.enrollment.repository.LessonProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * CRUD chương/bài học trong khóa học. Xóa dùng soft delete để không phá
 * lesson_progress của learner (BR-07); object trên storage chỉ dọn khi course
 * chưa từng publish (7.4).
 */
@Service
@RequiredArgsConstructor
public class CurriculumService {

    private final CourseOwnershipGuard ownershipGuard;
    private final CourseSectionRepository sectionRepository;
    private final LessonRepository lessonRepository;
    private final LessonResourceRepository resourceRepository;
    private final StorageService storageService;
    private final StorageProperties storageProperties;
    private final LessonResponseAssembler lessonAssembler;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository progressRepository;

    // ---- Read -------------------------------------------------------------

    @Transactional(readOnly = true)
    public CurriculumResponse getCurriculum(Long courseId, Long userId) {
        ownershipGuard.requireOwnedCourse(courseId, userId);
        return buildCurriculum(courseId);
    }

    @Transactional(readOnly = true)
    public CurriculumResponse getPublicCurriculum(Long courseId, Long studentId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));
        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new AppException(ErrorCode.COURSE_NOT_PUBLISHED);
        }

        boolean enrolled = false;
        Set<Long> completedLessonIds = Set.of();
        if (studentId != null) {
            Optional<Enrollment> enrollment = enrollmentRepository.findByStudent_IdAndCourse_Id(studentId, courseId);
            if (enrollment.isPresent()) {
                enrolled = true;
                completedLessonIds = progressRepository.findByEnrollment_Id(enrollment.get().getId())
                        .stream()
                        .map(lp -> lp.getLesson().getId())
                        .collect(Collectors.toSet());
            }
        }

        return buildPublicCurriculum(courseId, enrolled, completedLessonIds);
    }

    private CurriculumResponse buildPublicCurriculum(Long courseId, boolean enrolled, Set<Long> completedLessonIds) {
        List<CourseSection> sections = sectionRepository.findByCourse_IdOrderByPositionAsc(courseId);
        List<Long> sectionIds = sections.stream().map(CourseSection::getId).toList();

        List<Lesson> lessons = sectionIds.isEmpty()
                ? List.of()
                : lessonRepository.findBySection_IdInOrderByPositionAsc(sectionIds);
        List<Long> lessonIds = lessons.stream().map(Lesson::getId).toList();

        List<LessonResource> resources = lessonIds.isEmpty()
                ? List.of()
                : resourceRepository.findByLesson_IdInOrderByPositionAsc(lessonIds);

        Map<Long, List<Lesson>> lessonsBySection = lessons.stream()
                .collect(Collectors.groupingBy(l -> l.getSection().getId()));
        
        boolean isEnrolled = enrolled;
        Map<Long, List<LessonResourceResponse>> resourcesByLesson = resources.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getLesson().getId(),
                        Collectors.mapping(r -> {
                            if (isEnrolled) {
                                return lessonAssembler.toResource(r);
                            } else {
                                return CurriculumMapper.toResource(r, null);
                            }
                        }, Collectors.toList())));

        Set<Long> finalCompleted = completedLessonIds;
        List<SectionResponse> sectionResponses = sections.stream()
                .map(section -> {
                    List<LessonResponse> lessonResponses = lessonsBySection
                            .getOrDefault(section.getId(), List.of()).stream()
                            .map(l -> {
                                String contentUrl = null;
                                if (l.getStorageKey() != null && (isEnrolled || Boolean.TRUE.equals(l.getIsPreview()))) {
                                    contentUrl = storageService.presignDownload(l.getStorageKey(), storageProperties.getDownloadTtl());
                                }
                                LessonResponse resp = CurriculumMapper.toLesson(l, contentUrl,
                                        resourcesByLesson.getOrDefault(l.getId(), List.of()));
                                if (!finalCompleted.isEmpty()) {
                                    resp.setCompleted(finalCompleted.contains(l.getId()));
                                }
                                return resp;
                            })
                            .toList();
                    return CurriculumMapper.toSection(section, lessonResponses);
                })
                .toList();

        return CurriculumResponse.builder()
                .courseId(courseId)
                .sections(sectionResponses)
                .build();
    }

    private CurriculumResponse buildCurriculum(Long courseId) {
        List<CourseSection> sections = sectionRepository.findByCourse_IdOrderByPositionAsc(courseId);
        List<Long> sectionIds = sections.stream().map(CourseSection::getId).toList();

        List<Lesson> lessons = sectionIds.isEmpty()
                ? List.of()
                : lessonRepository.findBySection_IdInOrderByPositionAsc(sectionIds);
        List<Long> lessonIds = lessons.stream().map(Lesson::getId).toList();

        List<LessonResource> resources = lessonIds.isEmpty()
                ? List.of()
                : resourceRepository.findByLesson_IdInOrderByPositionAsc(lessonIds);

        // Nhóm sẵn để tránh N+1 và giữ đúng thứ tự position
        Map<Long, List<Lesson>> lessonsBySection = lessons.stream()
                .collect(Collectors.groupingBy(l -> l.getSection().getId()));
        Map<Long, List<LessonResourceResponse>> resourcesByLesson = resources.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getLesson().getId(),
                        Collectors.mapping(lessonAssembler::toResource, Collectors.toList())));

        List<SectionResponse> sectionResponses = sections.stream()
                .map(section -> {
                    List<LessonResponse> lessonResponses = lessonsBySection
                            .getOrDefault(section.getId(), List.of()).stream()
                            .map(l -> {
                                String contentUrl = null;
                                if (l.getStorageKey() != null) {
                                    contentUrl = storageService.presignDownload(l.getStorageKey(), storageProperties.getDownloadTtl());
                                }
                                return CurriculumMapper.toLesson(l, contentUrl,
                                        resourcesByLesson.getOrDefault(l.getId(), List.of()));
                            })
                            .toList();
                    return CurriculumMapper.toSection(section, lessonResponses);
                })
                .toList();

        return CurriculumResponse.builder()
                .courseId(courseId)
                .sections(sectionResponses)
                .build();
    }

    // ---- Section ----------------------------------------------------------

    @Transactional
    public SectionResponse addSection(Long courseId, CreateSectionRequest request, Long userId) {
        Course course = ownershipGuard.requireEditableCourse(courseId, userId);
        int position = (int) sectionRepository.countByCourse_Id(courseId);

        CourseSection section = CourseSection.builder()
                .course(course)
                .title(request.getTitle().trim())
                .description(trimToNull(request.getDescription()))
                .position(position)
                .build();

        CourseSection saved = sectionRepository.save(section);
        return CurriculumMapper.toSection(saved, List.of());
    }

    @Transactional
    public SectionResponse updateSection(Long courseId, Long sectionId,
                                         UpdateSectionRequest request, Long userId) {
        ownershipGuard.requireEditableCourse(courseId, userId);
        CourseSection section = ownershipGuard.requireSectionInCourse(sectionId, courseId);

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            section.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            section.setDescription(trimToNull(request.getDescription()));
        }
        CourseSection saved = sectionRepository.save(section);

        List<Lesson> lessons = lessonRepository.findBySection_IdOrderByPositionAsc(sectionId);
        List<LessonResponse> lessonResponses = lessons.stream()
                .map(lessonAssembler::assembleOne)
                .toList();
        return CurriculumMapper.toSection(saved, lessonResponses);
    }

    @Transactional
    public void deleteSection(Long courseId, Long sectionId, Long userId) {
        Course course = ownershipGuard.requireEditableCourse(courseId, userId);
        CourseSection section = ownershipGuard.requireSectionInCourse(sectionId, courseId);

        List<Lesson> lessons = lessonRepository.findBySection_IdOrderByPositionAsc(sectionId);
        softDeleteLessonsCascade(course, lessons);

        section.setDeletedAt(Instant.now());
        sectionRepository.save(section);

        // Dồn lại position của các section còn lại cho liên tục
        normalizeSectionPositions(courseId);
    }

    // ---- Lesson -----------------------------------------------------------

    @Transactional
    public LessonResponse addLesson(Long courseId, Long sectionId,
                                    CreateLessonRequest request, Long userId) {
        ownershipGuard.requireEditableCourse(courseId, userId);
        CourseSection section = ownershipGuard.requireSectionInCourse(sectionId, courseId);

        int position = (int) lessonRepository.countBySection_Id(sectionId);

        Lesson lesson = Lesson.builder()
                .section(section)
                .title(request.getTitle().trim())
                .contentType(request.getContentType())
                .uploadStatus(LessonUploadStatus.EMPTY)
                .position(position)
                .build();

        return lessonAssembler.assembleOne(lessonRepository.save(lesson));
    }

    @Transactional
    public LessonResponse updateLesson(Long courseId, Long lessonId,
                                       UpdateLessonRequest request, Long userId) {
        ownershipGuard.requireEditableCourse(courseId, userId);
        Lesson lesson = ownershipGuard.requireLessonInCourse(lessonId, courseId);

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            lesson.setTitle(request.getTitle().trim());
        }
        if (request.getIsPreview() != null) {
            lesson.setIsPreview(request.getIsPreview());
        }
        if (request.getContentText() != null) {
            if (lesson.getContentType() != LessonContentType.ARTICLE) {
                throw new AppException(ErrorCode.LESSON_CONTENT_TYPE_MISMATCH,
                        "contentText chỉ áp dụng cho lesson dạng ARTICLE");
            }
            lesson.setContentText(request.getContentText());
        }

        return lessonAssembler.assembleOne(lessonRepository.save(lesson));
    }

    @Transactional
    public void deleteLesson(Long courseId, Long lessonId, Long userId) {
        Course course = ownershipGuard.requireEditableCourse(courseId, userId);
        Lesson lesson = ownershipGuard.requireLessonInCourse(lessonId, courseId);
        Long sectionId = lesson.getSection().getId();

        softDeleteLessonsCascade(course, List.of(lesson));

        // Dồn lại position của các lesson còn lại trong section cho liên tục
        normalizeLessonPositions(sectionId);
    }

    // ---- Reorder & Move ---------------------------------------------------

    @Transactional
    public CurriculumResponse reorderSections(Long courseId, List<Long> sectionIds, Long userId) {
        ownershipGuard.requireEditableCourse(courseId, userId);

        List<CourseSection> sections = sectionRepository.findByCourse_IdOrderByPositionAsc(courseId);
        validateIdSetMatches(sectionIds, sections.stream().map(CourseSection::getId).toList());

        Map<Long, CourseSection> byId = sections.stream()
                .collect(Collectors.toMap(CourseSection::getId, s -> s));
        for (int i = 0; i < sectionIds.size(); i++) {
            byId.get(sectionIds.get(i)).setPosition(i);
        }
        sectionRepository.saveAll(sections);

        return buildCurriculum(courseId);
    }

    @Transactional
    public CurriculumResponse reorderLessons(Long courseId, Long sectionId,
                                             List<Long> lessonIds, Long userId) {
        ownershipGuard.requireEditableCourse(courseId, userId);
        ownershipGuard.requireSectionInCourse(sectionId, courseId);

        List<Lesson> lessons = lessonRepository.findBySection_IdOrderByPositionAsc(sectionId);
        validateIdSetMatches(lessonIds, lessons.stream().map(Lesson::getId).toList());

        Map<Long, Lesson> byId = lessons.stream()
                .collect(Collectors.toMap(Lesson::getId, l -> l));
        for (int i = 0; i < lessonIds.size(); i++) {
            byId.get(lessonIds.get(i)).setPosition(i);
        }
        lessonRepository.saveAll(lessons);

        return buildCurriculum(courseId);
    }

    @Transactional
    public CurriculumResponse moveLesson(Long courseId, Long lessonId,
                                         Long targetSectionId, int targetPosition, Long userId) {
        ownershipGuard.requireEditableCourse(courseId, userId);
        Lesson lesson = ownershipGuard.requireLessonInCourse(lessonId, courseId);
        CourseSection targetSection = ownershipGuard.requireSectionInCourse(targetSectionId, courseId);

        Long oldSectionId = lesson.getSection().getId();
        boolean sameSection = oldSectionId.equals(targetSectionId);

        // Đọc anh em ở section đích TRƯỚC khi mutate, loại chính lesson đang di chuyển
        List<Lesson> targetLessons = new ArrayList<>(
                lessonRepository.findBySection_IdOrderByPositionAsc(targetSectionId));
        targetLessons.removeIf(l -> l.getId().equals(lessonId));

        int insertIndex = Math.max(0, Math.min(targetPosition, targetLessons.size()));

        lesson.setSection(targetSection);
        targetLessons.add(insertIndex, lesson);
        for (int i = 0; i < targetLessons.size(); i++) {
            targetLessons.get(i).setPosition(i);
        }
        lessonRepository.saveAll(targetLessons);

        // Section cũ: query sau saveAll sẽ auto-flush việc đổi section_id nên
        // lesson đã chuyển không còn nằm ở đây; dồn lại position phần còn lại
        if (!sameSection) {
            normalizeLessonPositions(oldSectionId);
        }

        return buildCurriculum(courseId);
    }

    // ---- Helpers ----------------------------------------------------------

    /**
     * Soft delete danh sách lesson + resource con. Chỉ xóa object trên storage khi
     * course chưa từng publish (giữ lại nếu đã publish để không ảnh hưởng learner).
     */
    private void softDeleteLessonsCascade(Course course, List<Lesson> lessons) {
        if (lessons.isEmpty()) {
            return;
        }
        boolean purgeStorage = course.getPublishedAt() == null;
        Instant now = Instant.now();

        List<Long> lessonIds = lessons.stream().map(Lesson::getId).toList();
        List<LessonResource> resources = resourceRepository.findByLesson_IdInOrderByPositionAsc(lessonIds);
        for (LessonResource resource : resources) {
            resource.setDeletedAt(now);
            if (purgeStorage) {
                storageService.delete(resource.getStorageKey());
            }
        }
        resourceRepository.saveAll(resources);

        for (Lesson lesson : lessons) {
            lesson.setDeletedAt(now);
            if (purgeStorage && lesson.getStorageKey() != null) {
                storageService.delete(lesson.getStorageKey());
            }
        }
        lessonRepository.saveAll(lessons);
    }

    /**
     * Đảm bảo danh sách id gửi lên khớp CHÍNH XÁC tập hiện có (đúng số lượng, đúng
     * phần tử, không trùng lặp) — tránh việc FE gửi thiếu/thừa id làm item biến mất.
     */
    private void validateIdSetMatches(List<Long> submitted, List<Long> existing) {
        if (submitted == null
                || submitted.size() != existing.size()
                || !new HashSet<>(submitted).equals(new HashSet<>(existing))) {
            throw new AppException(ErrorCode.ORDER_PAYLOAD_MISMATCH);
        }
    }

    private void normalizeSectionPositions(Long courseId) {
        List<CourseSection> sections = sectionRepository.findByCourse_IdOrderByPositionAsc(courseId);
        for (int i = 0; i < sections.size(); i++) {
            sections.get(i).setPosition(i);
        }
        sectionRepository.saveAll(sections);
    }

    private void normalizeLessonPositions(Long sectionId) {
        List<Lesson> lessons = lessonRepository.findBySection_IdOrderByPositionAsc(sectionId);
        for (int i = 0; i < lessons.size(); i++) {
            lessons.get(i).setPosition(i);
        }
        lessonRepository.saveAll(lessons);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
