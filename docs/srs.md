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
| FR-AUTH-02 | Đăng nhập và duy trì phiên | Phiên hết hạn sau thời gian cấu hình | Must | 1 | BR-02 |
| FR-AUTH-03 | Người dùng chọn vai trò Learner hoặc Instructor | Một tài khoản chỉ có thể có 1 vai trò | Should | 1 | BR-03 |

### 3.2 Content Authoring (core)

| ID | Requirement | Acceptance criteria | Pri | Sprint | BR |
|----|-------------|---------------------|-----|--------|----|
| FR-CA-01 | Instructor tạo course với title, description, thumbnail, giá, category | Course lưu trạng thái `DRAFT`, chưa xuất hiện ở catalog | Must | 1 | BR-04 |
| FR-CA-02 | Instructor thêm và sắp xếp Section / Lecture | Thứ tự lưu lại và phản ánh đúng phía Learner | Must | 1 | |
| FR-CA-03 | Instructor upload video, quiz cho lecture | mp4, ≤ 500MB; hiển thị trạng thái xử lý | Must | 2 | BR-05 |
| FR-CA-04 | Instructor publish course | Chỉ publish khi có ≥1 section và ≥1 lecture có nội dung | Must | 2 | BR-06 |
| FR-CA-05 | Sửa course đã publish không ảnh hưởng learner đang học | Learner không mất tiến độ khi lecture được đổi tên / đổi thứ tự | Should | 3 | BR-07 |

### 3.3 Course Discovery & Purchase (core)

| ID | Requirement | Acceptance criteria | Pri | Sprint | BR |
|----|-------------|---------------------|-----|--------|----|
| FR-CD-01 | Tìm course theo keyword | Chỉ trả về course `PUBLISHED`, khớp title hoặc description | Must | 2 | BR-08 |
| FR-CD-02 | Lọc theo category, khoảng giá, rating | Nhiều filter kết hợp được | Should | 2 | BR-08 |
| FR-CD-03 | Learner mua course và được enroll sau khi thanh toán thành công | Enrollment chỉ tạo khi IPN hợp lệ | Must | 2 | BR-09 |
| FR-CD-04 | Không cho mua lại course đã sở hữu | Nút Mua đổi thành "Vào học" | Must | 2 | BR-10 |
| FR-CD-05 | Learner xem danh sách course đã mua | | Must | 2 | |
| FR-CD-06 | Learner thêm course vào wishlist | | Could | 4 | |
### 3.4 Content Delivery (core)

| ID | Requirement | Acceptance criteria | Pri | Sprint | BR |
|----|-------------|---------------------|-----|--------|----|
| FR-DL-01 | Chỉ learner đã enroll xem được nội dung đầy đủ | Request không có quyền trả về 403 | Must | 3 | BR-11 |
| FR-DL-02 | Video không truy cập được bằng URL chia sẻ lại | URL hết hạn sau TTL cấu hình (NFR-06) | Must | 3 | BR-12 |
| FR-DL-03 | Learner tua tới vị trí bất kỳ trong video | Seek không cần tải lại toàn bộ file | Must | 3 | |
| FR-DL-04 | Khi quyền truy cập video hết hạn giữa lúc đang xem, phiên xem được gia hạn mà không gián đoạn | Learner không bị dừng video hoặc mất vị trí đang xem | Must | 3 | BR-13 |
| FR-DL-05 | Lecture đánh dấu `preview` xem được không cần mua | | Should | 3 | BR-14 |

### 3.5 Knowledge Tracking (core)

| ID | Requirement | Acceptance criteria | Pri | Sprint | BR |
|----|-------------|---------------------|-----|--------|----|
| FR-KT-01 | Lưu vị trí xem hiện tại của mỗi lecture | Learner quay lại tiếp tục đúng vị trí (±5s) | Must | 3 | |
| FR-KT-02 | Ghi nhận phần thời lượng **thực sự đã xem** của mỗi lecture | Tua nhanh tới cuối không làm tăng watched coverage | Must | 3 | BR-15 |
| FR-KT-03 | Cập nhật tiến độ từ nhiều tab / thiết bị không làm mất dữ liệu | Coverage sau khi cập nhật không bao giờ nhỏ hơn trước đó | Must | 3 | BR-16 |
| FR-KT-04 | Hiển thị % hoàn thành theo lecture / section / course | | Must | 3 | |
| FR-KT-05 | Cấp chứng chỉ khi đạt ngưỡng hoàn thành | Coverage toàn course ≥ 90% | Should | 4 | BR-17 |

### 3.6 Payment (support)

| ID | Requirement | Acceptance criteria | Pri | Sprint | BR |
|----|-------------|---------------------|-----|--------|----|
| FR-PAY-01 | Tạo giao dịch qua Momo / VNPay | Order giữ trạng thái `PENDING` cho tới khi có IPN | Must | 2 | BR-18 |
| FR-PAY-02 | Xác nhận thanh toán bằng IPN có verify chữ ký và số tiền | Chữ ký sai hoặc số tiền lệch → từ chối, ghi log, giữ `PENDING` | Must | 2 | BR-09 |
| FR-PAY-03 | Xử lý IPN trùng lặp | Nhận IPN nhiều lần chỉ tạo đúng 1 enrollment | Must | 2 | BR-19 |
| FR-PAY-04 | Order không nhận được IPN sau thời gian chờ | Chuyển `EXPIRED`, learner được thông báo và có thể thử lại | Must | 2 | BR-20 |
| FR-PAY-05 | Không cấp quyền truy cập dựa trên redirect phía client | Quay lại URL success thủ công không tạo được enrollment | Must | 2 | NFR-08 |

