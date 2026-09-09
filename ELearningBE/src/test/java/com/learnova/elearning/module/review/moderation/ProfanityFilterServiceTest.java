package com.learnova.elearning.module.review.moderation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProfanityFilterServiceTest {

    private ProfanityFilterService filterService;

    @BeforeEach
    void setUp() {
        filterService = new ProfanityFilterService();
    }

    @Test
    @DisplayName("Nội dung sạch, khen ngợi khóa học => Phải được pass")
    void testCleanContent() {
        ProfanityFilterService.ModerationCheckResult result = filterService.check(
                "Khóa học rất hay và dễ hiểu, giảng viên nhiệt tình hỗ trợ thực hành!"
        );
        assertTrue(result.allowed());
        assertNull(result.reason());
    }

    @Test
    @DisplayName("Từ thô tục tiếng Việt trực tiếp => Phải bị chặn")
    void testDirectProfanity() {
        ProfanityFilterService.ModerationCheckResult result = filterService.check(
                "Dạy như lồn, phí tiền mua"
        );
        assertFalse(result.allowed());
        assertNotNull(result.reason());
    }

    @Test
    @DisplayName("Lách từ bằng dấu chấm hoặc ký tự xen kẽ (d.m, d_m, vcl) => Phải bị chặn")
    void testEvasionProfanity() {
        ProfanityFilterService.ModerationCheckResult result1 = filterService.check("Khóa học như d.m lừa đảo");
        assertFalse(result1.allowed());

        ProfanityFilterService.ModerationCheckResult result2 = filterService.check("Bực mình vcl");
        assertFalse(result2.allowed());
    }

    @Test
    @DisplayName("Nội dung chat sex, gạ gẫm 18+ => Phải bị chặn")
    void testSexualContent() {
        ProfanityFilterService.ModerationCheckResult result = filterService.check(
                "Ai muốn chat sex gái gọi liên hệ mình nhé"
        );
        assertFalse(result.allowed());
    }

    @Test
    @DisplayName("Nội dung quấy rối tình dục, nhận xét khiếm nhã giảng viên (nhìn ngon quá) => Phải bị chặn")
    void testSexualHarassment() {
        ProfanityFilterService.ModerationCheckResult result = filterService.check(
                "giảng viên nhìn ngon quá"
        );
        assertFalse(result.allowed());
    }

    @Test
    @DisplayName("Spam link Telegram hoặc URL cờ bạc => Phải bị chặn")
    void testSpamLink() {
        ProfanityFilterService.ModerationCheckResult result = filterService.check(
                "Vào link này nhận tài liệu miễn phí https://t.me/hackcourse"
        );
        assertFalse(result.allowed());
    }

    @Test
    @DisplayName("Spam số điện thoại => Phải bị chặn")
    void testSpamPhone() {
        ProfanityFilterService.ModerationCheckResult result = filterService.check(
                "Kết bạn Zalo 0987654321 để mua tài liệu giá rẻ"
        );
        assertFalse(result.allowed());
    }

    @Test
    @DisplayName("Không bị bắt nhầm từ hợp lệ có chứa chuỗi con (False Positive test)")
    void testFalsePositive() {
        ProfanityFilterService.ModerationCheckResult result = filterService.check(
                "Admin khóa học hỗ trợ rất nhiệt tình, bài học rất bổ ích!"
        );
        assertTrue(result.allowed());
    }
}
