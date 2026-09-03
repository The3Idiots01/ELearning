package com.learnova.elearning.module.quiz.controller;

import com.learnova.elearning.common.dto.ApiResponse;
import com.learnova.elearning.module.quiz.dto.request.SubmitQuizAttemptRequest;
import com.learnova.elearning.module.quiz.dto.response.QuizAttemptResponse;
import com.learnova.elearning.module.quiz.dto.response.QuizTakingResponse;
import com.learnova.elearning.module.quiz.service.QuizTakingService;
import com.learnova.elearning.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller dành cho Học viên lấy đề thi, làm bài và nộp bài kiểm tra (US-20).
 */
@RestController
@RequestMapping("/api/v1/learner/courses/{courseId}/lessons/{lessonId}/quiz")
@RequiredArgsConstructor
public class LearnerQuizController {

    private final QuizTakingService quizTakingService;

    @GetMapping
    public ResponseEntity<ApiResponse<QuizTakingResponse>> getQuizForTaking(
            @PathVariable Long courseId,
            @PathVariable Long lessonId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        QuizTakingResponse response = quizTakingService.getQuizForTaking(courseId, lessonId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Lấy đề thi bài quiz thành công", response));
    }

    @PostMapping("/attempts")
    public ResponseEntity<ApiResponse<QuizAttemptResponse>> submitAttempt(
            @PathVariable Long courseId,
            @PathVariable Long lessonId,
            @Valid @RequestBody SubmitQuizAttemptRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        QuizAttemptResponse response = quizTakingService.submitAttempt(courseId, lessonId, request, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Nộp bài thi thành công", response));
    }

    @GetMapping("/attempts")
    public ResponseEntity<ApiResponse<List<QuizAttemptResponse>>> getAttemptHistory(
            @PathVariable Long courseId,
            @PathVariable Long lessonId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        List<QuizAttemptResponse> response = quizTakingService.getAttemptHistory(courseId, lessonId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử làm bài thành công", response));
    }
}
