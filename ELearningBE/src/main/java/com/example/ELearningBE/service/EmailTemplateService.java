package com.example.ELearningBE.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

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

    /**
     * Tạo giao diện Web thông báo kích hoạt tài khoản THÀNH CÔNG sử dụng Thymeleaf
     */
    public String buildConfirmationSuccessPage(String fullName, String loginUrl) {
        Context context = new Context();
        context.setVariable("fullName", (fullName != null && !fullName.isBlank()) ? fullName : "Bạn");
        context.setVariable("loginUrl", loginUrl);

        return templateEngine.process("confirm-success", context);
    }

    /**
     * Tạo giao diện Web thông báo kích hoạt tài khoản THẤT BẠI / HẾT HẠN sử dụng Thymeleaf
     */
    public String buildConfirmationFailurePage(String errorMessage, String retryUrl) {
        Context context = new Context();
        context.setVariable("errorMessage", (errorMessage != null && !errorMessage.isBlank())
                ? errorMessage
                : "Liên kết xác nhận không hợp lệ hoặc đã hết hạn.");
        context.setVariable("retryUrl", retryUrl);

        return templateEngine.process("confirm-failure", context);
    }
}
