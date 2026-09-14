# Software Requirements Specification — Learnova

| | |
|---|---|
| **Project** | Learnova — Nền tảng học trực tuyến và quản lý khóa học |
| **Version / Baseline** | 0.3 — Đối chiếu với code hiện tại |
| **Last updated** | 14-09-2026 |

**Record of Changes**

| Date | Ver | Baseline | A/M/D | In charge | Change |
|------|-----|----------|-------|-----------|--------|
| 20-08-2026 | 0.1 | Sprint 1 | A | TienPQ9 | Khởi tạo SRS, đặc tả Content Authoring |
| 27-08-2026 | 0.2 | Sprint 2 | M | TienPQ9 | Cập nhật theo backlog và phạm vi Sprint 1–2 |
| 11-09-2026 | 0.3 | Code hiện tại | M | TienPQ9 | Đối chiếu lại yêu cầu với backend Spring Boot và frontend React; cập nhật PayOS, backward design, AI authoring, publication changes, moderation và loại bỏ luồng chưa có trong code |

---

## 1. Scope & Glossary

### 1.1 Scope

Tài liệu này mô tả hành vi của phiên bản Learnova hiện có trong repository, dựa trên API backend, các màn hình frontend và cấu hình ứng dụng. Các trạng thái hoặc tích hợp chỉ được khai báo trong schema/config nhưng chưa có luồng sử dụng hoàn chỉnh không được xem là chức năng đã triển khai.

**In scope:**
- **Auth & Profile:** đăng ký bằng email/mật khẩu, kích hoạt tài khoản qua email, đăng nhập/đăng xuất, làm mới phiên bằng JWT và xem/cập nhật hồ sơ cá nhân.
- **Course Authoring:** giảng viên tạo và cập nhật khóa học, quản lý giá, ảnh bìa, mô tả, chuẩn đầu ra, section, lesson, nội dung bài học, tài liệu bổ trợ và lesson preview.
- **Backward Design:** giảng viên tạo chuẩn đầu ra, tạo và gắn đánh giá quiz vào section, gắn lesson/assessment với chuẩn đầu ra, xem ma trận căn chỉnh và kiểm tra điều kiện xuất bản.
- **AI Authoring:** yêu cầu AI gợi ý chuẩn đầu ra, tạo bản nháp curriculum và tạo bản nháp quiz; giảng viên xem/chỉnh sửa và chủ động áp dụng nội dung được sinh.
- **Quiz & Assessment:** giảng viên soạn quiz trắc nghiệm thủ công hoặc từ tài liệu; học viên làm quiz, xem điểm, giải thích và lịch sử lượt làm.
- **Instructor Management:** xem và quản lý khóa học của mình, xuất bản/gỡ khóa học, xuất bản thay đổi của khóa học đang hoạt động, xem lịch sử trạng thái, Q&A, đánh giá và doanh thu.
- **Course Discovery:** duyệt danh mục, tìm kiếm/lọc khóa học đã xuất bản, xem chi tiết, chuẩn đầu ra, curriculum, đánh giá và lesson preview.
- **Payment & Enrollment:** tạo checkout PayOS cho khóa học trả phí, nhận webhook/xác minh trạng thái thanh toán để ghi danh; khóa học miễn phí được ghi danh trực tiếp.
- **Content Delivery & Learning:** xem nội dung khóa học đã ghi danh, phát video theo HTTP Range, tải/xem tài liệu được cấp quyền, theo dõi tiến độ và tiếp tục học.
- **Review & Q&A:** học viên đủ điều kiện gửi đánh giá, người dùng có quyền tham gia hỏi đáp theo khóa học; nội dung được kiểm duyệt trước khi lưu.
- **Recommendation:** hiển thị gợi ý khóa học liên tục và gợi ý do Gemini tạo cho người dùng đã đăng nhập.

**Out of scope:**
- Đăng nhập Google/OAuth, quên hoặc đặt lại mật khẩu, khóa tài khoản tự động sau nhiều lần đăng nhập sai.
- Cổng thanh toán MoMo/VNPay, hoàn tiền, xử lý tranh chấp, giỏ hàng và mua nhiều khóa học trong một đơn.
- Quy trình đăng ký/nâng cấp tài khoản thành giảng viên qua giao diện; vai trò giảng viên được cấp sẵn ngoài luồng đăng ký thông thường.
- Giao diện quản trị tổng quát để kiểm duyệt khóa học, quản lý người dùng hoặc gỡ nội dung. Một số vai trò/trạng thái và endpoint bảo trì nội bộ có trong backend nhưng không tạo thành module quản trị cho người dùng.
- Cấp chứng chỉ, lớp học trực tuyến theo lịch, điểm danh, ứng dụng di động native, chia/chi trả doanh thu cho giảng viên và AI tóm tắt/flashcard.

