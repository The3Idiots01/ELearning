package com.learnova.elearning.module.revenue.controller;

import com.learnova.elearning.common.dto.ApiResponse;
import com.learnova.elearning.module.revenue.dto.InstructorRevenueResponse;
import com.learnova.elearning.module.revenue.service.RevenueService;
import com.learnova.elearning.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lecturer/revenue")
@RequiredArgsConstructor
public class RevenueController {

    private final RevenueService revenueService;

    @GetMapping
    public ResponseEntity<ApiResponse<InstructorRevenueResponse>> getRevenue(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        InstructorRevenueResponse response = revenueService.getRevenue(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
