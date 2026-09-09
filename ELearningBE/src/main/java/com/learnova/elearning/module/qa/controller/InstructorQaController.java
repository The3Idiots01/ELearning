package com.learnova.elearning.module.qa.controller;

import com.learnova.elearning.common.dto.ApiResponse;
import com.learnova.elearning.module.qa.dto.response.CourseQuestionResponse;
import com.learnova.elearning.module.qa.service.QaService;
import com.learnova.elearning.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/lecturer/courses/{courseId}/questions")
@RequiredArgsConstructor
public class InstructorQaController {

    private final QaService qaService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CourseQuestionResponse>>> getInstructorQuestions(
            @PathVariable Long courseId,
            @RequestParam(required = false, defaultValue = "all") String filter,
            @AuthenticationPrincipal CustomUserDetails lecturer
    ) {
        List<CourseQuestionResponse> responses = qaService.getInstructorCourseQuestions(courseId, filter, lecturer.getId());
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách câu hỏi quản trị thành công", responses));
    }
}
