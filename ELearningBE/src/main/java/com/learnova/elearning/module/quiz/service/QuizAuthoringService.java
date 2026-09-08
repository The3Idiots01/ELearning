package com.learnova.elearning.module.quiz.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import com.learnova.elearning.module.course.entity.Assessment;
import com.learnova.elearning.module.course.entity.enums.AssessmentType;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Quiz authoring through a course assessment, independent from lesson plans. */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuizAuthoringService {

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final CourseOwnershipGuard ownershipGuard;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public QuizDetailResponse getQuizDetail(Long courseId, Long assessmentId, Long lecturerId) {
        ownershipGuard.requireOwnedCourse(courseId, lecturerId);
        Assessment assessment = requireQuizAssessment(courseId, assessmentId);
        Quiz quiz = requireQuiz(courseId, assessmentId);
        return toQuizDetailResponse(quiz, assessment);
    }

    @Transactional
    public QuizDetailResponse upsertQuiz(Long courseId, Long assessmentId,
                                         UpsertQuizRequest request, Long lecturerId) {
        ownershipGuard.requireEditableCourse(courseId, lecturerId);
        Assessment assessment = requireQuizAssessment(courseId, assessmentId);
        Quiz quiz = quizRepository.findByAssessment_Id(assessmentId)
                .orElseGet(() -> Quiz.builder().assessment(assessment).build());
        quiz.setPassingScore(request.getPassingScore());
        quiz.setMaxAttempts(request.getMaxAttempts());
        return toQuizDetailResponse(quizRepository.save(quiz), assessment);
    }

    @Transactional
    public QuestionDetailResponse addQuestion(Long courseId, Long assessmentId,
                                              UpsertQuestionRequest request, Long lecturerId) {
        ownershipGuard.requireEditableCourse(courseId, lecturerId);
        Assessment assessment = requireQuizAssessment(courseId, assessmentId);
        Quiz quiz = quizRepository.findByAssessment_Id(assessmentId)
                .orElseGet(() -> quizRepository.save(Quiz.builder().assessment(assessment).build()));

        List<QuizOptionDto> options = validateAndNormalizeOptions(request.getOptions(), request.getQuestionType());
        QuizQuestion question = QuizQuestion.builder()
                .quiz(quiz)
                .questionText(request.getQuestionText().trim())
                .questionType(request.getQuestionType())
                .points(request.getPoints())
                .position(quizQuestionRepository.countByQuiz_Id(quiz.getId()))
                .optionsJson(serializeOptions(options))
                .build();
        return toQuestionDetailResponse(quizQuestionRepository.save(question));
    }

    @Transactional
    public QuestionDetailResponse updateQuestion(Long courseId, Long assessmentId, Long questionId,
                                                 UpsertQuestionRequest request, Long lecturerId) {
        ownershipGuard.requireEditableCourse(courseId, lecturerId);
        Assessment assessment = requireQuizAssessment(courseId, assessmentId);
        Quiz quiz = requireQuiz(courseId, assessmentId);
        QuizQuestion question = quizQuestionRepository.findByIdAndQuiz_Id(questionId, quiz.getId())
                .orElseThrow(() -> new AppException(ErrorCode.QUIZ_QUESTION_NOT_FOUND));

        question.setQuestionText(request.getQuestionText().trim());
        question.setQuestionType(request.getQuestionType());
        question.setPoints(request.getPoints());
        question.setOptionsJson(serializeOptions(
                validateAndNormalizeOptions(request.getOptions(), request.getQuestionType())));
        return toQuestionDetailResponse(quizQuestionRepository.save(question));
    }

    @Transactional
    public void deleteQuestion(Long courseId, Long assessmentId, Long questionId, Long lecturerId) {
        ownershipGuard.requireEditableCourse(courseId, lecturerId);
        requireQuizAssessment(courseId, assessmentId);
        Quiz quiz = requireQuiz(courseId, assessmentId);
        QuizQuestion question = quizQuestionRepository.findByIdAndQuiz_Id(questionId, quiz.getId())
                .orElseThrow(() -> new AppException(ErrorCode.QUIZ_QUESTION_NOT_FOUND));

        quizQuestionRepository.delete(question);
        quizQuestionRepository.flush();
        normalizeQuestionPositions(quiz.getId());
    }

    @Transactional
    public void reorderQuestions(Long courseId, Long assessmentId,
                                 ReorderQuestionsRequest request, Long lecturerId) {
        ownershipGuard.requireEditableCourse(courseId, lecturerId);
        requireQuizAssessment(courseId, assessmentId);
        Quiz quiz = requireQuiz(courseId, assessmentId);
        List<QuizQuestion> existing = quizQuestionRepository.findByQuiz_IdOrderByPositionAsc(quiz.getId());
        List<Long> submittedIds = request.getQuestionIds();
        if (submittedIds == null || submittedIds.size() != existing.size()
                || !new HashSet<>(submittedIds).equals(
                        existing.stream().map(QuizQuestion::getId).collect(Collectors.toSet()))) {
            throw new AppException(ErrorCode.ORDER_PAYLOAD_MISMATCH);
        }

        Map<Long, QuizQuestion> byId = existing.stream()
                .collect(Collectors.toMap(QuizQuestion::getId, Function.identity()));
        for (int i = 0; i < submittedIds.size(); i++) {
            byId.get(submittedIds.get(i)).setPosition(i);
        }
        quizQuestionRepository.saveAll(existing);
    }

    private Assessment requireQuizAssessment(Long courseId, Long assessmentId) {
        Assessment assessment = ownershipGuard.requireAssessmentInCourse(assessmentId, courseId);
        if (assessment.getType() != AssessmentType.QUIZ) {
            throw new AppException(ErrorCode.ASSESSMENT_TYPE_UNSUPPORTED);
        }
        return assessment;
    }

    private Quiz requireQuiz(Long courseId, Long assessmentId) {
        return quizRepository.findByAssessment_IdAndAssessment_Course_Id(assessmentId, courseId)
                .orElseThrow(() -> new AppException(ErrorCode.QUIZ_NOT_FOUND));
    }

    private void normalizeQuestionPositions(Long quizId) {
        List<QuizQuestion> remaining = quizQuestionRepository.findByQuiz_IdOrderByPositionAsc(quizId);
        for (int i = 0; i < remaining.size(); i++) {
            remaining.get(i).setPosition(i);
        }
        quizQuestionRepository.saveAll(remaining);
    }

    private List<QuizOptionDto> validateAndNormalizeOptions(List<QuizOptionDto> options,
                                                            QuestionType questionType) {
        if (options == null || options.size() < 2) {
            throw new AppException(ErrorCode.VALIDATION_ERROR,
                    "Câu hỏi phải có ít nhất 2 đáp án lựa chọn");
        }
        long correctCount = options.stream().filter(option -> Boolean.TRUE.equals(option.getIsCorrect())).count();
        if (correctCount == 0) {
            throw new AppException(ErrorCode.QUIZ_QUESTION_INVALID_OPTIONS);
        }
        if (questionType == QuestionType.SINGLE_CHOICE && correctCount > 1) {
            throw new AppException(ErrorCode.QUIZ_SINGLE_CHOICE_MULTIPLE_CORRECT);
        }

        Set<String> seenIds = new HashSet<>();
        List<QuizOptionDto> normalized = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            QuizOptionDto option = options.get(i);
            String candidate = option.getId() != null && !option.getId().isBlank()
                    ? option.getId().trim() : "opt_" + (i + 1);
            if (!seenIds.add(candidate)) {
                throw new AppException(ErrorCode.QUIZ_QUESTION_INVALID_OPTIONS,
                        "Option IDs must be unique");
            }
            normalized.add(QuizOptionDto.builder()
                    .id(candidate)
                    .text(option.getText().trim())
                    .isCorrect(Boolean.TRUE.equals(option.getIsCorrect()))
                    .explanation(trimToNull(option.getExplanation()))
                    .build());
        }
        return normalized;
    }

    private QuizDetailResponse toQuizDetailResponse(Quiz quiz, Assessment assessment) {
        List<QuizQuestion> questions = quizQuestionRepository.findByQuiz_IdOrderByPositionAsc(quiz.getId());
        BigDecimal totalPoints = questions.stream()
                .map(question -> question.getPoints() != null ? question.getPoints() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return QuizDetailResponse.builder()
                .id(quiz.getId())
                .assessmentId(assessment.getId())
                .title(assessment.getTitle())
                .passingScore(quiz.getPassingScore())
                .maxAttempts(quiz.getMaxAttempts())
                .totalPoints(totalPoints)
                .questions(questions.stream().map(this::toQuestionDetailResponse).toList())
                .createdAt(quiz.getCreatedAt())
                .updatedAt(quiz.getUpdatedAt())
                .build();
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

    private String serializeOptions(List<QuizOptionDto> options) {
        try {
            return objectMapper.writeValueAsString(options);
        } catch (JsonProcessingException exception) {
            log.error("Cannot serialize quiz options", exception);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Lỗi xử lý dữ liệu đáp án JSON");
        }
    }

    private List<QuizOptionDto> deserializeOptions(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<QuizOptionDto>>() {});
        } catch (JsonProcessingException exception) {
            log.error("Cannot deserialize quiz options", exception);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Dữ liệu đáp án quiz không hợp lệ");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
