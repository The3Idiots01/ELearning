package com.learnova.elearning.module.course.repository;

import com.learnova.elearning.module.course.entity.Lesson;
import com.learnova.elearning.module.course.entity.enums.LessonContentType;
import com.learnova.elearning.module.course.entity.enums.LessonUploadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {

    Optional<Lesson> findByIdAndSection_Id(Long id, Long sectionId);

    /** Kiểm lesson thuộc đúng course qua chuỗi lesson -> section -> course (chống IDOR). */
    Optional<Lesson> findByIdAndSection_Course_Id(Long id, Long courseId);

    List<Lesson> findBySection_IdOrderByPositionAsc(Long sectionId);

    /** Toàn bộ lesson (chưa xóa) của một course — dùng cho publish-check. */
    List<Lesson> findBySection_Course_Id(Long courseId);

    /** Lấy lesson của nhiều section trong 1 query để ghép cây curriculum (tránh N+1). */
    List<Lesson> findBySection_IdInOrderByPositionAsc(Collection<Long> sectionIds);

    long countBySection_Id(Long sectionId);

    boolean existsBySection_Course_IdAndContentTypeAndUploadStatus(
            Long courseId, LessonContentType contentType, LessonUploadStatus uploadStatus);

    boolean existsBySection_Course_IdAndUploadStatusIn(
            Long courseId, Collection<LessonUploadStatus> uploadStatuses);
}
