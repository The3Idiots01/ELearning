package com.learnova.elearning.module.review.moderation;

import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentModerationService {

    private final ProfanityFilterService profanityFilterService;
    private final AiContentModerationService aiContentModerationService;

    /**
     * Xác thực nội dung review (mặc định ném REVIEW_CONTENT_VIOLATION):
     */
    public void validateContent(String text) {
        validateContent(text, ErrorCode.REVIEW_CONTENT_VIOLATION);
    }

    /**
     * Xác thực nội dung với mã lỗi tùy chọn:
     * - Bước 1: Lọc nhanh từ cấm, regex link/sđt qua Aho-Corasick.
     * - Bước 2: Kiểm tra ngữ cảnh nâng cao bằng AI (Gemini).
     *
     * @throws AppException nếu vi phạm
     */
    public void validateContent(String text, ErrorCode violationErrorCode) {
        if (text == null || text.isBlank()) {
            return;
        }

        ErrorCode code = (violationErrorCode != null) ? violationErrorCode : ErrorCode.REVIEW_CONTENT_VIOLATION;

        // Lớp 1: Offline check
        ProfanityFilterService.ModerationCheckResult offlineResult = profanityFilterService.check(text);
        if (!offlineResult.allowed()) {
            log.warn("Content blocked by ProfanityFilterService: {}", offlineResult.reason());
            throw new AppException(code, offlineResult.reason());
        }

        // Lớp 2: AI check
        ProfanityFilterService.ModerationCheckResult aiResult = aiContentModerationService.check(text);
        if (!aiResult.allowed()) {
            log.warn("Content blocked by AiContentModerationService: {}", aiResult.reason());
            String reason = (aiResult.reason() != null && !aiResult.reason().isBlank())
                    ? aiResult.reason()
                    : code.getMessage();
            throw new AppException(code, reason);
        }
    }

    /**
     * Xác thực nội dung câu hỏi Q&A (cả tiêu đề và nội dung)
     */
    public void validateQaQuestion(String title, String content) {
        validateContent(title, ErrorCode.QA_CONTENT_VIOLATION);
        validateContent(content, ErrorCode.QA_CONTENT_VIOLATION);
    }

    /**
     * Xác thực nội dung câu trả lời Q&A
     */
    public void validateQaAnswer(String content) {
        validateContent(content, ErrorCode.QA_CONTENT_VIOLATION);
    }
}
