# Software Requirements Specification — Learnova



| | |
|---|---|
| **Project** | Learnova — Online Course Marketplace |
| **Version / Sprint** | 0.1 — Sprint 1 |
| **Last updated** | YYYY-MM-DD |
| **Owner** | … |

**Record of Changes**

| Date | Ver | Sprint | A/M/D | In charge | Change |
|------|-----|--------|-------|-----------|--------|
| | 0.1 | 1 | A | | Khởi tạo SRS, đặc tả Content Authoring |

---

## 1. Scope & Glossary

### 1.1 Scope

**In scope:**
- Auth & User: đăng ký/đăng nhập, phân quyền Giảng viên/Học viên, đăng nhập qua
Google/Facebook.
- Course & Content Authoring: tạo/sửa khóa học và đặt giá bán, thêm bài học đa định
dạng (video/text/PDF), tạo quiz.
- Course Discovery & Purchase: tìm kiếm và lọc khóa học, xem trang chi tiết, mua khóa
học và được cấp quyền truy cập.
- Payment: thanh toán trực tuyến qua cổng nội địa (Momo hoặc VNPay), xác nhận giao
dịch qua IPN.
- Content Delivery: phát video bài học qua signed URL có thời hạn, chặn truy cập nội
dung khi chưa mua khóa học.
- Knowledge Tracking: ghi nhận tiến trình xem video theo giây, resume đúng vị trí, tính
% hoàn thành khóa học.
- Quiz: học viên làm quiz và được chấm điểm tự động phía server.
- Certification: cấp chứng chỉ tự động khi hoàn thành khóa học.
- Q&A: hỏi đáp giữa giảng viên và học viên.
- Learning History: lịch sử học tập của học viên.
- AI bonus: tóm tắt bài học tự động, gợi ý khóa học tương tự (content-based).

**Out of scope:** 
- Không bao gồm nghiệp vụ quản lý lớp học có lịch cố định: điểm danh, xếp lớp, quản lý
sĩ số, thời khóa biểu — sản phẩm phục vụ mô hình tự học theo tiến độ cá nhân.
- Không bao gồm ứng dụng mobile (chỉ web).
- Không bao gồm tính năng AI cảnh báo học viên sắp bỏ học.
- Không tự động chia doanh thu hoặc chi trả/rút tiền cho giảng viên qua cổng thanh toán
hệ thống chỉ ghi nhận doanh thu, việc chi trả xử lý thủ công ngoài hệ thống.
- Không bao gồm chức năng hoàn tiền (refund) và xử lý khiếu nại giao dịch.
- Không bao gồm quy trình kiểm duyệt chất lượng khóa học trước khi đăng bán.

**Assumptions:**
- Mentor cung cấp phản hồi/đánh giá sau mỗi sprint theo lịch đã thống nhất.
- Dữ liệu khóa học viên dùng để test do nhóm tự tạo (seed data), không cần dữ liệu thật
quy mô lớn.
- Sử dụng gói miễn phí/dùng thử của LLM API cho phần AI
**Tài liệu liên quan:** [Proposal](../proposal/) · [Database Design](../design/database.md) · [HLD](../design/hld.md) · Product Backlog (GitHub Projects)

### 1.2 Glossary

| Term | Definition |
|------|-----------|
| Course | Đơn vị bán hàng, gồm nhiều Section |
| Section | Chương của Course, gồm nhiều Lecture |
| Lecture | Đơn vị nội dung nhỏ nhất (video / quiz / text) |
| Order | Bản ghi giao dịch mua, có trạng thái `PENDING` / `PAID` / `EXPIRED` / `CANCELLED` |
| Enrollment | Quyền truy cập course của Learner, tạo sau khi Order `PAID` |
| Current position | Vị trí phát hiện tại của video, dùng để tiếp tục xem |
| Watched coverage | Tỉ lệ thời lượng video **thực sự được phát**; khác current position |
| Signed URL | URL truy cập video có chữ ký và thời hạn, thay cho URL public |
| IPN | Thông báo thanh toán server-to-server do payment gateway gửi về |

