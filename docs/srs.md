# Software Requirements Specification — Learnova

| | |
|---|---|
| **Project** | Learnova — Online Course Marketplace |
| **Version / Sprint** | 0.2 — Sprint 2 |
| **Last updated** | 27-08-2026 |

**Record of Changes**

| Date | Ver | Sprint | A/M/D | In charge | Change |
|------|-----|--------|-------|-----------|--------|
| 20-08-2026 | 0.1 | 1 | A | TienPQ9 | Khởi tạo SRS, đặc tả Content Authoring |
| 27-08-2026 | 0.2 | 2 | M | TienPQ9 | Đồng bộ SRS với backlog US-01..US-22: bỏ Certificate và Admin module khỏi scope; bỏ wishlist, AI tóm tắt/flashcard; bổ sung Profile, Quiz, Instructor Management, Review, Q&A; chỉnh FR/BR cho khớp phần đã code ở Sprint 1–2 |

---

## 1. Scope & Glossary

### 1.1 Scope

**In scope:**
- **Auth & Profile** (US-01..US-04): đăng ký tài khoản có xác thực email, đăng nhập
  JWT và phân quyền theo vai trò, đăng nhập Google, xem/sửa hồ sơ và đổi mật khẩu.
- **Course Authoring** (US-05, US-06): giảng viên tạo/sửa khóa học ở trạng thái draft,
  đặt giá, soạn ba khối mô tả landing page, dựng cây section–lesson, upload video/file
  qua presigned URL, và publish khi đủ điều kiện.
- **Quiz** (US-07, US-20): giảng viên soạn quiz gắn vào lesson; học viên làm bài và
  được server chấm điểm tự động.
- **Instructor Management** (US-08, US-09): danh sách khóa học của mình kèm trạng thái
  và số học viên, gỡ/đăng lại khóa học, xem doanh thu từ đơn hàng đã thanh toán.
- **Course Discovery** (US-10, US-11): tìm kiếm theo từ khóa, lọc theo danh mục/trình
  độ/giá, phân trang; trang chi tiết khóa học kèm curriculum và lesson preview miễn phí.
- **Payment** (US-13, US-14): tạo đơn hàng và chuyển sang cổng nội địa (MoMo/VNPay),
  xác nhận giao dịch bằng IPN server-to-server, cấp enrollment sau khi thanh toán thành công.
- **Content Delivery** (US-15): phát nội dung bài học qua signed URL có thời hạn ngắn,
  hỗ trợ HTTP Range để tua, chặn truy cập khi chưa ghi danh.
- **Learning & Progress** (US-16, US-17, US-19): trình phát bài học cho học viên đã ghi
  danh, danh sách khóa học đã mua kèm điểm tiếp tục, ghi nhận vị trí xem và phần thời
  lượng thực sự đã xem, tính % hoàn thành.
- **Review** (US-18): học viên đã ghi danh đánh giá sao và viết nhận xét cho khóa học.
- **Q&A** (US-21): học viên hỏi trên lesson, giảng viên trả lời.
- **AI** (US-22): gợi ý khóa học tương tự dựa trên hồ sơ / lịch sử học.

**Out of scope:**
- **Chứng chỉ (certificate)** — đã loại khỏi scope ở Sprint 2 Review; không cấp chứng chỉ
  khi hoàn thành khóa học.
- **Module quản trị (Admin)** — đã loại khỏi scope ở Sprint 2 Review: không có màn hình
  admin khóa tài khoản hay gỡ khóa học vi phạm. Vai trò `ADMIN` và trạng thái course
  `SUSPENDED` vẫn tồn tại trong schema như chỗ giữ sẵn nhưng không có chức năng đi kèm.
- **Wishlist / giỏ hàng** — mỗi lần mua đúng một khóa học.
- **AI tóm tắt bài học và sinh flashcard** — chỉ giữ lại gợi ý khóa học (US-22).
- **Học theo lịch cố định**: điểm danh, xếp lớp, lớp học trực tuyến theo giờ.
- **Ứng dụng mobile** (chỉ web responsive).
- **Chia doanh thu / chi trả cho giảng viên qua cổng thanh toán** — hệ thống chỉ ghi nhận
  và hiển thị doanh thu, việc chi trả xử lý thủ công ngoài hệ thống.