**Assumptions:**
- Ứng dụng chạy dưới dạng web SPA React và REST API Spring Boot; dữ liệu nghiệp vụ lưu trong PostgreSQL.
- Cấu hình kết nối DB, email, PayOS, Gemini và storage phụ thuộc biến môi trường. Các tích hợp có thể không hoạt động đầy đủ nếu thiếu thông tin cấu hình hợp lệ.
- Người dùng, khóa học mẫu và dữ liệu demo được tạo để phục vụ phát triển/kiểm thử; hệ thống chưa đặt yêu cầu tải thực tế hoặc SLA production trong tài liệu này.
- Storage hỗ trợ chế độ local cho phát triển và S3-compatible (ví dụ Cloudflare R2) khi được cấu hình.

### 1.2 Glossary

| Term | Definition |
|------|-----------|
| Learner | Người học có tài khoản và có thể ghi danh vào khóa học |
| Instructor / Lecturer | Người dùng có quyền tạo, quản lý khóa học của mình |
| Course | Khóa học thuộc một giảng viên, có trang giới thiệu, chuẩn đầu ra và curriculum |
| Section | Chương trong khóa học; chứa lesson và assessment được sắp xếp |
| Lesson | Bài học thuộc section; nội dung có thể là video, bài viết hoặc file |
| Learning outcome | Chuẩn đầu ra mô tả năng lực người học dự kiến đạt được |
| Assessment | Thành phần đánh giá được gắn vào section và căn chỉnh với learning outcome; hiện hỗ trợ loại quiz |
| Quiz attempt | Một lượt làm bài quiz của learner, gồm câu trả lời và kết quả chấm |
| Lesson resource | Tài liệu bổ trợ gắn với lesson |
| Presigned URL | URL có chữ ký và TTL để client tải tệp lên storage |
| Signed playback ticket / URL | Thông tin truy cập có chữ ký và thời hạn dùng để phát hoặc tải nội dung được bảo vệ |
| Enrollment | Bản ghi quyền học của learner đối với một course |
| PayOS order | Yêu cầu thanh toán cho một course, có mã order và trạng thái riêng |
| Webhook | Thông báo từ PayOS gửi về backend để cập nhật kết quả thanh toán |
| Publication status | Trạng thái nội dung curriculum; thay đổi nháp có thể chờ được áp dụng vào bản đang xuất bản |
| Watched coverage | Các khoảng nội dung video đã được ghi nhận là xem thực tế; dùng để tính tiến độ |

---

## 2. Actors & Permissions

| # | Actor | Description |
|---|-------|-------------|
| 1 | Guest | Chưa đăng nhập; xem danh mục, tìm kiếm, xem chi tiết khóa học và lesson được đánh dấu preview |
| 2 | Learner | Đăng ký/đăng nhập, ghi danh, học, làm quiz, theo dõi tiến độ, gửi review và tham gia Q&A trong khóa học có quyền truy cập |
| 3 | Instructor (`LECTURER`) | Quản lý khóa học của mình, chuẩn đầu ra, curriculum, quiz/assessment; trả lời Q&A và xem review/doanh thu |
| 4 | Administrator / Operations | Vai trò backend dùng cho một số thao tác đặc quyền và bảo trì; không có giao diện quản trị tổng quát trong frontend hiện tại |
| 5 | Payment Gateway (PayOS) | Tạo link thanh toán và gửi webhook kết quả giao dịch |
| 6 | Email Service (SendGrid) | Gửi email xác thực tài khoản |
| 7 | AI Provider (Google Gemini) | Hỗ trợ sinh nội dung authoring, gợi ý khóa học và kiểm duyệt nội dung |
| 8 | Object Storage | Lưu video, ảnh và tài liệu; có thể là local storage hoặc S3-compatible storage |

---

## 3. Functional Requirements

> Quy ước ID `FR-<MODULE>-<số>`. Các yêu cầu dưới đây mô tả chức năng có trong code hiện tại. Cột **US** giữ liên kết backlog khi có; cột **Baseline** thể hiện trạng thái triển khai của yêu cầu trong phiên bản được rà soát.

### 3.1 Authentication

