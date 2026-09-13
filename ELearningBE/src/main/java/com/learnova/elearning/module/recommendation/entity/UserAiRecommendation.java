package com.learnova.elearning.module.recommendation.entity;

import com.learnova.elearning.module.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_ai_recommendations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAiRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "goal", columnDefinition = "TEXT")
    private String goal;

    @Column(name = "preferred_level", length = 30)
    private String preferredLevel;

    @Column(name = "interests", length = 500)
    private String interests;

    @Column(name = "advice_summary", columnDefinition = "TEXT")
    private String adviceSummary;

    @OneToMany(mappedBy = "recommendation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("rankOrder ASC")
    @Builder.Default
    private List<UserAiRecommendationItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void addItem(UserAiRecommendationItem item) {
        items.add(item);
        item.setRecommendation(this);
    }

    public void removeItem(UserAiRecommendationItem item) {
        items.remove(item);
        item.setRecommendation(null);
    }
}