- **Hoàn tiền (refund)** và xử lý khiếu nại giao dịch.
- **Kiểm duyệt chất lượng khóa học trước khi đăng bán** — giảng viên tự publish.

**Assumptions:**
- Dữ liệu khóa học dùng để test do nhóm tự tạo (seed data), không cần dữ liệu thật quy mô lớn.
- Sử dụng gói miễn phí/dùng thử của LLM API cho phần AI.
- Vai trò `LECTURER` được gán ở tầng dữ liệu (seed/demo), chưa có luồng đăng ký làm giảng
  viên trên UI (xem BR-03).

### 1.2 Glossary

| Term | Definition |
|------|-----------|
| Course | Đơn vị bán hàng, thuộc một giảng viên, gồm nhiều Section |
| Section | Chương của Course, gồm nhiều Lesson |
| Lesson | Đơn vị nội dung nhỏ nhất trong curriculum: `VIDEO` / `ARTICLE` / `FILE` / `QUIZ` |
| Bullet | Dòng mô tả trên landing page, thuộc một trong ba nhóm: mục tiêu học, yêu cầu tiên quyết, đối tượng hướng tới |
| Publish check | Bộ điều kiện server kiểm tra trước khi cho phép publish (BR-12) |
| storage_key | Khóa định danh object trên storage; hệ thống lưu key chứ không lưu URL công khai |
| Presigned URL | URL có chữ ký, thời hạn ngắn, để client **upload** trực tiếp lên storage |
| Signed URL | URL có chữ ký, thời hạn ngắn, để client **tải / phát** nội dung đã mua |
| Order | Bản ghi giao dịch mua, trạng thái `PENDING` / `PAID` / `EXPIRED` / `CANCELLED` |
| order_code | Mã đơn hàng do hệ thống sinh, độc lập với transaction_id của cổng thanh toán |
| IPN | Thông báo thanh toán server-to-server do cổng thanh toán gửi về |
| Enrollment | Quyền truy cập course của Learner, trạng thái `ACTIVE` / `COMPLETED` / `CANCELLED` |
| Current position | Vị trí phát hiện tại của video, dùng để tiếp tục xem |
| Watched coverage | Tỉ lệ thời lượng video **thực sự được phát**; khác current position |

---

## 2. Actors & Permissions

| # | Actor | Description |
|---|-------|-------------|
| 1 | Guest | Chưa đăng nhập; duyệt catalog, tìm kiếm, xem trang chi tiết và lesson preview |
| 2 | Learner | Mua course, học, theo dõi tiến độ, làm quiz, đánh giá, đặt câu hỏi |
| 3 | Instructor (`LECTURER`) | Tạo và quản lý course của mình, soạn quiz, trả lời Q&A, xem doanh thu |
| 4 | Payment Gateway | Hệ thống ngoài; nhận thanh toán và gửi IPN xác nhận giao dịch |
| 5 | LLM Provider | Hệ thống ngoài; sinh gợi ý khóa học |

> Vai trò `ADMIN` tồn tại trong schema và trong cấu hình bảo mật (`/api/v1/admin/**`)
> nhưng module quản trị đã bị loại khỏi scope — không có FR nào trong tài liệu này.

---

## 3. Functional Requirements

> Quy ước ID `FR-<MODULE>-<số>`. Module: `AUTH`, `PROF` (Profile), `CA` (Course Authoring),
> `QZ` (Quiz), `INS` (Instructor Management), `CD` (Course Discovery), `PAY`, `DL`
> (Content Delivery), `LRN` (Learning), `KT` (Knowledge Tracking), `RV` (Review),
> `QA` (Q&A), `AI`.
>
> Cột **US** trỏ về user story trong backlog. Cột **Sprint** là sprint được lên kế hoạch;
> ✅ đánh dấu phần đã hoàn thành và đã demo.

### 3.1 Authentication (support)

