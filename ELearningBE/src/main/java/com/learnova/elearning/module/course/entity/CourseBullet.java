package com.learnova.elearning.module.course.entity;

import com.learnova.elearning.module.course.entity.enums.BulletType;
import jakarta.persistence.*;
import lombok.*;

/**
 * Một dòng mô tả trên landing page (learning objective / requirement / target audience).
 * Ghi đè theo nhóm khi instructor lưu form; position = thứ tự hiển thị trong nhóm.
 */
@Entity
@Table(name = "course_bullets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseBullet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Enumerated(EnumType.STRING)
    @Column(name = "bullet_type", nullable = false, length = 30)
    private BulletType bulletType;

    @Column(name = "content", nullable = false, length = 500)
    private String content;

    @Column(name = "position", nullable = false)
    @Builder.Default
    private Integer position = 0;
}
