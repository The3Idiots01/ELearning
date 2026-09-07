package com.learnova.elearning.module.delivery.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ghi {@code content_access_logs} — §4.x design_us15_us17.md. Không phải một
 * {@code JpaRepository} vì cột {@code client_ip} là kiểu {@code inet} của
 * PostgreSQL, không có ánh xạ JPA chuẩn; native insert với {@code CAST(... AS inet)}
 * đơn giản hơn nhiều so với viết một Hibernate UserType riêng cho một bảng
 * ghi-only.
 */
@Repository
public class ContentAccessLogRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void insert(Long userId, Long lessonId, String ticketJti, String scope, String outcome,
                        String clientIp, String userAgent) {
        entityManager.createNativeQuery("""
                        INSERT INTO content_access_logs
                            (user_id, lesson_id, ticket_jti, scope, outcome, client_ip, user_agent, created_at)
                        VALUES
                            (:userId, :lessonId, :ticketJti, :scope, :outcome, CAST(:clientIp AS inet), :userAgent, now())
                        """)
                .setParameter("userId", userId)
                .setParameter("lessonId", lessonId)
                .setParameter("ticketJti", ticketJti)
                .setParameter("scope", scope)
                .setParameter("outcome", outcome)
                .setParameter("clientIp", clientIp)
                .setParameter("userAgent", truncate(userAgent, 255))
                .executeUpdate();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
