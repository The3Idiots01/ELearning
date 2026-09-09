package com.learnova.elearning.module.quiz.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import com.learnova.elearning.module.course.entity.Lesson;
import com.learnova.elearning.module.enrollment.entity.Enrollment;
import com.learnova.elearning.module.enrollment.entity.LessonProgress;
import com.learnova.elearning.module.enrollment.entity.enums.CompletionSource;
import com.learnova.elearning.module.enrollment.repository.EnrollmentRepository;
import com.learnova.elearning.module.enrollment.repository.LessonProgressRepository;
import com.learnova.elearning.module.quiz.dto.QuizOptionDto;
import com.learnova.elearning.module.quiz.dto.request.SubmitQuizAttemptRequest;
import com.learnova.elearning.module.quiz.dto.response.QuestionTakingResponse;
import com.learnova.elearning.module.quiz.dto.response.QuizAttemptResponse;
import com.learnova.elearning.module.quiz.dto.response.QuizTakingResponse;
import com.learnova.elearning.module.quiz.entity.Quiz;
import com.learnova.elearning.module.quiz.entity.QuizAttempt;
import com.learnova.elearning.module.quiz.entity.QuizQuestion;
import com.learnova.elearning.module.quiz.entity.enums.QuestionType;
import com.learnova.elearning.module.quiz.repository.QuizAttemptRepository;
import com.learnova.elearning.module.quiz.repository.QuizQuestionRepository;
import com.learnova.elearning.module.quiz.repository.QuizRepository;
import com.learnova.elearning.module.user.entity.User;
import com.learnova.elearning.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service xử lý nghiệp vụ Làm bài kiểm tra & Chấm điểm tự động dành cho Học viên (US-20).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuizTakingService {

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    /**
     * 1. Lấy đề thi Quiz cho học viên làm bài.
     */
    @Transactional(readOnly = true)
    public QuizTakingResponse getQuizForTaking(Long courseId, Long lessonId, Long studentId) {
        // B1: Tìm Quiz theo lessonId và courseId
        Quiz quiz = quizRepository.findByLesson_IdAndLesson_Section_Course_Id(lessonId, courseId)
                .orElseThrow(() -> new AppException(ErrorCode.QUIZ_NOT_FOUND));

        // B2: Xác thực quyền: Học viên đã ghi danh HOẶC Giảng viên sở hữu khóa học
        boolean isLecturer = quiz.getLesson().getSection().getCourse().getLecturer().getId().equals(studentId);
        Optional<Enrollment> enrollmentOpt = enrollmentRepository.findByStudent_IdAndCourse_Id(studentId, courseId);
        if (enrollmentOpt.isEmpty() && !isLecturer) {
            throw new AppException(ErrorCode.ENROLLMENT_NOT_FOUND);
        }

        // B3: Truy vấn toàn bộ lịch sử các lần thi của học viên cho bài quiz này
        List<QuizAttempt> allAttempts = quizAttemptRepository
                .findByQuiz_IdAndLearner_IdOrderBySubmittedAtDesc(quiz.getId(), studentId);

        // B4: Tính số lượt đã làm trong ngày hôm nay (startOfDay)
        Instant startOfDay = LocalDate.now(VIETNAM_ZONE).atStartOfDay(VIETNAM_ZONE).toInstant();
        int attemptsUsedToday = (int) allAttempts.stream()
                .filter(a -> a.getSubmittedAt() != null && !a.getSubmittedAt().isBefore(startOfDay))
                .count();

        Integer attemptsRemaining = quiz.getMaxAttempts() != null
                ? Math.max(0, quiz.getMaxAttempts() - attemptsUsedToday)
                : null;

        // B5: Xác định Điểm cao nhất (highestScore) và Trạng thái đã từng đạt (hasPassed)
        BigDecimal highestScore = allAttempts.stream()
                .map(QuizAttempt::getScore)
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo)
                .orElse(null);

        boolean hasPassed = allAttempts.stream()
                .anyMatch(a -> Boolean.TRUE.equals(a.getIsPassed()));

        // B6: Truy vấn danh sách câu hỏi của Quiz
        List<QuizQuestion> questions = quizQuestionRepository.findByQuiz_IdOrderByPositionAsc(quiz.getId());

        // B7: Chuyển đổi sang QuestionTakingResponse — Bảo mật đề thi (BR-16)
        List<QuestionTakingResponse> questionResponses = questions.stream()
                .map(q -> {
                    List<QuizOptionDto> options = deserializeOptions(q.getOptionsJson());
                    List<QuestionTakingResponse.StudentOptionItem> studentOptions = options.stream()
                            .map(opt -> QuestionTakingResponse.StudentOptionItem.builder()
                                    .id(opt.getId())
                                    .text(opt.getText())
                                    .build())
                            .toList();

                    return QuestionTakingResponse.builder()
                            .id(q.getId())
                            .questionText(q.getQuestionText())
                            .questionType(q.getQuestionType())
                            .points(q.getPoints())
                            .position(q.getPosition())
                            .options(studentOptions)
                            .build();
                })
                .toList();

        return QuizTakingResponse.builder()
                .id(quiz.getId())
                .lessonId(quiz.getLesson().getId())
                .title(quiz.getTitle())
                .passingScore(quiz.getPassingScore())
                .maxAttempts(quiz.getMaxAttempts())
                .attemptsUsed(attemptsUsedToday)
                .attemptsRemaining(attemptsRemaining)
                .highestScore(highestScore)
                .hasPassed(hasPassed)
                .questions(questionResponses)
                .build();
    }

    /**
     * 2. Học viên nộp bài làm Quiz -> Server chấm điểm tự động & ghi nhận kết quả.
     */
    @Transactional
    public QuizAttemptResponse submitAttempt(Long courseId, Long lessonId, SubmitQuizAttemptRequest request, Long studentId) {
        // B1: Tìm Quiz theo lessonId
        Quiz quiz = quizRepository.findByLesson_IdAndLesson_Section_Course_Id(lessonId, courseId)
                .orElseThrow(() -> new AppException(ErrorCode.QUIZ_NOT_FOUND));

        // B2: Xác thực quyền: Học viên đã ghi danh HOẶC Giảng viên sở hữu khóa học
        boolean isLecturer = quiz.getLesson().getSection().getCourse().getLecturer().getId().equals(studentId);
        Optional<Enrollment> enrollmentOpt = enrollmentRepository.findByStudent_IdAndCourse_Id(studentId, courseId);
        if (enrollmentOpt.isEmpty() && !isLecturer) {
            throw new AppException(ErrorCode.ENROLLMENT_NOT_FOUND);
        }

        // B3: Kiểm tra giới hạn số lần làm bài theo ngày (Daily Attempt Window)
        Instant startOfDay = LocalDate.now(VIETNAM_ZONE).atStartOfDay(VIETNAM_ZONE).toInstant();
        int attemptsUsedToday = quizAttemptRepository
                .countByQuiz_IdAndLearner_IdAndSubmittedAtGreaterThanEqual(quiz.getId(), studentId, startOfDay);

        if (quiz.getMaxAttempts() != null && attemptsUsedToday >= quiz.getMaxAttempts()) {
            throw new AppException(ErrorCode.QUIZ_MAX_ATTEMPTS_REACHED);
        }

        // B4: Tải danh sách toàn bộ câu hỏi và đáp án đúng từ DB
        List<QuizQuestion> questions = quizQuestionRepository.findByQuiz_IdOrderByPositionAsc(quiz.getId());

        // Gom các câu trả lời của học viên theo questionId
        Map<Long, List<String>> userAnswersMap = request.getAnswers() != null
                ? request.getAnswers().stream()
                .filter(ans -> ans.getQuestionId() != null && ans.getSelectedOptionIds() != null)
                .collect(Collectors.toMap(
                        SubmitQuizAttemptRequest.QuestionAnswerItem::getQuestionId,
                        SubmitQuizAttemptRequest.QuestionAnswerItem::getSelectedOptionIds,
                        (existing, replacement) -> replacement
                ))
                : Map.of();

        // B5: Thuật toán chấm điểm tự động phía Server (Server-side Grading)
        BigDecimal totalEarnedPoints = BigDecimal.ZERO;
        BigDecimal totalMaxPoints = BigDecimal.ZERO;
        int correctQuestionsCount = 0;
        List<QuizAttemptResponse.QuestionResultItem> questionResults = new ArrayList<>();

        for (QuizQuestion question : questions) {
            List<QuizOptionDto> options = deserializeOptions(question.getOptionsJson());

            List<String> correctOptionIds = options.stream()
                    .filter(opt -> Boolean.TRUE.equals(opt.getIsCorrect()))
                    .map(QuizOptionDto::getId)
                    .toList();

            String explanation = options.stream()
                    .map(QuizOptionDto::getExplanation)
                    .filter(exp -> exp != null && !exp.isBlank())
                    .findFirst()
                    .orElse(null);

            List<String> userSelected = userAnswersMap.getOrDefault(question.getId(), List.of());
            Set<String> correctSet = new HashSet<>(correctOptionIds);
            Set<String> userSet = new HashSet<>(userSelected);

            boolean isCorrect;
            if (question.getQuestionType() == QuestionType.SINGLE_CHOICE) {
                isCorrect = (userSet.size() == 1 && userSet.equals(correctSet));
            } else {
                isCorrect = (!correctSet.isEmpty() && userSet.equals(correctSet));
            }

            BigDecimal questionPoints = question.getPoints() != null ? question.getPoints() : BigDecimal.ONE;
            totalMaxPoints = totalMaxPoints.add(questionPoints);

            BigDecimal earnedPoints = BigDecimal.ZERO;
            if (isCorrect) {
                earnedPoints = questionPoints;
                totalEarnedPoints = totalEarnedPoints.add(earnedPoints);
                correctQuestionsCount++;
            }

            questionResults.add(QuizAttemptResponse.QuestionResultItem.builder()
                    .questionId(question.getId())
                    .isCorrect(isCorrect)
                    .earnedPoints(earnedPoints)
                    .totalPoints(questionPoints)
                    .selectedOptionIds(userSelected)
                    .correctOptionIds(correctOptionIds)
                    .explanation(explanation)
                    .build());
        }

        // B6: Tính điểm tổng kết (%)
        BigDecimal score = BigDecimal.ZERO;
        if (totalMaxPoints.compareTo(BigDecimal.ZERO) > 0) {
            score = totalEarnedPoints.divide(totalMaxPoints, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        boolean isPassed = score.compareTo(quiz.getPassingScore()) >= 0;

        // B7: Khởi tạo và lưu thực thể QuizAttempt vào DB
        User learner = userRepository.findById(studentId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        String answersJson = serializeAnswers(request.getAnswers());

        QuizAttempt attempt = QuizAttempt.builder()
                .quiz(quiz)
                .learner(learner)
                .answersJson(answersJson)
                .score(score)
                .isPassed(isPassed)
                .submittedAt(Instant.now())
                .build();

        QuizAttempt savedAttempt = quizAttemptRepository.save(attempt);

        // B8: Xử lý tiến độ học tập (BR-29 & US-17) nếu học viên có bản ghi Enrollment
        if (isPassed && enrollmentOpt.isPresent()) {
            Enrollment enrollment = enrollmentOpt.get();
            Lesson lesson = quiz.getLesson();
            LessonProgress progress = lessonProgressRepository
                    .findByEnrollment_IdAndLesson_Id(enrollment.getId(), lesson.getId())
                    .orElseGet(() -> LessonProgress.builder()
                            .enrollment(enrollment)
                            .lesson(lesson)
                            .firstStartedAt(Instant.now())
                            .build());

            if (progress.getCompletedAt() == null) {
                progress.setCompletedAt(Instant.now());
                progress.setCompletionSource(CompletionSource.QUIZ);
                lessonProgressRepository.save(progress);
            }
            enrollmentRepository.recalculateCourseProgress(enrollment.getId(), courseId);
        }

        // B9: Trả về kết quả chi tiết cho học viên
        return QuizAttemptResponse.builder()
                .id(savedAttempt.getId())
                .quizId(quiz.getId())
                .score(score)
                .passingScore(quiz.getPassingScore())
                .isPassed(isPassed)
                .totalQuestions(questions.size())
                .correctQuestions(correctQuestionsCount)
                .submittedAt(savedAttempt.getSubmittedAt())
                .questionResults(questionResults)
                .build();
    }

    /**
     * 3. Lấy lịch sử các lần nộp bài của học viên đối với bài Quiz này.
     */
    @Transactional(readOnly = true)
    public List<QuizAttemptResponse> getAttemptHistory(Long courseId, Long lessonId, Long studentId) {
        Quiz quiz = quizRepository.findByLesson_IdAndLesson_Section_Course_Id(lessonId, courseId)
                .orElseThrow(() -> new AppException(ErrorCode.QUIZ_NOT_FOUND));

        boolean isLecturer = quiz.getLesson().getSection().getCourse().getLecturer().getId().equals(studentId);
        Optional<Enrollment> enrollmentOpt = enrollmentRepository.findByStudent_IdAndCourse_Id(studentId, courseId);
        if (enrollmentOpt.isEmpty() && !isLecturer) {
            throw new AppException(ErrorCode.ENROLLMENT_NOT_FOUND);
        }

        List<QuizAttempt> attempts = quizAttemptRepository
                .findByQuiz_IdAndLearner_IdOrderBySubmittedAtDesc(quiz.getId(), studentId);

        return attempts.stream()
                .map(a -> QuizAttemptResponse.builder()
                        .id(a.getId())
                        .quizId(quiz.getId())
                        .score(a.getScore())
                        .passingScore(quiz.getPassingScore())
                        .isPassed(a.getIsPassed())
                        .submittedAt(a.getSubmittedAt())
                        .build())
                .toList();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

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

    private String serializeAnswers(Object answers) {
        try {
            return objectMapper.writeValueAsString(answers != null ? answers : List.of());
        } catch (JsonProcessingException e) {
            log.error("Lỗi serialize answers: {}", e.getMessage(), e);
            return "[]";
        }
    }
}