| ID | Requirement | Acceptance criteria | Pri | US | Baseline | BR |
|----|-------------|---------------------|-----|----|----------|----|
| FR-AUTH-01 | Đăng ký tài khoản bằng họ tên, email và mật khẩu | Email được kiểm tra theo quy tắc duy nhất; hệ thống tạo đăng ký chờ xác thực và gửi email kích hoạt; tài khoản chưa kích hoạt không đăng nhập được | Must | US-01 | Đã triển khai | BR-01, BR-02 |
| FR-AUTH-02 | Kích hoạt tài khoản qua link trong email | Link hợp lệ và còn hạn kích hoạt tài khoản; link không hợp lệ, hết hạn hoặc đã dùng không tạo tài khoản mới | Must | US-01 | Đã triển khai | BR-02 |
| FR-AUTH-03 | Đăng nhập bằng email và mật khẩu | Thông tin không hợp lệ hoặc tài khoản chưa hoạt động bị từ chối; đăng nhập thành công cấp phiên JWT và thông tin người dùng | Must | US-02 | Đã triển khai | BR-03 |
| FR-AUTH-04 | Làm mới phiên và đăng xuất | Client có thể yêu cầu làm mới access token bằng refresh token; đăng xuất xóa/thu hồi thông tin phiên phía client/server theo cơ chế hiện có | Must | US-02 | Đã triển khai | BR-04 |
| FR-AUTH-05 | Lấy thông tin người dùng hiện tại | Request đã xác thực trả về thông tin tài khoản và vai trò hiện tại | Must | US-02 | Đã triển khai | BR-03 |

### 3.2 Profile

| ID | Requirement | Acceptance criteria | Pri | US | Baseline | BR |
|----|-------------|---------------------|-----|----|----------|----|
| FR-PROF-01 | Người dùng xem và cập nhật hồ sơ cá nhân | Thông tin hồ sơ được lưu và trả về sau cập nhật; frontend có luồng xem/sửa hồ sơ | Should | US-04 | Đã triển khai | BR-05 |
| FR-PROF-02 | Hoàn thiện hồ sơ và cập nhật ảnh đại diện | Người dùng có thể gửi thông tin hoàn thiện hồ sơ; ảnh đại diện được tải lên storage theo cơ chế presigned URL và lưu tham chiếu trong hồ sơ | Should | US-04 | Đã triển khai | BR-06 |

### 3.3 Course Authoring

| ID | Requirement | Acceptance criteria | Pri | US | Baseline | BR |
|----|-------------|---------------------|-----|----|----------|----|
| FR-CA-01 | Instructor tạo và cập nhật thông tin course | Course mới ở trạng thái `DRAFT`; có thể cập nhật title, subtitle, description, category, level, language và các trường course được hỗ trợ | Must | US-05 | Đã triển khai | BR-07, BR-08 |
| FR-CA-02 | Instructor quản lý giá và ảnh bìa course | Giá được kiểm tra phía server; ảnh bìa upload qua storage, chỉ chấp nhận storage key hợp lệ thuộc course tương ứng | Must | US-05 | Đã triển khai | BR-07, BR-09 |
| FR-CA-03 | Instructor quản lý phần giới thiệu và chuẩn đầu ra | Có thể tạo, sửa, xóa hoặc sắp xếp các learning outcome; quản lý các mục yêu cầu tiên quyết và đối tượng phù hợp trên landing page | Must | US-05 | Đã triển khai | BR-10 |
| FR-CA-04 | Instructor tạo và tổ chức curriculum | Có thể tạo/sửa/sắp xếp section, tạo/sửa lesson và di chuyển lesson giữa các section; thứ tự hiển thị được lưu ở backend | Must | US-06 | Đã triển khai | BR-08, BR-11 |
| FR-CA-05 | Instructor gắn nội dung chính và tài liệu bổ trợ cho lesson | Lesson hỗ trợ nội dung video, bài viết hoặc file; instructor có thể thêm/xóa tài liệu bổ trợ; file được lưu qua storage thay vì URL công khai cố định | Must | US-06 | Đã triển khai | BR-09, BR-12 |
| FR-CA-06 | Instructor đánh dấu lesson preview | Lesson được đánh dấu preview có thể được xem từ trang course mà không cần enrollment; nội dung khác vẫn qua kiểm tra quyền | Should | US-11 | Đã triển khai | BR-13 |
| FR-CA-07 | Instructor xem checklist và xuất bản course | Backend trả về các lỗi/điều kiện còn thiếu; publish bị từ chối nếu không đạt điều kiện; trạng thái và lịch sử chuyển trạng thái được ghi nhận | Must | US-05 | Đã triển khai | BR-14, BR-15 |
| FR-CA-08 | Instructor cập nhật nội dung course đã xuất bản qua bản nháp thay đổi | Thay đổi curriculum/nội dung có trạng thái chờ; instructor có thể xem publish check và áp dụng thay đổi; learner tiếp tục thấy bản nội dung đang xuất bản cho tới khi thay đổi được áp dụng | Must | US-05, US-06 | Đã triển khai | BR-16 |
| FR-CA-09 | Instructor xem lịch sử trạng thái course | Nhật ký thể hiện trạng thái trước/sau, người thực hiện, thời điểm và lý do nếu có | Could | US-08 | Đã triển khai | BR-15 |

### 3.4 Backward Design, Assessment & Quiz

