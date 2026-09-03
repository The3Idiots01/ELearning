package com.learnova.elearning.module.payment.controller;

import com.learnova.elearning.common.dto.ApiResponse;
import com.learnova.elearning.module.payment.dto.CheckoutResponse;
import com.learnova.elearning.module.payment.dto.CreateCheckoutRequest;
import com.learnova.elearning.module.payment.dto.PaymentStatusResponse;
import com.learnova.elearning.module.payment.service.PaymentService;
import com.learnova.elearning.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.payos.type.Webhook;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<CheckoutResponse>> createCheckout(
            @Valid @RequestBody CreateCheckoutRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        CheckoutResponse response = paymentService.createCheckout(request.getCourseId(), userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Boolean>> handleWebhook(@RequestBody Webhook webhook) {
        paymentService.processWebhook(webhook);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/{orderCode}/status")
    public ResponseEntity<ApiResponse<PaymentStatusResponse>> getPaymentStatus(@PathVariable Long orderCode) {
        PaymentStatusResponse response = paymentService.getPaymentStatus(orderCode);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