| ID | Requirement | Acceptance criteria | Pri | US | Sprint | BR |
|----|-------------|---------------------|-----|----|--------|----|
| FR-AUTH-01 | Đăng ký tài khoản bằng họ tên, email và mật khẩu | Email đã tồn tại bị từ chối; mật khẩu ≥ 6 ký tự; hệ thống gửi email kích hoạt và chưa tạo user | Must | US-01 | 1 ✅ | BR-01, BR-02 |
| FR-AUTH-02 | Kích hoạt tài khoản bằng link trong email | Link còn hạn → tạo user và cho phép đăng nhập; link hết hạn hoặc đã dùng → báo lỗi, không tạo user | Must | US-01 | 1 ✅ | BR-02 |
| FR-AUTH-03 | Đăng nhập, cấp access token + refresh token, phân quyền theo vai trò | Sai thông tin → từ chối; access token hết hạn theo cấu hình và làm mới được bằng refresh token; nhóm endpoint `/lecturer/**` chỉ mở cho `LECTURER` | Must | US-02 | 3 | BR-03, BR-05 |
| FR-AUTH-04 | Khóa tạm thời khi đăng nhập sai nhiều lần | Sai 5 lần trong 15 phút → khóa 30 phút; nhập đúng mật khẩu vẫn không vào được cho tới khi hết khóa | Must | US-02 | 3 | BR-04 |
| FR-AUTH-05 | Đăng xuất và thu hồi phiên | Sau khi đăng xuất, refresh token cũ không dùng lại được | Must | US-02 | 3 | |
| FR-AUTH-06 | Đăng nhập bằng Google | Lần đầu tạo tài khoản hoặc liên kết với email sẵn có; lần sau vào thẳng trạng thái đã đăng nhập | Should | US-03 | 4 | BR-06 |

### 3.2 Profile (support)

| ID | Requirement | Acceptance criteria | Pri | US | Sprint | BR |
|----|-------------|---------------------|-----|----|--------|----|
| FR-PROF-01 | Người dùng xem và sửa hồ sơ: họ tên, ảnh đại diện, giới thiệu, lĩnh vực, sở thích | Thay đổi lưu lại và hiển thị ngay; ảnh đại diện upload qua presigned URL | Should | US-04 | 2 ✅ | BR-10 |
| FR-PROF-02 | Người dùng đổi mật khẩu | Bắt buộc nhập đúng mật khẩu hiện tại; sai → từ chối | Should | US-04 | 2 ✅ | |
| FR-PROF-03 | Giảng viên hoàn thiện hồ sơ trước khi tạo khóa học | Thiếu thông tin bắt buộc của giảng viên → điều hướng về trang hồ sơ | Should | US-04 | 2 ✅ | |

### 3.3 Course Authoring (core)

| ID | Requirement | Acceptance criteria | Pri | US | Sprint | BR |
|----|-------------|---------------------|-----|----|--------|----|
| FR-CA-01 | Instructor tạo và sửa course: title, subtitle, mô tả, danh mục, trình độ, ngôn ngữ, giá, ảnh bìa, video giới thiệu | Course tạo ra ở trạng thái `DRAFT`, chưa xuất hiện ở catalog; slug sinh tự động và duy nhất | Must | US-05 | 2 ✅ | BR-07, BR-08 |
| FR-CA-02 | Instructor soạn ba khối mô tả landing page: học viên sẽ học được gì, yêu cầu tiên quyết, đối tượng hướng tới | Mỗi nhóm tối đa 20 dòng, mỗi dòng tối đa 500 ký tự | Must | US-05 | 2 ✅ | BR-09 |
| FR-CA-03 | Chỉ chủ sở hữu được sửa course | Instructor khác gọi API sửa → 403 | Must | US-05 | 2 ✅ | BR-08 |
| FR-CA-04 | Instructor thêm, sửa, xóa và sắp xếp Section / Lesson | Thứ tự lưu theo `position` và phản ánh đúng phía Learner; xóa là soft delete | Must | US-06 | 2 ✅ | |
| FR-CA-05 | Instructor gắn nội dung cho lesson: video, bài viết, hoặc file tài liệu | Video/file upload trực tiếp lên storage qua presigned URL; hệ thống lưu `storage_key`, không lưu URL công khai; trạng thái upload hiển thị (`PENDING` → `READY` / `FAILED`) | Must | US-06 | 2 ✅ | BR-10, BR-11 |
| FR-CA-06 | Instructor đính kèm tài liệu bổ trợ cho lesson | Thêm / xóa được từng tài liệu | Should | US-06 | 2 ✅ | BR-10 |
| FR-CA-07 | Instructor đánh dấu lesson là preview miễn phí | Lesson preview xem được khi chưa mua | Should | US-11 | 2 ✅ | BR-24 |
| FR-CA-08 | Instructor kiểm tra điều kiện publish và publish course | Checklist hiển thị đúng các mục còn thiếu; publish bị từ chối khi còn thiếu điều kiện, course giữ ở `DRAFT`; mọi lần đổi trạng thái đều ghi log | Must | US-05 | 2 ✅ | BR-12, BR-13 |
| FR-CA-09 | Sửa course đã publish không ảnh hưởng learner đang học | Đổi tên hoặc đổi thứ tự lesson không xóa tiến trình đã ghi nhận của learner | Should | US-05 | 3 | BR-14 |