| ID | Requirement | Acceptance criteria | Pri | US | Baseline | BR |
|----|-------------|---------------------|-----|----|----------|----|
| FR-QZ-01 | Instructor tạo và chỉnh sửa assessment quiz | Quiz được liên kết với course/section, có câu hỏi, lựa chọn, đáp án đúng và thiết lập như điểm đạt/lượt làm theo dữ liệu quiz hiện có | Must | US-07 | Đã triển khai | BR-17 |
| FR-QZ-02 | Instructor căn chỉnh lesson và assessment với learning outcome | Có thể gắn outcome vào lesson/assessment, xem ma trận căn chỉnh và nhận cảnh báo khi mapping thiếu trước khi publish | Must | US-05, US-07 | Đã triển khai | BR-14, BR-18 |
| FR-QZ-03 | Instructor tạo nháp quiz bằng AI từ tài liệu | Có thể tải tài liệu được hỗ trợ để AI trích xuất/gợi ý câu hỏi; kết quả là bản nháp và chỉ lưu vào quiz khi instructor chủ động áp dụng | Should | US-07 | Đã triển khai | BR-19 |
| FR-QZ-04 | Learner xem và làm quiz | Quiz trả về nội dung cần thiết để làm bài nhưng không gửi đáp án đúng trước khi nộp; server chấm bài và trả điểm/kết quả cùng giải thích được cấu hình | Must | US-20 | Đã triển khai | BR-17, BR-20 |
| FR-QZ-05 | Learner xem lịch sử attempt | Các lượt làm đã lưu có thể được truy vấn trong giao diện học tập; mỗi lần nộp hợp lệ được ghi thành một attempt riêng | Should | US-20 | Đã triển khai | BR-20 |

### 3.5 Instructor Management & Revenue

| ID | Requirement | Acceptance criteria | Pri | US | Baseline | BR |
|----|-------------|---------------------|-----|----|----------|----|
| FR-INS-01 | Instructor xem danh sách và chi tiết course của mình | Chỉ course thuộc instructor hiện tại được trả về khu vực quản lý; có thể lọc theo trạng thái/từ khóa theo API | Should | US-08 | Đã triển khai | BR-08 |
| FR-INS-02 | Instructor publish, unpublish và republish course | Chuyển trạng thái tuân theo state machine; unpublish loại course khỏi catalog công khai nhưng enrollment hiện tại vẫn có thể học | Must | US-08 | Đã triển khai | BR-15, BR-21 |
| FR-INS-03 | Instructor xem review của course | Màn hình giảng viên hiển thị các review của khóa học mình sở hữu và thông tin tóm tắt | Could | US-18 | Đã triển khai | BR-22 |
| FR-INS-04 | Instructor xem doanh thu | Báo cáo hiển thị giao dịch/đơn hàng đã thanh toán và tổng hợp doanh thu liên quan đến course của instructor | Could | US-09 | Đã triển khai | BR-23 |
| FR-INS-05 | Instructor theo dõi và trả lời câu hỏi của learner | Có thể xem câu hỏi theo course, lọc theo trạng thái đã/chưa có phản hồi của giảng viên và gửi trả lời | Should | US-21 | Đã triển khai | BR-24 |

### 3.6 Course Discovery

| ID | Requirement | Acceptance criteria | Pri | US | Baseline | BR |
|----|-------------|---------------------|-----|----|----------|----|
| FR-CD-01 | Guest/Learner duyệt và tìm kiếm course | Catalog hỗ trợ truy vấn khóa học và danh mục theo bộ lọc được backend cung cấp; kết quả công khai chỉ gồm course đã xuất bản | Should | US-10 | Đã triển khai | BR-21 |
| FR-CD-02 | Guest/Learner xem chi tiết course và curriculum công khai | Trang chi tiết hiển thị thông tin course, learning outcomes, thông tin giảng viên, rating và curriculum; có thể truy cập course bằng định danh được hỗ trợ | Must | US-11 | Đã triển khai | BR-21 |
| FR-CD-03 | Guest/Learner xem nội dung preview | Lesson được đánh dấu preview có thể mở công khai; API vẫn kiểm tra quyền cho nội dung không phải preview | Must | US-11 | Đã triển khai | BR-13 |
| FR-CD-04 | Guest/Learner xem danh mục course liên quan và review | Trang chi tiết cung cấp review/tóm tắt rating; giao diện có các khu vực gợi ý course khi API trả về dữ liệu | Should | US-11, US-22 | Đã triển khai | BR-22, BR-25 |

### 3.7 Payment & Enrollment