### 3.7 AI Assistant (support)

| ID | Requirement | Acceptance criteria | Pri | Sprint | BR |
|----|-------------|---------------------|-----|--------|----|
| FR-AI-01 | Sinh tóm tắt nội dung lecture | Learner xem được tóm tắt trong trang học | Should | 4 | |
| FR-AI-02 | Sinh flashcard / câu hỏi ôn tập từ lecture | ≥ 5 thẻ mỗi lecture | Could | 4 | |
| FR-AI-03 | Gợi ý course tiếp theo dựa trên lịch sử học | | Could | 4 | |

### 3.8 Administration (support)

| ID | Requirement | Acceptance criteria | Pri | Sprint | BR |
|----|-------------|---------------------|-----|--------|----|
| FR-ADM-01 | Admin khóa / mở tài khoản | Tài khoản bị khóa không đăng nhập được | Should | 4 | BR-21 |
| FR-ADM-02 | Admin gỡ (unpublish) course khỏi catalog | Course bị gỡ không còn xuất hiện ở catalog/tìm kiếm; learner đã mua trước đó vẫn truy cập được nội dung | Should | 4 | BR-22 |

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
| 3 | LLM Provider (Spring AI) | Tóm tắt, flashcard, gợi ý  |
| 4 | Email service | Xác thực tài khoản, chứng chỉ | Đưa vào queue và retry |

---

## 5. Business Rules

| ID | Rule Definition | FR liên quan |
|----|------------------|---------------|
| BR-01 | Một email chỉ đăng ký được một tài khoản | FR-AUTH-01 |
| BR-02 | Đăng nhập sai liên tiếp 5 lần trong vòng 15 phút → tài khoản bị khóa tạm thời 30 phút | FR-AUTH-02 |
| BR-03 | Mỗi tài khoản chỉ được gán một vai trò duy nhất: Learner hoặc Instructor | FR-AUTH-03 |
| BR-04 | Giá course do instructor đặt, trong khoảng 0 – 10.000.000 VND | FR-CA-01 |
| BR-05 | Video upload tối đa 500MB, định dạng mp4 | FR-CA-03 |
| BR-06 | Course chỉ publish được khi có ít nhất 1 section và 1 lecture có nội dung | FR-CA-04 |
| BR-07 | Chỉnh sửa course đã publish (đổi tên, thứ tự section/lecture, cập nhật nội dung) không được xóa hoặc reset tiến trình học (current position, watched coverage) đã ghi nhận của learner | FR-CA-05 |
| BR-08 | Catalog, kết quả tìm kiếm và bộ lọc chỉ hiển thị course ở trạng thái `PUBLISHED`; course `DRAFT` hoặc đã bị gỡ không xuất hiện | FR-CD-01, FR-CD-02 |
| BR-09 | Enrollment chỉ được tạo khi IPN có chữ ký hợp lệ và số tiền khớp với order | FR-CD-03, FR-PAY-02 |
| BR-10 | Learner không mua lại course đã sở hữu | FR-CD-04 |
| BR-11 | Quyền xem nội dung đầy đủ của course chỉ cấp cho learner có enrollment còn hiệu lực (không ở trạng thái `REVOKED`) đối với đúng course đó | FR-DL-01 |
| BR-12 | Video chỉ được truy cập qua signed URL có chữ ký và thời hạn hiệu lực ≤ 15 phút (NFR-06); không lưu trữ hoặc phân phối dưới dạng URL tĩnh công khai | FR-DL-02 |
| BR-13 | Khi signed URL hết hạn trong lúc learner đang phát video, hệ thống phải cấp lại URL mới mà không làm gián đoạn phiên phát hoặc mất vị trí đang xem, miễn learner vẫn còn quyền truy cập | FR-DL-04 |
| BR-14 | Lecture đánh dấu preview truy cập được bởi Guest và Learner chưa mua course, không cần enrollment | FR-DL-05 |
| BR-15 | Watched coverage chỉ tăng theo phần thời lượng được phát thực tế; tua qua không tính | FR-KT-02 |
| BR-16 | Khi ghi nhận watched coverage từ nhiều phiên (tab/thiết bị) cho cùng learner và lecture, hệ thống hợp nhất (OR theo từng đoạn); coverage sau cập nhật không bao giờ nhỏ hơn giá trị đã ghi nhận trước đó | FR-KT-03 |
| BR-17 | Chứng chỉ được cấp khi watched coverage toàn course ≥ 90% | FR-KT-05 |
| BR-18 | Mỗi order có một order_code duy nhất do hệ thống sinh, độc lập với transaction_id của cổng thanh toán, dùng để đối chiếu giao dịch | FR-PAY-01 |
| BR-19 | Xử lý IPN idempotent theo cặp (payment_method, transaction_id): IPN trùng lặp cho cùng giao dịch chỉ được ghi log, không tạo thêm enrollment hoặc thay đổi lại trạng thái order đã `PAID` | FR-PAY-03 |
| BR-20 | Order ở trạng thái `PENDING` quá thời gian chờ cấu hình (đề xuất 15 phút) mà không nhận được IPN hợp lệ sẽ tự động chuyển sang `EXPIRED` | FR-PAY-04 |
| BR-21 | Tài khoản bị Admin khóa (blocked) không thể đăng nhập, kể cả khi email/mật khẩu đúng, cho đến khi được mở khóa lại | FR-ADM-01 |
| BR-22 | Course bị gỡ vẫn phải truy cập được với learner đã mua trước đó | FR-ADM-02 |
