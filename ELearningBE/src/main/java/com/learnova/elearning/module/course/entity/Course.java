package com.learnova.elearning.module.course.entity;

import com.learnova.elearning.module.category.entity.Category;
import com.learnova.elearning.module.course.entity.enums.CourseLevel;
import com.learnova.elearning.module.course.entity.enums.CourseStatus;
import com.learnova.elearning.module.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Khóa học do giảng viên tạo. Gốc của nhánh nội dung (sections/lessons) và
 * nhánh giao dịch. Soft delete qua deleted_at; @Version chống ghi đè khi 2 tab
 * cùng sửa.
 */
@Entity
@Table(name = "courses")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lecturer_id", nullable = false)
    private User lecturer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "subtitle", length = 255)
    private String subtitle;

    @Column(name = "slug", nullable = false, unique = true, length = 280)
    private String slug;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "thumbnail_key", length = 500)
    private String thumbnailKey;

    @Column(name = "promo_video_key", length = 500)
    private String promoVideoKey;

    @Column(name = "language", nullable = false, length = 20)
    @Builder.Default
    private String language = "vi";

    @Enumerated(EnumType.STRING)
    @Column(name = "level", length = 20)
    private CourseLevel level;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private CourseStatus status = CourseStatus.DRAFT;

    @Column(name = "rating_avg", nullable = false, precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal ratingAvg = BigDecimal.ZERO;

    @Column(name = "total_students", nullable = false)
    @Builder.Default
    private Integer totalStudents = 0;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