---

## 2. Actors & Permissions

| # | Actor | Description |
|---|-------|-------------|
| 1 | Guest | Chưa đăng nhập; duyệt, tìm kiếm, xem lecture preview |
| 2 | Learner | Mua course, học, theo dõi tiến độ, nhận chứng chỉ |
| 3 | Instructor | Tạo và quản lý course của mình |
| 4 | Admin | Quản lý user, gỡ course vi phạm |
| 5 | Payment Gateway | Hệ thống ngoài; gửi IPN xác nhận giao dịch |
| 6 | LLM Provider | Hệ thống ngoài; sinh nội dung hỗ trợ học tập |

| Chức năng | Guest | Learner | Instructor | Admin |
|-----------|:-----:|:-------:|:----------:|:-----:|
| Duyệt / tìm kiếm catalog | X | X | X | X |
| Xem lecture preview | X | X | X | X |
| Mua course | | X | | |
| Xem nội dung đầy đủ | | X (đã enroll) | X (course của mình) | X |
| Tạo / sửa / publish course | | | X | |
| Quản lý user, gỡ course | | | | X |


---

## 3. Functional Requirements

> Quy ước ID `FR-<MODULE>-<số>`. Module: `AUTH`, `CA` (Content Authoring), `CD` (Course Discovery & Purchase), `DL` (Content Delivery), `KT` (Knowledge Tracking), `PAY`, `AI`, `ADM`.

### 3.1 Authentication (support)

| ID | Requirement | Acceptance criteria | Pri | Sprint | BR |
|----|-------------|---------------------|-----|--------|----|
| FR-AUTH-01 | Đăng ký tài khoản bằng email + mật khẩu | Email trùng bị từ chối | Must | 1 | BR-01 |
| FR-AUTH-02 | Đăng nhập và duy trì phiên | Phiên hết hạn sau thời gian cấu hình | Must | 1 | |
| FR-AUTH-03 | Người dùng chọn vai trò Learner hoặc Instructor | Một tài khoản có thể có cả hai vai trò | Should | 1 | |

### 3.2 Content Authoring (core)

| ID | Requirement | Acceptance criteria | Pri | Sprint | BR |
|----|-------------|---------------------|-----|--------|----|
| FR-CA-01 | Instructor tạo course với title, description, thumbnail, giá, category | Course lưu trạng thái `DRAFT`, chưa xuất hiện ở catalog | Must | 1 | BR-22 |
| FR-CA-02 | Instructor thêm và sắp xếp Section / Lecture | Thứ tự lưu lại và phản ánh đúng phía Learner | Must | 1 | |
| FR-CA-03 | Instructor upload video cho lecture | mp4, ≤ 500MB; hiển thị trạng thái xử lý | Must | 2 | BR-05 |
| FR-CA-04 | Instructor publish course | Chỉ publish khi có ≥1 section và ≥1 lecture có nội dung | Must | 2 | BR-03 |
| FR-CA-05 | Sửa course đã publish không ảnh hưởng learner đang học | Learner không mất tiến độ khi lecture được đổi tên / đổi thứ tự | Should | 3 | |

### 3.3 Course Discovery & Purchase (core)

| ID | Requirement | Acceptance criteria | Pri | Sprint | BR |
|----|-------------|---------------------|-----|--------|----|
| FR-CD-01 | Tìm course theo keyword | Chỉ trả về course `PUBLISHED`, khớp title hoặc description | Must | 2 | |
| FR-CD-02 | Lọc theo category, khoảng giá, rating | Nhiều filter kết hợp được | Should | 2 | |
| FR-CD-03 | Learner mua course và được enroll sau khi thanh toán thành công | Enrollment chỉ tạo khi IPN hợp lệ | Must | 2 | BR-10 |
| FR-CD-04 | Không cho mua lại course đã sở hữu | Nút Mua đổi thành "Vào học" | Must | 2 | BR-11 |
| FR-CD-05 | Learner xem danh sách course đã mua | | Must | 2 | |