### 3.4 Quiz (core)

| ID | Requirement | Acceptance criteria | Pri | US | Sprint | BR |
|----|-------------|---------------------|-----|----|--------|----|
| FR-QZ-01 | Instructor soạn quiz cho một lesson: câu hỏi, các lựa chọn, đáp án đúng | Quiz gắn 1–1 với lesson kiểu `QUIZ`; lesson `QUIZ` chưa có câu hỏi bị coi là chưa hoàn chỉnh khi publish | Should | US-07 | 3 | BR-12, BR-15 |
| FR-QZ-02 | Learner làm quiz và được chấm điểm tự động | Chấm điểm ở server; hiển thị tổng điểm và kết quả từng câu; mỗi lần làm được ghi lại thành một attempt | Should | US-20 | 3 | BR-15, BR-16 |

### 3.5 Instructor Management (support)

| ID | Requirement | Acceptance criteria | Pri | US | Sprint | BR |
|----|-------------|---------------------|-----|----|--------|----|
| FR-INS-01 | Instructor xem danh sách khóa học của mình kèm trạng thái và số học viên | Chỉ hiện course của chính mình, gồm cả `DRAFT` và `UNPUBLISHED` | Should | US-08 | 2 ✅ | BR-08 |
| FR-INS-02 | Instructor gỡ (unpublish) và đăng lại (republish) khóa học | Course `UNPUBLISHED` biến mất khỏi catalog/tìm kiếm; learner đã mua vẫn học được | Should | US-08 | 2 ✅ | BR-13, BR-17 |
| FR-INS-03 | Instructor xem lịch sử đổi trạng thái của khóa học | Mỗi bản ghi có trạng thái trước/sau, người thực hiện, thời điểm và lý do (nếu có) | Could | US-08 | 2 ✅ | BR-13 |
| FR-INS-04 | Instructor xem doanh thu theo từng khóa học và tổng doanh thu | Chỉ tính từ order ở trạng thái `PAID` | Could | US-09 | 4 | BR-18 |

### 3.6 Course Discovery (core)

| ID | Requirement | Acceptance criteria | Pri | US | Sprint | BR |
|----|-------------|---------------------|-----|----|--------|----|
| FR-CD-01 | Tìm course theo keyword và lọc theo danh mục, trình độ, khoảng giá; kết quả phân trang | Chỉ trả về course `PUBLISHED`; nhiều filter kết hợp được | Should | US-10 | 2 ✅ | BR-17 |
| FR-CD-02 | Guest / Learner xem trang chi tiết course: mô tả, ba khối bullet, curriculum, giá, rating, thông tin giảng viên | Truy cập được bằng id hoặc slug; course chưa `PUBLISHED` trả về 404 | Must | US-11 | 2 ✅ | BR-17 |
| FR-CD-03 | Guest / Learner chưa mua xem được lesson đánh dấu preview | Lesson không phải preview bị khóa trên curriculum công khai | Must | US-11 | 2 ✅ | BR-24 |

### 3.7 Payment (support)

