package com.learnova.elearning.module.auth.controller;

import com.learnova.elearning.module.auth.model.ConfirmResult;
import com.learnova.elearning.module.auth.service.RegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Render trang HTML kết quả xác nhận tài khoản.
 * Đây là tầng trình bày: gọi service lấy kết quả nghiệp vụ (ConfirmResult) rồi
 * để Thymeleaf view resolver render template tương ứng — service không sinh HTML.
 */
@Controller
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AccountConfirmationController {

    private final RegistrationService registrationService;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @GetMapping("/confirm-account")
    public String confirmAccount(
            @RequestParam("token") String token,
            HttpServletRequest httpRequest,
            Model model
    ) {
        ConfirmResult result = registrationService.confirmAccount(token, httpRequest);

        if (result.success()) {
            model.addAttribute("fullName", result.displayName());
            model.addAttribute("loginUrl", frontendUrl + "/login");
            return "confirm-success";
        }

        String retryPath = result.target() == ConfirmResult.RedirectTarget.LOGIN ? "/login" : "/register";
        model.addAttribute("errorMessage", result.message());
        model.addAttribute("retryUrl", frontendUrl + retryPath);
        return "confirm-failure";
    }
}
