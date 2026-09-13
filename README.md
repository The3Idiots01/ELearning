# Learnova — Nền Tảng Học Trực Tuyến & Khóa Học

> Hệ thống E-Learning xây dựng theo kiến trúc **Modular Monolith** với **Spring Boot 4 (Java 25)** ở Backend và **React 19 (TypeScript + Vite 8)** ở Frontend, tích hợp **Google Gemini AI** hỗ trợ giảng viên soạn bài trắc nghiệm tự động, gợi ý khóa học và kiểm duyệt ngôn từ thông minh.


---

## 🌟 Tổng quan hiện trạng hệ thống

Hệ thống hiện tại tập trung giải quyết trọn vẹn luồng trải nghiệm học tập và giảng dạy thực tế:
- **Học viên:** Duyệt xem khóa học, mua khóa học trực tiếp qua cổng thanh toán **PayOS** (quét mã VietQR), xem video bài giảng bảo mật (hỗ trợ tua video theo luồng dữ liệu HTTP 206), làm bài trắc nghiệm tính điểm tự động, tham gia hỏi đáp (Q&A) trong bài học và gửi đánh giá khóa học.
- **Giảng viên:** Quản lý khóa học, soạn thảo chương mục và bài giảng, tải lên video (tự động trích xuất độ dài video), **dùng AI (Gemini) tự động phân tích tài liệu PDF/DOCX để tạo bộ câu hỏi trắc nghiệm**, trả lời hỏi đáp của học viên và theo dõi doanh thu bán khóa học.
- **Bảo vệ nội dung:** Cơ chế **kiểm duyệt ngôn từ 2 lớp** (từ điển Aho-Corasick kết hợp Google Gemini AI) ngăn chặn từ ngữ thô tục hoặc không phù hợp trong Q&A và Đánh giá.

---

## 🛠 Công nghệ thực tế đang sử dụng

### 1. Backend (Spring Boot)
- **Ngôn ngữ & Framework:** Java 25, Spring Boot 4.1.0 (kiến trúc Modular Monolith).
- **Cơ sở dữ liệu & ORM:** PostgreSQL (kết nối Neon Serverless hoặc PostgreSQL cục bộ), Spring Data JPA / Hibernate, Flyway Database Migration.
- **Bảo mật & Xác thực:** Spring Security 6, JJWT (JSON Web Token), BCrypt Password Encoder. Xác thực Stateless với Access Token và Refresh Token lưu trữ qua Cookie HttpOnly.
- **Email Service:** SendGrid API phục vụ gửi link kích hoạt tài khoản đăng ký mới.
- **Trình bày kích hoạt tài khoản:** Thymeleaf Template (render trang xác nhận tài khoản thành công/thất bại).
- **Thanh toán trực tuyến:** PayOS Java SDK (`vn.payos:payos-java`), tạo đơn hàng VietQR và lắng nghe Webhook IPN tự động kích hoạt khóa học.
- **Trí tuệ nhân tạo (AI):** Google Gemini API (`gemini-3.5-flash-lite`):
  - Tự động sinh câu hỏi trắc nghiệm từ tài liệu giáo trình bài giảng.
  - Gợi ý khóa học theo sở thích (Course Recommendation).
  - Kiểm duyệt nội dung bình luận / câu hỏi (AI Moderation).
- **Xử lý tệp & Video:**
  - `Apache Tika 3.3.2`: Trích xuất nội dung văn bản từ các tệp tài liệu giảng dạy (PDF, DOCX).
  - `mp4parser (isoparser)`: Đọc metadata tệp MP4 (thời lượng phát) trực tiếp mà không cần cài đặt công cụ ngoài.
  - Hỗ trợ lưu trữ cục bộ (`./storage-data`) hoặc S3-compatible storage (AWS S3 / Cloudflare R2).
  - `ContentStreamController`: Phát video bài giảng qua giao thức `HTTP 206 Partial Content` (Range Header).
- **API Documentation:** SpringDoc OpenAPI 3 (`/swagger-ui.html`).

