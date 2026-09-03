package com.learnova.elearning.module.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import com.learnova.elearning.module.course.entity.Course;
import com.learnova.elearning.module.course.entity.enums.CourseStatus;
import com.learnova.elearning.module.course.repository.CourseRepository;
import com.learnova.elearning.module.enrollment.repository.EnrollmentRepository;
import com.learnova.elearning.module.enrollment.service.EnrollmentService;
import com.learnova.elearning.module.payment.dto.CheckoutResponse;
import com.learnova.elearning.module.payment.dto.PaymentStatusResponse;
import com.learnova.elearning.module.payment.entity.PaymentOrder;
import com.learnova.elearning.module.payment.entity.enums.PaymentStatus;
import com.learnova.elearning.module.payment.repository.PaymentOrderRepository;
import com.learnova.elearning.module.user.entity.User;
import com.learnova.elearning.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.PayOS;
import vn.payos.type.CheckoutResponseData;
import vn.payos.type.Webhook;
import vn.payos.type.WebhookData;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private final PaymentOrderRepository paymentOrderRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentService enrollmentService;
    private final PayOS payOS;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${payos.client-id:}")
    private String clientId;

    @Value("${payos.api-key:}")
    private String apiKey;

    @Value("${payos.checksum-key:}")
    private String checksumKey;

    private String getClientId() {
        if (clientId != null && !clientId.isBlank()) return clientId.trim();
        return Optional.ofNullable(System.getenv("PAYOS_CLIENT_ID")).orElse("").trim();
    }

    private String getApiKey() {
        if (apiKey != null && !apiKey.isBlank()) return apiKey.trim();
        return Optional.ofNullable(System.getenv("PAYOS_API_KEY")).orElse("").trim();
    }

    private String getChecksumKey() {
        if (checksumKey != null && !checksumKey.isBlank()) return checksumKey.trim();
        return Optional.ofNullable(System.getenv("PAYOS_CHECKSUM_KEY")).orElse("").trim();
    }

    @Transactional
    public CheckoutResponse createCheckout(Long courseId, Long studentId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));

        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new AppException(ErrorCode.COURSE_NOT_PUBLISHED);
        }

        if (course.getLecturer().getId().equals(studentId)) {
            throw new AppException(ErrorCode.LECTURER_CANNOT_ENROLL_OWN_COURSE);
        }

        if (enrollmentRepository.existsByStudent_IdAndCourse_Id(studentId, courseId)) {
            return CheckoutResponse.builder()
                    .courseId(courseId)
                    .amount(course.getPrice())
                    .status(PaymentStatus.PAID.name())
                    .isFree(course.getPrice() == null || course.getPrice().compareTo(BigDecimal.ZERO) <= 0)
                    .isEnrolled(true)
                    .build();
        }

        // Branch 1: Free course (Price <= 0 or null)
        if (course.getPrice() == null || course.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            safeEnroll(courseId, studentId);
            return CheckoutResponse.builder()
                    .courseId(courseId)
                    .amount(BigDecimal.ZERO)
                    .status(PaymentStatus.PAID.name())
                    .isFree(true)
                    .isEnrolled(true)
                    .build();
        }

        // Branch 2: Paid course -> Check existing pending order
        Optional<PaymentOrder> existingOrder = paymentOrderRepository
                .findByStudent_IdAndCourse_IdAndStatus(studentId, courseId, PaymentStatus.PENDING);
        if (existingOrder.isPresent()) {
            PaymentOrder pending = existingOrder.get();
            // Re-use pending order if created within the last 15 minutes
            if (pending.getCreatedAt() != null && pending.getCreatedAt().isAfter(Instant.now().minus(15, ChronoUnit.MINUTES))
                    && pending.getCheckoutUrl() != null) {
                return CheckoutResponse.builder()
                        .orderCode(pending.getOrderCode())
                        .courseId(courseId)
                        .checkoutUrl(pending.getCheckoutUrl())
                        .amount(pending.getAmount())
                        .status(PaymentStatus.PENDING.name())
                        .isFree(false)
                        .isEnrolled(false)
                        .build();
            } else {
                // Link is older than 15 minutes, mark as CANCELLED and create new link
                pending.setStatus(PaymentStatus.CANCELLED);
                paymentOrderRepository.save(pending);
            }
        }

        // Generate unique orderCode
        long orderCode;
        do {
            orderCode = System.currentTimeMillis() % 1_000_000_000L * 1000 + (long) (Math.random() * 900 + 100);
        } while (paymentOrderRepository.existsByOrderCode(orderCode));

        int priceInt = course.getPrice().intValue();
        String rawDesc = "Learnova " + courseId;
        String description = rawDesc.substring(0, Math.min(25, rawDesc.length()));

        String returnUrl = frontendUrl + "/payment-result?orderCode=" + orderCode + "&status=PAID&courseId=" + courseId;
        String cancelUrl = frontendUrl + "/payment-result?orderCode=" + orderCode + "&status=CANCELLED&courseId=" + courseId;

        try {
            CheckoutResponseData payosResponse = createPayOSPaymentLink(orderCode, priceInt, description, returnUrl, cancelUrl);

            PaymentOrder order = PaymentOrder.builder()
                    .orderCode(orderCode)
                    .student(student)
                    .course(course)
                    .amount(course.getPrice())
                    .status(PaymentStatus.PENDING)
                    .paymentLinkId(payosResponse.getPaymentLinkId())
                    .checkoutUrl(payosResponse.getCheckoutUrl())
                    .build();

            paymentOrderRepository.save(order);

            return CheckoutResponse.builder()
                    .orderCode(orderCode)
                    .courseId(courseId)
                    .checkoutUrl(payosResponse.getCheckoutUrl())
                    .amount(course.getPrice())
                    .status(PaymentStatus.PENDING.name())
                    .isFree(false)
                    .isEnrolled(false)
                    .build();
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to create PayOS payment link for courseId: {}", courseId, e);
            throw new AppException(ErrorCode.PAYMENT_GATEWAY_ERROR, "Lỗi khi kết nối đến cổng thanh toán PayOS: " + e.getMessage());
        }
    }

    private CheckoutResponseData createPayOSPaymentLink(long orderCode, int amount, String description, String returnUrl, String cancelUrl) throws Exception {
        String sigData = String.format("amount=%d&cancelUrl=%s&description=%s&orderCode=%d&returnUrl=%s",
                amount, cancelUrl, description, orderCode, returnUrl);
        String requestSignature = hmacSha256(getChecksumKey(), sigData);

        ObjectNode body = objectMapper.createObjectNode();
        body.put("orderCode", orderCode);
        body.put("amount", amount);
        body.put("description", description);
        body.put("cancelUrl", cancelUrl);
        body.put("returnUrl", returnUrl);
        body.put("signature", requestSignature);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api-merchant.payos.vn/v2/payment-requests"))
                .header("Content-Type", "application/json")
                .header("x-client-id", getClientId())
                .header("x-api-key", getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = objectMapper.readTree(response.body());

        String code = root.path("code").asText();
        if (!"00".equals(code)) {
            String desc = root.path("desc").asText("Lỗi từ PayOS");
            log.error("PayOS API create error: code={}, desc={}, fullResponse={}", code, desc, response.body());
            throw new AppException(ErrorCode.PAYMENT_GATEWAY_ERROR, desc);
        }

        JsonNode dataNode = root.get("data");
        return objectMapper.treeToValue(dataNode, CheckoutResponseData.class);
    }

    @Transactional
    public void processWebhook(Webhook webhook) {
        if (webhook == null || webhook.getData() == null) {
            log.info("PayOS webhook payload or data is null");
            return;
        }

        WebhookData data = webhook.getData();
        if (data.getOrderCode() == null || data.getOrderCode() == 0) {
            log.info("PayOS test webhook verification received (orderCode is null/0)");
            return;
        }

        // Verify webhook signature using PayOS SDK
        try {
            payOS.verifyPaymentWebhookData(webhook);
        } catch (Exception e) {
            log.error("PayOS Webhook signature verification failed for orderCode {}: {}", data.getOrderCode(), e.getMessage());
            throw new AppException(ErrorCode.INVALID_PAYMENT_SIGNATURE, "Chữ ký webhook không hợp lệ!");
        }

        Long orderCode = data.getOrderCode();
        log.info("Processing PayOS Webhook for orderCode: {}, code: {}", orderCode, webhook.getCode());

        Optional<PaymentOrder> orderOpt = paymentOrderRepository.findByOrderCode(orderCode);
        if (orderOpt.isEmpty()) {
            log.warn("Payment order not found for orderCode: {}", orderCode);
            return;
        }

        PaymentOrder order = orderOpt.get();
        if ("00".equals(webhook.getCode())) {
            if (order.getStatus() == PaymentStatus.PENDING) {
                order.setStatus(PaymentStatus.PAID);
                paymentOrderRepository.save(order);

                // Execute Enrollment safely
                safeEnroll(order.getCourse().getId(), order.getStudent().getId());
                log.info("Successfully enrolled student {} in course {} after PayOS payment",
                        order.getStudent().getId(), order.getCourse().getId());
            }
        } else {
            order.setStatus(PaymentStatus.CANCELLED);
            paymentOrderRepository.save(order);
        }
    }

    @Transactional
    public PaymentStatusResponse getPaymentStatus(Long orderCode) {
        PaymentOrder order = paymentOrderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        Long targetStudentId = order.getStudent().getId();

        // Active fallback sync if still PENDING
        if (order.getStatus() == PaymentStatus.PENDING) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api-merchant.payos.vn/v2/payment-requests/" + orderCode))
                        .header("x-client-id", getClientId())
                        .header("x-api-key", getApiKey())
                        .GET()
                        .build();

                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                JsonNode root = objectMapper.readTree(response.body());

                if ("00".equals(root.path("code").asText())) {
                    String onlineStatus = root.path("data").path("status").asText();
                    if ("PAID".equalsIgnoreCase(onlineStatus)) {
                        order.setStatus(PaymentStatus.PAID);
                        paymentOrderRepository.save(order);

                        safeEnroll(order.getCourse().getId(), targetStudentId);
                    } else if ("CANCELLED".equalsIgnoreCase(onlineStatus)) {
                        order.setStatus(PaymentStatus.CANCELLED);
                        paymentOrderRepository.save(order);
                    }
                }
            } catch (Exception e) {
                log.warn("Unable to fetch PayOS status online for orderCode: {}. Using local status.", orderCode, e);
            }
        }

        boolean enrolled = enrollmentRepository.existsByStudent_IdAndCourse_Id(targetStudentId, order.getCourse().getId());

        return PaymentStatusResponse.builder()
                .orderCode(order.getOrderCode())
                .courseId(order.getCourse().getId())
                .courseTitle(order.getCourse().getTitle())
                .amount(order.getAmount())
                .status(order.getStatus().name())
                .enrolled(enrolled)
                .build();
    }

    private void safeEnroll(Long courseId, Long studentId) {
        try {
            if (!enrollmentRepository.existsByStudent_IdAndCourse_Id(studentId, courseId)) {
                enrollmentService.enroll(courseId, studentId);
            } else {
                log.info("Student {} already enrolled in course {}", studentId, courseId);
            }
        } catch (AppException e) {
            if (e.getErrorCode() == ErrorCode.ALREADY_ENROLLED) {
                log.info("Student {} already enrolled in course {}", studentId, courseId);
            } else {
                log.error("Failed to enroll student {} in course {}: {}", studentId, courseId, e.getMessage());
            }
        } catch (Exception e) {
            log.error("Unexpected error during safeEnroll for student {} in course {}: {}", studentId, courseId, e.getMessage(), e);
        }
    }

    private String hmacSha256(String key, String data) throws Exception {
        Mac hmacSha256 = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        hmacSha256.init(secretKey);
        byte[] hash = hmacSha256.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
