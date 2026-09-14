# Low-Level Design — Learnova

| | |
|---|---|
| **Project** | Learnova — Online Course Marketplace |
| **Version / Sprint** | 0.2 — Sprint 4 |
| **Last updated** | 14-09-2026 |

**Record of Changes**

| Date | Ver | Sprint | A/M/D | In charge | Change |
|------|-----|--------|-------|-----------|--------|
| 20-08-2026 | 0.1 | 1 | A | | Khởi tạo LLD cho các luồng kỹ thuật chính |
| 13-09-2026 | 0.2 | 4 | M | | Thay luồng OAuth bằng luồng đang có; bổ sung payment, backward design, AI, publish changes và moderation |

---

## Mục đích & phạm vi

LLD mô tả các luồng triển khai có nhiều kiểm tra quyền, trạng thái hoặc tích hợp ngoài. Backend dùng controller → service → repository/entity; frontend gọi REST API qua các feature API. Các API quản lý course yêu cầu xác thực và kiểm tra quyền sở hữu ở backend.

Các luồng gồm: xác thực email, backward design/course authoring, publish changes, PayOS checkout, AI authoring, playback/progress, quiz attempt và moderation review/Q&A. Không có luồng OAuth trong code hiện tại.

---



## Flow 1 — Course authoring & backward design (US-23)

The authoring model separates course outcomes, lesson plans/content, and assessments. Stable outcome IDs are used by lesson and question mappings; an assessment belongs to a course/section and contains the quiz questions used by learner attempts.

```mermaid
sequenceDiagram
    actor I as Instructor
    participant FE as Authoring SPA
    participant API as Course API
    participant S as Course/Outcome/Assessment Services
    participant DB as PostgreSQL

    I->>FE: Create or edit course outcomes
    FE->>API: Outcome CRUD / reorder
    API->>S: Verify course ownership and outcome data
    S->>DB: Save outcomes with stable IDs
    I->>FE: Plan sections, lessons, assessments and mappings
    FE->>API: Save lesson/assessment/question changes
    API->>S: Validate ownership and same-course mappings
    S->>DB: Save draft authoring data
    I->>FE: Request publish check
    FE->>API: GET /publish-check
    API->>S: Validate course and backward-design alignment
    S-->>FE: canPublish + issue list
    I->>FE: Publish when no blocking issues remain
    FE->>API: POST /publish
    API->>S: Revalidate, apply pending content and publish atomically
    S->>DB: Update publication states, course status and status log
    API-->>FE: Published course
```

Publish validation checks course metadata, required outcomes and curriculum readiness. Each outcome must have lesson support and be measured by valid quiz questions; assessments must be placed in the course structure before publication. Incomplete draft courses may be saved without passing publish validation.

---

## Flow 2 — Changes to an already published course (US-27)

Changes to a published curriculum are staged as draft content. Replacing a published video stores pending file metadata separately so the active content remains available to learners until the instructor applies the changes.

```mermaid
sequenceDiagram
    actor I as Instructor
    participant FE as Instructor Studio
    participant API as CoursePublishController
    participant S as CoursePublishService
    participant DB as PostgreSQL
    participant L as Learner workspace

    I->>FE: Edit lesson, assessment or replace video
    FE->>API: Save curriculum change
    API->>S: Check owner and save draft/pending data
    S->>DB: Keep active publication and draft change separately
    L->>API: Request published course content
    API-->>L: Return active published version
    I->>FE: Open publish check
    FE->>API: GET /changes/publish-check
    API-->>FE: Blocking issues + pending change counts
    I->>FE: Apply publish changes
    FE->>API: POST /changes/publish
    API->>S: Revalidate and apply pending changes
    S->>DB: Promote drafts, update active enrollment progress
    API-->>FE: Updated curriculum and applied-change summary
```

The publish operation is owner-only and reuses course validation. Course status transitions are recorded in `course_status_logs`; curriculum changes are applied only after explicit instructor action.

---

## Flow 3 — PayOS checkout & enrollment

```mermaid
sequenceDiagram
    actor L as Learner
    participant FE as React SPA
    participant API as Payment API
    participant S as PaymentService
    participant DB as PostgreSQL
    participant P as PayOS

    L->>FE: Select a published course and checkout
    FE->>API: POST /api/v1/payments/checkout {courseId}
    API->>S: Check course, owner and existing enrollment
    alt Free course
        S->>DB: Create enrollment once
        API-->>FE: Enrolled
    else Paid course
        S->>DB: Reuse valid pending order or create order
        S->>P: Create payment link
        P-->>S: Checkout URL
        S->>DB: Save pending payment order
        API-->>FE: Checkout URL and order code
        FE->>P: Redirect learner to PayOS
        P->>API: POST /api/v1/payments/webhook
        API->>S: Verify webhook signature and order
        S->>DB: Mark paid and create enrollment once
        FE->>API: GET /api/v1/payments/{orderCode}/status
        API-->>FE: Verified status and enrollment state
    end
```

Frontend return/cancel URLs only navigate back to Learnova. Backend trusts a verified PayOS webhook or a backend status query; duplicate webhook/status processing must not create duplicate enrollments. A recent pending order can be reused; stale pending orders are cancelled before a replacement checkout is created.

---


## Error handling & security notes

- Backend validates authentication, role, course ownership, enrollment, payment signatures and storage access; client-side route guards are not security boundaries.
- External service credentials are configuration values and must be supplied outside source control.
- Payment status changes and enrollment creation are idempotent at the course/learner level.
- Validation errors are returned through the shared API response/error handling; storage, database and provider errors do not produce a successful domain operation.