| ID | Requirement | Acceptance criteria | Pri | US | Baseline | BR |
|----|-------------|---------------------|-----|----|----------|----|
| FR-PAY-01 | Learner checkout một course trả phí qua PayOS | Backend tạo order code và link checkout PayOS; order được lưu ở trạng thái chờ thanh toán | Must | US-13 | Đã triển khai | BR-26 |
| FR-PAY-02 | Hệ thống cập nhật thanh toán từ PayOS webhook | Backend xác minh webhook bằng cơ chế PayOS trước khi cập nhật order và tạo enrollment; webhook lặp không tạo enrollment trùng | Must | US-14 | Đã triển khai | BR-27, BR-28 |
| FR-PAY-03 | Learner tra cứu trạng thái order | Trang kết quả dùng order code để tra cứu; khi order còn chờ, backend có thể đồng bộ trạng thái từ PayOS; redirect của trình duyệt tự nó không chứng minh thanh toán thành công | Must | US-14 | Đã triển khai | BR-27 |
| FR-PAY-04 | Ghi danh vào course miễn phí hoặc course đã thanh toán | Course miễn phí có thể ghi danh trực tiếp; course trả phí được mở sau xác nhận thanh toán; learner đã ghi danh không bị ghi danh lặp | Must | US-13, US-14 | Đã triển khai | BR-28, BR-29 |

### 3.8 Content Delivery

| ID | Requirement | Acceptance criteria | Pri | US | Baseline | BR |
|----|-------------|---------------------|-----|----|----------|----|
| FR-DL-01 | Kiểm tra quyền trước khi cấp nội dung lesson | Learner có enrollment hợp lệ hoặc lesson là preview mới nhận được quyền xem tương ứng; truy cập không có quyền bị từ chối | Must | US-15 | Đã triển khai | BR-13, BR-30 |
| FR-DL-02 | Tải video/file lên storage bằng presigned upload | Backend cấp URL upload có thời hạn và xác nhận metadata/object theo quy trình upload hiện có; frontend không lưu URL công khai làm định danh nội dung | Must | US-06 | Đã triển khai | BR-09, BR-31 |
| FR-DL-03 | Phát nội dung video có hỗ trợ tua | Luồng video hỗ trợ HTTP Range/Partial Content để client yêu cầu các đoạn dữ liệu thay vì tải lại toàn bộ tệp | Must | US-15 | Đã triển khai | BR-30 |
| FR-DL-04 | Gia hạn quyền phát trong phiên học | Frontend có thể lấy playback ticket/URL mới khi cần; backend kiểm tra lại quyền truy cập trước khi cấp | Should | US-15 | Đã triển khai | BR-30, BR-32 |

### 3.9 Learning & Progress

| ID | Requirement | Acceptance criteria | Pri | US | Baseline | BR |
|----|-------------|---------------------|-----|----|----------|----|
| FR-LRN-01 | Learner đã ghi danh mở không gian học tập | Giao diện hiển thị curriculum và nội dung học được phép truy cập; course đã unpublish vẫn phục vụ người đã ghi danh | Must | US-16 | Đã triển khai | BR-29, BR-30 |
| FR-LRN-02 | Learner xem danh sách khóa học đã ghi danh | Mỗi course có tiến độ/trạng thái và đường dẫn tiếp tục học | Should | US-19 | Đã triển khai | BR-29 |
| FR-LRN-03 | Hệ thống ghi nhận tiến độ xem bằng heartbeat | Client gửi dữ liệu phiên xem; backend lưu vị trí/coverage theo nội dung và trả snapshot tiến độ | Must | US-17 | Đã triển khai | BR-33 |
| FR-LRN-04 | Hệ thống tính tiến độ course | Tiến độ tổng hợp từ các đơn vị học được hỗ trợ; nội dung video được đánh dấu hoàn thành theo ngưỡng coverage cấu hình; enrollment được cập nhật theo tiến độ | Must | US-17 | Đã triển khai | BR-33, BR-34 |

### 3.10 Review & Content Moderation

| ID | Requirement | Acceptance criteria | Pri | US | Baseline | BR |
|----|-------------|---------------------|-----|----|----------|----|
| FR-RV-01 | Learner xem review và phân bố rating | Trang course có danh sách review đã được duyệt và thống kê theo rating | Should | US-18 | Đã triển khai | BR-22 |
| FR-RV-02 | Learner đủ điều kiện tạo hoặc cập nhật review | Chỉ enrollment đủ điều kiện mới gửi review; learner có thể xem trạng thái quyền review và review của mình; review bị kiểm duyệt trước khi xuất hiện công khai | Should | US-18 | Đã triển khai | BR-22, BR-35 |
| FR-RV-03 | Hệ thống kiểm duyệt nội dung review và Q&A | Nội dung được kiểm tra bằng bộ lọc từ ngữ và dịch vụ AI nếu được cấu hình; nội dung không đạt quy tắc bị từ chối hoặc không được hiển thị công khai | Should | — | Đã triển khai | BR-36 |