| ID | Requirement | Acceptance criteria | Pri | US | Sprint | BR |
|----|-------------|---------------------|-----|----|--------|----|
| FR-PAY-01 | Learner tạo order cho một course và được chuyển sang MoMo / VNPay | Order sinh `order_code` duy nhất và giữ trạng thái `PENDING` cho tới khi có IPN | Must | US-13 | 3 | BR-19 |
| FR-PAY-02 | Hệ thống xác nhận thanh toán bằng IPN có verify chữ ký và số tiền, rồi tạo enrollment | Chữ ký sai hoặc số tiền lệch → từ chối, ghi log, order giữ `PENDING`, không tạo enrollment | Must | US-14 | 3 | BR-20 |
| FR-PAY-03 | Xử lý IPN trùng lặp | Nhận IPN nhiều lần cho cùng giao dịch chỉ tạo đúng 1 enrollment | Must | US-14 | 3 | BR-21 |
| FR-PAY-04 | Order không nhận được IPN sau thời gian chờ | Chuyển `EXPIRED`, learner được thông báo và có thể tạo order mới | Must | US-13 | 3 | BR-22 |
| FR-PAY-05 | Không cấp quyền truy cập dựa trên redirect phía client | Tự gõ lại URL success không tạo được enrollment | Must | US-14 | 3 | BR-20 |
| FR-PAY-06 | Không cho mua lại course đã sở hữu | Nút "Mua" đổi thành "Vào học" | Must | US-13 | 3 | BR-23 |

### 3.8 Content Delivery (core)

| ID | Requirement | Acceptance criteria | Pri | US | Sprint | BR |
|----|-------------|---------------------|-----|----|--------|----|
| FR-DL-01 | Chỉ learner đã ghi danh xem được nội dung đầy đủ | Request không có quyền trả về 403 | Must | US-15 | 3 | BR-24 |
| FR-DL-02 | Nội dung phát qua signed URL có thời hạn ngắn | URL hết hạn sau TTL cấu hình; chia sẻ lại sau khi hết hạn không dùng được | Must | US-15 | 3 | BR-11, BR-25 |
| FR-DL-03 | Learner tua tới vị trí bất kỳ trong video | Hỗ trợ HTTP Range; seek không cần tải lại toàn bộ file | Must | US-15 | 3 | |
| FR-DL-04 | Signed URL hết hạn giữa lúc đang xem thì được cấp lại mà không gián đoạn | Learner không bị dừng video hoặc mất vị trí đang xem | Must | US-15 | 3 | BR-26 |

### 3.9 Learning (core)

| ID | Requirement | Acceptance criteria | Pri | US | Sprint | BR |
|----|-------------|---------------------|-----|----|--------|----|
| FR-LRN-01 | Learner đã ghi danh mở trình học và duyệt cây section / lesson | Learner chưa ghi danh bị chặn khỏi màn hình học | Must | US-16 | 2 ✅ | BR-23, BR-24 |
| FR-LRN-02 | Learner xem danh sách course đã mua kèm điểm tiếp tục học | Chỉ liệt kê enrollment còn hiệu lực; mỗi dòng có link vào học tiếp | Should | US-19 | 2 ✅ | |

### 3.10 Knowledge Tracking (core)

| ID | Requirement | Acceptance criteria | Pri | US | Sprint | BR |
|----|-------------|---------------------|-----|----|--------|----|
| FR-KT-01 | Lưu vị trí xem hiện tại của mỗi lesson | Learner quay lại tiếp tục đúng vị trí (±5s) | Must | US-17 | 3 | |
| FR-KT-02 | Ghi nhận phần thời lượng **thực sự đã xem** của mỗi lesson | Tua nhanh tới cuối không làm tăng watched coverage | Must | US-17 | 3 | BR-27 |
| FR-KT-03 | Cập nhật tiến độ từ nhiều tab / thiết bị không làm mất dữ liệu | Coverage sau khi cập nhật không bao giờ nhỏ hơn trước đó | Must | US-17 | 3 | BR-28 |
| FR-KT-04 | Đánh dấu lesson hoàn thành và tính % hoàn thành của course | Lesson hoàn thành khi coverage ≥ 90%; % course tính theo số lesson đã hoàn thành; đủ 100% thì enrollment chuyển `COMPLETED` | Must | US-17 | 3 | BR-29, BR-30 |

