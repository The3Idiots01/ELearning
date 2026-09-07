package com.learnova.elearning.module.delivery.service;

/**
 * Kết quả quyết định quyền truy cập nội dung — §4.3 design_us15_us17.md.
 * {@code ContentAccessGuard} chỉ trả về khi {@code allowed = true}; mọi trường
 * hợp bị chặn được ném thẳng dưới dạng {@code AppException} tương ứng.
 */
public record AccessDecision(boolean allowed, AccessScope scope, Long enrollmentId) {
}