### 3.4 Content Delivery (core)

| ID | Requirement | Acceptance criteria | Pri | Sprint | BR |
|----|-------------|---------------------|-----|--------|----|
| FR-DL-01 | Chỉ learner đã enroll xem được nội dung đầy đủ | Request không có quyền trả về 403 | Must | 3 | |
| FR-DL-02 | Video không truy cập được bằng URL chia sẻ lại | URL hết hạn sau TTL cấu hình (NFR-06) | Must | 3 | |
| FR-DL-03 | Learner tua tới vị trí bất kỳ trong video | Seek không cần tải lại toàn bộ file | Must | 3 | |
| FR-DL-04 | Khi quyền truy cập video hết hạn giữa lúc đang xem, phiên xem được gia hạn mà không gián đoạn | Learner không bị dừng video hoặc mất vị trí đang xem | Must | 3 | |
| FR-DL-05 | Lecture đánh dấu `preview` xem được không cần mua | | Should | 3 | |

### 3.5 Knowledge Tracking (core)

| ID | Requirement | Acceptance criteria | Pri | Sprint | BR |
|----|-------------|---------------------|-----|--------|----|
| FR-KT-01 | Lưu vị trí xem hiện tại của mỗi lecture | Learner quay lại tiếp tục đúng vị trí (±5s) | Must | 3 | |
| FR-KT-02 | Ghi nhận phần thời lượng **thực sự đã xem** của mỗi lecture | Tua nhanh tới cuối không làm tăng watched coverage | Must | 3 | BR-20 |
| FR-KT-03 | Cập nhật tiến độ từ nhiều tab / thiết bị không làm mất dữ liệu | Coverage sau khi cập nhật không bao giờ nhỏ hơn trước đó | Must | 3 | |
| FR-KT-04 | Hiển thị % hoàn thành theo lecture / section / course | | Must | 3 | |
| FR-KT-05 | Cấp chứng chỉ khi đạt ngưỡng hoàn thành | Coverage toàn course ≥ 90% | Should | 4 | BR-21 |

### 3.6 Payment (support)

| ID | Requirement | Acceptance criteria | Pri | Sprint | BR |
|----|-------------|---------------------|-----|--------|----|
| FR-PAY-01 | Tạo giao dịch qua Momo / VNPay | Order giữ trạng thái `PENDING` cho tới khi có IPN | Must | 2 | |
| FR-PAY-02 | Xác nhận thanh toán bằng IPN có verify chữ ký và số tiền | Chữ ký sai hoặc số tiền lệch → từ chối, ghi log, giữ `PENDING` | Must | 2 | BR-10 |
| FR-PAY-03 | Xử lý IPN trùng lặp | Nhận IPN nhiều lần chỉ tạo đúng 1 enrollment | Must | 2 | |
| FR-PAY-04 | Order không nhận được IPN sau thời gian chờ | Chuyển `EXPIRED`, learner được thông báo và có thể thử lại | Must | 2 | |
| FR-PAY-05 | Không cấp quyền truy cập dựa trên redirect phía client | Quay lại URL success thủ công không tạo được enrollment | Must | 2 | NFR-08 |

### 3.7 AI Assistant (support)

| ID | Requirement | Acceptance criteria | Pri | Sprint | BR |
|----|-------------|---------------------|-----|--------|----|
| FR-AI-01 | Sinh tóm tắt nội dung lecture | Learner xem được tóm tắt trong trang học | Should | 4 | |
| FR-AI-02 | Sinh flashcard / câu hỏi ôn tập từ lecture | ≥ 5 thẻ mỗi lecture | Could | 4 | |
| FR-AI-03 | Gợi ý course tiếp theo dựa trên lịch sử học | | Could | 4 | |
| FR-AI-04 | LLM lỗi hoặc timeout không chặn luồng học | Hiển thị thông báo, các chức năng còn lại hoạt động bình thường | Must | 4 | |

