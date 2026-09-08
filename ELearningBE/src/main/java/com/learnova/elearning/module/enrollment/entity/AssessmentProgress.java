package com.learnova.elearning.module.enrollment.entity;

import com.learnova.elearning.module.course.entity.Assessment;
import com.learnova.elearning.module.enrollment.entity.enums.AssessmentCompletionSource;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "assessment_progress",
        uniqueConstraints = @UniqueConstraint(name = "uq_assessment_progress_enrollment_assessment",
                columnNames = {"enrollment_id", "assessment_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private Enrollment enrollment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessment_id", nullable = false)
    private Assessment assessment;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "completion_source", nullable = false, length = 20)
    @Builder.Default
    private AssessmentCompletionSource completionSource = AssessmentCompletionSource.QUIZ;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
