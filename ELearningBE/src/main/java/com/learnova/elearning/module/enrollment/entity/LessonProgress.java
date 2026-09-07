package com.learnova.elearning.module.enrollment.entity;

import com.learnova.elearning.module.course.entity.Lesson;
import com.learnova.elearning.module.enrollment.entity.enums.CompletionSource;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Tiến độ xem của một learner cho một lesson — US-17 Knowledge Tracking (§5.3
 * design_us15_us17.md). Sự tồn tại của dòng chỉ còn nghĩa "đã bắt đầu xem";
 * hoàn thành là {@code completedAt != null} (một chiều — §5.7).
 * <p>
 * Cột {@code watched_ranges} ({@code int4multirange}) KHÔNG map qua entity —
 * Hibernate không có kiểu chuẩn cho multirange của PostgreSQL. Merge nguyên tử
 * bằng câu SQL native dùng phép hợp {@code +} trong {@code WatchTrackingService}
 * (§5.5, Task 7); {@link #watchedSeconds} / {@link #coveragePercent} là bản vật
 * chất hoá, đọc được bình thường qua entity.
 */
@Entity
@Table(name = "lesson_progress")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private Enrollment enrollment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    /** Vị trí xem hiện tại — resume (FR-KT-01). Last-write-wins giữa nhiều tab. */
    @Column(name = "last_position_seconds", nullable = false)
    @Builder.Default
    private Integer lastPositionSeconds = 0;

    /** Vị trí xa nhất từng tới — gác điều kiện hoàn thành, KHÔNG dùng coverage (§5.1). */
    @Column(name = "max_position_seconds", nullable = false)
    @Builder.Default
    private Integer maxPositionSeconds = 0;

    /** Tổng giây coverage — vật chất hoá từ watched_ranges, chỉ để analytics (§5.1). */
    @Column(name = "watched_seconds", nullable = false)
    @Builder.Default
    private Integer watchedSeconds = 0;

    /** % coverage — vật chất hoá, KHÔNG gác completion (§5.1). */
    @Column(name = "coverage_percent", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal coveragePercent = BigDecimal.ZERO;

    /** Một chiều — set đúng một lần khi hoàn thành, không có nhánh nào xoá (§5.7). */
    @Column(name = "completed_at")
    private Instant completedAt;

    /** Đi cùng completedAt theo cặp (CHECK ck_lesson_progress_completion_pair). */
    @Enumerated(EnumType.STRING)
    @Column(name = "completion_source", length = 20)
    private CompletionSource completionSource;

    @CreationTimestamp
    @Column(name = "first_started_at", nullable = false, updatable = false)
    private Instant firstStartedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