### 3.11 Review (support)

| ID | Requirement | Acceptance criteria | Pri | US | Sprint | BR |
|----|-------------|---------------------|-----|----|--------|----|
| FR-RV-01 | Learner đã ghi danh đánh giá sao và viết nhận xét cho course | Mỗi learner chỉ một review trên một course, sửa được; `rating_avg` của course cập nhật lại | Could | US-18 | 4 | BR-31 |

### 3.12 Q&A (support)

| ID | Requirement | Acceptance criteria | Pri | US | Sprint | BR |
|----|-------------|---------------------|-----|----|--------|----|
| FR-QA-01 | Learner đặt câu hỏi trên một lesson và instructor trả lời | Thread hiển thị ngay trên lesson; chỉ learner đã ghi danh mới đặt được câu hỏi | Should | US-21 | 4 | BR-32 |

### 3.13 AI Assistant (support)

| ID | Requirement | Acceptance criteria | Pri | US | Sprint | BR |
|----|-------------|---------------------|-----|----|--------|----|
| FR-AI-01 | Gợi ý course tương tự dựa trên hồ sơ / lịch sử học | Hiển thị ở trang chi tiết course và trang chủ; lỗi AI không làm hỏng trang | Should | US-22 | 4 | BR-33 |

---

## 4. Non-Functional Requirements

| ID | Category | Requirement |
|----|----------|-------------|
| NFR-01 | Security | Mật khẩu lưu dạng hash (bcrypt), không lưu plaintext |
| NFR-02 | Security | URL upload và URL phát nội dung đều có chữ ký và hết hạn sau ≤ 15 phút |
| NFR-03 | Security | Phân quyền và mọi business rule kiểm tra ở server cho mọi endpoint; FE chỉ hiển thị |
| NFR-04 | Security | Access token hết hạn sau 15 phút, refresh token sau 7 ngày (cấu hình được) |
| NFR-05 | Reliability | Enrollment chỉ tạo từ xác nhận server-to-server của cổng thanh toán |
| NFR-06 | Reliability | Lỗi của AI hoặc email không gián đoạn luồng học và luồng thanh toán |
| NFR-07 | Reliability | Sửa đồng thời cùng một course từ 2 phiên được phát hiện bằng optimistic locking |
| NFR-08 | Usability | Responsive, hoạt động trên màn hình ≥ 360px |
| NFR-09 | Compatibility | Chrome, Edge, Firefox, Safari phiên bản mới nhất |
| NFR-10 | Maintainability | Code theo convention thống nhất, README hướng dẫn chạy local |

**External interfaces**

| # | Hệ thống ngoài | Mục đích | Yêu cầu khi lỗi |
|---|----------------|----------|-----------------|
| 1 | MoMo / VNPay | Thu tiền, xác nhận giao dịch qua IPN | Timeout → Order `EXPIRED` (FR-PAY-04); chữ ký sai → từ chối + ghi log (FR-PAY-02) |
| 2 | Object Storage / CDN | Lưu và phân phối video, file, ảnh | Lỗi tải → client retry, hiển thị lỗi phát video; upload thất bại → lesson giữ trạng thái `FAILED` |
| 3 | Google OAuth | Đăng nhập bằng tài khoản Google | Lỗi → quay về form đăng nhập bằng mật khẩu |
| 4 | LLM Provider (Spring AI) | Gợi ý khóa học tương tự | Lỗi → ẩn khối gợi ý, không chặn trang |
| 5 | Email service | Kích hoạt tài khoản, thông báo | Đưa vào queue và retry; gửi lỗi không chặn luồng đăng ký |

---

## 5. Business Rules

