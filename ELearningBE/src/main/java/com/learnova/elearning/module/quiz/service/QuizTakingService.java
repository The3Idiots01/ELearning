package com.learnova.elearning.module.quiz.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import com.learnova.elearning.module.enrollment.entity.Enrollment;
import com.learnova.elearning.module.enrollment.repository.AssessmentProgressRepository;
import com.learnova.elearning.module.enrollment.repository.EnrollmentRepository;
import com.learnova.elearning.module.quiz.dto.QuizAttemptSnapshot;
import com.learnova.elearning.module.quiz.dto.QuizOptionDto;
import com.learnova.elearning.module.quiz.dto.request.SubmitQuizAttemptRequest;
import com.learnova.elearning.module.quiz.dto.request.SubmitQuizAttemptRequest.QuestionAnswerItem;
import com.learnova.elearning.module.quiz.dto.response.QuestionTakingResponse;
import com.learnova.elearning.module.quiz.dto.response.QuizAttemptResponse;
import com.learnova.elearning.module.quiz.dto.response.QuizAttemptResponse.QuestionResultItem;
import com.learnova.elearning.module.quiz.dto.response.QuizTakingResponse;
import com.learnova.elearning.module.quiz.entity.Quiz;
import com.learnova.elearning.module.quiz.entity.QuizAttempt;
import com.learnova.elearning.module.quiz.entity.QuizQuestion;
import com.learnova.elearning.module.quiz.entity.enums.QuestionType;
import com.learnova.elearning.module.course.entity.enums.PublicationStatus;
import com.learnova.elearning.module.quiz.repository.QuizAttemptRepository;
import com.learnova.elearning.module.quiz.repository.QuizQuestionRepository;
import com.learnova.elearning.module.quiz.repository.QuizRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Learner quiz delivery, server-side grading, attempt history, and completion. */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuizTakingService {

    private static final BigDecimal DEFAULT_PASSING_SCORE = new BigDecimal("80.00");

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AssessmentProgressRepository assessmentProgressRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public QuizTakingResponse getQuizForTaking(Long courseId, Long assessmentId, Long studentId) {
        requireEnrollment(courseId, studentId);
        Quiz quiz = requireQuiz(courseId, assessmentId);
        int attemptsUsed = quizAttemptRepository.countByQuiz_IdAndLearner_Id(quiz.getId(), studentId);
        Integer remaining = remainingAttempts(quiz.getMaxAttempts(), attemptsUsed);

        List<QuestionTakingResponse> questions = quizQuestionRepository
                .findByQuiz_IdOrderByPositionAsc(quiz.getId()).stream()
                .map(this::toTakingQuestion)
                .toList();

        return QuizTakingResponse.builder()
                .id(quiz.getId())
                .assessmentId(assessmentId)
                .title(quiz.getAssessment().getTitle())
                .passingScore(passingScore(quiz))
                .maxAttempts(quiz.getMaxAttempts())
                .attemptsUsed(attemptsUsed)
                .attemptsRemaining(remaining)
                .hasPassed(quizAttemptRepository.existsByQuiz_IdAndLearner_IdAndIsPassedTrue(
                        quiz.getId(), studentId))
                .questions(questions)
                .build();
    }

    @Transactional
    public QuizAttemptResponse submitAttempt(Long courseId, Long assessmentId,
                                             SubmitQuizAttemptRequest request, Long studentId) {
        Enrollment enrollment = requireEnrollment(courseId, studentId);
        // Lock the quiz row so concurrent submissions cannot both pass the max-attempt check.
        Quiz quiz = quizRepository.findForUpdateByAssessmentAndCourse(assessmentId, courseId)
                .orElseThrow(() -> new AppException(ErrorCode.QUIZ_NOT_FOUND));
        int attemptsUsed = quizAttemptRepository.countByQuiz_IdAndLearner_Id(quiz.getId(), studentId);
        if (quiz.getMaxAttempts() != null && attemptsUsed >= quiz.getMaxAttempts()) {
            throw new AppException(ErrorCode.QUIZ_MAX_ATTEMPTS_REACHED);
        }

        List<QuizQuestion> questions = quizQuestionRepository.findByQuiz_IdOrderByPositionAsc(quiz.getId());
        if (questions.isEmpty()) {
            throw new AppException(ErrorCode.QUIZ_WITHOUT_QUESTION);
        }
        Map<Long, List<String>> answers = validateAndIndexAnswers(request.getAnswers(), questions);
        GradingResult grading = grade(questions, answers);
        BigDecimal threshold = passingScore(quiz);
        boolean passed = grading.score().compareTo(threshold) >= 0;

        QuizAttemptSnapshot snapshot = QuizAttemptSnapshot.builder()
                .version(1)
                .passingScore(threshold)
                .totalQuestions(questions.size())
                .correctQuestions(grading.correctQuestions())
                .questionResults(grading.results())
                .build();
        QuizAttempt attempt = QuizAttempt.builder()
                .quiz(quiz)
                .learner(enrollment.getStudent())
                .answersJson(serializeSnapshot(snapshot))
                .score(grading.score())
                .isPassed(passed)
                .build();
        QuizAttempt saved = quizAttemptRepository.save(attempt);

        if (passed) {
            int newlyCompleted = assessmentProgressRepository.markCompletedByQuiz(
                    enrollment.getId(), assessmentId);
            // A passed retry is intentionally a no-op for progress. Roll up only
            // when the assessment changes from incomplete to completed.
            if (newlyCompleted > 0) {
                enrollmentRepository.recalculateCourseProgress(enrollment.getId(), courseId);
            }
        }
        return toAttemptResponse(saved, quiz, snapshot);
    }

    @Transactional(readOnly = true)
    public List<QuizAttemptResponse> getAttemptHistory(Long courseId, Long assessmentId, Long studentId) {
        requireEnrollment(courseId, studentId);
        Quiz quiz = requireQuiz(courseId, assessmentId);
        return quizAttemptRepository.findByQuiz_IdAndLearner_IdOrderBySubmittedAtDesc(quiz.getId(), studentId)
                .stream()
                .map(attempt -> toAttemptResponse(attempt, quiz, deserializeSnapshot(attempt.getAnswersJson())))
                .toList();
    }

    private Enrollment requireEnrollment(Long courseId, Long studentId) {
        return enrollmentRepository.findByStudent_IdAndCourse_Id(studentId, courseId)
                .orElseThrow(() -> new AppException(ErrorCode.ENROLLMENT_NOT_FOUND));
    }

    private Quiz requireQuiz(Long courseId, Long assessmentId) {
        Quiz quiz = quizRepository.findByAssessment_IdAndAssessment_Course_Id(assessmentId, courseId)
                .orElseThrow(() -> new AppException(ErrorCode.QUIZ_NOT_FOUND));
        if (quiz.getAssessment().getPublicationStatus() != null
                && quiz.getAssessment().getPublicationStatus() != PublicationStatus.PUBLISHED) {
            throw new AppException(ErrorCode.QUIZ_NOT_FOUND);
        }
        return quiz;
    }

    private QuestionTakingResponse toTakingQuestion(QuizQuestion question) {
        List<QuestionTakingResponse.StudentOptionItem> safeOptions = deserializeOptions(question.getOptionsJson())
                .stream()
                .map(option -> QuestionTakingResponse.StudentOptionItem.builder()
                        .id(option.getId())
                        .text(option.getText())
                        .build())
                .toList();
        return QuestionTakingResponse.builder()
                .id(question.getId())
                .questionText(question.getQuestionText())
                .questionType(question.getQuestionType())
                .points(question.getPoints())
                .position(question.getPosition())
                .options(safeOptions)
                .build();
    }

    private Map<Long, List<String>> validateAndIndexAnswers(List<QuestionAnswerItem> submitted,
                                                             List<QuizQuestion> questions) {
        Set<Long> validQuestionIds = questions.stream().map(QuizQuestion::getId).collect(Collectors.toSet());
        Map<Long, List<String>> answers = new HashMap<>();
        for (QuestionAnswerItem answer : submitted) {
            if (!validQuestionIds.contains(answer.getQuestionId())
                    || answers.putIfAbsent(answer.getQuestionId(), List.copyOf(answer.getSelectedOptionIds())) != null) {
                throw new AppException(ErrorCode.INVALID_REQUEST,
                        "Answers contain an unknown or duplicated question");
            }
        }

        Map<Long, QuizQuestion> byId = questions.stream()
                .collect(Collectors.toMap(QuizQuestion::getId, question -> question));
        for (Map.Entry<Long, List<String>> entry : answers.entrySet()) {
            List<String> selected = entry.getValue();
            if (selected.stream().anyMatch(id -> id == null || id.isBlank())
                    || new HashSet<>(selected).size() != selected.size()) {
                throw new AppException(ErrorCode.INVALID_REQUEST,
                        "Selected option IDs must be non-blank and unique");
            }
            Set<String> validOptionIds = deserializeOptions(byId.get(entry.getKey()).getOptionsJson())
                    .stream().map(QuizOptionDto::getId).collect(Collectors.toSet());
            if (!validOptionIds.containsAll(selected)) {
                throw new AppException(ErrorCode.INVALID_REQUEST,
                        "Selected option does not belong to the question");
            }
        }
        return answers;
    }

    private GradingResult grade(List<QuizQuestion> questions, Map<Long, List<String>> answers) {
        BigDecimal earned = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        int correctQuestions = 0;
        List<QuestionResultItem> results = new ArrayList<>();

        for (QuizQuestion question : questions) {
            List<QuizOptionDto> options = deserializeOptions(question.getOptionsJson());
            Set<String> correctIds = options.stream()
                    .filter(option -> Boolean.TRUE.equals(option.getIsCorrect()))
                    .map(QuizOptionDto::getId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            List<String> selectedList = answers.getOrDefault(question.getId(), List.of());
            Set<String> selectedIds = new LinkedHashSet<>(selectedList);
            boolean correct = question.getQuestionType() == QuestionType.SINGLE_CHOICE
                    ? selectedIds.size() == 1 && selectedIds.equals(correctIds)
                    : !correctIds.isEmpty() && selectedIds.equals(correctIds);
            BigDecimal points = question.getPoints() != null ? question.getPoints() : BigDecimal.ZERO;
            total = total.add(points);
            if (correct) {
                earned = earned.add(points);
                correctQuestions++;
            }

            String explanation = options.stream()
                    .filter(option -> Boolean.TRUE.equals(option.getIsCorrect()))
                    .map(QuizOptionDto::getExplanation)
                    .filter(value -> value != null && !value.isBlank())
                    .distinct()
                    .collect(Collectors.joining("\n"));
            results.add(QuestionResultItem.builder()
                    .questionId(question.getId())
                    .isCorrect(correct)
                    .earnedPoints(correct ? points : BigDecimal.ZERO)
                    .totalPoints(points)
                    .selectedOptionIds(List.copyOf(selectedList))
                    .correctOptionIds(List.copyOf(correctIds))
                    .explanation(explanation.isBlank() ? null : explanation)
                    .build());
        }

        BigDecimal score = total.signum() == 0
                ? BigDecimal.ZERO.setScale(2)
                : earned.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP);
        return new GradingResult(score, correctQuestions, results);
    }

    private QuizAttemptResponse toAttemptResponse(QuizAttempt attempt, Quiz quiz, QuizAttemptSnapshot snapshot) {
        QuizAttemptSnapshot safe = snapshot != null ? snapshot : QuizAttemptSnapshot.builder()
                .passingScore(passingScore(quiz))
                .totalQuestions(0)
                .correctQuestions(0)
                .questionResults(List.of())
                .build();
        return QuizAttemptResponse.builder()
                .id(attempt.getId())
                .quizId(quiz.getId())
                .assessmentId(quiz.getAssessment().getId())
                .score(attempt.getScore())
                .passingScore(safe.getPassingScore() != null ? safe.getPassingScore() : passingScore(quiz))
                .isPassed(attempt.getIsPassed())
                .totalQuestions(safe.getTotalQuestions())
                .correctQuestions(safe.getCorrectQuestions())
                .submittedAt(attempt.getSubmittedAt())
                .questionResults(safe.getQuestionResults() != null ? safe.getQuestionResults() : List.of())
                .build();
    }

    private Integer remainingAttempts(Integer maxAttempts, int attemptsUsed) {
        return maxAttempts == null ? null : Math.max(0, maxAttempts - attemptsUsed);
    }

    private BigDecimal passingScore(Quiz quiz) {
        return quiz.getPassingScore() != null ? quiz.getPassingScore() : DEFAULT_PASSING_SCORE;
    }

    private List<QuizOptionDto> deserializeOptions(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<QuizOptionDto>>() {});
        } catch (JsonProcessingException exception) {
            log.error("Cannot deserialize options for grading", exception);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Dữ liệu đáp án quiz không hợp lệ");
        }
    }

    private String serializeSnapshot(QuizAttemptSnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException exception) {
            log.error("Cannot serialize quiz attempt snapshot", exception);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Không thể lưu kết quả bài làm");
        }
    }

    private QuizAttemptSnapshot deserializeSnapshot(String json) {
        try {
            return objectMapper.readValue(json, QuizAttemptSnapshot.class);
        } catch (JsonProcessingException exception) {
            // Attempts created before Task 06 stored only raw answer arrays.
            try {
                objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
                return null;
            } catch (JsonProcessingException ignored) {
                log.warn("Cannot read attempt snapshot; returning summary only");
                return null;
            }
        }
    }

    private record GradingResult(BigDecimal score, int correctQuestions,
                                 List<QuestionResultItem> results) {}
}
