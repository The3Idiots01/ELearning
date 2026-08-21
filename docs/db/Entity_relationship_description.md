# Mô tả Entity & Relationship — Hệ thống LMS

Sơ đồ gồm **22 entity (bảng)**, xoay quanh 4 nhóm nghiệp vụ chính: **Người dùng & Khóa học**, **Nội dung học tập**, **Học tập & Đánh giá**, **Giao dịch & Thanh toán**. Bên dưới là mô tả từng entity chính và các mối quan hệ (relationship) giữa chúng.

---

## 1. Nhóm Người dùng & Khóa học

### `users`
Bảng trung tâm, lưu thông tin mọi người dùng trong hệ thống (cả **learner** lẫn **lecturer**, phân biệt qua cột `role`).
- Thuộc tính chính: `id (PK)`, `full_name`, `email`, `password_hash`, `auth_provider`, `role`, `bio`, `interests`, `expertise`, `is_active`, `created_at`.

### `categories`
Danh mục phân loại khóa học.
- Thuộc tính: `id (PK)`, `name`, `slug`.

### `courses`
Khóa học do giảng viên (`lecturer_id`) tạo, thuộc một danh mục (`category_id`).
- Thuộc tính: `id (PK)`, `lecturer_id`, `category_id`, `title`, `slug`, `description`, `price`, `status`, `rating_avg`, `total_students`, `published_at`...

### `course_status_logs`
Nhật ký thay đổi trạng thái của khóa học (ví dụ: nháp → chờ duyệt → công khai), ghi lại người thực hiện (`actor_id`).
- Thuộc tính: `id (PK)`, `course_id`, `actor_id`, `from_status`, `to_status`, `comment`.

**Quan hệ:**
| Quan hệ | Từ | Đến | Bản chất |
|---|---|---|---|
| teaches | `courses.lecturer_id` | `users.id` | Một user (lecturer) dạy nhiều courses (1–n) |
| categorizes | `courses.category_id` | `categories.id` | Một category chứa nhiều courses (1–n) |
| has | `course_status_logs.course_id` | `courses.id` | Một course có nhiều log trạng thái (1–n) |
| logs | `course_status_logs.actor_id` | `users.id` | Một user thực hiện nhiều thao tác đổi trạng thái (1–n) |

---

## 2. Nhóm Nội dung học tập

### `course_sections`
Chương/phần trong một khóa học, có thứ tự hiển thị (`position`).

### `lessons`
Bài học thuộc một section, có nhiều dạng nội dung (`content_type`, `content_url`/`content_text`), có thể xem trước (`is_preview`).

### `quizzes`
Bài kiểm tra gắn với một lesson, có điểm đạt (`passing_score`) và số lần làm tối đa.

### `quiz_questions`
Câu hỏi trong một quiz, gồm loại câu hỏi, đáp án (`options_json`), điểm số.

### `lesson_summaries`
Bản tóm tắt nội dung bài học (có thể do AI sinh ra), gắn với 1 lesson.

### `flashcards`
Thẻ ghi nhớ (câu hỏi – đáp án) sinh ra từ nội dung 1 lesson.

**Quan hệ:**
| Quan hệ | Từ | Đến | Bản chất |
|---|---|---|---|
| has | `course_sections.course_id` | `courses.id` | Một course có nhiều sections (1–n) |
| has | `lessons.section_id` | `course_sections.id` | Một section có nhiều lessons (1–n) |
| has | `quizzes.lesson_id` | `lessons.id` | Một lesson có thể có nhiều quizzes (1–n) |
| has | `quiz_questions.quiz_id` | `quizzes.id` | Một quiz có nhiều câu hỏi (1–n) |
| has | `lesson_summaries.lesson_id` | `lessons.id` | Một lesson có nhiều bản tóm tắt (1–n) |
| has | `flashcards.lesson_id` | `lessons.id` | Một lesson có nhiều flashcards (1–n) |