### 2. Frontend (React SPA)
- **Framework & Build Tool:** React 19, TypeScript, Vite 8.
- **Điều hướng (Routing):** React Router DOM v6 với cơ chế Route Guard phân tách luồng người dùng:
  - Khách / Chưa đăng nhập (`/login`, `/register`).
  - Học viên (`/courses`, `/courses/:id`, `/my-courses`, `/learning/:id`, `/payment-result`).
  - Giảng viên (`/instructor/courses`, `/instructor/courses/:id/settings`, `/instructor/courses/:id/curriculum`, `/instructor/courses/:id/qa`, `/instructor/revenue`).
- **Giao diện & Trải nghiệm:** Tailwind CSS, Material Symbols Icons, Video Player tùy biến theo luồng Range.

---

## 🚀 Các phân hệ & tính năng đã hoàn thiện

### 1. Xác thực & Hồ sơ cá nhân (Auth & Profile)
- **Đăng ký tài khoản:** Học viên/Giảng viên đăng ký với Email và Mật khẩu. Hệ thống gửi email chứa liên kết kích hoạt an toàn qua SendGrid.
- **Xác thực tài khoản:** Người dùng bấm liên kết trong email -> Backend render trang thông báo kích hoạt thành công (Thymeleaf) -> Chuyển hướng về trang đăng nhập.
- **Đăng nhập & Quản lý phiên:** Cấp Access Token (JWT) và Refresh Token tự động làm mới phiên khi hết hạn.
- **Hồ sơ:** Xem và cập nhật thông tin cá nhân.

### 2. Dành cho Học viên (Student Experience)
- **Danh mục khóa học (`/courses`):** Duyệt danh sách khóa học đã phát hành, tìm kiếm theo tên, lọc theo danh mục khóa học.
- **Chi tiết khóa học (`/courses/:courseId`):** Xem mô tả, danh sách chuẩn đầu ra, đề cương các chương bài học và các đánh giá sao từ học viên khác.
- **Mua khóa học qua PayOS:**
  - Bấm Mua ngay -> Hệ thống gọi PayOS tạo đơn hàng VietQR.
  - Học viên quét mã QR trên ứng dụng ngân hàng.
  - PayOS gửi Webhook xác nhận -> Backend tự động tạo bản ghi ghi danh (`enrollment`) và mở khóa học ngay lập tức.
  - Chuyển hướng về trang kết quả thanh toán (`/payment-result`).
- **Khóa học của tôi (`/my-courses`):** Danh sách các khóa học học viên đã mua và đang tham gia.
- **Không gian học tập (`/learning/:courseId`):**
  - Trình phát video bài giảng hỗ trợ tua mượt mà (`HTTP 206 Partial Content`).
  - Tự động lưu tiến độ học tập bài giảng qua heartbeat.
  - **Làm bài tập trắc nghiệm (Quiz):** Học viên làm bài trắc nghiệm của bài học, nộp bài và nhận điểm số + giải thích ngay lập tức.
  - **Hỏi đáp bài học (Q&A):** Đặt câu hỏi trong bài giảng, xem phản hồi từ giảng viên hoặc học viên khác.
  - **Đánh giá khóa học (Reviews):** Chấm điểm (1 đến 5 sao) và để lại bình luận nhận xét về khóa học.

### 3. Dành cho Giảng viên (Instructor Studio)
- **Quản lý khóa học (`/instructor/courses`):** Tạo mới khóa học, xem danh sách khóa học đang giảng dạy.
- **Cài đặt khóa học (`/instructor/courses/:id/settings`):**
  - Cập nhật thông tin cơ bản: Tiêu đề, mô tả, mức giá, ảnh bìa (thumbnail).
  - Quản lý chuẩn đầu ra (Learning Outcomes).
  - Kiểm tra điều kiện xuất bản (Publish Course).