### 3.11 Q&A

| ID | Requirement | Acceptance criteria | Pri | US | Baseline | BR |
|----|-------------|---------------------|-----|----|----------|----|
| FR-QA-01 | Learner/instructor xem câu hỏi và câu trả lời của lesson/course | Có thể truy vấn câu hỏi theo lesson hoặc course; giao diện hiển thị tác giả, nội dung và các câu trả lời | Should | US-21 | Đã triển khai | BR-24 |
| FR-QA-02 | Người có quyền tham gia tạo câu hỏi hoặc câu trả lời | Learner đã ghi danh và giảng viên chủ khóa học được tham gia; tác giả, giảng viên hoặc admin có quyền xóa nội dung theo chính sách backend | Should | US-21 | Đã triển khai | BR-24, BR-36 |

### 3.12 AI Assistant & Authoring

| ID | Requirement | Acceptance criteria | Pri | US | Baseline | BR |
|----|-------------|---------------------|-----|----|----------|----|
| FR-AI-01 | Hệ thống tạo gợi ý course cho người dùng | Frontend có thể yêu cầu recommendation liên tục hoặc recommendation AI; các course gợi ý đến từ tập dữ liệu mà backend cho phép hiển thị | Should | US-22 | Đã triển khai | BR-25 |
| FR-AI-02 | Hệ thống hỗ trợ giảng viên soạn course và quiz | AI có thể đề xuất learning outcome, curriculum hoặc câu hỏi quiz; kết quả authoring được trả dưới dạng draft để instructor xem xét trước khi áp dụng | Should | US-07 | Đã triển khai | BR-19, BR-37 |

---

## 4. Non-Functional Requirements

| ID | Category | Requirement |
|----|----------|-------------|
| NFR-01 | Security | Mật khẩu được mã hóa một chiều bằng BCrypt trước khi lưu; mật khẩu dạng rõ không được lưu trong DB |
| NFR-02 | Security | API dùng xác thực JWT stateless; refresh token được quản lý qua cookie theo cấu hình; cookie phiên phát nội dung được ký và có thời hạn |
| NFR-03 | Security | Quyền sở hữu course, vai trò, enrollment và quyền truy cập nội dung được kiểm tra ở backend; frontend route guard chỉ bổ sung trải nghiệm giao diện |
| NFR-04 | Security | URL upload/download của storage có TTL cấu hình; mặc định upload/download là 15 phút; playback ticket/session có TTL riêng theo cấu hình |
| NFR-05 | Reliability | Enrollment của course trả phí được tạo từ xử lý thanh toán backend (webhook hoặc đồng bộ trạng thái PayOS), không dựa riêng vào query parameter redirect trên trình duyệt |
| NFR-06 | Reliability | Các tích hợp ngoài như email, storage, PayOS và Gemini phụ thuộc cấu hình; lỗi tích hợp được trả/xử lý theo từng luồng và ghi log bởi backend |
| NFR-07 | Data integrity | Cập nhật course hỗ trợ kiểm tra version để phát hiện dữ liệu cũ; thao tác xuất bản curriculum áp dụng thay đổi ở backend |
| NFR-08 | Compatibility | Frontend là SPA React/TypeScript chạy trên trình duyệt hiện đại; layout hỗ trợ kích thước màn hình khác nhau |
| NFR-09 | Maintainability | Backend chia module nghiệp vụ trong một ứng dụng Spring Boot; database schema được quản lý bằng migration; frontend chia theo feature |
| NFR-10 | Performance | Video được phân phối theo byte range; danh sách catalog/review hỗ trợ phân trang tại các API tương ứng |

**External interfaces**

| # | Hệ thống ngoài | Mục đích | Yêu cầu khi lỗi |
|---|-----------------|---------|-----------------|
| 1 | PayOS | Tạo payment link, nhận webhook và truy vấn trạng thái order | Backend giữ kết quả theo trạng thái đã xác minh; lỗi kết nối được ghi nhận và trạng thái có thể tra cứu lại |
| 2 | Object Storage (local/S3-compatible) | Lưu và phân phối ảnh, video và tài liệu | Upload/đọc lỗi được trả về cho luồng gọi; nội dung chưa sẵn sàng không được xem là nội dung đã upload thành công |
| 3 | SendGrid | Gửi email kích hoạt tài khoản | Lỗi gửi email khiến người dùng chưa thể hoàn tất kích hoạt cho tới khi nhận được link hợp lệ hoặc thực hiện lại theo cơ chế hiện có |
| 4 | Google Gemini | AI authoring, recommendation và moderation | Chức năng dựa trên AI cần cấu hình API; kết quả sinh nội dung ở dạng draft để người dùng kiểm tra |
| 5 | PostgreSQL | Lưu tài khoản, course, enrollment, giao dịch, tiến độ và nội dung nghiệp vụ | Lỗi DB khiến thao tác phụ thuộc dữ liệu không thể hoàn tất; backend trả lỗi theo xử lý ngoại lệ chung |