---

## 3. Nhóm Học tập & Tương tác

### `enrollments`
Ghi danh của một learner (`learner_id`) vào một course, theo dõi tiến độ tổng thể (`progress_percent`, `last_lesson_id`, `completed_at`).

### `lesson_progress`
Tiến độ học chi tiết của một enrollment ở từng lesson cụ thể (vị trí xem dở, trạng thái hoàn thành).

### `quiz_attempts`
Lượt làm bài quiz của một learner, lưu đáp án (`answers_json`) và điểm số.

### `certificates`
Chứng chỉ được cấp khi một enrollment hoàn thành khóa học.

### `qna_posts`
Bài đăng hỏi-đáp trong một course/lesson, có thể là bình luận trả lời (`parent_id` tự tham chiếu).

### `reviews`
Đánh giá (rating + comment) của learner cho một course.

### `course_recommendations`
Gợi ý khóa học cho learner (có điểm số `score` và lý do `reason`, thường do hệ thống gợi ý sinh ra).

**Quan hệ:**
| Quan hệ | Từ | Đến | Bản chất |
|---|---|---|---|
| enrolls | `enrollments.learner_id` | `users.id` | Một learner ghi danh nhiều courses (1–n) |
| has | `enrollments.course_id` | `courses.id` | Một course có nhiều lượt ghi danh (1–n) |
| resumes | `enrollments.last_lesson_id` | `lessons.id` | Enrollment lưu vị trí bài học đang học dở |
| tracks | `lesson_progress.enrollment_id` | `enrollments.id` | Một enrollment có nhiều bản ghi tiến độ theo lesson (1–n) |
| has | `lesson_progress.lesson_id` | `lessons.id` | Một lesson có nhiều bản ghi tiến độ từ nhiều learner (1–n) |
| issues | `certificates.enrollment_id` | `enrollments.id` | Một enrollment cấp tối đa 1 chứng chỉ (1–1/1–n) |
| attempts | `quiz_attempts.learner_id` | `users.id` | Một learner có nhiều lượt làm quiz (1–n) |
| has | `quiz_attempts.quiz_id` | `quizzes.id` | Một quiz có nhiều lượt làm bài (1–n) |
| has / writes | `qna_posts.course_id`, `qna_posts.lesson_id`, `qna_posts.user_id` | `courses.id`, `lessons.id`, `users.id` | Một bài Q&A gắn với 1 course, 1 lesson và 1 user viết |
| replies | `qna_posts.parent_id` | `qna_posts.id` | Tự tham chiếu — một bài đăng có nhiều bài trả lời (1–n) |
| has / writes | `reviews.course_id`, `reviews.learner_id` | `courses.id`, `users.id` | Một course nhận nhiều reviews; một learner viết nhiều reviews |
| gets / suggests | `course_recommendations.learner_id`, `.course_id` | `users.id`, `courses.id` | Một learner nhận nhiều gợi ý khóa học |

---

## 4. Nhóm Giao dịch & Thanh toán

### `shopping_cart`
Giỏ hàng — các course mà learner đã thêm nhưng chưa thanh toán.

### `wishlist`
Danh sách khóa học yêu thích của learner.

### `payments`
Giao dịch thanh toán của learner (số tiền, phương thức, trạng thái, mã giao dịch).

### `payment_items`
Chi tiết từng course trong một payment (hỗ trợ mua nhiều course/lần), kèm doanh thu chia cho giảng viên (`lecturer_revenue`).

### `payouts`
Khoản chi trả doanh thu cho giảng viên (`lecturer_id`), gồm thông tin ngân hàng nhận tiền.

