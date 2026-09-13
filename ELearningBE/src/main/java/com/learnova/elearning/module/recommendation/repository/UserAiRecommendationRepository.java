package com.learnova.elearning.module.recommendation.repository;

import com.learnova.elearning.module.recommendation.entity.UserAiRecommendation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAiRecommendationRepository extends JpaRepository<UserAiRecommendation, Long> {

    @EntityGraph(attributePaths = {"items", "items.course", "items.course.category", "items.course.lecturer"})
    Optional<UserAiRecommendation> findByUserId(Long userId);

    @Modifying
    @Query("DELETE FROM UserAiRecommendation r WHERE r.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