### 3.8 Administration (support)

| ID | Requirement | Acceptance criteria | Pri | Sprint | BR |
|----|-------------|---------------------|-----|--------|----|
| FR-ADM-01 | Admin khóa / mở tài khoản | Tài khoản bị khóa không đăng nhập được | Should | 4 | |
| FR-ADM-02 | Admin gỡ course vi phạm | Course chuyển `UNPUBLISHED`; learner đã mua vẫn xem được | Should | 4 | |

---

## 4. Non-Functional Requirements


| ID | Category | Requirement |
|----|----------|-------------|
| NFR-01 | Performance | API đọc (catalog, course detail) < 500ms ở p95 với 50 concurrent users |
| NFR-02 | Performance | Video bắt đầu phát < 3s kể từ khi bấm play trên mạng ≥ 10Mbps |
| NFR-03 | Performance | Ghi nhận tiến độ tối đa 1 request / 10s / learner |
| NFR-04 | Scalability | ≥ 100 learner xem video đồng thời trong môi trường demo |
| NFR-05 | Security | Mật khẩu lưu dạng hash (bcrypt), không lưu plaintext |
| NFR-06 | Security | URL video có chữ ký, hết hạn sau ≤ 15 phút |
| NFR-07 | Security | Phân quyền kiểm tra ở server cho mọi endpoint |
| NFR-08 | Reliability | Enrollment chỉ tạo từ xác nhận server-to-server |
| NFR-09 | Reliability | Lỗi của AI hoặc email không gián đoạn luồng học và luồng thanh toán |
| NFR-10 | Usability | Responsive, hoạt động trên màn hình ≥ 360px |
| NFR-11 | Compatibility | Chrome, Edge, Firefox, Safari phiên bản mới nhất |
| NFR-12 | Maintainability | Code theo convention thống nhất, README hướng dẫn chạy local |

**External interfaces**

| # | Hệ thống ngoài | Mục đích | Yêu cầu khi lỗi |
|---|----------------|----------|-----------------|
| 1 | Momo / VNPay | Thu tiền, xác nhận giao dịch qua IPN | Timeout → Order `EXPIRED` (FR-PAY-04); chữ ký sai → từ chối + log (FR-PAY-02) |
| 2 | Object Storage / CDN | Lưu và phân phối video | Lỗi tải → client retry, hiển thị lỗi phát video |
| 3 | LLM Provider (Spring AI) | Tóm tắt, flashcard, gợi ý | Timeout / hết quota → tắt tính năng AI, không chặn luồng học (FR-AI-04) |
| 4 | Email service | Xác thực tài khoản, chứng chỉ | Đưa vào queue và retry |

---

## 5. Business Rules

| ID | Rule |
|----|------|
| BR-01 | Một email chỉ đăng ký được một tài khoản |
| BR-03 | Course chỉ publish được khi có ít nhất 1 section và 1 lecture có nội dung |
| BR-05 | Video upload tối đa 500MB, định dạng mp4 |
| BR-10 | Enrollment chỉ được tạo khi IPN có chữ ký hợp lệ và số tiền khớp với order |
| BR-11 | Learner không mua lại course đã sở hữu |
| BR-20 | Watched coverage chỉ tăng theo phần thời lượng được phát thực tế; tua qua không tính |
| BR-21 | Chứng chỉ được cấp khi watched coverage toàn course ≥ 90% |
| BR-22 | Giá course do instructor đặt, trong khoảng 0 – 10.000.000 VND |
| BR-23 | Course bị gỡ vẫn phải truy cập được với learner đã mua trước đó |