**Quan hệ:**
| Quan hệ | Từ | Đến | Bản chất |
|---|---|---|---|
| has / in | `shopping_cart.learner_id`, `.course_id` | `users.id`, `courses.id` | Một learner có nhiều course trong giỏ hàng |
| has / in | `wishlist.learner_id`, `.course_id` | `users.id`, `courses.id` | Một learner có nhiều course trong wishlist |
| makes | `payments.learner_id` | `users.id` | Một learner thực hiện nhiều lượt thanh toán (1–n) |
| has | `payment_items.payment_id` | `payments.id` | Một payment gồm nhiều payment_items (1–n, hỗ trợ mua combo) |
| sold_as | `payment_items.course_id` | `courses.id` | Một course xuất hiện trong nhiều payment_items (1–n) |
| receives | `payouts.lecturer_id` | `users.id` | Một lecturer nhận nhiều đợt payout (1–n) |

---

## 5. Tổng kết mối quan hệ cốt lõi

- **`users`** là entity trung tâm nhất: vừa là **lecturer** (tạo `courses`, nhận `payouts`), vừa là **learner** (tạo `enrollments`, `payments`, `reviews`, `qna_posts`, `quiz_attempts`, `shopping_cart`, `wishlist`, `course_recommendations`).
- **`courses`** là entity thứ hai quan trọng nhất, là gốc của nhánh nội dung (`course_sections` → `lessons` → `quizzes`/`lesson_summaries`/`flashcards`) và nhánh giao dịch/tương tác (`enrollments`, `reviews`, `qna_posts`, `payment_items`, `shopping_cart`, `wishlist`).

