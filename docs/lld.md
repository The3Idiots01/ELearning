# Low-Level Design — Learnova

| | |
|---|---|
| **Project** | Learnova — Online Course Marketplace |
| **Version / Sprint** | 0.1 — Sprint 1 |
| **Last updated** | 20-08-2026 |


**Record of Changes**

| Date | Ver | Sprint | A/M/D | In charge | Change |
|------|-----|--------|-------|-----------|--------|
| | 0.1 | 1 | A | | Khởi tạo LLD: OAuth login + thiết kế trước 2 hard core |



---

## Mục đích & phạm vi

LLD đặc tả chi tiết các **luồng có rủi ro kỹ thuật cao hoặc tích hợp bên thứ ba**. Tài liệu này gồm:

- **Flow 1 — OAuth Login (Sprint 2):** .
- **Flow 2 — Protected Video Delivery (Sprint 3):** hard core #1.
- **Flow 3 — Knowledge Tracking (Sprint 3):** hard core #2.
- **Flow 4 — Learner Quiz Taking & Daily Attempt Window (Sprint 3):** US-20.


---

## Flow 1 — OAuth Login (Google / Facebook)



```mermaid
sequenceDiagram
    participant U as Learner
    participant FE as React SPA
    participant BE as Spring Boot API
    participant P as OAuth Provider

    U->>FE: Bấm "Đăng nhập với Google"
    FE->>BE: GET /oauth/authorize?provider=google
    BE-->>FE: Authorization URL + state
    FE->>P: Chuyển hướng tới trang consent (kèm state)
    P-->>FE: Redirect về kèm authorization code
    FE->>BE: GET /oauth/callback?code=..&state=..
    BE->>BE: Verify state (chống CSRF)
    BE->>P: Đổi code lấy access token (server-side)
    P-->>BE: Access token + hồ sơ: email, provider_id, verified
    BE->>BE: Phân giải theo (auth_provider, auth_provider_id) rồi email
    alt Tài khoản mới hoặc chưa chọn role
        BE-->>FE: Yêu cầu chọn vai trò Learner/Instructor
        FE->>BE: POST /oauth/role {role}
    end
    BE->>BE: Tạo/liên kết user, kiểm is_active, cấp JWT
    BE-->>FE: JWT + hồ sơ người dùng
    FE-->>U: Đăng nhập thành công
```


### Edge case & lỗi

- `state` không khớp → từ chối (nghi CSRF).
- Provider không trả email hoặc email chưa verified → **không** tự liên kết; yêu cầu xác thực thêm.
- Tài khoản bị Admin khóa (`is_active = false`, BR-21) → chặn đăng nhập kể cả qua OAuth.


---

## Flow 2 — Protected Video Delivery (signed URL + HTTP Range)


### Luồng cấp phát & phát video
`lessons.storage_key` chỉ trỏ tới object trong Object Storage — **không lưu URL public** (BR-12). URL chỉ được **ký tại thời điểm có request hợp lệ**, với thời hạn ngắn.

```mermaid
sequenceDiagram
    participant FE as React SPA
    participant BE as Spring Boot API
    participant ST as Object Storage / CDN

    FE->>BE: GET /lessons/{id}/play-url
    BE->>BE: Kiểm quyền — is_preview hoặc enrollment hiệu lực (không REVOKED)
    alt Không có quyền
        BE-->>FE: 403 Forbidden
    else Có quyền
        BE->>BE: Ký signed URL từ storage_key, TTL <= 15 phút
        BE-->>FE: signed URL
        FE->>ST: GET video, Range: bytes=start-
        ST-->>FE: 206 Partial Content
        Note over FE,ST: Seek = gửi Range mới, không tải lại toàn bộ
    end
    Note over FE,BE: URL sắp/đã hết hạn giữa chừng
    FE->>BE: Xin signed URL mới (chủ động hoặc khi gặp 403)
    BE->>BE: Kiểm quyền lại
    BE-->>FE: signed URL mới
    FE->>ST: Tiếp tục phát tại last_position (Range)
```


---

## Flow 3 — Knowledge Tracking (watched coverage bằng bitmap)

Làm trong sprint 2

---

## Flow 4 — Learner Quiz Taking & Daily Attempt Window (US-20)

