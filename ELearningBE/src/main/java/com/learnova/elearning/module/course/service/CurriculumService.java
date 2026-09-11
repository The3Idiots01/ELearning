package com.learnova.elearning.module.course.service;

import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import com.learnova.elearning.integration.storage.StorageService;
import com.learnova.elearning.module.course.dto.request.CreateLessonRequest;
import com.learnova.elearning.module.course.dto.request.CreateSectionRequest;
import com.learnova.elearning.module.course.dto.request.UpdateLessonRequest;
import com.learnova.elearning.module.course.dto.request.UpdateSectionRequest;
import com.learnova.elearning.module.course.dto.response.CurriculumResponse;
import com.learnova.elearning.module.course.dto.response.AssessmentResponse;
import com.learnova.elearning.module.course.dto.response.LessonResourceResponse;
import com.learnova.elearning.module.course.dto.response.LessonResponse;
import com.learnova.elearning.module.course.dto.response.SectionResponse;
import com.learnova.elearning.module.course.entity.Course;
import com.learnova.elearning.module.course.entity.CourseSection;
import com.learnova.elearning.module.course.entity.Lesson;
import com.learnova.elearning.module.course.entity.LessonResource;
import com.learnova.elearning.module.course.entity.Assessment;
import com.learnova.elearning.module.course.entity.LearningOutcome;
import com.learnova.elearning.module.course.entity.enums.LessonContentType;
import com.learnova.elearning.module.course.entity.enums.LessonUploadStatus;
import com.learnova.elearning.module.course.entity.enums.CourseStatus;
import com.learnova.elearning.module.course.entity.enums.PublicationStatus;
import com.learnova.elearning.module.course.mapper.CurriculumMapper;
import com.learnova.elearning.module.course.mapper.AssessmentMapper;
import com.learnova.elearning.module.course.repository.CourseRepository;
import com.learnova.elearning.module.course.repository.CourseSectionRepository;
import com.learnova.elearning.module.course.repository.LessonRepository;
import com.learnova.elearning.module.course.repository.LessonResourceRepository;
import com.learnova.elearning.module.course.repository.AssessmentRepository;
import com.learnova.elearning.module.course.repository.LearningOutcomeRepository;
import com.learnova.elearning.module.enrollment.entity.Enrollment;
import com.learnova.elearning.module.enrollment.entity.LessonProgress;
import com.learnova.elearning.module.enrollment.repository.EnrollmentRepository;
import com.learnova.elearning.module.enrollment.repository.LessonProgressRepository;
import com.learnova.elearning.module.enrollment.repository.AssessmentProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Comparator;
import java.util.Set;
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
    private final LessonResponseAssembler lessonAssembler;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository progressRepository;
    private final AssessmentService assessmentService;
    private final AssessmentRepository assessmentRepository;
    private final LearningOutcomeRepository outcomeRepository;
    private final AssessmentProgressRepository assessmentProgressRepository;
    private CoursePublishValidator publishValidator;

    @Autowired
    void setPublishValidator(CoursePublishValidator publishValidator) {
        this.publishValidator = publishValidator;
    }

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
        if (course.getStatus() != CourseStatus.PUBLISHED && (studentId == null || !course.getLecturer().getId().equals(studentId))) {
            throw new AppException(ErrorCode.COURSE_NOT_PUBLISHED);
        }

        boolean enrolled = false;
        Map<Long, LessonProgress> progressByLessonId = Map.of();
        Set<Long> completedAssessmentIds = Set.of();
        if (studentId != null) {
            if (course.getLecturer().getId().equals(studentId)) {
                enrolled = true;
            }
            Optional<Enrollment> enrollment = enrollmentRepository.findByStudent_IdAndCourse_Id(studentId, courseId);
            if (enrollment.isPresent()) {
                enrolled = true;
                progressByLessonId = progressRepository.findByEnrollment_Id(enrollment.get().getId())
                        .stream()
                        .collect(Collectors.toMap(lp -> lp.getLesson().getId(), lp -> lp));
                completedAssessmentIds = assessmentProgressRepository.findByEnrollment_Id(enrollment.get().getId())
                        .stream()
                        .map(progress -> progress.getAssessment().getId())
                        .collect(Collectors.toSet());
            }
        }

        CurriculumResponse response = buildPublicCurriculum(courseId, enrolled, progressByLessonId, completedAssessmentIds);
        Set<Long> playableVideoIds = response.getSections().stream().flatMap(s -> s.getLessons().stream())
                .filter(l -> l.getContentType() == com.learnova.elearning.module.course.entity.enums.LessonContentType.VIDEO
                        && Boolean.TRUE.equals(l.getPlayable()))
                .map(LessonResponse::getId).collect(Collectors.toSet());
        response.setResumeLessonId(progressByLessonId.values().stream()
                .filter(p -> playableVideoIds.contains(p.getLesson().getId()))
                .max(Comparator.comparing((LessonProgress p) -> p.getUpdatedAt() != null ? p.getUpdatedAt() : p.getFirstStartedAt(),
                        Comparator.nullsFirst(Comparator.naturalOrder())).thenComparing(LessonProgress::getId))
                .map(p -> p.getLesson().getId()).orElse(null));
        return response;
    }

    private CurriculumResponse buildPublicCurriculum(Long courseId, boolean enrolled,
                                                     Map<Long, LessonProgress> progressByLessonId,
                                                     Set<Long> completedAssessmentIds) {
        List<CourseSection> sections = sectionRepository.findByCourse_IdOrderByPositionAsc(courseId);
        List<Long> sectionIds = sections.stream().map(CourseSection::getId).toList();

        List<Lesson> lessons = sectionIds.isEmpty()
                ? List.of()
                : lessonRepository.findBySection_IdInOrderByPositionAsc(sectionIds).stream()
                    .filter(l -> l.getPublicationStatus() == PublicationStatus.PUBLISHED)
                    .toList();
        List<Long> lessonIds = lessons.stream().map(Lesson::getId).toList();

        List<LessonResource> resources = lessonIds.isEmpty()
                ? List.of()
                : resourceRepository.findByLesson_IdInOrderByPositionAsc(lessonIds);

        List<Assessment> assessments = (assessmentRepository == null ? List.<Assessment>of()
                : assessmentRepository.findByCourse_Id(courseId)).stream()
                .filter(assessment -> assessment.getPublicationStatus() == PublicationStatus.PUBLISHED)
                .filter(assessment -> assessment.getSection() != null)
                .sorted(Comparator.comparing((Assessment a) -> a.getSection().getPosition())
                        .thenComparing(Assessment::getPosition)
                        .thenComparing(Assessment::getId))
                .toList();

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

        Map<Long, List<AssessmentResponse>> assessmentsBySection = assessments.stream()
                .collect(Collectors.groupingBy(
                        assessment -> assessment.getSection().getId(),
                        Collectors.mapping(assessment -> AssessmentMapper.toResponse(
                                assessment, enrolled && completedAssessmentIds.contains(assessment.getId())),
                                Collectors.toList())));

        List<SectionResponse> sectionResponses = sections.stream()
                .filter(section -> lessonsBySection.containsKey(section.getId())
                        || assessmentsBySection.containsKey(section.getId()))
                .map(section -> {
                    List<LessonResponse> lessonResponses = lessonsBySection
                            .getOrDefault(section.getId(), List.of()).stream()
                            .map(l -> {
                                // Không ký URL hàng loạt nữa (G1) — chỉ báo learner có thể xin
                                // PlaybackTicket cho lesson này hay không; URL cấp riêng lúc bấm phát.
                                boolean playable = l.getStorageKey() != null
                                        && l.getUploadStatus() == LessonUploadStatus.READY
                                        && (isEnrolled || Boolean.TRUE.equals(l.getIsPreview()));
                                LessonProgress progress = progressByLessonId.get(l.getId());
                                LessonResponse resp = CurriculumMapper.toLesson(l, playable,
                                        progress != null ? progress.getLastPositionSeconds() : null,
                                        progress != null ? progress.getCoveragePercent() : null,
                                        resourcesByLesson.getOrDefault(l.getId(), List.of()));
                                if (isEnrolled) {
                                    // Dòng lesson_progress chỉ nghĩa "đã bắt đầu xem" từ V9 —
                                    // hoàn thành phải lọc completedAt != null (§7.2), không phải
                                    // đếm sự tồn tại của dòng.
                                    resp.setCompleted(progress != null && progress.getCompletedAt() != null);
                                }
                                return resp;
                            })
                            .toList();
                    return CurriculumMapper.toSection(section, lessonResponses,
                            assessmentsBySection.getOrDefault(section.getId(), List.of()));
                })
                .toList();

        return CurriculumResponse.builder()
                .courseId(courseId)
                .sections(sectionResponses)
                .unplacedAssessments(List.of())
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

        List<Assessment> assessments = (assessmentRepository == null ? List.<Assessment>of()
                : assessmentRepository.findByCourse_Id(courseId)).stream()
                .sorted(Comparator.comparing((Assessment a) -> a.getSection() == null ? 1 : 0)
                        .thenComparing(a -> a.getSection() == null ? Integer.MAX_VALUE : a.getSection().getPosition())
                        .thenComparing(Assessment::getPosition)
                        .thenComparing(Assessment::getId))
                .toList();

        Map<Long, List<AssessmentResponse>> assessmentsBySection = assessments.stream()
                .filter(assessment -> assessment.getSection() != null)
                .collect(Collectors.groupingBy(
                        assessment -> assessment.getSection().getId(),
                        Collectors.mapping(AssessmentMapper::toResponse, Collectors.toList())));
        List<AssessmentResponse> unplacedAssessments = assessments.stream()
                .filter(assessment -> assessment.getSection() == null)
                .map(AssessmentMapper::toResponse)
                .toList();

        List<SectionResponse> sectionResponses = sections.stream()
                .map(section -> {
                    List<LessonResponse> lessonResponses = lessonsBySection
                            .getOrDefault(section.getId(), List.of()).stream()
                            .map(l -> {
                                boolean playable = l.getStorageKey() != null
                                        && l.getUploadStatus() == LessonUploadStatus.READY;
                                return CurriculumMapper.toLesson(l, playable, null, null,
                                        resourcesByLesson.getOrDefault(l.getId(), List.of()));
                            })
                            .toList();
                    return CurriculumMapper.toSection(section, lessonResponses,
                            assessmentsBySection.getOrDefault(section.getId(), List.of()));
                })
                .toList();

        return CurriculumResponse.builder()
                .courseId(courseId)
                .sections(sectionResponses)
                .unplacedAssessments(unplacedAssessments)
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
        List<AssessmentResponse> assessmentResponses = (assessmentRepository == null ? List.<Assessment>of()
                : assessmentRepository.findBySection_IdOrderByPositionAscIdAsc(sectionId)).stream()
                .map(AssessmentMapper::toResponse)
                .toList();
        return CurriculumMapper.toSection(saved, lessonResponses, assessmentResponses);
    }

    @Transactional
    public void deleteSection(Long courseId, Long sectionId, Long userId) {
        deleteSection(courseId, sectionId, userId, false);
    }

    @Transactional
    public void deleteSection(Long courseId, Long sectionId, Long userId, boolean confirmed) {
        Course course = ownershipGuard.requireEditableCourse(courseId, userId);
        CourseSection section = ownershipGuard.requireSectionInCourse(sectionId, courseId);

        List<Lesson> lessons = lessonRepository.findBySection_IdOrderByPositionAsc(sectionId);
        boolean liveContent = lessons.stream().anyMatch(l -> l.getPublicationStatus() == PublicationStatus.PUBLISHED)
                || (assessmentRepository != null && assessmentRepository.findBySection_IdOrderByPositionAscIdAsc(sectionId)
                    .stream().anyMatch(a -> a.getPublicationStatus() == PublicationStatus.PUBLISHED));
        if (course.getStatus() == CourseStatus.PUBLISHED && liveContent && !confirmed) {
            throw new AppException(ErrorCode.COURSE_NOT_READY_TO_PUBLISH,
                    "Xác nhận để ẩn chương đã xuất bản và tính lại tiến độ");
        }
        softDeleteLessonsCascade(course, lessons);

        // Assessments are designed independently from curriculum. Removing a
        // section only returns them to the unplaced pool; quiz data stays intact.
        assessmentService.unassignFromSection(sectionId);

        section.setDeletedAt(Instant.now());
        sectionRepository.save(section);

        // Dồn lại position của các section còn lại cho liên tục
        normalizeSectionPositions(courseId);
        validateLiveCourse(course);
        recalculateActiveEnrollments(courseId);
    }

    // ---- Lesson -----------------------------------------------------------

    @Transactional
    public LessonResponse addLesson(Long courseId, Long sectionId,
                                    CreateLessonRequest request, Long userId) {
        ownershipGuard.requireEditableCourse(courseId, userId);
        CourseSection section = ownershipGuard.requireSectionInCourse(sectionId, courseId);
        Set<LearningOutcome> outcomes = resolveOutcomes(courseId, request.getOutcomeIds());

        int position = (int) lessonRepository.countBySection_Id(sectionId);

        Lesson lesson = Lesson.builder()
                .section(section)
                .title(request.getTitle().trim())
                .contentType(request.getContentType())
                .outcomes(outcomes)
                .uploadStatus(LessonUploadStatus.EMPTY)
                .publicationStatus(PublicationStatus.DRAFT)
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
        if (request.getOutcomeIds() != null) {
            lesson.setOutcomes(resolveOutcomes(courseId, request.getOutcomeIds()));
        }

        LessonResponse response = lessonAssembler.assembleOne(lessonRepository.save(lesson));
        validateLiveCourse(lesson.getSection().getCourse());
        return response;
    }

    @Transactional
    public void deleteLesson(Long courseId, Long lessonId, Long userId) {
        deleteLesson(courseId, lessonId, userId, false);
    }

    @Transactional
    public void deleteLesson(Long courseId, Long lessonId, Long userId, boolean confirmed) {
        Course course = ownershipGuard.requireEditableCourse(courseId, userId);
        Lesson lesson = ownershipGuard.requireLessonInCourse(lessonId, courseId);
        Long sectionId = lesson.getSection().getId();

        if (course.getStatus() == CourseStatus.PUBLISHED
                && lesson.getPublicationStatus() == PublicationStatus.PUBLISHED && !confirmed) {
            throw new AppException(ErrorCode.COURSE_NOT_READY_TO_PUBLISH,
                    "Xác nhận để archive bài học đã xuất bản và tính lại tiến độ");
        }

        softDeleteLessonsCascade(course, List.of(lesson));

        // Dồn lại position của các lesson còn lại trong section cho liên tục
        normalizeLessonPositions(sectionId);
        validateLiveCourse(course);
        recalculateActiveEnrollments(courseId);
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

    private void validateLiveCourse(Course course) {
        if (course.getStatus() == CourseStatus.PUBLISHED && publishValidator != null) {
            List<com.learnova.elearning.module.course.dto.response.PublishIssue> issues =
                    publishValidator.validateLiveSnapshot(course);
            if (!issues.isEmpty()) throw new com.learnova.elearning.module.course.exception.CourseNotReadyException(issues);
        }
    }

    private void recalculateActiveEnrollments(Long courseId) {
        if (enrollmentRepository == null) return;
        enrollmentRepository.findByCourse_Id(courseId).stream()
                .filter(e -> e.getStatus().name().equals("ACTIVE"))
                .forEach(e -> enrollmentRepository.recalculateCourseProgress(e.getId(), courseId));
    }

    private Set<LearningOutcome> resolveOutcomes(Long courseId, List<Long> outcomeIds) {
        if (outcomeIds == null || outcomeIds.isEmpty()) {
            return new LinkedHashSet<>();
        }
        Set<Long> uniqueIds = new LinkedHashSet<>(outcomeIds);
        if (uniqueIds.size() != outcomeIds.size()) {
            throw new AppException(ErrorCode.ASSESSMENT_OUTCOME_MISMATCH,
                    "outcomeIds must not contain duplicates");
        }
        List<LearningOutcome> outcomes = outcomeRepository.findByCourse_IdAndIdIn(courseId, uniqueIds);
        if (outcomes.size() != uniqueIds.size()) {
            throw new AppException(ErrorCode.OUTCOME_NOT_IN_COURSE);
        }
        Map<Long, LearningOutcome> byId = outcomes.stream()
                .collect(Collectors.toMap(LearningOutcome::getId, outcome -> outcome));
        return uniqueIds.stream().map(byId::get)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