## 6. Database Schema
```mermaid
erDiagram
    users {
        bigint id PK
        varchar(150) full_name
        varchar(150) email UK
        varchar(255) password_hash
        varchar(20) auth_provider
        varchar(255) auth_provider_id
        varchar(500) avatar_url
        user_role role
        text bio
        jsonb interests
        varchar(255) expertise
        boolean is_active
        timestamptz created_at
    }
    categories {
        bigint id PK
        varchar(150) name
        varchar(150) slug UK
    }
    courses {
        bigint id PK
        bigint lecturer_id FK
        bigint category_id FK
        varchar(255) title
        varchar(255) slug UK
        text description
        varchar(500) thumbnail_url
        course_level level
        decimal(12,2) price
        course_status status
        decimal(3,2) rating_avg
        int total_students
        timestamptz created_at
        timestamptz published_at
    }
    course_status_logs {
        bigint id PK
        bigint course_id FK
        bigint actor_id FK
        course_status from_status
        course_status to_status
        text comment
        timestamptz created_at
    }
    course_sections {
        bigint id PK
        bigint course_id FK
        varchar(255) title
        int position
    }
    lessons {
        bigint id PK
        bigint section_id FK
        varchar(255) title
        lesson_content_type content_type
        varchar(500) storage_key
        text content_text
        int duration_seconds
        boolean is_preview
        int position
    }
    quizzes {
        bigint id PK
        bigint lesson_id FK
        varchar(255) title
        decimal(5,2) passing_score
        int max_attempts
    }
    quiz_questions {
        bigint id PK
        bigint quiz_id FK
        text question_text
        question_type question_type
        jsonb options_json
        decimal(5,2) points
        int position
    }
    quiz_attempts {
        bigint id PK
        bigint quiz_id FK
        bigint learner_id FK
        jsonb answers_json
        decimal(6,2) score
        timestamptz submitted_at
    }
    enrollments {
        bigint id PK
        bigint learner_id FK
        bigint course_id FK
        bigint payment_item_id FK,UK
        enrollment_status status
        decimal(5,2) progress_percent
        bigint last_lesson_id FK
        timestamptz last_accessed_at
        timestamptz enrolled_at
        timestamptz completed_at
    }
    lesson_progress {
        bigint id PK
        bigint enrollment_id FK
        bigint lesson_id FK
        lesson_progress_status status
        int last_position_seconds
        bytea watched_bitmap
        int watched_seconds
        timestamptz updated_at
        timestamptz completed_at
    }
    certificates {
        bigint id PK
        bigint enrollment_id FK
        varchar(50) certificate_code UK
        varchar(150) learner_name
        varchar(255) course_title
        varchar(500) certificate_url
        timestamptz issued_at
    }
    qna_posts {
        bigint id PK
        bigint course_id FK
        bigint lesson_id FK
        bigint user_id FK
        bigint parent_id FK
        text content
        boolean is_resolved
        timestamptz created_at
    }
    reviews {
        bigint id PK
        bigint course_id FK
        bigint learner_id FK
        int rating
        text comment
        timestamptz created_at
    }
    shopping_cart {
        bigint id PK
        bigint learner_id FK
        bigint course_id FK
        timestamptz created_at
    }
    wishlist {
        bigint id PK
        bigint learner_id FK
        bigint course_id FK
        timestamptz created_at
    }
    payments {
        bigint id PK
        varchar(50) order_code UK
        bigint learner_id FK
        decimal(12,2) amount
        payment_method_type payment_method
        varchar(150) transaction_id
        payment_status status
        jsonb gateway_response
        timestamptz paid_at
        timestamptz expires_at
        timestamptz created_at
        timestamptz updated_at
    }
    payment_items {
        bigint id PK
        bigint payment_id FK
        bigint course_id FK
        decimal(12,2) price
        decimal(12,2) lecturer_revenue
    }
    payouts {
        bigint id PK
        bigint lecturer_id FK
        decimal(12,2) amount
        varchar(100) bank_name
        varchar(50) account_number
        varchar(150) account_holder
        payout_status status
        timestamptz created_at
        timestamptz processed_at
    }
    course_recommendations {
        bigint id PK
        bigint learner_id FK
        bigint course_id FK
        decimal(5,4) score
        text reason
        timestamptz generated_at
    }
    lesson_summaries {
        bigint id PK
        bigint lesson_id FK
        text summary_text
        timestamptz generated_at
    }
    flashcards {
        bigint id PK
        bigint lesson_id FK
        text question
        text answer
        flashcard_source source
    }
    categories ||--o{ courses : "category_id"
    users ||--o{ courses : "lecturer_id"
    courses ||--o{ course_status_logs : "course_id"
    users ||--o{ course_status_logs : "actor_id"
    courses ||--o{ course_sections : "course_id"
    course_sections ||--o{ lessons : "section_id"
    lessons ||--o{ quizzes : "lesson_id"
    quizzes ||--o{ quiz_questions : "quiz_id"
    quizzes ||--o{ quiz_attempts : "quiz_id"
    users ||--o{ quiz_attempts : "learner_id"
    users ||--o{ enrollments : "learner_id"
    courses ||--o{ enrollments : "course_id"
    payment_items ||--o| enrollments : "payment_item_id"
    lessons ||--o{ enrollments : "last_lesson_id"
    enrollments ||--o{ lesson_progress : "enrollment_id"
    lessons ||--o{ lesson_progress : "lesson_id"
    enrollments ||--o| certificates : "enrollment_id"
    courses ||--o{ qna_posts : "course_id"
    lessons ||--o{ qna_posts : "lesson_id"
    users ||--o{ qna_posts : "user_id"
    qna_posts ||--o{ qna_posts : "parent_id"
    courses ||--o{ reviews : "course_id"
    users ||--o{ reviews : "learner_id"
    users ||--o{ shopping_cart : "learner_id"
    courses ||--o{ shopping_cart : "course_id"
    users ||--o{ wishlist : "learner_id"
    courses ||--o{ wishlist : "course_id"
    users ||--o{ payments : "learner_id"
    payments ||--o{ payment_items : "payment_id"
    courses ||--o{ payment_items : "course_id"
    users ||--o{ payouts : "lecturer_id"
    users ||--o{ course_recommendations : "learner_id"
    courses ||--o{ course_recommendations : "course_id"
    lessons ||--o{ lesson_summaries : "lesson_id"
    lessons ||--o{ flashcards : "lesson_id"
```