| ID | Rule Definition | FR liên quan |
|----|------------------|---------------|
| BR-01 | Một email chỉ đăng ký được một tài khoản | FR-AUTH-01 |
| BR-02 | Đăng ký chỉ tạo bản ghi chờ có TTL 15 phút; tài khoản thật chỉ được tạo khi người dùng bấm link kích hoạt còn hạn. Đăng ký lại cùng email khi chưa kích hoạt sẽ ghi đè bản ghi chờ và làm mới TTL | FR-AUTH-01, FR-AUTH-02 |
| BR-03 | Mỗi tài khoản chỉ mang **một** vai trò: `LEARNER`, `LECTURER` hoặc `ADMIN`. Tài khoản đăng ký qua form luôn là `LEARNER`; vai trò `LECTURER` được gán ở tầng dữ liệu, chưa có luồng nâng cấp trên UI | FR-AUTH-03 |
| BR-04 | Đăng nhập sai liên tiếp 5 lần trong vòng 15 phút → tài khoản bị khóa tạm thời 30 phút | FR-AUTH-04 |
| BR-05 | Tài khoản `is_active = false` không đăng nhập được kể cả khi mật khẩu đúng | FR-AUTH-03 |
| BR-06 | Đăng nhập Google khớp theo email: email đã có tài khoản `LOCAL` thì liên kết vào tài khoản đó, không tạo tài khoản trùng email | FR-AUTH-06 |
| BR-07 | Giá course do instructor đặt, trong khoảng 0 – 10.000.000 VND | FR-CA-01 |
| BR-08 | Chỉ chủ sở hữu (`lecturer_id`) mới đọc/sửa được course ở khu vực quản lý; mọi thao tác quản lý course đều kiểm tra quyền sở hữu ở server | FR-CA-01, FR-CA-03, FR-INS-01 |
| BR-09 | Mỗi nhóm bullet (mục tiêu học / yêu cầu / đối tượng) tối đa 20 dòng, mỗi dòng tối đa 500 ký tự | FR-CA-02 |
| BR-10 | Giới hạn upload theo mục đích: ảnh bìa jpeg/png/webp ≤ 5MB; video bài học và video giới thiệu mp4 ≤ 500MB; file bài học pdf/zip/doc(x)/ppt(x) ≤ 200MB; tài liệu bổ trợ mọi định dạng ≤ 100MB. Server kiểm tra lại content-type và dung lượng thật sau khi client upload xong | FR-CA-05, FR-CA-06, FR-PROF-01 |
| BR-11 | Video và file lưu bằng `storage_key`, không lưu và không phân phối dưới dạng URL tĩnh công khai | FR-CA-05, FR-DL-02 |
| BR-12 | Course chỉ publish được khi thỏa toàn bộ: có title; mô tả ≥ 200 ký tự; có danh mục; có ảnh bìa; giá hợp lệ (BR-07); ≥ 4 mục tiêu học, ≥ 1 yêu cầu, ≥ 1 đối tượng; ≥ 1 section; ≥ 1 lesson có nội dung; và **mọi** lesson đều đã có nội dung hoàn chỉnh. "Có nội dung" nghĩa là: `VIDEO` / `FILE` đã upload xong (`READY`), `ARTICLE` có nội dung text, `QUIZ` có đủ câu hỏi và đáp án. Các ngưỡng số lượng là cấu hình được | FR-CA-08, FR-QZ-01 |
| BR-13 | Vòng đời course: `DRAFT` → `PUBLISHED` ⇄ `UNPUBLISHED`. Chỉ publish được từ `DRAFT` hoặc `UNPUBLISHED`, chỉ unpublish được từ `PUBLISHED`. Mọi lần đổi trạng thái đều ghi vào lịch sử trạng thái; `published_at` chỉ set ở lần publish đầu tiên | FR-CA-08, FR-INS-02, FR-INS-03 |
| BR-14 | Chỉnh sửa course đã publish (đổi tên, đổi thứ tự section/lesson, cập nhật nội dung) không được xóa hoặc reset tiến trình học đã ghi nhận của learner | FR-CA-09 |
| BR-15 | Quiz gắn 1–1 với một lesson kiểu `QUIZ`; mỗi câu hỏi phải có ít nhất một đáp án được đánh dấu đúng | FR-QZ-01, FR-QZ-02 |
| BR-16 | Điểm quiz do server chấm dựa trên đáp án lưu trong DB; đáp án đúng không được trả về client trước khi learner nộp bài | FR-QZ-02 |
| BR-17 | Catalog, kết quả tìm kiếm và trang chi tiết công khai chỉ hiển thị course ở trạng thái `PUBLISHED`; course `DRAFT` hoặc `UNPUBLISHED` không xuất hiện, nhưng learner đã mua trước đó vẫn truy cập được nội dung | FR-CD-01, FR-CD-02, FR-INS-02 |
| BR-18 | Doanh thu chỉ cộng từ order ở trạng thái `PAID`; order `PENDING` / `EXPIRED` / `CANCELLED` không tính | FR-INS-04 |
| BR-19 | Mỗi order có một `order_code` duy nhất do hệ thống sinh, độc lập với transaction_id của cổng thanh toán, dùng để đối chiếu giao dịch | FR-PAY-01 |
| BR-20 | Enrollment chỉ được tạo khi IPN có chữ ký hợp lệ và số tiền khớp với order; redirect phía client không bao giờ là căn cứ cấp quyền | FR-PAY-02, FR-PAY-05 |
| BR-21 | Xử lý IPN idempotent theo cặp (`payment_method`, `transaction_id`): IPN trùng lặp cho cùng giao dịch chỉ ghi log, không tạo thêm enrollment và không đổi lại trạng thái order đã `PAID` | FR-PAY-03 |
| BR-22 | Order ở trạng thái `PENDING` quá thời gian chờ cấu hình (đề xuất 15 phút) mà không nhận được IPN hợp lệ sẽ tự động chuyển sang `EXPIRED` | FR-PAY-04 |
| BR-23 | Một learner chỉ có một enrollment trên một course — không mua lại course đã sở hữu. Course phải đang `PUBLISHED` mới ghi danh được, và instructor không ghi danh vào course của chính mình. Mỗi lần ghi danh thành công làm tăng `total_students` của course | FR-PAY-06, FR-LRN-01 |
| BR-24 | Quyền xem nội dung đầy đủ của course chỉ cấp cho learner có enrollment còn hiệu lực đối với đúng course đó. Ngoại lệ duy nhất: lesson đánh dấu preview, xem được bởi Guest và learner chưa mua | FR-CA-07, FR-CD-03, FR-DL-01, FR-LRN-01 |
| BR-25 | Nội dung chỉ truy cập qua signed URL có chữ ký và thời hạn ≤ 15 phút (NFR-02) | FR-DL-02 |
| BR-26 | Khi signed URL hết hạn trong lúc learner đang phát video, hệ thống cấp lại URL mới mà không làm gián đoạn phiên phát hoặc mất vị trí đang xem, miễn learner vẫn còn quyền truy cập | FR-DL-04 |
| BR-27 | Watched coverage chỉ tăng theo phần thời lượng được phát thực tế (ghi theo từng khoảng 5 giây); tua qua không tính | FR-KT-02 |
| BR-28 | Khi ghi nhận watched coverage từ nhiều phiên (tab/thiết bị) cho cùng learner và lesson, hệ thống hợp nhất theo từng đoạn; coverage sau cập nhật không bao giờ nhỏ hơn giá trị đã ghi nhận trước đó | FR-KT-03 |
| BR-29 | Một lesson `VIDEO` được coi là hoàn thành khi watched coverage ≥ 90%; lesson `ARTICLE` / `FILE` / `QUIZ` hoàn thành theo điều kiện riêng của loại nội dung | FR-KT-04 |
| BR-30 | % hoàn thành của course = số lesson đã hoàn thành / tổng số lesson. Đạt 100% thì enrollment chuyển sang `COMPLETED` và ghi `completed_at`; nếu sau đó instructor thêm lesson mới, enrollment quay lại `ACTIVE` | FR-KT-04 |
| BR-31 | Chỉ learner có enrollment mới được review, mỗi learner một review trên một course; `rating_avg` của course tính lại từ toàn bộ review hiện có | FR-RV-01 |
| BR-32 | Chỉ learner đã ghi danh mới đặt được câu hỏi trên lesson; instructor chủ khóa học trả lời được mọi thread của khóa học đó | FR-QA-01 |
| BR-33 | Gợi ý khóa học chỉ lấy từ course đang `PUBLISHED` và không gợi ý course learner đã sở hữu | FR-AI-01 |
