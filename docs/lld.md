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
