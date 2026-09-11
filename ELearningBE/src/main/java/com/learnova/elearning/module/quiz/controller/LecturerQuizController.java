package com.learnova.elearning.module.quiz.controller;

import com.learnova.elearning.common.dto.ApiResponse;
import com.learnova.elearning.module.quiz.dto.request.ReorderQuestionsRequest;
import com.learnova.elearning.module.quiz.dto.request.ApplyQuizDraftRequest;
import com.learnova.elearning.module.quiz.dto.request.GenerateQuizDraftRequest;
import com.learnova.elearning.module.quiz.dto.request.UpsertQuestionRequest;
import com.learnova.elearning.module.quiz.dto.request.UpsertQuizRequest;
import com.learnova.elearning.module.quiz.dto.response.QuestionDetailResponse;
import com.learnova.elearning.module.quiz.dto.response.QuizDetailResponse;
import com.learnova.elearning.module.quiz.dto.response.QuizDraftResponse;
import com.learnova.elearning.module.quiz.service.QuizAiAuthoringService;
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
@RequestMapping("/api/v1/lecturer/courses/{courseId}/assessments/{assessmentId}/quiz")
@RequiredArgsConstructor
public class LecturerQuizController {

    private final QuizAuthoringService quizAuthoringService;
    private final QuizAiAuthoringService quizAiAuthoringService;

    @GetMapping
    public ResponseEntity<ApiResponse<QuizDetailResponse>> getQuiz(
            @PathVariable Long courseId,
            @PathVariable Long assessmentId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        QuizDetailResponse response = quizAuthoringService.getQuizDetail(courseId, assessmentId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin bài quiz thành công", response));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<QuizDetailResponse>> upsertQuiz(
            @PathVariable Long courseId,
            @PathVariable Long assessmentId,
            @Valid @RequestBody UpsertQuizRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        QuizDetailResponse response = quizAuthoringService.upsertQuiz(courseId, assessmentId, request, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật bài quiz thành công", response));
    }

    @PostMapping("/ai/draft")
    public ResponseEntity<ApiResponse<QuizDraftResponse>> generateAiDraft(
            @PathVariable Long courseId,
            @PathVariable Long assessmentId,
            @Valid @RequestBody(required = false) GenerateQuizDraftRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ResponseEntity.ok(ApiResponse.success("AI đã tạo bản nháp quiz",
                quizAiAuthoringService.generateDraft(courseId, assessmentId, request, user.getId())));
    }

    @PostMapping("/ai/draft/apply")
    public ResponseEntity<ApiResponse<QuizDetailResponse>> applyAiDraft(
            @PathVariable Long courseId,
            @PathVariable Long assessmentId,
            @Valid @RequestBody ApplyQuizDraftRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ResponseEntity.ok(ApiResponse.success("Đã áp dụng bản nháp AI vào quiz",
                quizAuthoringService.applyAiDraft(courseId, assessmentId, request, user.getId())));
    }

    @PostMapping("/questions")
    public ResponseEntity<ApiResponse<QuestionDetailResponse>> addQuestion(
            @PathVariable Long courseId,
            @PathVariable Long assessmentId,
            @Valid @RequestBody UpsertQuestionRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        QuestionDetailResponse response = quizAuthoringService.addQuestion(courseId, assessmentId, request, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Thêm câu hỏi mới thành công", response));
    }

    @PutMapping("/questions/{questionId}")
    public ResponseEntity<ApiResponse<QuestionDetailResponse>> updateQuestion(
            @PathVariable Long courseId,
            @PathVariable Long assessmentId,
            @PathVariable Long questionId,
            @Valid @RequestBody UpsertQuestionRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        QuestionDetailResponse response = quizAuthoringService.updateQuestion(courseId, assessmentId, questionId, request, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật câu hỏi thành công", response));
    }

    @DeleteMapping("/questions/{questionId}")
    public ResponseEntity<ApiResponse<Void>> deleteQuestion(
            @PathVariable Long courseId,
            @PathVariable Long assessmentId,
            @PathVariable Long questionId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        quizAuthoringService.deleteQuestion(courseId, assessmentId, questionId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Xóa câu hỏi thành công", null));
    }

    @PatchMapping("/questions/reorder")
    public ResponseEntity<ApiResponse<Void>> reorderQuestions(
            @PathVariable Long courseId,
            @PathVariable Long assessmentId,
            @Valid @RequestBody ReorderQuestionsRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        quizAuthoringService.reorderQuestions(courseId, assessmentId, request, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Sắp xếp lại câu hỏi thành công", null));
    }
}
