package com.learnova.elearning.module.quiz.controller;

import com.learnova.elearning.common.dto.ApiResponse;
import com.learnova.elearning.module.quiz.dto.request.ReorderQuestionsRequest;
import com.learnova.elearning.module.quiz.dto.request.UpsertQuestionRequest;
import com.learnova.elearning.module.quiz.dto.request.UpsertQuizRequest;
import com.learnova.elearning.module.quiz.dto.response.QuestionDetailResponse;
import com.learnova.elearning.module.quiz.dto.response.QuizDetailResponse;
import com.learnova.elearning.module.quiz.service.QuizAuthoringService;
import com.learnova.elearning.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Controller dành cho Giảng viên quản trị cấu hình bài Quiz và ngân hàng câu hỏi (US-07).
 */
@RestController
@RequestMapping("/api/v1/lecturer/courses/{courseId}/lessons/{lessonId}/quiz")
@RequiredArgsConstructor
public class LecturerQuizController {

    private final QuizAuthoringService quizAuthoringService;

    @GetMapping
    public ResponseEntity<ApiResponse<QuizDetailResponse>> getQuiz(
            @PathVariable Long courseId,
            @PathVariable Long lessonId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        QuizDetailResponse response = quizAuthoringService.getQuizDetail(courseId, lessonId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin bài quiz thành công", response));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<QuizDetailResponse>> upsertQuiz(
            @PathVariable Long courseId,
            @PathVariable Long lessonId,
            @Valid @RequestBody UpsertQuizRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        QuizDetailResponse response = quizAuthoringService.upsertQuiz(courseId, lessonId, request, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật bài quiz thành công", response));
    }

    @PostMapping("/questions")
    public ResponseEntity<ApiResponse<QuestionDetailResponse>> addQuestion(
            @PathVariable Long courseId,
            @PathVariable Long lessonId,
            @Valid @RequestBody UpsertQuestionRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        QuestionDetailResponse response = quizAuthoringService.addQuestion(courseId, lessonId, request, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Thêm câu hỏi mới thành công", response));
    }

    @PutMapping("/questions/{questionId}")
    public ResponseEntity<ApiResponse<QuestionDetailResponse>> updateQuestion(
            @PathVariable Long courseId,
            @PathVariable Long lessonId,
            @PathVariable Long questionId,
            @Valid @RequestBody UpsertQuestionRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        QuestionDetailResponse response = quizAuthoringService.updateQuestion(courseId, lessonId, questionId, request, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật câu hỏi thành công", response));
    }

    @DeleteMapping("/questions/{questionId}")
    public ResponseEntity<ApiResponse<Void>> deleteQuestion(
            @PathVariable Long courseId,
            @PathVariable Long lessonId,
            @PathVariable Long questionId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        quizAuthoringService.deleteQuestion(courseId, lessonId, questionId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Xóa câu hỏi thành công", null));
    }

    @PatchMapping("/questions/reorder")
    public ResponseEntity<ApiResponse<Void>> reorderQuestions(
            @PathVariable Long courseId,
            @PathVariable Long lessonId,
            @Valid @RequestBody ReorderQuestionsRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        quizAuthoringService.reorderQuestions(courseId, lessonId, request, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Sắp xếp lại câu hỏi thành công", null));
    }
}
