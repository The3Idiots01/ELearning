package com.learnova.elearning.module.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Dựng nội dung HTML cho email của luồng đăng ký (email-only).
 * Việc render các trang web kết quả xác nhận đã chuyển sang tầng controller/view.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailTemplateService {

    private final TemplateEngine templateEngine;
    /**
     * Tạo nội dung HTML Email kích hoạt tài khoản sử dụng Thymeleaf
     */
    public String buildAccountActivationEmail(String fullName, String email, String confirmationLink) {
        Context context = new Context();
        context.setVariable("fullName", (fullName != null && !fullName.isBlank()) ? fullName : "Học viên");
        context.setVariable("email", email != null ? email : "");
        context.setVariable("confirmationLink", confirmationLink);

        return templateEngine.process("activation-email", context);
    }
}
