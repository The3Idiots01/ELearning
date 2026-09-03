package com.learnova.elearning.module.quiz.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import com.learnova.elearning.module.course.entity.Lesson;
import com.learnova.elearning.module.course.entity.enums.LessonContentType;
import com.learnova.elearning.module.course.repository.LessonRepository;
import com.learnova.elearning.module.course.service.CourseOwnershipGuard;
import com.learnova.elearning.module.quiz.dto.QuizOptionDto;
import com.learnova.elearning.module.quiz.dto.request.ReorderQuestionsRequest;
import com.learnova.elearning.module.quiz.dto.request.UpsertQuestionRequest;
import com.learnova.elearning.module.quiz.dto.request.UpsertQuizRequest;
import com.learnova.elearning.module.quiz.dto.response.QuestionDetailResponse;
import com.learnova.elearning.module.quiz.dto.response.QuizDetailResponse;
import com.learnova.elearning.module.quiz.entity.Quiz;
import com.learnova.elearning.module.quiz.entity.QuizQuestion;
import com.learnova.elearning.module.quiz.entity.enums.QuestionType;
import com.learnova.elearning.module.quiz.repository.QuizQuestionRepository;
import com.learnova.elearning.module.quiz.repository.QuizRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service xử lý nghiệp vụ Soạn thảo bài Quiz dành cho Giảng viên (US-07).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuizAuthoringService {

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final LessonRepository lessonRepository;
    private final CourseOwnershipGuard ownershipGuard;
    private final ObjectMapper objectMapper;

    /**
     * 1. Lấy thông tin chi tiết bài Quiz và danh sách câu hỏi kèm đáp án đúng.
     */
    @Transactional(readOnly = true)
    public QuizDetailResponse getQuizDetail(Long courseId, Long lessonId, Long lecturerId) {
        ownershipGuard.requireOwnedCourse(courseId, lecturerId);
        Lesson lesson = ownershipGuard.requireLessonInCourse(lessonId, courseId);
        validateLessonIsQuiz(lesson);

        Quiz quiz = quizRepository.findByLesson_Id(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.QUIZ_NOT_FOUND));

        List<QuizQuestion> questions = quizQuestionRepository.findByQuiz_IdOrderByPositionAsc(quiz.getId());
        List<QuestionDetailResponse> questionResponses = questions.stream()
                .map(this::toQuestionDetailResponse)
                .toList();

        BigDecimal totalPoints = questions.stream()
                .map(QuizQuestion::getPoints)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return QuizDetailResponse.builder()
                .id(quiz.getId())
                .lessonId(lesson.getId())
                .title(quiz.getTitle())
                .passingScore(quiz.getPassingScore())
                .maxAttempts(quiz.getMaxAttempts())
                .totalPoints(totalPoints)
                .questions(questionResponses)
                .createdAt(quiz.getCreatedAt())
                .updatedAt(quiz.getUpdatedAt())
                .build();
    }

    /**
     * 2. Tạo mới hoặc cập nhật cấu hình chung của bài Quiz (tiêu đề, điểm đạt, số lần làm).
     */
    @Transactional
    public QuizDetailResponse upsertQuiz(Long courseId, Long lessonId, UpsertQuizRequest request, Long lecturerId) {
        ownershipGuard.requireEditableCourse(courseId, lecturerId);
        Lesson lesson = ownershipGuard.requireLessonInCourse(lessonId, courseId);
        validateLessonIsQuiz(lesson);

        Quiz quiz = quizRepository.findByLesson_Id(lessonId)
                .orElseGet(() -> Quiz.builder()
                        .lesson(lesson)
                        .build());

        quiz.setTitle(request.getTitle());
        if (request.getPassingScore() != null) {
            quiz.setPassingScore(request.getPassingScore());
        }
        quiz.setMaxAttempts(request.getMaxAttempts());

        Quiz saved = quizRepository.save(quiz);

        List<QuizQuestion> questions = quizQuestionRepository.findByQuiz_IdOrderByPositionAsc(saved.getId());
        List<QuestionDetailResponse> questionResponses = questions.stream()
                .map(this::toQuestionDetailResponse)
                .toList();

        BigDecimal totalPoints = questions.stream()
                .map(QuizQuestion::getPoints)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return QuizDetailResponse.builder()
                .id(saved.getId())
                .lessonId(lesson.getId())
                .title(saved.getTitle())
                .passingScore(saved.getPassingScore())
                .maxAttempts(saved.getMaxAttempts())
                .totalPoints(totalPoints)
                .questions(questionResponses)
                .createdAt(saved.getCreatedAt())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }

    /**
     * 3. Thêm một câu hỏi mới vào bài Quiz.
     */
    @Transactional
    public QuestionDetailResponse addQuestion(Long courseId, Long lessonId, UpsertQuestionRequest request, Long lecturerId) {
        ownershipGuard.requireEditableCourse(courseId, lecturerId);
        Lesson lesson = ownershipGuard.requireLessonInCourse(lessonId, courseId);
        validateLessonIsQuiz(lesson);

        Quiz quiz = quizRepository.findByLesson_Id(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.QUIZ_NOT_FOUND));

        List<QuizOptionDto> normalizedOptions = validateAndNormalizeOptions(request.getOptions(), request.getQuestionType());
        String optionsJson = serializeOptions(normalizedOptions);

        int nextPosition = quizQuestionRepository.countByQuiz_Id(quiz.getId());

        QuizQuestion question = QuizQuestion.builder()
                .quiz(quiz)
                .questionText(request.getQuestionText())
                .questionType(request.getQuestionType())
                .points(request.getPoints())
                .position(nextPosition)
                .optionsJson(optionsJson)
                .build();

        QuizQuestion saved = quizQuestionRepository.save(question);
        return toQuestionDetailResponse(saved);
    }

    /**
     * 4. Cập nhật nội dung câu hỏi, loại câu hỏi hoặc danh sách đáp án.
     */
    @Transactional
    public QuestionDetailResponse updateQuestion(Long courseId, Long lessonId, Long questionId, UpsertQuestionRequest request, Long lecturerId) {
        ownershipGuard.requireEditableCourse(courseId, lecturerId);
        Lesson lesson = ownershipGuard.requireLessonInCourse(lessonId, courseId);
        validateLessonIsQuiz(lesson);

        Quiz quiz = quizRepository.findByLesson_Id(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.QUIZ_NOT_FOUND));

        QuizQuestion question = quizQuestionRepository.findByIdAndQuiz_Id(questionId, quiz.getId())
                .orElseThrow(() -> new AppException(ErrorCode.QUIZ_QUESTION_NOT_FOUND));

        List<QuizOptionDto> normalizedOptions = validateAndNormalizeOptions(request.getOptions(), request.getQuestionType());
        String optionsJson = serializeOptions(normalizedOptions);

        question.setQuestionText(request.getQuestionText());
        question.setQuestionType(request.getQuestionType());
        question.setPoints(request.getPoints());
        question.setOptionsJson(optionsJson);

        QuizQuestion saved = quizQuestionRepository.save(question);
        return toQuestionDetailResponse(saved);
    }

    /**
     * 5. Xóa một câu hỏi khỏi bài Quiz.
     */
    @Transactional
    public void deleteQuestion(Long courseId, Long lessonId, Long questionId, Long lecturerId) {
        ownershipGuard.requireEditableCourse(courseId, lecturerId);
        Lesson lesson = ownershipGuard.requireLessonInCourse(lessonId, courseId);
        validateLessonIsQuiz(lesson);

        Quiz quiz = quizRepository.findByLesson_Id(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.QUIZ_NOT_FOUND));

        QuizQuestion question = quizQuestionRepository.findByIdAndQuiz_Id(questionId, quiz.getId())
                .orElseThrow(() -> new AppException(ErrorCode.QUIZ_QUESTION_NOT_FOUND));

        quizQuestionRepository.delete(question);

        // Đánh số lại thứ tự position cho các câu còn lại (0, 1, 2...)
        List<QuizQuestion> remaining = quizQuestionRepository.findByQuiz_IdOrderByPositionAsc(quiz.getId());
        for (int i = 0; i < remaining.size(); i++) {
            remaining.get(i).setPosition(i);
        }
        quizQuestionRepository.saveAll(remaining);
    }

    /**
     * 6. Sắp xếp lại thứ tự (Reorder) các câu hỏi trong bài Quiz.
     */
    @Transactional
    public void reorderQuestions(Long courseId, Long lessonId, ReorderQuestionsRequest request, Long lecturerId) {
        ownershipGuard.requireEditableCourse(courseId, lecturerId);
        Lesson lesson = ownershipGuard.requireLessonInCourse(lessonId, courseId);
        validateLessonIsQuiz(lesson);

        Quiz quiz = quizRepository.findByLesson_Id(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.QUIZ_NOT_FOUND));

        List<QuizQuestion> existing = quizQuestionRepository.findByQuiz_IdOrderByPositionAsc(quiz.getId());
        if (existing.size() != request.getQuestionIds().size()) {
            throw new AppException(ErrorCode.ORDER_PAYLOAD_MISMATCH);
        }

        Map<Long, QuizQuestion> questionMap = existing.stream()
                .collect(Collectors.toMap(QuizQuestion::getId, Function.identity()));

        for (int i = 0; i < request.getQuestionIds().size(); i++) {
            Long qId = request.getQuestionIds().get(i);
            QuizQuestion q = questionMap.get(qId);
            if (q == null) {
                throw new AppException(ErrorCode.ORDER_PAYLOAD_MISMATCH);
            }
            q.setPosition(i);
        }

        quizQuestionRepository.saveAll(existing);
    }

    // --- Helper Methods ---

    private void validateLessonIsQuiz(Lesson lesson) {
        if (lesson.getContentType() != LessonContentType.QUIZ) {
            throw new AppException(ErrorCode.LESSON_NOT_A_QUIZ);
        }
    }

    private List<QuizOptionDto> validateAndNormalizeOptions(List<QuizOptionDto> options, QuestionType questionType) {
        if (options == null || options.size() < 2) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Câu hỏi phải có ít nhất 2 đáp án lựa chọn");
        }

        long correctCount = options.stream()
                .filter(opt -> Boolean.TRUE.equals(opt.getIsCorrect()))
                .count();

        if (correctCount == 0) {
            throw new AppException(ErrorCode.QUIZ_QUESTION_INVALID_OPTIONS);
        }

        if (questionType == QuestionType.SINGLE_CHOICE && correctCount > 1) {
            throw new AppException(ErrorCode.QUIZ_SINGLE_CHOICE_MULTIPLE_CORRECT);
        }

        List<QuizOptionDto> normalized = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            QuizOptionDto opt = options.get(i);
            String id = (opt.getId() != null && !opt.getId().isBlank()) ? opt.getId().trim() : "opt_" + (i + 1);
            normalized.add(QuizOptionDto.builder()
                    .id(id)
                    .text(opt.getText().trim())
                    .isCorrect(Boolean.TRUE.equals(opt.getIsCorrect()))
                    .explanation(opt.getExplanation())
                    .build());
        }
        return normalized;
    }

    private String serializeOptions(List<QuizOptionDto> options) {
        try {
            return objectMapper.writeValueAsString(options);
        } catch (JsonProcessingException e) {
            log.error("Lỗi serialize options_json: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Lỗi xử lý dữ liệu đáp án JSON");
        }
    }

    private List<QuizOptionDto> deserializeOptions(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<QuizOptionDto>>() {});
        } catch (JsonProcessingException e) {
            log.error("Lỗi deserialize options_json: {}", e.getMessage(), e);
            return List.of();
        }
    }

    private QuestionDetailResponse toQuestionDetailResponse(QuizQuestion question) {
        return QuestionDetailResponse.builder()
                .id(question.getId())
                .quizId(question.getQuiz().getId())
                .questionText(question.getQuestionText())
                .questionType(question.getQuestionType())
                .points(question.getPoints())
                .position(question.getPosition())
                .options(deserializeOptions(question.getOptionsJson()))
                .createdAt(question.getCreatedAt())
                .updatedAt(question.getUpdatedAt())
                .build();
    }
}