### 1. Mục đích & Nguyên lý
Phục vụ học viên làm bài trắc nghiệm gắn với bài học (`Lesson.contentType = 'QUIZ'`). Hệ thống thực hiện:
- **Bảo mật đề thi (BR-16)**: Ẩn hoàn toàn cờ `isCorrect` và `explanation` khi cấp đề cho học viên.
- **Chấm điểm tự động tại Server**: Hỗ trợ câu hỏi đơn lựa chọn (`SINGLE_CHOICE`) và đa lựa chọn (`MULTIPLE_CHOICE`).
- **Gác số lần làm bài theo ngày (Daily Attempt Window)**: Tận dụng mốc thời gian `submitted_at >= startOfDay` để giới hạn số lần làm bài trong 1 ngày theo `quizzes.max_attempts`. Tự động khôi phục số lượt làm bài về ban đầu vào 00:00:00 ngày mới mà không cần cronjob.
- **Bảo toàn điểm cao nhất (Best Score Preservation)**: Học viên đã Pass vẫn có thể làm lại nhiều lần để ôn tập kiến thức. Hệ thống chọn lần có điểm số cao nhất (`highestScore = max(score)`) làm đại diện và không bao giờ hạ trạng thái Đạt của học viên.
- **Ghi nhận tiến độ khóa học (BR-29 & US-17)**: Khi `isPassed == true`, tạo/cập nhật `LessonProgress` với `completion_source = QUIZ` và tính lại `% hoàn thành khóa học`.

### 2. Sơ đồ tuần tự (Sequence Diagram)

```mermaid
sequenceDiagram
    autonumber
    actor U as Learner (Học viên)
    participant FE as React SPA (QuizTakingView)
    participant BE as Spring Boot (LearnerQuizController / QuizTakingService)
    participant DB as PostgreSQL (quizzes, attempts, lesson_progress)

    U->>FE: Mở bài học Quiz
    FE->>BE: GET /api/v1/learner/courses/{courseId}/lessons/{lessonId}/quiz
    BE->>DB: Kiểm tra Enrollment & Quiz tồn tại
    BE->>DB: Đếm attemptsUsedToday (submitted_at >= startOfDay)
    BE->>DB: Tính highestScore & hasPassed từ toàn bộ lịch sử attempts
    BE->>DB: Lấy đề thi (chuyển options_json sang StudentOptionItem, ẩn isCorrect)
    BE-->>FE: QuizTakingResponse (Đề thi sạch + attemptsRemaining + highestScore)

    alt Còn lượt làm bài trong ngày (hoặc max_attempts = null)
        FE-->>U: Hiển thị giao diện làm bài (Radio / Checkbox)
        U->>FE: Chọn phương án & bấm "Nộp bài"
        FE->>BE: POST /api/v1/learner/.../quiz/attempts {answers}
        BE->>DB: Kiểm tra lại attemptsUsedToday < maxAttempts
        BE->>BE: Server-side grading: đối chiếu selectedOptionIds với options_json
        BE->>BE: score = (earnedPoints / totalPoints) * 100, isPassed = score >= passingScore
        BE->>DB: INSERT INTO quiz_attempts (lưu snapshot bài nộp + điểm)
        opt isPassed == true
            BE->>DB: Cập nhật/tạo LessonProgress (completion_source = QUIZ)
            BE->>DB: recalculateCourseProgress (Enrollment)
        end
        BE-->>FE: QuizAttemptResponse (Điểm lần này, đáp án đúng, giải thích từng câu)
        FE-->>U: Hiển thị kết quả chi tiết & cập nhật Best Score trên UI
    else Đã hết lượt làm bài trong ngày
        FE-->>U: Khóa làm bài, thông báo số lượt sẽ được khôi phục vào ngày mai
    end
```

### 3. Edge Cases & Xử lý ngoại lệ
- Học viên chưa đăng ký khóa học (`Enrollment` không tồn tại) → ném lỗi `ENROLLMENT_NOT_FOUND` (404).
- Học viên cố tình gửi request nộp bài khi đã hết lượt trong ngày → ném lỗi `QUIZ_MAX_ATTEMPTS_REACHED` (400).
- Học viên nộp bài làm lại đạt điểm thấp hơn lần thi Đạt trước đó → điểm lần này vẫn ghi nhận, nhưng `LessonProgress` và `highestScore` được giữ nguyên vẹn ở mốc tốt nhất.

