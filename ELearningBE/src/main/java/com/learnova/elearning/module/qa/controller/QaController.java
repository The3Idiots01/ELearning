package com.learnova.elearning.module.qa.controller;

import com.learnova.elearning.common.dto.ApiResponse;
import com.learnova.elearning.module.qa.dto.request.CreateAnswerRequest;
import com.learnova.elearning.module.qa.dto.request.CreateQuestionRequest;
import com.learnova.elearning.module.qa.dto.response.CourseAnswerResponse;
import com.learnova.elearning.module.qa.dto.response.CourseQuestionResponse;
import com.learnova.elearning.module.qa.service.QaService;
import com.learnova.elearning.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/courses/{courseId}")
@RequiredArgsConstructor
public class QaController {

    private final QaService qaService;

    @GetMapping("/lessons/{lessonId}/questions")
    public ResponseEntity<ApiResponse<List<CourseQuestionResponse>>> getLessonQuestions(
            @PathVariable Long courseId,
            @PathVariable Long lessonId
    ) {
        List<CourseQuestionResponse> responses = qaService.getQuestionsByLesson(courseId, lessonId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách câu hỏi của bài học thành công", responses));
    }

    @GetMapping("/questions")
    public ResponseEntity<ApiResponse<List<CourseQuestionResponse>>> getCourseQuestions(
            @PathVariable Long courseId
    ) {
        List<CourseQuestionResponse> responses = qaService.getQuestionsByCourse(courseId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách câu hỏi của khóa học thành công", responses));
    }

    @PostMapping("/lessons/{lessonId}/questions")
    public ResponseEntity<ApiResponse<CourseQuestionResponse>> createQuestion(
            @PathVariable Long courseId,
            @PathVariable Long lessonId,
            @Valid @RequestBody CreateQuestionRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        CourseQuestionResponse response = qaService.createQuestion(courseId, lessonId, request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đặt câu hỏi thành công", response));
    }

    @PostMapping("/questions/{questionId}/answers")
    public ResponseEntity<ApiResponse<CourseAnswerResponse>> createAnswer(
            @PathVariable Long courseId,
            @PathVariable Long questionId,
            @Valid @RequestBody CreateAnswerRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        CourseAnswerResponse response = qaService.createAnswer(courseId, questionId, request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Gửi câu trả lời thành công", response));
    }

    @DeleteMapping("/questions/{questionId}")
    public ResponseEntity<ApiResponse<Void>> deleteQuestion(
            @PathVariable Long courseId,
            @PathVariable Long questionId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        qaService.deleteQuestion(courseId, questionId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Xóa câu hỏi thành công", null));
    }

    @DeleteMapping("/questions/{questionId}/answers/{answerId}")
    public ResponseEntity<ApiResponse<Void>> deleteAnswer(
            @PathVariable Long courseId,
            @PathVariable Long questionId,
            @PathVariable Long answerId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        qaService.deleteAnswer(courseId, questionId, answerId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Xóa câu trả lời thành công", null));
    }
}