---

## 5. Business Rules

| ID | Rule Definition | FR liên quan |
|----|------------------|---------------|
| BR-01 | Một email không được tạo nhiều tài khoản đã kích hoạt. Yêu cầu đăng ký chờ xác thực được lưu riêng cho đến khi kích hoạt. | FR-AUTH-01 |
| BR-02 | Link kích hoạt tài khoản có TTL mặc định 15 phút. Chỉ link hợp lệ và chưa hết hạn mới được kích hoạt tài khoản. | FR-AUTH-01, FR-AUTH-02 |
| BR-03 | Người đăng ký thông thường được tạo với vai trò learner; quyền instructor/admin được cấp qua dữ liệu/quy trình ngoài luồng đăng ký hiện tại. | FR-AUTH-03, FR-AUTH-05 |
| BR-04 | Access token có TTL mặc định 24 giờ và refresh token 7 ngày theo cấu hình ứng dụng; thời hạn thực tế có thể được ghi đè bằng biến môi trường. | FR-AUTH-04 |
| BR-05 | Hồ sơ cá nhân chỉ được đọc/cập nhật bởi người dùng đã xác thực tương ứng, theo quyền backend. | FR-PROF-01 |
| BR-06 | Ảnh đại diện được tải qua storage; backend lưu thông tin tham chiếu/key theo cấu hình storage. | FR-PROF-02 |
| BR-07 | Giá course nằm trong khoảng 0 đến 10.000.000 VND; giá 0 hoặc null được xử lý như khóa học miễn phí ở luồng checkout. | FR-CA-02, FR-PAY-04 |
| BR-08 | Instructor chỉ được quản lý course do mình sở hữu; server kiểm tra quyền sở hữu khi đọc/sửa các tài nguyên quản lý course. | FR-CA-01, FR-CA-04, FR-INS-01 |
| BR-09 | Tệp nội dung được lưu qua storage và gắn với course/lesson; backend kiểm tra mục đích upload, key và metadata theo API tương ứng. | FR-CA-02, FR-CA-05, FR-DL-02 |
| BR-10 | Learning outcome được lưu riêng khỏi danh sách requirement/audience. Mặc định cần tối thiểu 2 outcome, 1 requirement và 1 target audience để publish; cấu hình publish có thể thay đổi ngưỡng. | FR-CA-03, FR-QZ-02 |
| BR-11 | Section và lesson có thứ tự xác định; thao tác reorder/move lưu thứ tự curriculum. | FR-CA-04 |
| BR-12 | Nội dung lesson chính hỗ trợ video, bài viết hoặc file; lesson có thể có nhiều tài liệu bổ trợ. | FR-CA-05 |
| BR-13 | Guest chỉ được truy cập nội dung được đánh dấu preview; nội dung còn lại yêu cầu enrollment/quyền hợp lệ. | FR-CA-06, FR-CD-03, FR-DL-01 |
| BR-14 | Điều kiện publish kiểm tra thông tin course (title, mô tả tối thiểu 200 ký tự, category, thumbnail, giá), outcome/requirement/audience tối thiểu, section và nội dung hoàn chỉnh; lesson và assessment cần đáp ứng quy tắc căn chỉnh outcome. | FR-CA-07, FR-QZ-02 |
| BR-15 | Vòng đời course đang dùng gồm `DRAFT`, `PUBLISHED` và `UNPUBLISHED`; publish chỉ từ draft/unpublished, unpublish chỉ từ published. Mỗi lần chuyển trạng thái được ghi log. Các enum khác không tạo thành quy trình nghiệp vụ đang hoạt động trong UI hiện tại. | FR-CA-07, FR-CA-09, FR-INS-02 |
| BR-16 | Với course đã publish, các nội dung curriculum có trạng thái nháp được giữ tách khỏi bản đang công khai cho đến khi instructor áp dụng publish changes; khi áp dụng, hệ thống tính lại tiến độ enrollment đang hoạt động. | FR-CA-08 |
| BR-17 | Quiz là assessment được gắn vào course/section; quiz cần cấu hình hợp lệ và câu hỏi/đáp án để làm bài; server giữ đáp án đúng để chấm sau khi learner nộp. | FR-QZ-01, FR-QZ-04 |
| BR-18 | Outcome phải được căn chỉnh với nội dung lesson và assessment theo điều kiện publish; assessment cần được đặt trong section trước khi publish. | FR-QZ-02, FR-CA-07 |
| BR-19 | Kết quả sinh bởi AI không tự động trở thành nội dung đã publish; giảng viên phải xem xét và áp dụng draft. Tài liệu AI quiz chỉ nhận các loại/giới hạn tệp được API hỗ trợ. | FR-QZ-03, FR-AI-02 |
| BR-20 | Điểm quiz được tính ở backend. Mỗi lần learner nộp bài được lưu thành attempt để có thể xem kết quả/lịch sử. | FR-QZ-04, FR-QZ-05 |
| BR-21 | Catalog và trang course công khai chỉ hiển thị course `PUBLISHED`; course đã unpublish không xuất hiện trong discovery. | FR-INS-02, FR-CD-01, FR-CD-02 |
| BR-22 | Review công khai phải đạt trạng thái được duyệt. Backend lưu rating summary và chỉ tính nội dung phù hợp theo trạng thái duyệt; learner đã bắt đầu học tối thiểu 20% mới đủ điều kiện review nếu chưa có review trước đó. | FR-INS-03, FR-CD-04, FR-RV-01, FR-RV-02 |
| BR-23 | Doanh thu instructor lấy từ payment order thành công (`PAID`); các trạng thái chưa thanh toán, thất bại hoặc hủy không tính vào doanh thu đã thanh toán. | FR-INS-04 |
| BR-24 | Chỉ learner đã ghi danh, instructor sở hữu course hoặc admin theo quyền backend được tham gia Q&A; backend phân biệt phản hồi của instructor trong thread. Nội dung hỏi đáp chịu kiểm duyệt. | FR-INS-05, FR-QA-01, FR-QA-02 |
| BR-25 | Recommendation chỉ hiển thị course được backend lựa chọn theo trạng thái/điều kiện truy vấn; endpoint AI yêu cầu xác thực người dùng. | FR-CD-04, FR-AI-01 |
| BR-26 | Mỗi PayOS order gắn learner, course, số tiền và order code. Checkout course trả phí tái sử dụng order pending còn checkout URL và chưa quá 15 phút; order pending cũ được đánh dấu `CANCELLED` khi tạo link mới. | FR-PAY-01 |
| BR-27 | Order chỉ chuyển sang `PAID` khi trạng thái được xác minh qua webhook PayOS hoặc truy vấn trạng thái PayOS từ backend; redirect success/cancel của frontend chỉ điều hướng và tra cứu trạng thái. | FR-PAY-02, FR-PAY-03 |
| BR-28 | Xử lý một giao dịch lặp không tạo enrollment thứ hai cho cùng learner/course; hệ thống kiểm tra enrollment hiện có trước khi ghi danh. | FR-PAY-02, FR-PAY-04 |
| BR-29 | Một learner chỉ có một enrollment cho cùng một course. Instructor không thể ghi danh vào course của chính mình. Khóa học miễn phí có thể tạo enrollment mà không cần giao dịch PayOS. | FR-PAY-04, FR-LRN-01, FR-LRN-02 |
| BR-30 | Nội dung được cấp theo enrollment hoặc quyền preview; course đã unpublish vẫn có thể được người đã ghi danh tiếp tục học. | FR-INS-02, FR-DL-01, FR-DL-04, FR-LRN-01 |
| BR-31 | Upload/download dùng TTL mặc định 15 phút; playback ticket và cookie phiên có thời hạn riêng theo cấu hình delivery, hiện mặc định 8 giờ. | FR-DL-02, FR-DL-04 |
| BR-32 | Luồng video hỗ trợ HTTP byte range; quyền truy cập được kiểm tra khi cấp playback ticket/URL và khi phục vụ nội dung theo thiết kế delivery. | FR-DL-03, FR-DL-04 |
| BR-33 | Heartbeat lưu trạng thái xem và coverage của learner/lesson. Video hoàn thành khi watched coverage đạt ngưỡng cấu hình, mặc định 90%. | FR-LRN-03, FR-LRN-04 |
| BR-34 | Tiến độ course được tổng hợp từ các đơn vị học và enrollment; khi curriculum đã xuất bản thay đổi, backend tính lại tiến độ enrollment đang hoạt động. | FR-LRN-04, FR-CA-08 |
| BR-35 | Mỗi learner có thể quản lý review của mình cho course theo API; review mới yêu cầu enrollment và đạt ngưỡng tiến độ, còn review hiện có vẫn có thể được truy cập để cập nhật. | FR-RV-02 |
| BR-36 | Nội dung review, câu hỏi và câu trả lời được đưa qua bộ kiểm duyệt từ khóa và/hoặc Gemini theo cấu hình. | FR-RV-03, FR-QA-02 |
| BR-37 | Nội dung AI phụ thuộc Gemini API và cấu hình bật AI; chức năng thủ công của course/quiz không được thay bằng kết quả AI tự động. | FR-QZ-03, FR-AI-01, FR-AI-02 |
