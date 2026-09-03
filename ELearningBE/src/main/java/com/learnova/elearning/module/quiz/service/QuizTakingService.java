package com.learnova.elearning.module.quiz.service;

import com.learnova.elearning.module.enrollment.repository.EnrollmentRepository;
import com.learnova.elearning.module.enrollment.repository.LessonProgressRepository;
import com.learnova.elearning.module.quiz.dto.request.SubmitQuizAttemptRequest;
import com.learnova.elearning.module.quiz.dto.response.QuizAttemptResponse;
import com.learnova.elearning.module.quiz.dto.response.QuizTakingResponse;
import com.learnova.elearning.module.quiz.repository.QuizAttemptRepository;
import com.learnova.elearning.module.quiz.repository.QuizQuestionRepository;
import com.learnova.elearning.module.quiz.repository.QuizRepository;
import com.learnova.elearning.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service xử lý nghiệp vụ Làm bài kiểm tra & Chấm điểm tự động dành cho Học viên (US-20).
 *
 * LƯU Ý: Hiện tại chỉ định nghĩa chữ ký hàm và mô tả workflow nghiệp vụ bằng comment.
 * Chưa lập trình logic thực thi bên trong theo yêu cầu của dự án.
 */
@Service
@RequiredArgsConstructor
public class QuizTakingService {

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final UserRepository userRepository;

    /**
     * 1. Lấy đề thi Quiz cho học viên làm bài.
     *
     * WORKFLOW:
     * - B1: Kiểm tra học viên đã đăng ký khóa học này chưa (enrollmentRepository.findByStudent_IdAndCourse_Id).
     *       + Nếu chưa đăng ký: throw AppException(ErrorCode.ENROLLMENT_NOT_FOUND) hoặc UNAUTHORIZED.
     * - B2: Tìm Quiz theo lessonId và courseId (nếu không có -> throw QUIZ_NOT_FOUND).
     * - B3: Đếm số lần học viên đã nộp bài:
     *       + attemptsUsed = quizAttemptRepository.countByQuiz_IdAndLearner_Id(quizId, studentId).
     *       + attemptsRemaining = maxAttempts != null ? max(0, maxAttempts - attemptsUsed) : null.
     * - B4: Kiểm tra học viên đã từng có bài thi ĐẠT (isPassed == true) hay chưa.
     * - B5: Truy vấn danh sách câu hỏi của Quiz từ quizQuestionRepository.
     * - B6: Chuyển đổi sang QuestionTakingResponse:
     *       + [QUAN TRỌNG - BR-16]: Bóc tách options_json sang List<StudentOptionItem> CHỈ gồm {id, text}.
     *       + Tuyệt đối KHÔNG trả về cờ isCorrect hay explanation trong response này để bảo mật đề thi.
     * - B7: Đóng gói toàn bộ thông tin vào QuizTakingResponse và trả về cho client.
     */
    @Transactional(readOnly = true)
    public QuizTakingResponse getQuizForTaking(Long courseId, Long lessonId, Long studentId) {
        // [WORKFLOW] Chưa triển khai logic - Xem workflow mô tả chi tiết phía trên
        throw new UnsupportedOperationException("Chưa triển khai logic: getQuizForTaking. Xem workflow mô tả trong comment.");
    }

    /**
     * 2. Học viên nộp bài làm Quiz -> Server chấm điểm tự động & ghi nhận kết quả.
     *
     * WORKFLOW:
     * - B1: Kiểm tra Enrollment của học viên trong Course.
     * - B2: Tìm Quiz theo lessonId.
     * - B3: Kiểm tra giới hạn số lần làm bài (maxAttempts):
     *       + Lấy attemptsUsed hiện tại.
     *       + Nếu maxAttempts != null && attemptsUsed >= maxAttempts -> throw QUIZ_MAX_ATTEMPTS_REACHED.
     * - B4: Tải danh sách toàn bộ câu hỏi và đáp án đúng từ DB qua quizQuestionRepository.
     * - B5: Thuật toán chấm điểm tự động phía Server (Server-side Grading):
     *       + Khởi tạo totalEarnedPoints = 0, correctQuestionsCount = 0.
     *       + Với mỗi câu hỏi trong đề:
     *         * Lấy tập hợp đáp án đúng từ options_json (correctOptionIds).
     *         * Lấy tập hợp đáp án học viên đã chọn trong request (selectedOptionIds).
     *         * Kiểm tra:
     *           - Nếu questionType == SINGLE_CHOICE: đúng khi selectedOptionIds có 1 phần tử và khớp đáp án đúng.
     *           - Nếu questionType == MULTIPLE_CHOICE: đúng khi tập hợp lựa chọn khớp hoàn toàn với đáp án đúng.
     *         * Nếu đúng: cộng điểm points của câu hỏi vào totalEarnedPoints, tăng correctQuestionsCount.
     *         * Tạo QuestionResultItem ghi lại kết quả chi tiết từng câu (đáp án của trò, đáp án đúng, giải thích).
     * - B6: Tính điểm tổng kết:
     *       + scorePercentage = (totalEarnedPoints / totalMaxPoints) * 100 (làm tròn 2 chữ số thập phân).
     *       + isPassed = scorePercentage >= quiz.getPassingScore().
     * - B7: Khởi tạo và lưu thực thể QuizAttempt vào DB (lưu snapshot bài làm + điểm số).
     * - B8: Xử lý tiến độ học tập (BR-29):
     *       + Nếu isPassed == true:
     *         * Kiểm tra xem đã có bản ghi trong lesson_progress cho lesson này chưa.
     *         * Nếu chưa có: tạo mới bản ghi LessonProgress với completed_at = now() để đánh dấu hoàn thành lesson.
     *         * Kích hoạt cập nhật lại tỷ lệ hoàn thành khóa học trên bảng enrollments (progress %).
     * - B9: Map kết quả chấm thi sang QuizAttemptResponse và trả về cho học viên xem điểm.
     */
    @Transactional
    public QuizAttemptResponse submitAttempt(Long courseId, Long lessonId, SubmitQuizAttemptRequest request, Long studentId) {
        // [WORKFLOW] Chưa triển khai logic - Xem workflow mô tả chi tiết phía trên
        throw new UnsupportedOperationException("Chưa triển khai logic: submitAttempt. Xem workflow mô tả trong comment.");
    }

    /**
     * 3. Lấy lịch sử các lần nộp bài của học viên đối với bài Quiz này.
     *
     * WORKFLOW:
     * - B1: Kiểm tra Enrollment của học viên.
     * - B2: Tìm Quiz theo lessonId.
     * - B3: Truy vấn danh sách QuizAttempt của học viên theo quizId, sắp xếp theo submittedAt giảm dần.
     * - B4: Map danh sách entity QuizAttempt sang List<QuizAttemptResponse> (tóm tắt điểm số, trạng thái Đạt/Trượt, thời gian nộp).
     * - B5: Trả về danh sách lịch sử cho học viên.
     */
    @Transactional(readOnly = true)
    public List<QuizAttemptResponse> getAttemptHistory(Long courseId, Long lessonId, Long studentId) {
        // [WORKFLOW] Chưa triển khai logic - Xem workflow mô tả chi tiết phía trên
        throw new UnsupportedOperationException("Chưa triển khai logic: getAttemptHistory. Xem workflow mô tả trong comment.");
    }
}