- **Soạn thảo giáo trình (`/instructor/courses/:id/curriculum`):**
  - Tạo chương (Section) và bài học (Lesson).
  - Tải lên video bài giảng (hệ thống tự tính thời lượng video).
  - Tải lên tài liệu đính kèm.
  - **Tạo Quiz thủ công:** Tự thêm các câu hỏi trắc nghiệm, các lựa chọn và đáp án đúng.
  - **Tạo Quiz tự động bằng AI:** Giảng viên tải lên file giáo trình (PDF hoặc Word), hệ thống dùng Apache Tika trích xuất văn bản và chuyển tới Google Gemini AI để sinh bộ câu hỏi trắc nghiệm chuẩn xác.
- **Hỏi đáp giảng viên (`/instructor/courses/:id/qa`):** Theo dõi toàn bộ thắc mắc của học viên theo từng khóa học và gửi câu trả lời.
- **Báo cáo doanh thu (`/instructor/revenue`):** Thống kê tổng doanh thu bán khóa học và danh sách giao dịch đã thanh toán thành công.

### 4. Gợi ý khóa học thông minh (AI Recommendation)
- Dựa trên dữ liệu khóa học và nhu cầu học viên, Gemini AI phân tích để đưa ra danh sách các khóa học gợi ý phù hợp trên giao diện học viên.

---

## 📁 Cấu trúc mã nguồn

```text
ELearning/
├── ELearningBE/                             # Ứng dụng Backend Spring Boot (Java 25)
│   ├── src/main/java/com/learnova/elearning/
│   │   ├── common/                          # Xử lý ngoại lệ toàn cục, ApiResponse, ErrorCode, SecurityConfig
│   │   ├── config/                          # Cấu hình PayOS
│   │   ├── integration/
│   │   │   ├── ai/                          # GeminiClient, AiProperties, gọi Gemini REST API
│   │   │   └── storage/                     # LocalStorageController, S3/Storage properties
│   │   ├── security/                        # JwtAuthenticationFilter, JwtTokenProvider, CustomUserDetails
│   │   └── module/                          # Các module nghiệp vụ
│   │       ├── auth/                        # Đăng ký, Đăng nhập, Xác thực email (Thymeleaf)
│   │       ├── category/                    # Danh mục khóa học
│   │       ├── course/                      # Quản lý khóa học, giáo trình, bài học, upload video, AI authoring
│   │       ├── delivery/                    # Streaming video Range 206, cấp vé xem video
│   │       ├── enrollment/                  # Quản lý ghi danh khóa học của học viên
│   │       ├── payment/                     # Tích hợp cổng thanh toán PayOS & Webhook xử lý đơn hàng
│   │       ├── qa/                          # Hỏi đáp bài học & kiểm duyệt nội dung
│   │       ├── quiz/                        # Quản lý bài tập trắc nghiệm & AI Quiz Generator
│   │       ├── recommendation/              # Gợi ý khóa học bằng Gemini AI
│   │       ├── revenue/                     # Báo cáo doanh thu cho giảng viên
│   │       ├── review/                      # Đánh giá khóa học & kiểm duyệt nội dung 2 lớp
│   │       ├── tracking/                    # Theo dõi tiến độ xem bài học
│   │       └── user/                        # Quản lý thông tin tài khoản người dùng
│   ├── src/main/resources/
│   │   ├── db/migration/                    # Flyway SQL migrations
│   │   ├── templates/                       # Giao diện xác thực tài khoản qua email (Thymeleaf)
│   │   └── application.properties           # File cấu hình Spring Boot
│   ├── .env.example                         # Mẫu khai báo biến môi trường
│   └── pom.xml                              # Danh sách thư viện Maven
│
├── ELearningFE/                             # Ứng dụng Frontend React (Vite + TypeScript)
│   ├── src/
│   │   ├── app/
│   │   │   ├── context/AuthContext.tsx      # Quản lý trạng thái xác thực và phiên làm việc
│   │   │   └── routes/                      # Route components cho Học viên và Giảng viên
│   │   ├── components/                      # UI components dùng chung (Header, Footer, Navbar...)
│   │   ├── features/                        # Giao diện theo từng tính năng
│   │   │   ├── auth/                        # Form đăng nhập, đăng ký
│   │   │   ├── category/                    # Bộ lọc danh mục
│   │   │   ├── course/                      # Chi tiết khóa học, Learning Workspace, Quiz, Q&A, Review Modal
│   │   │   ├── payment/                     # Trang xác nhận kết quả thanh toán PayOS
│   │   │   ├── recommendation/              # Khối gợi ý khóa học AI
│   │   │   └── revenue/                     # Bảng điều khiển doanh thu giảng viên
│   │   ├── lib/api/                         # Các hàm gọi REST API (auth, course, payment, review, qa, quiz...)
│   │   └── types/                           # Định nghĩa kiểu dữ liệu TypeScript
│   ├── package.json
│   └── vite.config.ts
│
└── README.md                                # Hướng dẫn dự án
```

