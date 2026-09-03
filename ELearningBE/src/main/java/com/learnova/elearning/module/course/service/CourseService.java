package com.learnova.elearning.module.course.service;

import com.learnova.elearning.common.dto.PageResponse;
import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import com.learnova.elearning.common.util.SlugGenerator;
import com.learnova.elearning.integration.storage.StorageKeyFactory;
import com.learnova.elearning.integration.storage.StorageProperties;
import com.learnova.elearning.integration.storage.StorageService;
import com.learnova.elearning.module.category.entity.Category;
import com.learnova.elearning.module.category.service.CategoryService;
import com.learnova.elearning.module.course.dto.request.CreateCourseRequest;
import com.learnova.elearning.module.course.dto.request.UpdateBulletsRequest;
import com.learnova.elearning.module.course.dto.request.UpdateCourseRequest;
import com.learnova.elearning.module.course.dto.request.UpdatePriceRequest;
import com.learnova.elearning.module.course.dto.request.UpdateThumbnailRequest;
import com.learnova.elearning.module.course.dto.response.CourseResponse;
import com.learnova.elearning.module.course.dto.response.CourseSummaryResponse;
import com.learnova.elearning.module.course.entity.Course;
import com.learnova.elearning.module.course.entity.CourseBullet;
import com.learnova.elearning.module.course.entity.enums.BulletType;
import com.learnova.elearning.module.course.entity.enums.CourseLevel;
import com.learnova.elearning.module.course.entity.enums.CourseStatus;
import com.learnova.elearning.module.course.mapper.CourseMapper;
import com.learnova.elearning.module.course.repository.CourseBulletRepository;
import com.learnova.elearning.module.course.repository.CourseRepository;
import com.learnova.elearning.module.enrollment.repository.EnrollmentRepository;
import com.learnova.elearning.module.user.entity.User;
import com.learnova.elearning.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * CRUD khóa học cho instructor. Quyền sở hữu do {@link CourseOwnershipGuard} bảo
 * đảm: chỉ chủ sở hữu (lecturer) sửa được — admin dù vào được endpoint cũng bị
 * chặn vì id không khớp lecturer_id (không có quyền sửa nội dung course).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CourseService {

    private static final String DEFAULT_SLUG_BASE = "khoa-hoc";
    private static final int MAX_BULLETS_PER_GROUP = 20;
    private static final int MAX_BULLET_LENGTH = 500;
    private static final BigDecimal PRICE_MIN = BigDecimal.ZERO;
    private static final BigDecimal PRICE_MAX = new BigDecimal("10000000");

    private final CourseRepository courseRepository;
    private final CourseBulletRepository bulletRepository;
    private final CategoryService categoryService;
    private final CourseOwnershipGuard ownershipGuard;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final StorageKeyFactory keyFactory;
    private final EnrollmentRepository enrollmentRepository;
    private final StorageProperties storageProperties;

    // ---- Create -----------------------------------------------------------

    @Transactional
    public CourseResponse create(CreateCourseRequest request, Long userId) {
        User lecturer = userRepository.getReferenceById(userId);
        Category category = request.getCategoryId() != null
                ? categoryService.getByIdOrThrow(request.getCategoryId())
                : null;

        Course course = Course.builder()
                .lecturer(lecturer)
                .category(category)
                .title(request.getTitle().trim())
                .slug(generateUniqueSlug(request.getTitle()))
                .status(CourseStatus.DRAFT)
                .build();

        Course saved = courseRepository.save(course);
        log.info("Instructor [{}] created course [{}] (slug={})", userId, saved.getId(), saved.getSlug());
        return toDetail(saved);
    }

    // ---- Read -------------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<CourseSummaryResponse> listMine(Long userId, CourseStatus status,
                                                        String keyword, Pageable pageable) {
        String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        return PageResponse.from(
                courseRepository.search(userId, status, kw, pageable),
                course -> CourseMapper.toSummary(course, signThumbnail(course)));
    }

    @Transactional(readOnly = true)
    public CourseResponse getDetail(Long courseId, Long userId) {
        Course course = ownershipGuard.requireOwnedCourse(courseId, userId);
        return toDetail(course);
    }

    // ---- Update -----------------------------------------------------------

    @Transactional
    public CourseResponse update(Long courseId, UpdateCourseRequest request, Long userId) {
        Course course = ownershipGuard.requireEditableCourse(courseId, userId);
        checkOptimisticVersion(course, request.getVersion());

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            String newTitle = request.getTitle().trim();
            boolean titleChanged = !newTitle.equals(course.getTitle());
            course.setTitle(newTitle);
            // Slug tự cập nhật khi còn nháp (chưa từng publish); đóng băng sau lần publish đầu
            if (titleChanged && course.getPublishedAt() == null) {
                course.setSlug(generateUniqueSlug(newTitle));
            }
        }
        if (request.getSubtitle() != null) {
            course.setSubtitle(request.getSubtitle().trim());
        }
        if (request.getDescription() != null) {
            course.setDescription(request.getDescription());
        }
        if (request.getLevel() != null) {
            course.setLevel(request.getLevel());
        }
        if (request.getLanguage() != null && !request.getLanguage().isBlank()) {
            course.setLanguage(request.getLanguage().trim());
        }
        if (request.getCategoryId() != null) {
            course.setCategory(categoryService.getByIdOrThrow(request.getCategoryId()));
        }

        return toDetail(courseRepository.save(course));
    }

    @Transactional
    public CourseResponse updateThumbnail(Long courseId, UpdateThumbnailRequest request, Long userId) {
        Course course = ownershipGuard.requireEditableCourse(courseId, userId);
        String key = request.getStorageKey();

        // Key phải thuộc đúng course này (chống gắn key của course khác)
        if (!key.startsWith(keyFactory.coursePrefix(courseId) + "thumbnail/")) {
            throw new AppException(ErrorCode.UPLOAD_METADATA_MISMATCH,
                    "storageKey does not belong to this course thumbnail");
        }
        // Xác nhận object đã upload thật
        if (storageService.head(key).isEmpty()) {
            throw new AppException(ErrorCode.UPLOAD_OBJECT_NOT_FOUND);
        }

        String oldKey = course.getThumbnailKey();
        course.setThumbnailKey(key);
        Course saved = courseRepository.save(course);

        if (oldKey != null && !oldKey.equals(key)) {
            storageService.delete(oldKey);
        }
        return toDetail(saved);
    }

    // ---- Price & Bullets --------------------------------------------------

    @Transactional
    public CourseResponse updatePrice(Long courseId, UpdatePriceRequest request, Long userId) {
        Course course = ownershipGuard.requireEditableCourse(courseId, userId);
        checkOptimisticVersion(course, request.getVersion());

        BigDecimal price = request.getPrice().setScale(2, RoundingMode.HALF_UP);
        if (price.compareTo(PRICE_MIN) < 0 || price.compareTo(PRICE_MAX) > 0) {
            throw new AppException(ErrorCode.COURSE_PRICE_OUT_OF_RANGE);
        }
        course.setPrice(price);
        return toDetail(courseRepository.save(course));
    }

    @Transactional
    public CourseResponse updateBullets(Long courseId, UpdateBulletsRequest request, Long userId) {
        Course course = ownershipGuard.requireEditableCourse(courseId, userId);
        checkOptimisticVersion(course, request.getVersion());

        replaceBulletGroup(course, BulletType.LEARNING_OBJECTIVE, request.getLearningObjectives());
        replaceBulletGroup(course, BulletType.REQUIREMENT, request.getRequirements());
        replaceBulletGroup(course, BulletType.TARGET_AUDIENCE, request.getTargetAudiences());

        return toDetail(course);
    }

    private void replaceBulletGroup(Course course, BulletType type, List<String> rawLines) {
        List<String> cleaned = cleanBulletLines(type, rawLines);

        bulletRepository.deleteByCourseIdAndBulletType(course.getId(), type);

        List<CourseBullet> bullets = new ArrayList<>(cleaned.size());
        for (int i = 0; i < cleaned.size(); i++) {
            bullets.add(CourseBullet.builder()
                    .course(course)
                    .bulletType(type)
                    .content(cleaned.get(i))
                    .position(i)
                    .build());
        }
        bulletRepository.saveAll(bullets);
    }

    /** Trim, bỏ dòng rỗng, kiểm tối đa 20 dòng và mỗi dòng ≤ 500 ký tự. */
    private List<String> cleanBulletLines(BulletType type, List<String> rawLines) {
        if (rawLines == null) {
            return List.of();
        }
        List<String> cleaned = new ArrayList<>();
        for (String line : rawLines) {
            if (line == null) {
                continue;
            }
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.length() > MAX_BULLET_LENGTH) {
                throw new AppException(ErrorCode.VALIDATION_ERROR,
                        "Mỗi dòng của " + type + " tối đa " + MAX_BULLET_LENGTH + " ký tự");
            }
            cleaned.add(trimmed);
        }
        if (cleaned.size() > MAX_BULLETS_PER_GROUP) {
            throw new AppException(ErrorCode.VALIDATION_ERROR,
                    type + " tối đa " + MAX_BULLETS_PER_GROUP + " dòng");
        }
        return cleaned;
    }

    // ---- Delete -----------------------------------------------------------

    @Transactional
    public void delete(Long courseId, Long userId) {
        Course course = ownershipGuard.requireOwnedCourse(courseId, userId);
        if (course.getStatus() == CourseStatus.SUSPENDED) {
            throw new AppException(ErrorCode.COURSE_LOCKED_BY_ADMIN);
        }
        // BR-22: không xóa nếu đã có học viên đăng ký học
        if (enrollmentRepository.existsByCourse_Id(courseId)) {
            throw new AppException(ErrorCode.COURSE_HAS_ENROLLMENTS);
        }

        course.setDeletedAt(Instant.now());
        courseRepository.save(course);
        storageService.deleteByPrefix(keyFactory.coursePrefix(courseId));
        log.info("Instructor [{}] soft-deleted course [{}]", userId, courseId);
    }

    // ---- Helpers ----------------------------------------------------------

    private CourseResponse toDetail(Course course) {
        Map<BulletType, List<String>> grouped = new EnumMap<>(BulletType.class);
        for (BulletType type : BulletType.values()) {
            grouped.put(type, List.of());
        }
        List<CourseBullet> bullets = bulletRepository.findByCourse_IdOrderByBulletTypeAscPositionAsc(course.getId());
        for (BulletType type : BulletType.values()) {
            grouped.put(type, bullets.stream()
                    .filter(b -> b.getBulletType() == type)
                    .map(CourseBullet::getContent)
                    .toList());
        }
        return CourseMapper.toDetail(course, signThumbnail(course),
                grouped.get(BulletType.LEARNING_OBJECTIVE),
                grouped.get(BulletType.REQUIREMENT),
                grouped.get(BulletType.TARGET_AUDIENCE));
    }

    private String signThumbnail(Course course) {
        if (course.getThumbnailKey() == null) {
            return null;
        }
        return storageService.presignDownload(course.getThumbnailKey(), storageProperties.getDownloadTtl());
    }

    private void checkOptimisticVersion(Course course, Long expectedVersion) {
        if (expectedVersion != null && !expectedVersion.equals(course.getVersion())) {
            throw new AppException(ErrorCode.COURSE_MODIFIED_CONCURRENTLY);
        }
    }

    private String generateUniqueSlug(String title) {
        String base = SlugGenerator.toSlug(title);
        if (base.isEmpty()) {
            base = DEFAULT_SLUG_BASE;
        }
        String candidate = base;
        int suffix = 2;
        while (courseRepository.existsBySlugRaw(candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    @Transactional(readOnly = true)
    public PageResponse<CourseSummaryResponse> searchPublic(Long categoryId, CourseLevel level,
                                                            String keyword, Long excludeLecturerId, Pageable pageable) {
        String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        return PageResponse.from(
                courseRepository.searchPublic(categoryId, level, kw, excludeLecturerId, pageable),
                course -> CourseMapper.toSummary(course, signThumbnail(course)));
    }

    @Transactional(readOnly = true)
    public CourseResponse getPublicDetail(Long courseId) {
        Course course = courseRepository.findByIdAndStatus(courseId, CourseStatus.PUBLISHED)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));
        return toDetail(course);
    }

    @Transactional(readOnly = true)
    public CourseResponse getPublicDetailBySlug(String slug) {
        Course course = courseRepository.findBySlugAndStatus(slug, CourseStatus.PUBLISHED)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));
        return toDetail(course);
    }
}
