package com.learnova.elearning.module.course.repository;

import com.learnova.elearning.module.course.entity.Lesson;
import com.learnova.elearning.module.course.entity.enums.LessonContentType;
import com.learnova.elearning.module.course.entity.enums.LessonUploadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Lesson l WHERE l.id=:lessonId AND l.section.course.id=:courseId")
    Optional<Lesson> findForMetadataUpdate(@Param("lessonId") Long lessonId, @Param("courseId") Long courseId);


    Optional<Lesson> findByIdAndSection_Id(Long id, Long sectionId);

    /** Kiểm lesson thuộc đúng course qua chuỗi lesson -> section -> course (chống IDOR). */
    Optional<Lesson> findByIdAndSection_Course_Id(Long id, Long courseId);

    List<Lesson> findBySection_IdOrderByPositionAsc(Long sectionId);

    /** Toàn bộ lesson (chưa xóa) của một course — dùng cho publish-check. */
    List<Lesson> findBySection_Course_Id(Long courseId);

    /** Lấy lesson của nhiều section trong 1 query để ghép cây curriculum (tránh N+1). */
    List<Lesson> findBySection_IdInOrderByPositionAsc(Collection<Long> sectionIds);

    long countBySection_Id(Long sectionId);

    long countBySection_Course_Id(Long courseId);

    boolean existsBySection_Course_IdAndContentTypeAndUploadStatus(
            Long courseId, LessonContentType contentType, LessonUploadStatus uploadStatus);

    boolean existsBySection_Course_IdAndUploadStatusIn(
            Long courseId, Collection<LessonUploadStatus> uploadStatuses);

    /** Lesson VIDEO đã READY — dùng cho job backfill duration (§7.5, Task 0c). */
    List<Lesson> findByContentTypeAndUploadStatus(LessonContentType contentType, LessonUploadStatus uploadStatus);

    /** Job nền vừa đo được duration thật -> chuyển PROCESSING sang READY (§7.5). */
    @Modifying
    @Query("UPDATE Lesson l SET l.uploadStatus = com.learnova.elearning.module.course.entity.enums.LessonUploadStatus.READY, "
            + "l.durationSeconds = :durationSeconds WHERE l.id = :lessonId")
    int markReady(@Param("lessonId") Long lessonId, @Param("durationSeconds") int durationSeconds);

    /** Job nền không đọc được duration -> FAILED, instructor phải tải lại (§7.5). */
    @Modifying
    @Query("UPDATE Lesson l SET l.uploadStatus = com.learnova.elearning.module.course.entity.enums.LessonUploadStatus.FAILED "
            + "WHERE l.id = :lessonId")
    int markFailed(@Param("lessonId") Long lessonId);
}