---

## ⚙️ Cấu hình môi trường (.env)

Tạo file `.env` tại thư mục `ELearningBE/` (hoặc cấu hình trực tiếp trên máy):

```properties
# 1. Cơ sở dữ liệu (PostgreSQL / Neon)
DB_URL=jdbc:postgresql://<neon_host>/neondb?sslmode=require&channelBinding=require
DB_USERNAME=neondb_owner
DB_PASSWORD=your_db_password

# 2. Redis Cloud
REDIS_HOST=your_redis_host
REDIS_PORT=19655
REDIS_USERNAME=default
REDIS_PASSWORD=your_redis_password

# 3. Bảo mật JWT
JWT_SECRET=your_super_secret_jwt_key_at_least_256_bits_long
JWT_ACCESS_TOKEN_EXPIRATION_MS=86400000
JWT_REFRESH_TOKEN_EXPIRATION_MS=604800000

# 4. Gửi Email (SendGrid)
SENDGRID_API_KEY=your_sendgrid_api_key
SENDGRID_FROM_EMAIL=your_verified_sender_email@gmail.com

# 5. Lưu trữ Video / Tài liệu (Mặc định local lưu tại ./storage-data)
STORAGE_PROVIDER=local

# 6. Cổng thanh toán PayOS
PAYOS_CLIENT_ID=your_payos_client_id
PAYOS_API_KEY=your_payos_api_key
PAYOS_CHECKSUM_KEY=your_payos_checksum_key

# 7. Tích hợp Google Gemini AI
GEMINI_API_KEY=your_gemini_api_key
GEMINI_MODEL=gemini-3.5-flash-lite
AI_ENABLED=true
```

---

## 🛠 Hướng dẫn cài đặt & Khởi chạy

### Yêu cầu môi trường
- **JDK 25**
- **Node.js 18+** & **npm**
- Cơ sở dữ liệu PostgreSQL (hoặc tài khoản Neon PostgreSQL)

### 1. Khởi chạy Backend (Spring Boot)
1. Mở terminal tại thư mục `ELearningBE`:
   ```powershell
   cd ELearningBE
   ```
2. Đảm bảo file `ELearningBE/.env` đã có đầy đủ thông tin kết nối DB và các khóa API.
3. Chạy ứng dụng:
   - **Windows:**
     ```powershell
     .\mvnw.cmd spring-boot:run
     ```
   - **macOS / Linux:**
     ```bash
     ./mvnw spring-boot:run
     ```
4. Backend khởi động tại: `http://localhost:8080`.
   - Xem tài liệu API Swagger: `http://localhost:8080/swagger-ui.html`

### 2. Khởi chạy Frontend (React + Vite)
1. Mở terminal tại thư mục `ELearningFE`:
   ```powershell
   cd ELearningFE
   ```
2. Cài đặt các gói phụ thuộc:
   ```bash
   npm install
   ```
3. Chạy giao diện phát triển:
   ```bash
   npm run dev
   ```
4. Truy cập giao diện ứng dụng tại: `http://localhost:5173`.

---
© 2026 Learnova Project. All rights reserved.
